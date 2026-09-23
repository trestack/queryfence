#!/usr/bin/env bash
# Builds the examples and checks that each one fails for the reason its README claims.
#
# The examples ship a query that leaks on purpose, so a green build would mean the example stopped
# demonstrating anything. This script therefore expects a failing build that mentions QueryFence.
#
# It needs the databases the examples point at (localhost:3306 MySQL, localhost:5432 Postgres),
# either from `docker compose up` in each example, or from CI service containers. Set
# QF_MYSQL_URL or QF_POSTGRES_URL to point an example at a database somewhere else.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
mvnw="$root/mvnw"

echo "==> Installing QueryFence into the local repository"
"$mvnw" -B -ntp -q install -DskipTests

status=0
for example in mysql-mybatis postgres-jpa; do
  directory="$root/examples/$example"
  log="$(mktemp)"
  case "$example" in
    mysql-mybatis) url="${QF_MYSQL_URL:-}" ;;
    postgres-jpa) url="${QF_POSTGRES_URL:-}" ;;
  esac
  override=()
  [ -n "$url" ] && override=("-Dspring.datasource.url=$url")
  echo "==> $example"
  if (cd "$directory" && "$mvnw" -B -ntp "${override[@]+"${override[@]}"}" verify > "$log" 2>&1); then
    echo "    FAILED: the build was green, but this example is supposed to ship a leak"
    tail -30 "$log"
    status=1
    continue
  fi
  if grep -q "QueryFence: 1 violation" "$log"; then
    echo "    OK: build fails with the documented QueryFence violation"
    grep -A6 "QueryFence: 1 violation" "$log" | head -8 | sed 's/^/    /'
  else
    echo "    FAILED: the build failed, but not because of QueryFence"
    tail -40 "$log"
    status=1
  fi
done
exit $status
