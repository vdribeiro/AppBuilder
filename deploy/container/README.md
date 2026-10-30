# Managed Container Deployment

Runs the backend as a single container on a managed host, with the database on Neon's free tier.
Neither sleeps when idle — the property that matters here, since the push suite holds WebSocket and SSE connections open and a sleeping instance drops them.

```
  internet ──► host (TLS, provider domain) ──► server ──► neon postgres
                                               ~512 MB     (public internet, TLS required)
```

Unlike a self-managed VPS, there is no reverse proxy and no Postgres container.
The host terminates TLS and hands you a domain; Neon runs the database. What remains is one container.

The walkthrough below is written for **Northflank**, but nothing in this directory is specific to it.
[`Dockerfile`](Dockerfile) and [`env.example`](env.example) apply to any host that can pull an image
from a registry and set environment variables.

## Choosing a host

The app constrains the choice more than it looks:

- **It must not sleep.** Background sync and live connections are the point of this backend.
  A host that spins the container down after fifteen minutes idle, as Render's free tier does,
  degrades exactly the features the app was built around.
- **It must terminate TLS.** `Http.kt` builds production URLs as `https://$HOST` and `wss://$HOST`
  with no way to downgrade, so a release client cannot talk to a plain-HTTP endpoint at all.
- **It must pull a prebuilt image.** Building this repository needs the Android SDK to configure
  `:clientApp` before `:server` can be assembled, which no managed builder provides.

Northflank's **Sandbox** plan fits: two always-on services, one database, no spin-down, and
external registry images. A card is taken at signup as anti-abuse verification and is not charged
unless you upgrade.

**Koyeb is no longer an option.** Mistral AI acquired it in February 2026 and the free tier is
closed to new signups; new accounts must subscribe to the paid plan. Existing accounts continue.

Realistically a free tier fits a trial with 50–100 users. It is not where a successful app stays:
at that point a small VM with 4 GB costs about €6/month and removes every constraint below.

## One-time: the database

