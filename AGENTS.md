# AGENTS.md / CLAUDE.md

This project generates Datomic cheatsheets, forked from https://github.com/clojure/clojure-cheatsheets.

## Commands

Build: `cd src/clj_jvm && clj -M -m generator.generator links-to-datomic`

Test: `cd src/clj_jvm && lein test`

## Deploy to GitHub Pages

1. Build (see above)
2. Copy the `*-full.html` files from `src/clj_jvm/` into a checkout of `gh-pages`
3. Commit and push to `origin/gh-pages`
