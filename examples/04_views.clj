;; examples/04_views.clj — View statistics
;;
;; Usage:
;;   clojure -M examples/04_views.clj YOUR_ACCESS_TOKEN [PAGE_PATH]
;;
;; PAGE_PATH is the :path value from create-page!, e.g. "My-Article-01-01".
;; If omitted, a test page is created automatically.

(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node])

(def token  (or (first *command-line-args*)
                (throw (Exception. "Usage: clojure -M examples/04_views.clj TOKEN [PAGE_PATH]"))))
(def client (telegraph/make-client))

(def path
  (or (second *command-line-args*)
      (let [page (telegraph/create-page! client token "Views Demo"
                   [(node/p "A page to demonstrate get-views!")])]
        (println "Created test page:" (:url page))
        (:path page))))

(println "\nQuerying views for path:" path)

;; Total views -----------------------------------------------------------------
(println "\n=== get-views! (total) ===")
(println "Total views:" (:views (telegraph/get-views! client path)))

;; By year ---------------------------------------------------------------------
(println "\n=== get-views! by year ===")
(doseq [y [2023 2024 2025]]
  (let [{:keys [views]} (telegraph/get-views! client path {:year y})]
    (println (str "  " y ": " views " views"))))

;; By month (2025) -------------------------------------------------------------
(println "\n=== get-views! by month (2025) ===")
(doseq [m (range 1 13)]
  (let [{:keys [views]} (telegraph/get-views! client path {:year 2025 :month m})]
    (when (pos? views)
      (println (format "  2025-%02d: %d views" m views)))))

;; safe- variant ---------------------------------------------------------------
(println "\n=== safe-get-views! ===")
(let [{:keys [ok result error]} (telegraph/safe-get-views! client path)]
  (if ok
    (println "views (safe):" (:views result))
    (println "error:" error)))
