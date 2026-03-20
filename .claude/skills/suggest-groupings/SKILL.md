---
name: suggest-groupings
description: Read API snapshots and suggest cheatsheet placement for any unplaced symbols based on their docstrings.
---

Identify unplaced symbols and suggest where they belong in the cheatsheet structures.

## Steps

1. Read both snapshot files:
   - `src/clj_jvm/resources/api-snapshot-peer.edn`
   - `src/clj_jvm/resources/api-snapshot-client.edn`
   If either file is missing, tell the user to run `/fetch-apis` first.

2. Read `src/clj_jvm/src/generator/generator.clj` to identify which symbols are already placed.
   - For peer symbols: scan `peer-cheatsheet-structure` for each bare function name
   - For client symbols: scan `client-cheatsheet-structure` for each bare function name

3. For each **unplaced** symbol (in snapshot but not found in the relevant cheatsheet structure):
   - Read its `:doc` from the snapshot
   - Suggest: which cheatsheet (peer/client), which box, which row, and a proposed row label
   - Base the suggestion on the docstring and the name, compared to similar symbols already in the structure

4. Present results as a Markdown table:

   | Symbol | Suggested box | Suggested row label | Rationale |
   |--------|--------------|---------------------|-----------|
   | `datomic.api/foo` | Connection | Connect | "Creates a connection..." |

   If all symbols are placed, say so explicitly and note the snapshot timestamp.

5. Do **not** edit any files automatically. Present suggestions only; the user decides what to accept.
