#!/usr/bin/env bb
;; Fetches Datomic API docs, writes EDN snapshots, and reports coverage gaps.
;; Run from repo root or src/clj_jvm/:
;;   bb src/clj_jvm/scripts/fetch-apis.bb

(require '[babashka.http-client :as http]
         '[clojure.string :as str])

(def peer-url "https://docs.datomic.com/clojure/index.html")
(def client-url "https://docs.datomic.com/client-api/datomic.client.api.html")

(def script-dir
  (-> (System/getProperty "babashka.file")
      java.io.File.
      .getAbsoluteFile
      .getParentFile
      .getAbsolutePath))

(def resources-dir (str script-dir "/../resources"))
(def generator-file (str script-dir "/../src/generator/generator.clj"))

;;; HTML utilities

(defn strip-tags [s]
  (str/replace s #"<[^>]++>" ""))

(defn decode-html [s]
  (-> s
      (str/replace "&amp;" "&")
      (str/replace "&lt;" "<")
      (str/replace "&gt;" ">")
      (str/replace "&quot;" "\"")
      (str/replace "&#39;" "'")
      (str/replace "&nbsp;" " ")))

(defn clean [s]
  (when s
    (-> s strip-tags decode-html str/trim)))

;;; Peer API parsing
;; Page structure (one entry per var):
;;   <div id="var-entry">
;;     <h2 id="datomic.api/FNAME">FNAME</h2>
;;     <pre id="var-usage">Usage: (FNAME ...)\n</pre>
;;     <pre id="var-docstr">Docstring text.</pre>
;;   </div>

(defn parse-peer [html]
  (let [entries (str/split html #"<div id=\"var-entry\">")]
    (reduce
     (fn [acc entry]
       (if-let [[_ sym-str-raw] (re-find #"<h2 id=\"(datomic\.api/[^\"]+)\"" entry)]
         (let [sym-str (decode-html sym-str-raw)
               usage (some-> (re-find #"(?s)<pre id=\"var-usage\">(.*?)</pre>" entry)
                             second clean (str/replace #"^Usage:\s*" ""))
               doc (some-> (re-find #"(?s)<pre id=\"var-docstr\">(.*?)</pre>" entry)
                           second clean)]
           (assoc acc (symbol sym-str) {:doc doc :usage usage}))
         acc))
     {}
     entries)))

;;; Client API parsing
;; Page structure (all on one line per var):
;;   <div class="public anchor" id="var-FNAME">
;;     <h3>FNAME</h3>
;;     <div class="usage"><code>(FNAME ...)</code><code>(FNAME ...)</code></div>
;;     <div class="doc"><pre class="plaintext">Docstring text.</pre></div>
;;   </div>

(defn parse-client [html]
  (let [entries (str/split html #"<div class=\"public anchor\"")]
    (reduce
     (fn [acc entry]
       (if-let [[_ fname] (re-find #"id=\"var-([^\"]+)\"" entry)]
         (let [sym-str (str "datomic.client.api/" fname)
               usage (some-> (re-find #"(?s)<div class=\"usage\">(.*?)</div>" entry)
                             second clean)
               doc (some-> (re-find #"(?s)<pre class=\"plaintext\">(.*?)</pre>" entry)
                           second clean)]
           (assoc acc (symbol sym-str) {:doc doc :usage usage}))
         acc))
     {}
     entries)))

;;; Fetch and parse

(defn fetch [url]
  (println (str "Fetching " url " ..."))
  (:body (http/get url)))

(def snapshot-time
  (-> (java.time.Instant/now) str))

(defn make-snapshot [symbols]
  {:snapshot-time snapshot-time
   :symbols symbols})

;;; Coverage diff
;; Reads generator.clj as a string and checks bare fname presence.

(defn generator-source []
  (slurp generator-file))

(defn fname [sym]
  (name sym))

(defn in-generator? [gen-src sym]
  (str/includes? gen-src (fname sym)))

(defn coverage-diff [snapshot-syms label gen-src]
  (let [missing-from-sheet (remove #(in-generator? gen-src %) (keys snapshot-syms))
        in-sheet-not-in-doc (let [doc-fnames (set (map fname (keys snapshot-syms)))]
                              ;; Extract fnames from the relevant symbol-url-pairs section
                              (let [section-pat (case label
                                                  :peer #"datomic-peer-symbol-url-pairs[\s\S]*?\[\s*'([\s\S]*?)\]"
                                                  :client #"datomic-client-symbol-url-pairs[\s\S]*?\[\s*'([\s\S]*?)\]")
                                    section (some-> (re-find section-pat gen-src) second)]
                                (when section
                                  (let [fnames-in-sheet (re-seq #"\b([a-z][a-z0-9\-]*(?:\-[a-z0-9]+)*)\b" section)]
                                    (->> fnames-in-sheet
                                         (map second)
                                         (remove #{"map" "fn" "let" "str" "base"})
                                         (remove doc-fnames))))))]
    {:missing-from-cheatsheet (sort-by fname missing-from-sheet)
     :missing-from-snapshot (sort in-sheet-not-in-doc)}))

(defn print-diff [{:keys [missing-from-cheatsheet missing-from-snapshot]} api-name]
  (println (str "\n=== Coverage diff: " api-name " ==="))
  (if (empty? missing-from-cheatsheet)
    (println "  All snapshot symbols are present in the cheatsheet.")
    (do (println (str "  NEW (in docs, not in cheatsheet) [" (count missing-from-cheatsheet) "]:"))
        (doseq [s missing-from-cheatsheet]
          (println (str "    " s)))))
  (if (empty? missing-from-snapshot)
    (println "  All cheatsheet symbols appear in the snapshot.")
    (do (println (str "  REMOVED? (in cheatsheet, not in docs) [" (count missing-from-snapshot) "]:"))
        (doseq [s missing-from-snapshot]
          (println (str "    " s))))))

;;; Main

(defn -main []
  (let [peer-html (fetch peer-url)
        client-html (fetch client-url)
        peer-syms (parse-peer peer-html)
        client-syms (parse-client client-html)
        gen-src (generator-source)]

    (println (str "  Parsed " (count peer-syms) " peer symbols."))
    (println (str "  Parsed " (count client-syms) " client symbols."))

    ;; Write snapshots
    (let [peer-out (str resources-dir "/api-snapshot-peer.edn")
          client-out (str resources-dir "/api-snapshot-client.edn")]
      (spit peer-out (pr-str (make-snapshot peer-syms)))
      (spit client-out (pr-str (make-snapshot client-syms)))
      (println (str "\nWrote " peer-out))
      (println (str "Wrote " client-out)))

    ;; Coverage diffs
    (print-diff (coverage-diff peer-syms :peer gen-src) "datomic.api (peer)")
    (print-diff (coverage-diff client-syms :client gen-src) "datomic.client.api (client)")))

(-main)
