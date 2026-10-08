# Oracle Cloud Deployment

Runs the backend on a single Oracle Cloud **Always Free** ARM VM: Postgres, the Ktor server and Caddy, as three containers on one box, with no time limit, no idle shutdown and no request duration cap.
That last part is the reason to prefer it here. The push suite holds WebSocket and SSE connections open indefinitely, which most free tiers either sleep out from under you or cut at a fixed timeout.

```
            :443                      :8080                     :5432
  internet ──────► caddy ───────────► server ────────────────► postgres
                   (TLS, WS, SSE)     (fat jar on temurin)     (LISTEN/NOTIFY → PostgresSignal)
```

Because Postgres is a real instance on the same box, `PostgresSignal` works as designed. It is doing nothing useful on one instance, but nothing has to change the day a second one appears.

## About the domain

A domain name is what lets the deployment get a TLS certificate, and a certificate is what makes `https://` and `wss://` work. 
Let's Encrypt will issue one for a name, never for a bare IP address, and Caddy fetches and renews it automatically once it knows the name.

This is not optional for this project. `Http.kt:208` builds the production base URL as `https://$HOST` and the socket URL as `wss://$HOST`, with no way to downgrade either. 
A release build of any client will only ever speak TLS. Without a certificate, the VM answers on `http://<ip>` and the apps cannot reach it at all.

Point your domain at the VM's public IP, and put that name in `SITE_ADDRESS`. Caddy handles the certificate from there with no further configuration. Any registrar works the same way.

Leaving `SITE_ADDRESS` unset is supported, but only really useful for confirming the box is alive with `curl -I http://<ip>/api/probe` before the DNS record exists.

## One-time: create the VM

1. Sign up at [cloud.oracle.com](https://cloud.oracle.com) and pick a home region close to you. The choice is permanent, so it is worth a moment.
2. **Compute → Instances → Create instance.**
3. **Image and shape → Change shape → Ampere** → `VM.Standard.A1.Flex`. Take the largest Always Free allocation the console offers you.
4. **Image:** Canonical Ubuntu 24.04 (the aarch64 build, the console selects it automatically for an Ampere shape). The stack is architecture independent: the fat jar is bytecode, and both `eclipse-temurin:21-jre-alpine` and `postgres:17-alpine` publish arm64 images.
5. Upload your SSH public key and make sure a public IPv4 address is assigned.
6. Create it.

If you get **"Out of host capacity"**, that is the usual Ampere shortage rather than anything you did.
Retry, try another availability domain, or try again later.

## One-time: open ports 80 and 443

> Both of the host-side steps below — the iptables rules and the Docker install — are done for you if
> you paste [`cloud-init.yaml`](cloud-init.yaml) into **Initialization script** while creating the instance.
> The VCN security list still has to be changed by hand, since that firewall is not on the machine.


Two separate firewalls both block them by default, and missing either one looks identical from outside.

**The virtual network.** Networking → Virtual Cloud Networks → your VCN → the public subnet's security list → *Add Ingress Rules*. Twice, both stateless unchecked, source CIDR `0.0.0.0/0`, IP protocol TCP, destination port `80` and then `443`.

**The host.** Oracle's Ubuntu images ship with iptables rules that drop everything except SSH:

```shell
sudo iptables -I INPUT -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

On an Oracle Linux image it is firewalld instead:

```shell
sudo firewall-cmd --permanent --add-port=80/tcp --add-port=443/tcp && sudo firewall-cmd --reload
```

## One-time: install Docker

```shell
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
```

Log out and back in for the group to take effect.

## First deploy

From this repository, on your machine:

```shell
SSH_HOST=ubuntu@<public-ip> ./deploy/oracle/deploy.sh
```

The first run stops early and uploads `env.example`, because the secrets are deliberately not generated for you. Fill it in on the VM:

```shell
ssh ubuntu@<public-ip>
cp ~/appbuilder/env.example ~/appbuilder/.env
uuidgen                      # ADMIN_UUID
openssl rand -base64 48      # JWT_SECRET
openssl rand -base64 24      # DB_PASSWORD, ADMIN_PASSWORD
nano ~/appbuilder/.env
```

Then run the same command again. It builds the fat jar locally, uploads it next to `server/Dockerfile`, and brings the stack up. The VM never needs a JDK, Gradle or a copy of the repository.

Verify:

```shell
curl -I https://<your-domain>/api/probe    # 200, and a valid certificate
```

The server seeds the admin account on first boot from `ADMIN_*`, so you can log in immediately.

## Pointing the apps at it

Two edits, both of which currently name the Cloud Run deployment:

- `shared/src/commonMain/kotlin/com/app/builder/data/http/URL.kt:12` — set `HOST` to your domain.
  Every client and the server's own CORS rule read it from there.
- `server/src/main/kotlin/com/app/builder/data/http/plugin/Cors.kt:20` — in production the server only allows requests from `HOST` itself. 
That is enough today because Firebase Hosting rewrites `/api/**` to Cloud Run, so the browser sees one origin and CORS never comes into play. 
A Firebase Hosting rewrite cannot proxy to an arbitrary external host, so once the web client calls the VM directly it becomes a genuine cross-origin request and your Firebase Hosting domain has to be added with `allowHost`.

Changing `HOST` repoints every client at once, including the Cloud Run based web build, which is why it is left alone here.

## Day to day

```shell
ssh ubuntu@<ip> 'cd appbuilder && docker compose logs -f server'   # follow logs
ssh ubuntu@<ip> 'cd appbuilder && docker compose restart server'   # restart
ssh ubuntu@<ip> 'cd appbuilder && docker compose down'             # stop everything
SSH_HOST=ubuntu@<ip> ./deploy/oracle/deploy.sh                     # ship a new build
```

Back up the database before anything destructive — the volume is the only copy:

```shell
ssh ubuntu@<ip> 'cd appbuilder && docker compose exec -T postgres pg_dump -U appbuilder appbuilder' > backup.sql
```

## Worth knowing

- **Rate limiting sees one client.** `RateLimit.kt:31` keys the limiter on `request.origin.remoteAddress`, which behind Caddy is Caddy's container address for every request, so the per-IP buckets collapse into one shared bucket. 
Installing Ktor's `ForwardedHeaders` plugin and keying on the forwarded address fixes it. The same already applies on Cloud Run.
- **Production mode logs nothing without Sentry.** `ServerLogger` is only registered when
  `DEVELOPMENT=true`, so with an empty `SENTRY_DSN` the application's own telemetry goes nowhere.
  `docker compose logs` still shows Ktor and JVM output.
- **FCM needs credentials.** `GoogleCredentials.getApplicationDefault()` finds nothing outside GCP, so the server falls back to `NoOpFcmService` and delivers over WebSocket and SSE only. 
To enable it, put a service account JSON at `~/appbuilder/secrets/firebase.json` and uncomment the volume and `GOOGLE_APPLICATION_CREDENTIALS` in `docker-compose.yml`.
- **One instance, one box.** Nothing here is highly available. A reboot of the VM is downtime, and Oracle reclaims Always Free compute that stays idle for long stretches.
