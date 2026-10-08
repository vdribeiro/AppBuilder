#!/usr/bin/env bash
# Builds the server fat jar locally and rolls it out to the Oracle Cloud VM.
# The VM only ever receives the jar and the three compose files; the repository and the JDK stay on this machine.
#
#   SSH_HOST=ubuntu@140.238.0.1 ./deploy/oracle/deploy.sh
#
# Optional:
#   REMOTE_DIR  target directory on the VM, relative to the login user's home
#   SSH_KEY     private key to authenticate with, when it is not the ssh default
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REMOTE_DIR="${REMOTE_DIR:-appbuilder}"

if [[ -z "${SSH_HOST:-}" ]]; then
	echo "SSH_HOST is required, e.g. SSH_HOST=ubuntu@140.238.0.1 $0" >&2
	exit 1
fi

# Expanded defensively so an empty array does not trip `set -u` under the bash macOS ships.
SSH_ARGS=()
[[ -n "${SSH_KEY:-}" ]] && SSH_ARGS=(-i "$SSH_KEY")
ssh_run() { ssh ${SSH_ARGS[@]+"${SSH_ARGS[@]}"} "$SSH_HOST" "$@"; }
scp_put() { scp ${SSH_ARGS[@]+"${SSH_ARGS[@]}"} "$@"; }

echo "==> Building the fat jar"
"$REPO_ROOT/gradlew" -p "$REPO_ROOT" :server:buildFatJar

# The Dockerfile copies build/libs/*-all.jar, so exactly one jar may match.
JARS=()
while IFS= read -r jar; do JARS+=("$jar"); done < <(find "$REPO_ROOT/server/build/libs" -maxdepth 1 -name '*-all.jar' -type f)
if [[ ${#JARS[@]} -ne 1 ]]; then
	echo "Expected exactly one *-all.jar in server/build/libs, found ${#JARS[@]}." >&2
	printf '  %s\n' "${JARS[@]+"${JARS[@]}"}" >&2
	echo "Run './gradlew :server:clean :server:buildFatJar' and retry." >&2
	exit 1
fi
JAR="${JARS[0]}"
echo "==> Using $(basename "$JAR")"

echo "==> Preparing $SSH_HOST:~/$REMOTE_DIR"
ssh_run "mkdir -p '$REMOTE_DIR/server/build/libs'"

# Abort before touching a running deployment if the environment was never filled in.
if ! ssh_run "test -f '$REMOTE_DIR/.env'"; then
	scp_put "$SCRIPT_DIR/env.example" "$SSH_HOST:$REMOTE_DIR/env.example"
	echo >&2
	echo "No .env on the VM yet. It holds the secrets and is deliberately not generated here." >&2
	echo "env.example has been uploaded. Fill it in on the VM, then run this script again:" >&2
	echo "  ssh $SSH_HOST 'cp $REMOTE_DIR/env.example $REMOTE_DIR/.env && nano $REMOTE_DIR/.env'" >&2
	exit 1
fi

echo "==> Uploading"
scp_put "$SCRIPT_DIR/docker-compose.yml" "$SCRIPT_DIR/Caddyfile" "$SSH_HOST:$REMOTE_DIR/"
scp_put "$REPO_ROOT/server/Dockerfile" "$SSH_HOST:$REMOTE_DIR/server/Dockerfile"
# Clear stale jars first, otherwise the Dockerfile's glob matches more than one after a version bump.
ssh_run "rm -f '$REMOTE_DIR/server/build/libs/'*-all.jar"
scp_put "$JAR" "$SSH_HOST:$REMOTE_DIR/server/build/libs/"

echo "==> Restarting"
ssh_run "cd '$REMOTE_DIR' && docker compose up -d --build && docker image prune -f"

echo "==> Deployed. Recent server output:"
ssh_run "cd '$REMOTE_DIR' && docker compose logs --tail 30 server"
