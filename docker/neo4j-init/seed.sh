#!/usr/bin/env bash
# Seeds the Neo4j location hierarchy (Earth + ~250 countries).
#
# Idempotent by design: the repo's Cypher script opens with
# `MATCH (l:Location) DETACH DELETE l`, which would wipe every state,
# district, town and village a developer has created. So we only run it
# when the graph has no Earth node yet.
set -euo pipefail

SEED_FILE="/seed-data/Countries"
PASSWORD="${NEO4J_PASSWORD:-famtree-local-dev}"
CYPHER=(cypher-shell -a "bolt://neo4j:7687" -u neo4j -p "$PASSWORD" --format plain)

if [[ ! -f "$SEED_FILE" ]]; then
  echo "seed: $SEED_FILE not found - nothing to load" >&2
  exit 1
fi

existing="$("${CYPHER[@]}" 'MATCH (l:Location {id:"E"}) RETURN count(l)' | tail -n 1 | tr -d '[:space:]')"

if [[ "$existing" != "0" ]]; then
  echo "seed: Earth node already present - skipping (use 'make reseed' to force)"
  exit 0
fi

echo "seed: loading location hierarchy from $SEED_FILE ..."
"${CYPHER[@]}" --file "$SEED_FILE"

countries="$("${CYPHER[@]}" 'MATCH (c:Location {locationType:"COUNTRY"}) RETURN count(c)' | tail -n 1 | tr -d '[:space:]')"
echo "seed: done - $countries countries under Earth"
