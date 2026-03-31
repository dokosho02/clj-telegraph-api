;; examples/03_pages.clj — Page CRUD: create, edit, get, list
;;
;; Usage:
;;   clojure -M examples/03_pages.clj YOUR_ACCESS_TOKEN

(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node])

(def token  (or (first *command-line-args*)
                (throw (Exception. "Usage: clojure -M examples/03_pages.clj TOKEN"))))
(def client (telegraph/make-client))

;; 1. Create -------------------------------------------------------------------
(println "\n=== create-page! ===")
(def page
  (telegraph/create-page! client token "Pages Demo"
    [(node/h3 "Hello from clj-telegraph-api")
     (node/p "This page was created programmatically from Clojure.")
     (node/p (node/link "Telegraph API docs" "https://telegra.ph/api"))]))
(println "✅ Created!")
(println "URL  :" (:url page))
(println "Path :" (:path page))

(def path (:path page))

;; 2. Edit ---------------------------------------------------------------------
(println "\n=== edit-page! ===")
(def edited
  (telegraph/edit-page! client token path "Pages Demo (Edited)"
    [(node/h3 "Updated Content")
     (node/p "This page was edited after creation.")
     (node/ul
       (node/li "Created ✅")
       (node/li "Edited  ✅"))]))
(println "✅ Edited!")
(println "URL  :" (:url edited))

;; 3. Get ----------------------------------------------------------------------
(println "\n=== get-page! ===")
(def info (telegraph/get-page! client path))
(println "title :" (:title info))
(println "views :" (:views info))
(println "url   :" (:url   info))

(println "\n=== get-page! with :return-content? ===")
(def info2 (telegraph/get-page! client path {:return-content? true}))
(println "node count:" (count (:content info2)))

;; 4. List ---------------------------------------------------------------------
(println "\n=== get-page-list! ===")
(def result (telegraph/get-page-list! client token {:limit 5}))
(println "total pages:" (:total_count result))
(println "first 5:")
(doseq [p (:pages result)]
  (println " " (:title p) "->" (:url p)))

;; 5. safe- variant ------------------------------------------------------------
(println "\n=== safe-create-page! ===")
(let [{:keys [ok result error]}
      (telegraph/safe-create-page! client token "Safe Variant Demo"
        [(node/p "Created with safe-create-page!")])]
  (if ok
    (do (println "✅ safe create succeeded!")
        (println "URL:" (:url result)))
    (println "❌ failed:" error)))
