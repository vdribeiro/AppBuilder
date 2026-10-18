# Koyeb Deployment

Runs the backend on Koyeb's free instance, with the database on Neon's free tier.
Both are free indefinitely rather than for a trial period, and neither sleeps when idle —
which is the property that matters here, since the push suite holds WebSocket and SSE
connections open and a sleeping instance drops them.

```
  internet ──► koyeb (TLS, *.koyeb.app) ──► server ──► neon postgres
                                            512 MB      (public internet, TLS required)
```

Unlike [the Oracle setup](../oracle/README.md) there is no Caddy and no Postgres container.
Koyeb terminates TLS and hands you a domain; Neon runs the database. What remains is one container.

## What the free tier gives you, and what it costs you

One web service, 512 MB of memory, 0.1 vCPU, in Frankfurt or Washington D.C.
It never spins down, and it does not expire.

**512 MB is the binding constraint.** A JVM left to its own devices will size its heap
against what it thinks is available and be killed long before the garbage collector
intervenes, so [`Dockerfile`](Dockerfile) caps every memory region explicitly and leaves
about 50 MB of headroom. Those numbers are not decoration; raising the heap without
lowering something else will produce a container that dies under load rather than one
that runs faster.

**0.1 vCPU makes startup slow.** Expect 60–120 seconds from deploy to first response.
The health check has to be configured with that in mind or Koyeb will kill the instance
mid-boot and retry forever.

**Signup now requires a card.** Since February 2026 Koyeb places a €29 pre-authorisation
hold, which is released rather than charged, and the signup flow defaults to the paid plan.
Pick the free instance deliberately.

Realistically this fits a trial with 50–100 users. It is not where a successful app stays:
at that point a small server with 4 GB costs about €6/month and removes every constraint above.

## One-time: the database

1. Sign up at [neon.com](https://neon.com) and create a project in **Frankfurt**, so it sits
   next to the Koyeb instance rather than across an ocean.
2. Copy the connection string from the dashboard. It looks like:
   `postgresql://user:password@ep-something-123456.eu-central-1.aws.neon.tech/dbname?sslmode=require`
3. Take the five parts out of it for `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_NAME`, and port `5432`.

Neon's free tier suspends compute after five minutes of inactivity and resumes on the next
connection, which costs a second or so on the first query after a quiet period. The server
reconnects on its own, and `PostgresSignal` re-establishes its `LISTEN` subscription, so this
is a latency blip rather than an outage.

## One-time: publish the image

Koyeb pulls a prebuilt image rather than building the repository, because building it would
require the Android SDK to configure `:clientApp` before `:server` can be assembled.
[`publish-server-image.yml`](../../.github/workflows/publish-server-image.yml) does this on
GitHub's runners, which carry that SDK already.

1. Push the branch, then run **Actions → Publish server image → Run workflow**.
2. When it finishes, open **your GitHub profile → Packages → appbuilder-server → Package settings**
   and change the visibility to **public**. Koyeb then pulls it without credentials.
   Leaving it private also works, but means giving Koyeb a registry token.

The image lands at `ghcr.io/vdribeiro/appbuilder-server:latest`.

## Create the service

**Create Web Service → Docker image**, with:

| Setting | Value |
|---|---|
| Image | `ghcr.io/vdribeiro/appbuilder-server:latest` |
| Region | Frankfurt |
| Instance | Free |
| Scaling | 1 instance, fixed |
| Port | 8000 |
| Health check | TCP on 8000 (Koyeb's default — nothing to change) |
| Health check grace period | **180 seconds** (the default of 5 is far too short) |

Health checks are configured per port, inside the port's entry in the "Exposing your service"
section, which is why they are easy to miss.

**Leave the protocol as TCP.** It is Koyeb's default, and the only setting that works here
without further changes. An HTTP check issues a `GET`, which `/api/probe` rejects with a 405
since it only answers `HEAD` — and even with a `GET` handler added it would still fail, because
Koyeb probes every second during startup while `rateLimiterProbeLimit` allows 5 requests per
30 seconds, shared across every caller once the proxy collapses them to one address. Either way
Koyeb reads the response as a failure and restarts a server that is working perfectly.

**Raise the grace period**, which is the one value that does need changing. It is how long Koyeb
waits before probing at all, and its default of 5 seconds expires long before a JVM on 0.1 vCPU
has bound the port.

Then add the environment variables from [`env.example`](env.example), marking
`DB_PASSWORD`, `ADMIN_PASSWORD` and `JWT_SECRET` as secrets.

Deploy, and watch the logs. A healthy first boot ends with the seeded admin account
and the server accepting connections.

## Pointing the apps at it

Koyeb gives the service a domain of the form `<service>-<org>.koyeb.app`. Two edits:

- `shared/src/commonMain/kotlin/com/app/builder/data/http/URL.kt` — set `HOST` to that domain.
  Both the clients and the server's own CORS rule read it from there.
- `server/.../plugin/Cors.kt` — in production only `HOST` is allowed as an origin. That suffices
  while Firebase Hosting rewrites `/api/**` to Cloud Run and the browser sees a single origin.
  A Firebase rewrite cannot proxy to Koyeb, so once the web client calls Koyeb directly it becomes
  a genuine cross-origin request and the Firebase Hosting domain must be added with `allowHost`.

Changing `HOST` repoints every client at once, so it is left alone here.

## Worth knowing

- **Memory is the thing that will bite.** If the container restarts under load, it is almost
  certainly the heap. Lower `-Xmx` before raising anything else, and turn the `firebase` server
  flag off if FCM is not in use — the Firebase SDK is the single largest thing the server loads.
- **Rate limiting sees one client.** `RateLimit.kt:31` keys on `request.origin.remoteAddress`,
  which behind Koyeb's proxy is the same value for every request, collapsing the per-IP buckets
  into one. Ktor's `ForwardedHeaders` plugin fixes it. The same already applies on Cloud Run.
- **One instance, no redundancy.** A deploy is a brief outage, and there is no second instance
  to fail over to. `PostgresSignal` is still correct, it simply has nothing to talk to.
- **Neon's free tier is 0.5 GB of storage.** Ample for a trial, worth watching if it grows.
