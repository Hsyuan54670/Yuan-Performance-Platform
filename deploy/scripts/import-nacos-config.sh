#!/usr/bin/env sh
set -eu

ENV_NAME="dev"
SERVER_ADDR="127.0.0.1:8848"
NAMESPACE="public"
GROUP="DEFAULT_GROUP"
USERNAME="${NACOS_USERNAME:-nacos}"
PASSWORD="${NACOS_PASSWORD:-nacos}"
SCHEME="http"

usage() {
  cat <<'EOF'
Usage: sh deploy/scripts/import-nacos-config.sh [options]

Options:
  -e, --env <dev|test|prod>       Config directory under deploy/nacos (default: dev)
  -s, --server-addr <host:port>   Nacos server address (default: 127.0.0.1:8848)
  -n, --namespace <tenant>        Nacos tenant/namespace (default: public)
  -g, --group <group>             Nacos group (default: DEFAULT_GROUP)
  -u, --username <username>       Nacos username (default: env NACOS_USERNAME or nacos)
  -p, --password <password>       Nacos password (default: env NACOS_PASSWORD or nacos)
      --scheme <http|https>       Request scheme (default: http)
  -h, --help                      Show this help message
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    -e|--env)
      ENV_NAME="$2"
      shift 2
      ;;
    -s|--server-addr)
      SERVER_ADDR="$2"
      shift 2
      ;;
    -n|--namespace)
      NAMESPACE="$2"
      shift 2
      ;;
    -g|--group)
      GROUP="$2"
      shift 2
      ;;
    -u|--username)
      USERNAME="$2"
      shift 2
      ;;
    -p|--password)
      PASSWORD="$2"
      shift 2
      ;;
    --scheme)
      SCHEME="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

case "$ENV_NAME" in
  dev|test|prod) ;;
  *)
    echo "Invalid env: $ENV_NAME" >&2
    exit 1
    ;;
esac

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 1
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(dirname "$SCRIPT_DIR")
CONFIG_DIR="$BASE_DIR/nacos/$ENV_NAME"
BASE_URL="$SCHEME://$SERVER_ADDR"

if [ ! -d "$CONFIG_DIR" ]; then
  echo "Config directory not found: $CONFIG_DIR" >&2
  exit 1
fi

FILES=$(find "$CONFIG_DIR" -maxdepth 1 -type f -name '*.yml' | sort)
if [ -z "$FILES" ]; then
  echo "No config files found under $CONFIG_DIR" >&2
  exit 1
fi

extract_token() {
  printf '%s' "$1" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
}

ACCESS_TOKEN=""
for endpoint in "/nacos/v1/auth/users/login" "/nacos/v1/auth/login"; do
  RESPONSE=$(curl -fsS -X POST "$BASE_URL$endpoint" \
    --data-urlencode "username=$USERNAME" \
    --data-urlencode "password=$PASSWORD" 2>/dev/null || true)

  ACCESS_TOKEN=$(extract_token "$RESPONSE")
  if [ -n "$ACCESS_TOKEN" ]; then
    break
  fi
done

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Failed to login to Nacos at $SERVER_ADDR with username $USERNAME" >&2
  exit 1
fi

echo "Importing Nacos configs from $CONFIG_DIR"

printf '%s\n' "$FILES" | while IFS= read -r file; do
  [ -n "$file" ] || continue
  data_id=$(basename "$file")
  response=$(curl -fsS -X POST "$BASE_URL/nacos/v1/cs/configs?accessToken=$ACCESS_TOKEN" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$GROUP" \
    --data-urlencode "tenant=$NAMESPACE" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file")

  if [ "$response" != "true" ]; then
    echo "Failed to import $data_id: $response" >&2
    exit 1
  fi

  echo "Imported $data_id => $response"
done

echo "Nacos import finished."