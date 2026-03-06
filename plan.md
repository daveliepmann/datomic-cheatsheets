# Plan: Refactor to Datomic Cheatsheet Generator

## Context

This repo is a fork of jafingerhut/clojure-cheatsheets. The goal is to repurpose the rendering engine to produce cheatsheets for the Datomic APIs.

**Decisions:**
- Four separate cheatsheets: Peer, Client, Async, Local+Monitoring
- Build tool: `clj`/`deps.edn` only — delete `project.clj` and all `lein` usage
- Clojure cheatsheet content deleted; rendering engine kept

---

## Steps

### Step 1 — Demolition ✅
Delete all obsolete files. Strip `generator.clj` of `cheatsheet-structure`, clojuredocs/grimoire code, and Clojure-namespace requires. Delete `project.clj`, clean `deps.edn`. Add empty stub defs for the four cheatsheet structures.

*Milestone:* `clj -M -m generator.generator` runs without error and produces four empty-but-valid HTML files. `generator.clj` is ~250 lines of pure rendering engine.

### Step 2 — URL maps + wiring ✅
Add the four `symbol-url-pairs` functions (hand-coded). Update dispatch, `parse-args`, `-main`.

*Milestone:* `clj -M -m generator.generator links-to-datomic` produces four HTML files. `warnings.log` shows all symbols missing (stubs empty). `nolinks` variant works too.

### Step 3 — Peer content ✅
Replace `peer-cheatsheet-structure` stub with full two-page structure for `datomic.api` (~60 functions).

*Milestone:* `cheatsheet-peer-full.html` renders correctly in browser, all functions linked. No Peer symbols in `warnings.log`.

### Step 4 — Client + Async content ✅
Replace `client-cheatsheet-structure` and `async-cheatsheet-structure` stubs.

*Milestone:* Both HTML files render correctly. No Client/Async symbols in `warnings.log`.

### Step 5 — Local/Monitoring + finish ✅
Replace `local-cheatsheet-structure` stub (`:str` rows for monitoring). Update `scripts/run.sh` to use `clj`. Update `README.markdown`.

*Milestone:* All four HTML files correct. `./scripts/run.sh` produces all output files. `ls ../../pdf/` shows all PDFs. `warnings.log` clean.

---

## What to Keep (Rendering Engine)

All in `src/clj_jvm/src/generator/generator.clj`:

- `output-cheatsheet`, `output-page`, `output-col`, `output-box`, `output-table`, `output-table-row`, `output-table-cmd-list`
- `htmlize-str`, `cond-str`, `wrap-line`, `verify`
- `html-header-*`, `html-footer`, `latex-header-*`, `latex-footer`, `embeddable-html-*`
- `table-one-cmd-to-str`, `table-cmds-to-str`
- `output-title`, `url-encode`
- `parse-args`, `-main` — adapted for four-cheatsheet output

---

## What to Delete

**In `generator.clj`:**
- `cheatsheet-structure` (~2200 lines)
- `symbol-url-pairs-for-whole-namespaces`, `symbol-url-pairs-specified-by-hand`
- All `clojuredocs-*` functions
- All `grimoire-*` functions and `grimoire-munge-map`
- All `require`s for Clojure namespaces loaded for URL introspection

**Files:**
- `src/clj_jvm/clojure_cheatsheet_generator.clj`
- `src/clj_jvm/src/generator/clojure_metadata_checks.clj`
- `src/clj_jvm/scripts/clj-meta-checks.sh`
- `src/clj_jvm/clojuredocs-export.json`
- `src/clj_jvm/clojuredocs-snapshot.edn`
- `src/clj_jvm/scratch.clj`
- `src/clj_jvm/unused-symbols.txt`
- `src/clj_jvm/test/generator/core_test.clj`
- `src/clj_jvm/project.clj`

**Dependencies to remove from `deps.edn`:**
- `org.clojure/core.async`, `org.clojure/data.priority-map`, `org.clojure/data.avl`,
  `org.clojure/data.int-map`, `org.flatland/ordered`, `org.flatland/useful`,
  `org.clojure/test.check`, `org.clojure/data.json`

