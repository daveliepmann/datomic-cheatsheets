---
name: restructure
description: TRIGGER when user asks to group, rearrange, rename, or move sections/rows/entries in a cheatsheet structure. Use whenever the user says things like "group X and Y into Z", "rename this section", "move connect before create", "combine these into one line", even if they don't say "restructure".
---

## File to edit

`src/clj_jvm/src/generator/generator.clj`

## Four cheatsheet structures

| def name | API namespace |
|---|---|
| `peer-cheatsheet-structure` | `datomic.api` |
| `client-cheatsheet-structure` | `datomic.client.api` |
| `async-cheatsheet-structure` | `datomic.client.api.async` |
| `local-cheatsheet-structure` | `datomic.local` |

## Data shape

Table rows: `["label" :cmds '[sym1 sym2 ...]]`

Multiple symbols in one `:cmds` vector renders them grouped on one line. Subsections and sections are `:subsection "Name"` and `:section "Name"` keywords in the box vector.

## Operations

- **Group**: merge multiple rows into one — list all symbols in a single `:cmds` vector with a new label
- **Rename**: change the label string on a row, `:subsection`, or `:section`
- **Rearrange**: reorder rows within a `:table`, or reorder subsections/sections within a box

## Preferred edit tools

1. `mcp__clojure__clojure_edit_replace_sexp` — for targeted s-expression replacement
2. `mcp__clojure__clojure_edit` — fallback for whole-form replacement

## Verify after editing

```bash
cd src/clj_jvm && lein run nolinks no-tooltips 2>&1 | head -20
```

Confirm the generator runs clean (no exceptions, output ends with timing info).