1. Sign up at [neon.com](https://neon.com) and create a project in **Frankfurt** (`eu-central-1`),
   so it sits next to the container rather than across an ocean.
2. Copy the connection string from the dashboard. It looks like:
   `postgresql://user:password@ep-something-pooler.eu-central-1.aws.neon.tech/dbname?sslmode=require`
3. Take the parts out of it for `DB_USER`, `DB_PASSWORD`, `DB_HOST` and `DB_NAME`, with port `5432`.

Prefer the **pooler** host, the one with `-pooler` in the name. It keeps connections on Neon's side
so a small instance does not exhaust them.

Drop `channel_binding=require` if the string carries it. It is a client-side option the r2dbc driver
does not implement, and Neon does not require it server-side. The connection is encrypted by
`DB_SSL_MODE`, which defaults to `REQUIRE` when unset.

Neon's free tier suspends compute after five minutes of inactivity and resumes on the next
connection, which costs a second or so on the first query after a quiet period. The server
reconnects on its own, and `PostgresSignal` re-establishes its `LISTEN` subscription, so this
is a latency blip rather than an outage.

## One-time: publish the image

[`publish-server-image.yml`](../../.github/workflows/publish-server-image.yml) builds the fat jar on
GitHub's runners, which carry the Android SDK already, and pushes the image to GHCR.

1. Push the branch, then run **Actions → Publish server image → Run workflow**.
2. When it finishes, open **your GitHub profile → Packages → appbuilder-server → Package settings**
   and change the visibility to **public**, so the host pulls it without credentials.
   Leaving it private also works, but means storing a registry token on the host.

The image lands at `ghcr.io/vdribeiro/appbuilder-server:latest`.

The workflow does not redeploy anything. Hosts do not watch registries for new tags, so shipping a
change is two steps: push, then trigger a redeploy on the host.

## Create the service

On Northflank: **Create new → Service → Deployment**, source **External image**.

| Setting | Value |
|---|---|
| Image path | `ghcr.io/vdribeiro/appbuilder-server:latest` |
| Credentials | none — the package is public |
| Region | `europe-west-frankfurt` |
| Plan | the free Sandbox compute class |
| Instances | 1 |
| Port | 8000, HTTP, **publicly exposed** |

Paste the image path and let it verify before continuing; a typo surfaces here rather than as a
failed deploy later. Under **Port & DNS**, tick **"Publicly expose this port to the internet"** —
without it the service runs but nothing can reach it. The host then generates the HTTPS domain.

Then add the environment variables from [`env.example`](env.example), marking `DB_PASSWORD`,
`ADMIN_PASSWORD` and `JWT_SECRET` as secrets, and add `PORT=8000` alongside them.
`Env.port` reads `PORT`, and setting it explicitly avoids depending on the host to inject it.

Deploy, and watch the logs. A healthy first boot ends with the seeded admin account and the server
accepting connections. Expect 60–120 seconds on a shared-CPU plan.

If the host offers a health check grace period, set it to **180 seconds**. The default is usually a
few seconds, which expires long before a JVM on a fraction of a vCPU has bound the port, and the
host then kills a server that is working perfectly and retries forever.

Prefer a **TCP** check over HTTP. `/api/probe` only answers `HEAD` and rejects a `GET` with 405, and
even with a `GET` handler it would still fail, because probes arrive every second during startup
while `rateLimiterProbeLimit` allows 5 requests per 30 seconds — shared across every caller once the
proxy collapses them to one address.

Keep the database out of the checks. `GET /` answers without touching Neon, so a database outage
does not restart a server that a restart cannot fix, and probes do not keep Neon's compute awake.
Once there is more than one instance, a readiness check that does query the database becomes
worthwhile: it pulls a single instance that has lost its connection out of the load balancer while
the others keep serving. Add it as a separate `GET /ready` endpoint and point only the readiness
probe at it, leaving liveness on `/`.

## Pointing the apps at it

The host gives the service a domain. Two edits:

- `shared/src/commonMain/kotlin/com/app/builder/data/http/URL.kt` — set `HOST` to that domain.
  Both the clients and the server's own CORS rule read it from there.
- `server/.../plugin/Cors.kt` — in production only `HOST` is allowed as an origin. That suffices
  while Firebase Hosting rewrites `/api/**` to Cloud Run and the browser sees a single origin.
  A Firebase rewrite cannot proxy to an arbitrary external host, so once the web client calls this
  one directly it becomes a genuine cross-origin request and the Firebase Hosting domain must be
  added with `allowHost`.

Changing `HOST` repoints every client at once, so it is left alone here.

## Worth knowing

- **Memory is the thing that will bite.** [`Dockerfile`](Dockerfile) caps every region explicitly
  for a 512 MB box and leaves about 50 MB of headroom, because a JVM left to its own devices sizes
  its heap against what it thinks is available and is killed before the collector intervenes.
  Those numbers are not decoration. If the host's free class turns out to be 256 MB, every value
  has to come down together rather than `-Xmx` alone, and turning the `firebase` server flag off
  frees the most — the Firebase SDK is the largest thing the server loads.
- **Rate limiting needs `TRUSTED_PROXY_HOPS`.** `request.origin.remoteAddress` is the host's proxy
  for every request, which would collapse the per-IP buckets into one. `RateLimit.kt` reads the
  caller out of `X-Forwarded-For` instead, counting back the configured number of hops. One hop
  covers a single router in front; verify against the host's own documentation before trusting it,
  since an overcount lets a caller forge the address it is limited by. The server logs a warning on
  startup when it is unset in production.
- **One instance, no redundancy.** A deploy is a brief outage, and there is no second instance to
  fail over to. `PostgresSignal` is still correct, it simply has nothing to talk to.
- **Neon's free tier is 0.5 GB of storage**, plus 5 GB of object storage and 5 GB of egress a month.
  Ample for a trial. Egress is the one that bites first if images start being served from there.
