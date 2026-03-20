---
name: fetch-apis
description: Fetch latest Datomic API docs for peer and client, write EDN snapshots, report coverage gaps.
---

Run the fetch script, then summarise results.

## Steps

1. Run `bb src/clj_jvm/scripts/fetch-apis.bb` from the repo root. Capture stdout.

2. Read both snapshot files:
   - `src/clj_jvm/resources/api-snapshot-peer.edn`
   - `src/clj_jvm/resources/api-snapshot-client.edn`

3. Report to the user:
   - How many symbols were parsed for each API
   - The snapshot timestamp
   - The coverage diff output from the script verbatim (NEW / REMOVED? sections)
   - If the diff is clean, say so explicitly

If the script fails (network error, parse error), show the error and suggest checking the doc URLs manually.
