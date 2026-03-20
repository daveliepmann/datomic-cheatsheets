(ns generator.generator
  (:import (java.net URLEncoder))
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]))

;; Inlined from generator.clojure-metadata-checks
(defn ^:private printf-to-writer [w fmt-str & args]
  (binding [*out* w]
    (apply clojure.core/printf fmt-str args)
    (flush)))

(defn iprintf [fmt-str-or-writer & args]
  (if (instance? CharSequence fmt-str-or-writer)
    (apply printf-to-writer *out* fmt-str-or-writer args)
    (apply printf-to-writer fmt-str-or-writer args)))

;; Stub cheatsheet structures — content added in Steps 3-5
(def peer-cheatsheet-structure
  [:title {:html "Datomic Peer API Cheat Sheet"
           :latex "Datomic Peer API Cheat Sheet (\\texttt{datomic.api})"
           :namespace "datomic.api"}
   :page
   [:column
    [:box "green2"
     :section "Connection"
     :table [["Setup" :cmds '[datomic.api/connect datomic.api/create-database]]
             ["Teardown" :cmds '[datomic.api/release datomic.api/shutdown]]
             ["Database management" :cmds '[datomic.api/delete-database datomic.api/rename-database
                                            datomic.api/get-database-names datomic.api/db-stats
                                            datomic.api/cancel datomic.api/administer-system
                                            datomic.api/request-index datomic.api/gc-storage]]]
     :subsection "Sync"
     :table [["Coordination" :cmds '[datomic.api/sync datomic.api/sync-schema
                                     datomic.api/sync-index datomic.api/sync-excise]]
             ["T values" :cmds '[datomic.api/basis-t datomic.api/as-of-t datomic.api/since-t datomic.api/next-t
                                 datomic.api/t->tx datomic.api/tx->t]]]]
    [:box "green2"
     :section "Databases"
     :table [["Value" :cmds '[datomic.api/db]]
             ["Of an entity" :cmds '[datomic.api/entity-db]]
             ["Filtered value" :cmds '[datomic.api/filter datomic.api/is-filtered]]]
     :subsection "Temporal queries"
     :table [["point-in-time filters" :cmds '[datomic.api/as-of datomic.api/since]]
             ["unfiltered present + past" :cmds '[datomic.api/history]]]]
    [:box "blue"
     :section "Novelty processing"
     :table [["Submit" :cmds '[datomic.api/transact datomic.api/transact-async]]
             ["Speculative" :cmds '[datomic.api/with]]
             ["Transaction functions" :cmds '[datomic.api/function datomic.api/invoke]]]]
    :column
    [:box "orange"
     :section "Perception"
     :subsection "Reads"
     :table [["Datalog query" :cmds '[datomic.api/q datomic.api/qseq datomic.api/query]]
             ["Hierarchical entity selection" :cmds '[datomic.api/pull datomic.api/pull-many]]
             ["By index" :cmds '[datomic.api/datoms datomic.api/seek-datoms
                                 datomic.api/index-range datomic.api/index-pull]]]
     :subsection "Entity operations"
     :table [["Entity API" :cmds '[datomic.api/entity datomic.api/touch]]
             ["Entity IDs" :cmds '[datomic.api/entid datomic.api/entid-at]]
             ["Partitions" :cmds '[datomic.api/part datomic.api/implicit-part datomic.api/implicit-part-id]]]
     :subsection "Transactions"
     :table [["Transaction log" :cmds '[datomic.api/log datomic.api/tx-range]]
             ["Report queue " :cmds '[datomic.api/tx-report-queue datomic.api/remove-tx-report-queue]]]]
    [:box "green2"
     :section "Utility"
     :table [["Temp IDs" :cmds '[datomic.api/tempid datomic.api/resolve-tempid]]
             ["Future" :cmds '[datomic.api/add-listener]]
             ["SQUUIDs" :cmds '[datomic.api/squuid datomic.api/squuid-time-millis]]
             ["Schema" :cmds '[datomic.api/ident datomic.api/attribute]]]]]])

(def client-cheatsheet-structure
  [:title {:html "Datomic Client API Cheat Sheet"
           :latex "Datomic Client API Cheat Sheet (\\texttt{datomic.client.api})"
           :namespace "datomic.client.api"}
   :page
   [:column
    [:box "green"
     :section "Connection"
     :table [["Setup" :cmds '[datomic.client.api/client datomic.client.api/connect datomic.client.api/create-database]]
             ["Database management" :cmds '[datomic.client.api/delete-database datomic.client.api/list-databases
                                            datomic.client.api/administer-system datomic.client.api/db-stats]]
             ["Coordination" :cmds '[datomic.client.api/sync]]]]
    [:box "green"
     :section "Databases"
     :table [["Value" :cmds '[datomic.client.api/db datomic.client.api/with-db]]]
     :subsection "Temporal queries"
     :table [["point-in-time filters" :cmds '[datomic.client.api/as-of datomic.client.api/since]]
             ["unfiltered present + past" :cmds '[datomic.client.api/history]]]]
    [:box "blue"
     :section "Novelty processing"
     :table [["Submit" :cmds '[datomic.client.api/transact]]
             ["Speculative" :cmds '[datomic.client.api/with]]]]
    :column
    [:box "orange"
     :section "Perception"
     :subsection "Reads"
     :table [["Datalog query" :cmds '[datomic.client.api/q datomic.client.api/qseq]]
             ["Hierarchical entity selection" :cmds '[datomic.client.api/pull]]
             ["By index" :cmds '[datomic.client.api/datoms datomic.client.api/index-range datomic.client.api/index-pull]]
             ["Transaction log" :cmds '[datomic.client.api/tx-range]]]]]])

(def async-cheatsheet-structure
  [:title {:html "Datomic Async Client API Cheat Sheet"
           :latex "Datomic Async Client API Cheat Sheet (\\texttt{datomic.client.api.async})"
           :namespace "datomic.client.api.async"}
   :page
   [:column
    [:box "green"
     :section {:html "Client &amp; Connection" :latex "Client \\& Connection"}
     :subsection "Client"
     :table [["" :cmds '[datomic.client.api.async/client]]]
     :subsection "Databases"
     :table [["connect" :cmds '[datomic.client.api.async/connect]]
             ["create" :cmds '[datomic.client.api.async/create-database]]
             ["delete" :cmds '[datomic.client.api.async/delete-database]]
             ["list" :cmds '[datomic.client.api.async/list-databases]]]]
    [:box "green"
     :section "Database Value"
     :table [["current" :cmds '[datomic.client.api.async/db]]
             ["as-of" :cmds '[datomic.client.api.async/as-of]]
             ["since" :cmds '[datomic.client.api.async/since]]
             ["unfiltered present + past" :cmds '[datomic.client.api.async/history]]
             ["with-db" :cmds '[datomic.client.api.async/with-db]]
             ["stats" :cmds '[datomic.client.api.async/db-stats]]
             ["sync" :cmds '[datomic.client.api.async/sync]]]]
    [:box "blue"
     :section "Transactions"
     :subsection "Submit"
     :table [["" :cmds '[datomic.client.api.async/transact]]]
     :subsection "Speculative"
     :table [["" :cmds '[datomic.client.api.async/with]]]
     :subsection "Transaction Log"
     :table [["" :cmds '[datomic.client.api.async/tx-range]]]]
    :column
    [:box "orange"
     :section "Querying"
     :subsection "Datalog"
     :table [["" :cmds '[datomic.client.api.async/q datomic.client.api.async/qseq]]]
     :subsection "Pull"
     :table [["" :cmds '[datomic.client.api.async/pull]]]
     :subsection "Raw Index"
     :table [["" :cmds '[datomic.client.api.async/datoms]]
             ["" :cmds '[datomic.client.api.async/index-range datomic.client.api.async/index-pull]]]]
    [:box "grey"
     :section "System"
     :table [["" :cmds '[datomic.client.api.async/administer-system]]]]]])

(def local-cheatsheet-structure
  [:title {:html "Datomic Local &amp; Monitoring Reference"
           :latex "Datomic Local \\& Monitoring Reference"
           :namespace "datomic.local"}
   :page
   [:column
    [:box "green2"
     :section "datomic.local"
     :table [["divert" :cmds '[datomic.local/divert-system]]
             ["release" :cmds '[datomic.local/release-db]]
             ["import" :cmds '[datomic.local/import-cloud]]]]
    :column
    [:box "grey"
     :section "Monitoring"
     :subsection "io-stats"
     :table [["enable" :str {:html "<code>:io-context :your-op</code> key in <code>q</code>/<code>transact</code> arg-map"
                             :latex "\\texttt{:io-context :your-op} key in \\texttt{q}/\\texttt{transact} arg-map"}]
             ["returns" :str {:html "<code>:api</code> <code>:api-ms</code> <code>:reads</code> <code>:nested</code>"
                              :latex "\\texttt{:api :api-ms :reads :nested}"}]]
     :subsection "query-stats"
     :table [["enable" :str {:html "<code>:query-stats true</code> key in query arg-map"
                             :latex "\\texttt{:query-stats true} key in query arg-map"}]
             ["returns" :str {:html "<code>:ret</code> <code>:query-stats</code> <code>:phases</code> <code>:clauses</code>"
                              :latex "\\texttt{:ret :query-stats :phases :clauses}"}]]
     :subsection "tx-stats"
     :table [["returns" :str {:html "in transaction result: <code>:res-ct</code> <code>:comp-ct</code> <code>:dedup-ct</code> <code>:ucheck-ct</code>"
                              :latex "in transaction result: \\texttt{:res-ct :comp-ct :dedup-ct :ucheck-ct}"}]]]]])

;; URL mapping — filled in Step 2
(defn- datomic-peer-symbol-url-pairs []
  (let [base "https://docs.datomic.com/clojure/index.html#datomic.api/"]
    (map (fn [fname] [(str "datomic.api/" fname) (str base fname)])
         '[add-listener administer-system as-of as-of-t attribute basis-t
           cancel connect create-database datoms db db-stats delete-database
           entid entid-at entity entity-db filter function gc-storage
           get-database-names history ident implicit-part implicit-part-id
           index-pull index-range invoke is-filtered log next-t part pull
           pull-many q qseq query release remove-tx-report-queue rename-database
           request-index resolve-tempid seek-datoms shutdown since since-t
           squuid squuid-time-millis sync sync-excise sync-index sync-schema
           t->tx tempid touch transact transact-async tx->t tx-range
           tx-report-queue with])))

(defn- datomic-client-symbol-url-pairs []
  (let [base "https://docs.datomic.com/client-api/datomic.client.api.html#var-"]
    (map (fn [fname] [(str "datomic.client.api/" fname) (str base fname)])
         '[administer-system as-of client connect create-database datoms db
           db-stats delete-database history index-pull index-range list-databases
           pull q qseq since sync transact tx-range with with-db])))

(defn- datomic-async-symbol-url-pairs []
  (let [base "https://docs.datomic.com/client-api/datomic.client.api.async.html#var-"]
    (map (fn [fname] [(str "datomic.client.api.async/" fname) (str base fname)])
         '[administer-system as-of client connect create-database datoms db
           db-stats delete-database history index-pull index-range list-databases
           pull q qseq since sync transact tx-range with with-db])))

(defn- datomic-local-symbol-url-pairs []
  ;; datomic.local has no per-function anchors in the docs
  (let [base "https://docs.datomic.com/api/datomic-local.html"]
    (map (fn [fname] [(str "datomic.local/" fname) base])
         '[divert-system release-db import-cloud])))

(defn symbol-url-pairs [link-target-site cheatsheet-type]
  (if (= link-target-site :nolinks)
    []
    (case cheatsheet-type
      :peer (datomic-peer-symbol-url-pairs)
      :client (datomic-client-symbol-url-pairs)
      :async (datomic-async-symbol-url-pairs)
      :local (datomic-local-symbol-url-pairs))))

(defn die [fmt-str & args]
  (apply iprintf *err* fmt-str args)
  (System/exit 1))

(defn read-edn-safely [x & opts]
  (with-open [r (java.io.PushbackReader. (apply io/reader x opts))]
    (clojure.edn/read r)))

;; Function url-enccode was copied from
;; https://github.com/cemerick/url/blob/master/src/cemerick/url.cljx.
;; The README says that this code is copyright Chas Emerick,
;; distributed under the Eclipse Public License version 1.0, as the
;; clojure-cheatsheet repository code is.

(defn url-encode
  [string]
  (some-> string str (URLEncoder/encode "UTF-8") (.replace "+" "%20")))

;; Use the following usepackage line if you want text with clickable
;; links in the PDF file to look no different from normal text:

;; \usepackage[colorlinks=false,breaklinks=true,pdfborder={0 0 0},dvipdfm]{hyperref}

;; The following line causes blue boxes to appear around words that
;; have links in the PDF file.  This can be good for debugging, but
;; might not be what you want long term.

;; \\usepackage[dvipdfm]{hyperref}

(def latex-header-except-documentclass
  "
% Authors: Steve Tayon, Andy Fingerhut
% Comments, errors, suggestions: Create an issue at
% https://github.com/jafingerhut/clojure-cheatsheets

% Most of the content is based on the clojure wiki, api and source code by Rich Hickey on https://clojure.org/.

% License
% Eclipse Public License v1.0
% https://opensource.org/licenses/eclipse-1.0.php

% Packages
\\usepackage[utf8]{inputenc}
\\usepackage[T1]{fontenc}
\\usepackage{textcomp}
\\usepackage[english]{babel}
\\usepackage{tabularx}
\\usepackage[colorlinks=false,breaklinks=true,pdfborder={0 0 0},dvipdfm]{hyperref}
\\usepackage{lmodern}
\\renewcommand*\\familydefault{\\sfdefault}


\\usepackage[table]{xcolor}

% Set column space
\\setlength{\\columnsep}{0.25em}

% Define colours
\\definecolorset{hsb}{}{}{red,0,.4,0.95;orange,.1,.4,0.95;green,.25,.4,0.95;yellow,.15,.4,0.95}

\\definecolorset{hsb}{}{}{blue,.55,.4,0.95;purple,.7,.4,0.95;pink,.8,.4,0.95;blue2,.58,.4,0.95}

\\definecolorset{hsb}{}{}
{magenta,.9,.4,0.95;green2,.29,.4,0.95}

\\definecolor{grey}{hsb}{0.25,0,0.85}

\\definecolor{white}{hsb}{0,0,1}

% Redefine sections
\\makeatletter
\\renewcommand{\\section}{\\@startsection{section}{1}{0mm}
	{-1.7ex}{0.7ex}{\\normalfont\\large\\bfseries}}
\\renewcommand{\\subsection}{\\@startsection{subsection}{2}{0mm}
	{-1.7ex}{0.5ex}{\\normalfont\\normalsize\\bfseries}}
\\makeatother

% No section numbers
\\setcounter{secnumdepth}{0}

% No indentation
\\setlength{\\parindent}{0em}

% No header and footer
\\pagestyle{empty}


% A few shortcuts
\\newcommand{\\cmd}[1] {\\texttt{\\textbf{{#1}}}}
\\newcommand{\\cmdline}[1] {
	\\begin{tabularx}{\\hsize}{X}
			\\texttt{\\textbf{{#1}}}
	\\end{tabularx}
}

\\newcommand{\\colouredbox}[2] {
	\\colorbox{#1!40}{
		\\begin{minipage}{0.95\\linewidth}
			{
			\\rowcolors[]{1}{#1!20}{#1!10}
			#2
			}
		\\end{minipage}
	}
}

\\begin{document}

")

(def latex-header-after-title "")

(def latex-footer
  "
\\end{document}
")

(def latex-a4-header-before-title
  (str "\\documentclass[footinclude=false,twocolumn,DIV40,fontsize=6.1pt]{scrreprt}\n"
       latex-header-except-documentclass))

;; US letter is a little shorter, so formatting gets completely messed
;; up unless we use a slightly smaller font size.
(def latex-usletter-header-before-title
  (str "\\documentclass[footinclude=false,twocolumn,DIV40,fontsize=5.9pt,letterpaper]{scrreprt}\n"
       latex-header-except-documentclass))

(def html-header-before-title "<!doctype html>
<html lang=\"en\">
<head>
  <meta charset=\"utf-8\">
  <link rel=\"icon\" href=\"cheatsheet_files/favicon.ico\" type=\"image/x-icon\">
")

(defn inline-css [& {:keys [js?]}]
  (let [css (slurp (io/resource "inline.css"))]
    (if js?
      (str/replace css #"\n" "\\\\n")
      css)))

(def html-header-after-title (format "
  <link rel=\"stylesheet\" href=\"cheatsheet_files/style.css\" type=\"text/css\" />
  <style type=\"text/css\">
  %s
  </style>
  <link href=\"cheatsheet_files/tipTip.css\" rel=\"stylesheet\">
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
  <script src=\"cheatsheet_files/jquery.js\"></script>
  <script src=\"cheatsheet_files/jquery.tipTip.js\"></script>
  <script>
  $(function(){
      $(\".tooltip\").tipTip();
  });

  escape_re=function(s) {
    return s.replace(/[-\\/\\\\^$*+?.()|[\\]{}]/g, '\\\\$&');
  };
  $(function(){
    var $links = $('a');
    $(window).on('keyup', function(e) {
      if (e.keyCode == 27) {
        $('#search').focus();
      }
    });
    $('#search').keydown(function(e) {
      var val = $(this).val();
      if (!val && e.key && (e.key == \"'\" || e.key == \"/\")) {
        this.blur();
      }
    });
    $('#search').keyup(function() {
       var val = $(this).val(),
       strs = $.trim(val).split(/\\s+/);
       strs = $.map(strs,escape_re);
       regstr = '^.*' + strs.join('.*') + '.*$',
       reg = RegExp(regstr, 'i');
       console.log(val, reg);

       var matched = $links.filter(function() {
          var text = $(this).text().replace(/\\s+/g, ' ');
          if ($.trim(val)) {return reg.test(text)};
       });

       if (matched.length > 0) {
        $('table').hide();
        $('table').prev('h3').hide();
        $('.section').hide();
        $('a').removeClass('highlight');
        $('#search').removeClass('highlight');

        matched.closest('table').prev('h3').show();
        matched.closest('table').show();
        matched.closest('.section').show();
        matched.addClass('highlight');
       }
       else {
        $('table').show();
        $('table').prev('h3').show();
        $('.section').show();
        $('a').removeClass('highlight');
        if ($.trim(val) == '') {
         $('#search').removeClass('highlight');
       }
       else {
         $('#search').addClass('highlight');
       };
      };
     });
     $('#search').keyup();
  })
  </script>
</head>

<body id=\"cheatsheet\">
" (inline-css)))

(def html-nav-and-content-open "  <nav class=\"search\"><input type='text' id='search' placeholder='Type to search...' autofocus='autofocus'></nav>
  <div class=\"wiki wikiPage\" id=\"content_view\">
")

(def html-footer "  </div>
</body>
</html>
")

(def embeddable-html-fragment-header-before-title "")
(def embeddable-html-fragment-header-after-title (format "
<script language=\"JavaScript\" type=\"text/javascript\">
//<![CDATA[
document.write('<style type=\"text/css\">%s<\\/style>')
//]]>
</script>
" (inline-css :js? true)))
(def embeddable-html-fragment-footer "")

(defmacro verify [cond]
  `(when (not ~cond)
     (iprintf "%s\n" (str "verify of this condition failed: " '~cond))
     (throw (Exception.))))

(defn wrap-line
  "Given a string 'line' that is assumed not to contain line separators,
  but may contain spaces and tabs, return a sequence of strings where
  each is at most width characters long, and all 'words' (consecutive
  sequences of non-whitespace characters) are kept together in the
  same line.  The only exception to the maximum width are if a single
  word is longer than width, in which case it is kept together on one
  line.  Whitespace in the original string is kept except it is
  removed from the end and where lines are broken.  As a special case,
  any whitespace before the first word is preserved.  The second and
  all later lines will always begin with a non-whitespace character."
  [line width]
  (let [space-plus-words (map first (re-seq #"(\s*\S+)|(\s+)"
                                            (str/trimr line)))]
    (loop [finished-lines []
           partial-line []
           len 0
           remaining-words (seq space-plus-words)]
      (if-let [word (first remaining-words)]
        (if (zero? len)
          ;; Special case for first word of first line.  Keep it as
          ;; is, including any leading whitespace it may have.
          (recur finished-lines [word] (count word) (rest remaining-words))
          (let [word-len (count word)
                len-if-append (+ len word-len)]
            (if (<= len-if-append width)
              (recur finished-lines (conj partial-line word) len-if-append
                     (rest remaining-words))
              ;; else we're done with current partial-line and need to
              ;; start a new one.  Trim leading whitespace from word,
              ;; which will be the first word of the next line.
              (let [trimmed-word (str/triml word)]
                (recur (conj finished-lines (apply str partial-line))
                       [trimmed-word]
                       (count trimmed-word)
                       (rest remaining-words))))))
        (if (zero? len)
          [""]
          (conj finished-lines (apply str partial-line)))))))

(defn output-title [fmt t]
  (let [t (if (map? t)
            (get t (:fmt fmt))
            t)]
    (iprintf "%s" (case (:fmt fmt)
                    :latex (format "{\\Large{\\textbf{%s}}}\n\n" t)
                    :html (format "  <title>%s</title>\n" t)
                    :verify-only ""))))

(defn htmlize-str [s]
  (str/escape s {\" "&quot;"
                 \& "&amp;"
                 \< "&lt;"
                 \> "&gt;"}))

;; Handle a thing that could be a string, symbol, or a 'conditional
;; string'

(defn cond-str [fmt cstr & htmlize]
  (cond (string? cstr) cstr
        (symbol? cstr) (if (= (:fmt (first htmlize)) :html)
                         (htmlize-str (str cstr))
                         (str cstr))
        (map? cstr) (do
                      (verify (contains? cstr (:fmt fmt)))
                      (cstr (:fmt fmt)))
        :else (do
                (iprintf "%s\n" (str "cond-str: cstr=" cstr " is not a string, symbol, or map"))
                (verify (or (string? cstr) (symbol? cstr) (map? cstr))))))

(def symbols-looked-up (atom #{}))

(defn url-for-cmd-doc [opts cmd-str]
  (when (:warn-about-unknown-symbols opts)
    (swap! symbols-looked-up conj cmd-str))
  (if-let [url-str (get (:symbol-name-to-url opts) cmd-str)]
    url-str
    (do
      (when (:warn-about-unknown-symbols opts)
        (iprintf *err* "No URL known for symbol with name: '%s'\n" cmd-str))
      nil)))

(defn escape-latex-hyperref-url [url]
  (-> url
      (str/replace "#" "\\#")
      (str/replace "%" "\\%")
      (str/replace "<" "\\%3C")
      (str/replace "=" "\\%3D")
      (str/replace ">" "\\%3E")
      (str/replace "&" "\\&")))

(defn escape-latex-hyperref-target [target]
  (-> target
      ;; -> doesn't seem to have a problem in LaTeX, but ->> looks
      ;; like - followed by a special symbol that is two >'s
      ;; combined, not two separate characters.
      (str/replace "->>" "-{>}{>}")
      (str/replace "&" "\\&")))

;; Only remove the namespaces that are very commonly used in the
;; cheatsheet.  For the ones that only have one or a few symbol there,
;; it seems best to leave the namespace in there explicitly.

(def +common-namespaces-to-remove-from-shown-symbols+
  ["datomic.api/"
   "datomic.client.api/"
   "datomic.client.api.async/"
   "datomic.local/"])

(defn remove-common-ns-prefix [s]
  (if-let [pre (first (filter #(str/starts-with? s %)
                              +common-namespaces-to-remove-from-shown-symbols+))]
    (subs s (count pre))
    s))

(defn cleanup-doc-str-tooltip
  "Get rid of the first line of the doc string, which is always a line
of dashes, and keep at most the first 25 lines of the doc string, to
keep the tooltip from being too large.  Also replace double quote
characters (\") with &quot;"
  [s]
  (let [max-line-width 80
        lines (-> s (str/split-lines) (rest))
        lines (mapcat #(wrap-line % max-line-width) lines)
        max-to-keep 25
        combined-lines
        (if (> (count lines) max-to-keep)
          (str (str/trim-newline (str/join "\n" (take max-to-keep lines)))
               "\n\n[ documentation truncated.  Click link for the rest. ]")
          (str/trim-newline (str/join "\n" lines)))]
    (htmlize-str combined-lines)))

(defn doc-for-symbol-str [_s]
  nil)

(defn count-examples [sym-info]
  (count (:examples sym-info)))

(defn count-comments [sym-info]
  (count (:comments sym-info)))

(defn see-also-names [sym-info]
  (map :name (:see-alsos sym-info)))

(defn example-line-to-count [example-line-string]
  ;; Do not count example lines containing nothing but <pre> or
  ;; </pre>, since clojuredocs.org doesn't show those as separate
  ;; lines.
  (not (re-find #"(?i)^\s*<\s*/?\s*pre\s*>\s*$" example-line-string)))

(defn count-example-lines [sym-info]
  (->> (:examples sym-info)
       (map :body)
       (mapcat str/split-lines)
       (filter example-line-to-count)
       count))

(defn clojuredocs-content-summary [snap-time sym-info]
  (let [num-examples (count-examples sym-info)
        total-example-lines (count-example-lines sym-info)
        see-also-name-strings (see-also-names sym-info)
        num-see-alsos (count see-also-name-strings)
        num-comments (count-comments sym-info)
        see-also-style :list-see-alsos]
    (str (case num-examples
           0 "0 examples"
           1 (format "1 example with %d lines"
                     total-example-lines)
           (format "%d examples totaling %d lines"
                   num-examples total-example-lines))
         (if (and (= see-also-style :number-of-see-alsos)
                  (> num-see-alsos 0))
           (format ", %d see also%s" num-see-alsos
                   (if (== num-see-alsos 1) "" "s"))
           "")
         (if (zero? num-comments)
           ""
           (format ", %d comment%s" num-comments
                   (if (== num-comments 1) "" "s")))
         " on " snap-time
         (if (and (= see-also-style :list-see-alsos)
                  (> num-see-alsos 0))
           (str/join "\n"
                     (wrap-line (str "\nSee also: "
                                     (str/join ", " see-also-name-strings))
                                72))
           ""))))

(defn table-one-cmd-to-str [fmt cmd prefix suffix]
  (let [cmd-str (cond-str fmt cmd)
        whole-cmd (str prefix cmd-str suffix)
        url-str (url-for-cmd-doc fmt whole-cmd)
        ;; cmd-str-to-show has < converted to HTML &lt; among other
        ;; things, if (:fmt fmt) is :html
        cmd-str-to-show (remove-common-ns-prefix (cond-str fmt cmd fmt))
;;        _ (iprintf *err* "andy-debug: cmd='%s' prefix='%s' suffix='%s' (class whole-cmd)='%s' whole-cmd='%s'\n"
;;                   cmd prefix suffix (class whole-cmd) whole-cmd)
        orig-doc-str (doc-for-symbol-str whole-cmd)
        cleaned-doc-str (if orig-doc-str
                          (cleanup-doc-str-tooltip orig-doc-str))
        clojuredocs-snapshot (:clojuredocs-snapshot fmt)
        cleaned-doc-str (if cleaned-doc-str
                          (do
;;                            (iprintf *err* "whole-cmd='%s' sym-info='%s'\n"
;;                                     whole-cmd
;;                                     (get-in clojuredocs-snapshot
;;                                             [:snapshot-info whole-cmd]))
                            (if-let [sym-info
                                     (or (get-in clojuredocs-snapshot
                                                 [:snapshot-info whole-cmd])
                                         (get-in clojuredocs-snapshot
                                                 [:snapshot-info
                                                  (str "clojure.core/"
                                                       whole-cmd)]))]
                              (str cleaned-doc-str "\n\n"
                                   (clojuredocs-content-summary
                                    (get clojuredocs-snapshot :snapshot-time)
                                    sym-info))
                              cleaned-doc-str)))]
    (if url-str
      (case (:fmt fmt)
        :latex (str "\\href{" (escape-latex-hyperref-url url-str)
                    "}{" (escape-latex-hyperref-target cmd-str-to-show) "}")
        :html (str "<a href=\"" url-str "\""
                   (if cleaned-doc-str
                     (case (:tooltips fmt)
                       :no-tooltips ""
                       :tiptip (str " class=\"tooltip\" title=\"<pre>"
                                    cleaned-doc-str "</pre>\"")
                       :use-title-attribute (str " title=\""
                                                 cleaned-doc-str "\""))
                     ;; else no tooltip available to show
                     "")
                   ">" cmd-str-to-show "</a>")
        :verify-only "")
      cmd-str-to-show)))

;; When expand? is true, we expand prefixes and suffixes.
;; Disadvantage: longer output, which is especially bad for the PDF
;; cheatsheet.  Advantage: can search for the complete names of the
;; vars.

;; When expand? is false, don't expand prefixes or suffixes -- leave
;; them 'compressed'.

(defn table-cmds-to-str [fmt cmds]
  (if (vector? cmds)
    (let [expand? (:expand-common-prefixes-or-suffixes fmt)
          [keyw & cmds] cmds
          [pre suff cmds] (case keyw
                            :common-prefix [(first cmds) nil (rest cmds)]
                            :common-suffix [nil (first cmds) (rest cmds)]
                            :common-prefix-suffix [(first cmds) (second cmds) (nnext cmds)])
          [before between after] (if expand?
                                   ["" " " ""]
                                   (case (:fmt fmt)
                                     :latex ["\\{" ", " "\\}"]
                                     :html ["{" ", " "}"]
                                     :verify-only ["" "" ""]))
          pre-str (if pre (cond-str fmt pre) "")
          suff-str (if suff (cond-str fmt suff) "")
          str-list (if expand?
                     (map #(table-one-cmd-to-str fmt (str pre-str
                                                          (cond-str fmt %)
                                                          suff-str)
                                                 "" "")
                          cmds)
                     (map #(table-one-cmd-to-str fmt % pre-str suff-str)
                          cmds))
          most-str (str before
                        (str/join between str-list)
                        after)
          ;; pre-to-show has < converted to HTML &lt; etc., if fmt is
          ;; :html
          pre-to-show (if (and pre (not expand?))
                        (cond-str fmt pre fmt)
                        "")
          suff-to-show (if (and suff (not expand?))
                         (cond-str fmt suff fmt)
                         "")]
      (str pre-to-show most-str suff-to-show))
    ;; handle the one thing, with no prefix or suffix
    (table-one-cmd-to-str fmt cmds "" "")))

(defn output-table-cmd-list [fmt k cmds]
  (if (= k :str)
    (iprintf "%s" (cond-str fmt cmds))
    (do
      (iprintf "%s" (case (:fmt fmt)
                      :latex
                      (case k
                        :cmds "\\cmd{"
                        :cmds-one-line "\\cmdline{")
                      :html "<code>"
                      :verify-only ""))
      (iprintf "%s" (str/join " " (map #(table-cmds-to-str fmt %) cmds)))
      (iprintf "%s" (case (:fmt fmt)
                      :latex "}"
                      :html "</code>"
                      :verify-only "")))))

(defn output-table-row [fmt row row-num nrows]
  (verify (not= nil (#{:cmds :str} (second row))))

  (let [[row-title k cmd-desc] row]
    (iprintf "%s" (case (:fmt fmt)
                    :latex (str (cond-str fmt row-title fmt) " & ")
                    :html (format "              <tr class=\"%s\">
                <td>%s</td>
                <td>"
                                  (if (even? row-num) "even" "odd")
                                  (cond-str fmt row-title fmt))
                    :verify-only ""))
    (output-table-cmd-list fmt k cmd-desc)
    (iprintf "%s" (case (:fmt fmt)
                    :latex (if (= row-num nrows) "\n" " \\\\\n")
                    :html "</td>
              </tr>\n"
                    :verify-only ""))))

(defn output-table [fmt tbl]
  (iprintf "%s" (case (:fmt fmt)
                  :latex "\\begin{tabularx}{\\hsize}{lX}\n"
                  :html "          <table>
            <tbody>
"
                  :verify-only ""))
  (let [nrows (count tbl)]
    (doseq [[row row-num] (map (fn [& args] (vec args))
                               tbl (iterate inc 1))]
      (output-table-row fmt row row-num nrows)))
  (iprintf "%s" (case (:fmt fmt)
                  :latex "\\end{tabularx}\n"
                  :html "            </tbody>
          </table>
"
                  :verify-only "")))

(defn output-cmds-one-line [fmt tbl]
  (iprintf "%s" (case (:fmt fmt)
                  :latex ""
                  :html "          <div class=\"single_row\">
            "
                  :verify-only ""))
  (output-table-cmd-list fmt :cmds-one-line tbl)
  (iprintf "%s" (case (:fmt fmt)
                  :latex "\n"
                  :html "
          </div>\n"
                  :verify-only "")))

(defn output-box [fmt box]
  (verify (even? (count box)))
  (verify (= :box (first box)))
  (let [box-color (if (:colors fmt)
                    (case (:colors fmt)
                      :color (second box)
                      :grey "grey"
                      :bw "white")
                    nil)
        key-val-pairs (partition 2 (nnext box))]
    (iprintf "%s" (case (:fmt fmt)
                    :latex (format "\\colouredbox{%s}{\n" box-color)
                    :html (format "        <div class=\"section%s\">\n" (if box-color (str " " box-color) ""))
                    :verify-only ""))
    (doseq [[k v] key-val-pairs]
      (case k
        :section
        (case (:fmt fmt)
          :latex (iprintf "\\section{%s}\n" (cond-str fmt v))
          :html (iprintf "          <h2>%s</h2>\n" (cond-str fmt v)))
        :subsection
        (case (:fmt fmt)
          :latex (iprintf "\\subsection{%s}\n" (cond-str fmt v))
          :html (iprintf "          <h3>%s</h3>\n" (cond-str fmt v)))
        :table
        (output-table fmt v)
        :cmds-one-line
        (output-cmds-one-line fmt v)))
    (iprintf "%s" (case (:fmt fmt)
                    :latex "}\n\n"
                    :html "        </div><!-- /section -->\n"
                    :verify-only ""))))

(defn output-col [fmt col]
  (iprintf "%s" (case (:fmt fmt)
                  :latex ""
                  :html "      <div class=\"column\">\n"
                  :verify-only ""))
  (doseq [box col]
    (output-box fmt box))
  (iprintf "%s" (case (:fmt fmt)
                  ;;:latex "\\columnbreak\n\n"
                  :latex "\n\n"
                  :html "      </div><!-- /column -->\n"
                  :verify-only "")))

(defn output-page [fmt pg]
  (verify (= (first pg) :column))
  (verify (== 2 (count (filter #(= % :column) pg))))
  (iprintf "%s" (case (:fmt fmt)
                  :latex ""
                  :html "    <div class=\"page\">\n"
                  :verify-only ""))
  (let [tmp (rest pg)
        [col1 col2] (split-with #(not= % :column) tmp)
        col2 (rest col2)]
    (output-col fmt col1)
    (output-col fmt col2))
  (iprintf "%s" (case (:fmt fmt)
                  :latex ""
                  :html "    </div><!-- /page -->\n"
                  :verify-only "")))

(defn output-cheatsheet [fmt cs]
  (verify (even? (count cs)))
  (iprintf "%s" (case (:fmt fmt)
                  :latex (case (:paper fmt)
                           :a4 latex-a4-header-before-title
                           :usletter latex-usletter-header-before-title)
                  :html html-header-before-title
                  :embeddable-html embeddable-html-fragment-header-before-title
                  :verify-only ""))
  (let [[k title & pages] cs
        [show-title fmt-passed-down]
        (if (= (:fmt fmt) :embeddable-html)
          [false (assoc fmt :fmt :html)]
          [true fmt])]
    (verify (= k :title))
    (when show-title
      (output-title fmt-passed-down title))
    (iprintf "%s" (case (:fmt fmt)
                    :latex latex-header-after-title
                    :html html-header-after-title
                    :embeddable-html embeddable-html-fragment-header-after-title
                    :verify-only ""))
    (when (and show-title (= (:fmt fmt) :html) (map? title))
      (let [html-title (:html title)
            ns-str (:namespace title)]
        (iprintf "<div id=\"cheatsheet-header\">\n")
        (iprintf "  <h1>%s</h1>\n" html-title)
        (when ns-str
          (iprintf "  <span class=\"namespace\"><code>%s</code></span>\n" ns-str))
        (iprintf "</div>\n")))
    (when (= (:fmt fmt) :html)
      (iprintf "%s" html-nav-and-content-open))
    (doseq [[k pg] (partition 2 pages)]
      (verify (= k :page))
      (output-page fmt-passed-down pg)))
  (iprintf "%s" (case (:fmt fmt)
                  :latex latex-footer
                  :html html-footer
                  :embeddable-html embeddable-html-fragment-footer
                  :verify-only "")))

(defn print-warnings [wrtr symbol-name-to-url symbols-looked-up]
  (let [never-used (set/difference (set (keys symbol-name-to-url))
                                   symbols-looked-up)]
    (iprintf wrtr "\n\n%d symbols successfully looked up.\n\n"
             (count symbols-looked-up))
    (iprintf wrtr "\n\n%d symbols in URL table never used:\n\n"
             (count never-used))
    (iprintf wrtr "%s\n" (str/join "\n" (sort (seq never-used))))))

(defn parse-args [args]
  (let [supported-link-targets #{"nolinks" "links-to-datomic"}
        link-target-site (if (< (count args) 1)
                           :links-to-datomic
                           (let [arg (nth args 0)]
                             (if (supported-link-targets arg)
                               (keyword arg)
                               (die "Unrecognized argument: %s\nSupported args: %s\n"
                                    arg
                                    (str/join " " (seq supported-link-targets))))))]
    {:link-target-site link-target-site
     :tooltips :no-tooltips}))

(defn -main [& args]
  (let [opts (parse-args args)
        base-opts (merge opts
                         {:clojuredocs-snapshot {}
                          :expand-common-prefixes-or-suffixes true})]
    (doseq [{:keys [name structure cheatsheet-type]} [{:name "peer"
                                                       :structure peer-cheatsheet-structure
                                                       :cheatsheet-type :peer}
                                                      {:name "client"
                                                       :structure client-cheatsheet-structure
                                                       :cheatsheet-type :client}
                                                      {:name "async"
                                                       :structure async-cheatsheet-structure
                                                       :cheatsheet-type :async}
                                                      {:name "local"
                                                       :structure local-cheatsheet-structure
                                                       :cheatsheet-type :local}]]
      (let [symbol-name-to-url (into {} (symbol-url-pairs (:link-target-site opts)
                                                          cheatsheet-type))
            opts+ (merge base-opts {:symbol-name-to-url symbol-name-to-url})]
        (binding [*out* (io/writer (str "cheatsheet-" name "-full.html"))
                  *err* (io/writer (str "cheatsheet-" name "-warnings.log"))]
          (output-cheatsheet (merge opts+ {:fmt :html :colors :color
                                           :warn-about-unknown-symbols true})
                             structure)
          (print-warnings *err* symbol-name-to-url @symbols-looked-up)
          (.close *out*)
          (.close *err*))
        (doseq [x [{:filename (str "cheatsheet-" name "-embeddable.html")
                    :format {:fmt :embeddable-html}}
                   {:filename (str "cheatsheet-" name "-a4-color.tex")
                    :format {:fmt :latex :paper :a4 :colors :color}}
                   {:filename (str "cheatsheet-" name "-a4-grey.tex")
                    :format {:fmt :latex :paper :a4 :colors :grey}}
                   {:filename (str "cheatsheet-" name "-a4-bw.tex")
                    :format {:fmt :latex :paper :a4 :colors :bw}}
                   {:filename (str "cheatsheet-" name "-usletter-color.tex")
                    :format {:fmt :latex :paper :usletter :colors :color}}
                   {:filename (str "cheatsheet-" name "-usletter-grey.tex")
                    :format {:fmt :latex :paper :usletter :colors :grey}}
                   {:filename (str "cheatsheet-" name "-usletter-bw.tex")
                    :format {:fmt :latex :paper :usletter :colors :bw}}]]
          (binding [*out* (io/writer (:filename x))]
            (output-cheatsheet (merge opts+ (:format x)) structure)
            (.close *out*)))))))
