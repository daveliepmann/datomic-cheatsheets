# Datomic cheatsheet generator

The program `src/generator/generator.clj` and accompanying shell script
`./scripts/run.sh` generate HTML and LaTeX versions of four Datomic API
cheatsheets. A suitable LaTeX installation can then produce PDFs.

Cheatsheets generated:
- **Peer API** — `datomic.api` (~61 functions)
- **Client API** — `datomic.client.api` (22 functions)
- **Async API** — `datomic.client.api.async` (22 functions)
- **Local & Monitoring** — `datomic.local` + monitoring stats

All cheatsheet content is defined in `peer-cheatsheet-structure`,
`client-cheatsheet-structure`, `async-cheatsheet-structure`, and
`local-cheatsheet-structure` in the generator source file. Links point to
[docs.datomic.com][datomic-docs].

[datomic-docs]: https://docs.datomic.com/


# Requirements

- JDK 11+
- [Clojure CLI tools][clj-install] (`clj`)

For PDF generation, LaTeX is required:

```bash
# Ubuntu/Debian
sudo apt-get install texlive-latex-base texlive-latex-extra

# macOS (MacPorts)
sudo port install texlive-latex-recommended texlive-fonts-recommended
```

[clj-install]: https://clojure.org/guides/install_clojure


# Generating cheatsheets

Edit `./scripts/run.sh` to set `LINK_TARGET` and `PRODUCE_PDF`, then run:

```bash
cd src/clj_jvm
./scripts/run.sh
```

Or to generate HTML only without PDFs:

```bash
cd src/clj_jvm
clj -M -m generator.generator links-to-datomic
```

## Output files

HTML:
- `cheatsheet-{peer,client,async,local}-full.html` — standalone HTML
- `cheatsheet-{peer,client,async,local}-embeddable.html` — embeddable variant

LaTeX/PDF (one set per cheatsheet name × paper size × color variant):
- `cheatsheet-{name}-{a4,usletter}-{color,grey,bw}.tex`
- `cheatsheet-{name}-{a4,usletter}-{color,grey,bw}.pdf` (moved to `../../pdf/`)

Warnings:
- `cheatsheet-{name}-warnings.log` — symbols missing from URL map, or URL map entries unused


# License

Originally: Copyright (C) 2012-2020 Andy Fingerhut

Distributed under the Eclipse Public License, the same as Clojure.
