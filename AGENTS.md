# AGENTS.md / CLAUDE.md

## Persona

You are a senior full-stack developer with expertise in Clojure and Datomic. You live for well-crafted code and well-documented repos. You work with care and rigor, taking extra care to avoid regressions.

## Overview

This project generates Datomic cheatsheets (HTML, LaTeX, PDF formats), forked from the Clojure cheatsheet generator at https://github.com/clojure/clojure-cheatsheets.

The active working directory is `src/clj_jvm/`.

## Commands

All commands run from `src/clj_jvm/`:

**Generate all cheatsheet variants:**
```bash
cd src/clj_jvm && ./scripts/run.sh
```
This generates HTML files, LaTeX `.tex` files, and optionally PDFs (requires LaTeX). PDFs are moved to `../../pdf/`.

**Run with lein (single variant):**
```bash
cd src/clj_jvm && lein run <link-target> <tooltips> [clojuredocs-snapshot]
```
Where `link-target` is one of: `nolinks`, `links-to-clojure`, `links-to-clojuredocs`, `links-to-grimoire`; and `tooltips` is one of: `no-tooltips`, `use-title-attribute`, `tiptip`.

**Run with clj (deps.edn):**
```bash
cd src/clj_jvm && clj -M -m generator.generator <args>
```

**Run tests:**
```bash
cd src/clj_jvm && lein test
```

**Run Clojure metadata checks across versions:**
```bash
cd src/clj_jvm && ./scripts/clj-meta-checks.sh
```

**Update ClojureDocs snapshot:**
```bash
curl -O https://clojuredocs.org/clojuredocs-export.json
```

## Architecture

### Two entry points / namespaces

1. **`src/generator/generator.clj`** (`generator.generator`) — the primary, refactored namespace used with `deps.edn` and `lein`. The `-main` function accepts CLI args for link target, tooltip style, and optional ClojureDocs snapshot file.

2. **`clojure_cheatsheet_generator.clj`** (`clj-jvm.clojure-cheatsheet-generator`) — the original monolithic file; appears to be the legacy version.

### Key data structure: `cheatsheet-structure`

The entire cheatsheet content is defined as a single nested Clojure data structure (`cheatsheet-structure` in `generator.clj`). The shape is:
- Top level: `[:title "..." :page <page-desc> ...]`
- Pages contain `:column` markers and `<box-desc>` entries
- Boxes contain `:section`, `:subsection`, `:table`, and `:cmds-one-line` entries
- Tables are vectors of rows: `["label" :cmds '[sym1 sym2 ...]]`
- Symbols can use shorthand: `[:common-prefix bit- and or]`, `[:common-suffix -thread-bindings get push]`, `[:common-prefix-suffix unchecked- -int add dec]`

### Output formats

The generator produces multiple output variants driven by three orthogonal parameters:
- **Link target**: where symbol links point (clojuredocs, clojure.github.com, grimoire, or no links)
- **Tooltip style**: no tooltips, HTML title attribute, or TipTip jQuery plugin
- **ClojureDocs snapshot**: optionally enriches tooltips with example/comment counts from a local JSON export

Output files from `scripts/run.sh`:
- `cheatsheet-{tooltips}-{cdocs-summary}.html` (5 full standalone variants)
- `cheatsheet-embeddable-for-clojure.org.html` (stripped-down embeddable version)
- `cheatsheet-{paper}-{color}.tex` / `.pdf` (6 LaTeX/PDF variants: a4/usletter × color/grey/bw)
- `warnings.log` (symbols missing from URL map, or in URL map but not in cheatsheet)

### Supporting files

- `src/generator/clojure_metadata_checks.clj` — standalone utility to audit Clojure namespace metadata across versions; has its own `-main`
- `cheatsheet_files/` — jQuery and TipTip JS/CSS assets for tooltip variants
- `resources/inline.css` — CSS inlined into the embeddable HTML
