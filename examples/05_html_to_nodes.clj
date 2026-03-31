;; examples/05_html_to_nodes.clj — html->nodes conversion and publish
;;
;; Usage:
;;   clojure -M examples/05_html_to_nodes.clj YOUR_ACCESS_TOKEN [URL]
;;
;; Without URL: converts a built-in HTML sample and publishes it.
;; With URL: fetches that page, converts, and publishes to Telegraph.

(require '[clj-telegraph-api.core :as telegraph]
         ;; '[clj-telegraph-api.node :as node]
         '[hato.client :as hato])

(def token  (or (first *command-line-args*)
                (throw (Exception. "Usage: clojure -M examples/05_html_to_nodes.clj TOKEN [URL]"))))
(def url    (second *command-line-args*))
(def client (telegraph/make-client))

;; Demo 1: built-in HTML sample -----------------------------------------------
(println "\n=== html->nodes basic conversion ===")

(def sample-html
  "<h3>What is Clojure?</h3>
   <p>Clojure is a <b>dynamic</b>, <em>functional</em> programming language
      that runs on the <a href=\"https://www.java.com\">JVM</a>.</p>
   <ul>
     <li>Immutable data structures</li>
     <li>First-class functions</li>
     <li>Lisp syntax</li>
   </ul>
   <blockquote>Simple things should be simple, complex things should be possible.</blockquote>
   <img src=\"https://www.clojure.org/images/clojure-logo-120b.png\">")

(def nodes-from-html (telegraph/html->nodes sample-html))
(println "Converted" (count nodes-from-html) "top-level nodes:")
(doseq [n nodes-from-html]
  (println " " (if (string? n) (str "TEXT: " n) (str "TAG:  " (:tag n)))))

(println "\nPublishing built-in HTML sample...")
(def page1 (telegraph/create-page! client token "html->nodes Demo" nodes-from-html))
(println "✅ Published!")
(println "URL:" (:url page1))

;; Demo 2: relative URL resolution --------------------------------------------
(println "\n=== html->nodes with :base-url ===")
(def nodes-resolved
  (telegraph/html->nodes
   "<p><a href=\"/api\">Telegraph API</a></p><img src=\"/file/placeholder.jpg\">"
   {:base-url "https://telegra.ph"}))
(println "Resolved nodes:")
(doseq [n nodes-resolved] (println " " n))

;; Demo 3: fetch a real URL (if provided) -------------------------------------
(when url
  (println "\n=== Fetching" url "===")
  (try
    (let [html  (:body (hato/get url {:as              :string
                                      :connect-timeout 10000
                                      :request-timeout 10000}))
          nodes (telegraph/html->nodes html {:base-url url})
          title (or (second (re-find #"<title[^>]*>([^<]+)</title>" html))
                    "Untitled")]
      (println "Fetched" (count nodes) "nodes.")
      (println "Publishing to Telegraph...")
      (def page2 (telegraph/create-page! client token title nodes))
      (println "✅ Published!")
      (println "URL:" (:url page2)))
    (catch Exception e
      (println "❌ Fetch failed:" (ex-message e)))))