---

## Four Cheatsheet Structure Defs

### 1. `peer-cheatsheet-structure` — `datomic.api`, 2 pages

**Page 1:**
```
Left column:
  [Connection & Database — green]
    Setup: connect  release  shutdown
           create-database  delete-database  rename-database  get-database-names
    Database Value: db  as-of  since  history  filter  is-filtered
                    basis-t  as-of-t  since-t  next-t  db-stats
  [Transactions — blue]
    Submit: transact  transact-async
    Speculative: with
    Temp IDs: tempid  resolve-tempid
    Time: t->tx  tx->t

Right column:
  [Querying — orange]
    Datalog: q  qseq  query
    Pull: pull  pull-many
    Raw Index: datoms  seek-datoms  index-range  index-pull
  [Entity Operations — yellow]
    Entity: entity  entity-db  touch
    Identity: entid  entid-at  ident  part  implicit-part  implicit-part-id
  [Transaction Log — purple]
    log  tx-range
```

**Page 2:**
```
Left column:
  [Schema & Functions — teal]
    Schema: attribute
    Functions: function  invoke
    UUIDs: squuid  squuid-time-millis
  [Sync & Coordination — gray]
    Sync: sync  sync-schema  sync-index  sync-excise
    Maintenance: request-index  gc-storage
    Notifications: tx-report-queue  remove-tx-report-queue  add-listener
    Admin: cancel  administer-system
```

### 2. `client-cheatsheet-structure` — `datomic.client.api`, 1 page

```
Left column:
  [Client & Connection — green]
    client  connect  create-database  delete-database  list-databases
    db  as-of  since  history  with-db  db-stats  sync
  [Transactions — blue]
    transact  with  tx-range

Right column:
  [Querying — orange]
    q  qseq  pull  datoms  index-range  index-pull
  [System — gray]
    administer-system
```

### 3. `async-cheatsheet-structure` — `datomic.client.api.async`, 1 page

Same 22 functions as Client, returns `core.async` channels.

```
Left column:
  [Client & Connection — green]
    client  connect  create-database  delete-database  list-databases
    db  as-of  since  history  with-db  db-stats  sync
  [Transactions — blue]
    transact  with  tx-range

Right column:
  [Querying — orange]
    q  qseq  pull  datoms  index-range  index-pull
  [System — gray]
    administer-system
```

### 4. `local-cheatsheet-structure` — `datomic.local` + monitoring, 1 page

```
Left column:
  [datomic.local — teal]
    divert-system  release-db  import-cloud

Right column:
  [Monitoring — light gray]
    io-stats: :io-context in q/transact arg-map → :api :api-ms :reads :nested
    query-stats: :query-stats true in query map → :ret :query-stats :phases :clauses
    tx-stats: in transaction result → :res-ct :comp-ct :dedup-ct :ucheck-ct
```

---

## URL Mapping

| Namespace | URL pattern |
|-----------|------------|
| `datomic.api` | `https://docs.datomic.com/clojure/index.html#datomic.api/FNAME` |
| `datomic.client.api` | `https://docs.datomic.com/client-api/datomic.client.api.html#datomic.client.api/FNAME` |
| `datomic.client.api.async` | `https://docs.datomic.com/client-api/datomic.client.api.async.html#datomic.client.api.async/FNAME` |
| `datomic.local` | `https://docs.datomic.com/api/datomic-local.html` (page-level only) |

```clojure
(defn datomic-peer-symbol-url-pairs [] ...)
(defn datomic-client-symbol-url-pairs [] ...)
(defn datomic-async-symbol-url-pairs [] ...)
(defn datomic-local-symbol-url-pairs [] ...)
(defn symbol-url-pairs [link-target-site cheatsheet-type] ...)
```

---

## Verification (final)

```bash
cd src/clj_jvm
clj -M -m generator.generator links-to-datomic
ls cheatsheet-peer-full.html cheatsheet-client-full.html \
   cheatsheet-async-full.html cheatsheet-local-full.html
cat warnings.log
./scripts/run.sh
ls ../../pdf/
```
