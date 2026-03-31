;; examples/06_error_handling.clj — Error handling patterns
;;
;; Demonstrates:
;;   - try/catch ExceptionInfo for :telegraph/api-error
;;   - safe- variants returning {:ok bool ...}
;;   - retry helper for transient network errors
;;   - bulk operations with per-item error collection
;;
;; Usage:
;;   clojure -M examples/06_error_handling.clj YOUR_ACCESS_TOKEN

(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node])

(def token  (or (first *command-line-args*)
                (throw (Exception. "Usage: clojure -M examples/06_error_handling.clj TOKEN"))))
(def client (telegraph/make-client))

;; 1. API error via throw ------------------------------------------------------
(println "\n=== API error (bad token) via try/catch ===")
(try
  (telegraph/create-page! client "bad-token-12345" "Title" [(node/p "body")])
  (catch clojure.lang.ExceptionInfo e
    (let [{:keys [type error endpoint]} (ex-data e)]
      (println "Caught ExceptionInfo!")
      (println "  type    :" type)
      (println "  error   :" error)
      (println "  endpoint:" endpoint))))

;; 2. safe- returns {:ok false} ------------------------------------------------
(println "\n=== safe-create-page! with bad token ===")
(let [{:keys [ok error data]} (telegraph/safe-create-page! client "bad-token" "T" [(node/p "b")])]
  (println "ok?    :" ok)
  (println "error  :" error)
  (println "type   :" (:type data)))

;; 3. safe- with good token ----------------------------------------------------
(println "\n=== safe-create-page! with good token ===")
(let [{:keys [ok result error]}
      (telegraph/safe-create-page! client token "Error Handling Demo"
        [(node/h3 "Error Handling Examples")
         (node/p "This page was created by the error handling example.")
         (node/ul
           (node/li "try/catch for ExceptionInfo")
           (node/li "safe- variants for {:ok bool} returns")
           (node/li "Bulk operations with error collection"))])]
  (if ok
    (do (println "✅ Created!")
        (println "URL:" (:url result)))
    (println "❌ Failed:" error)))

;; 4. Retry helper -------------------------------------------------------------
(println "\n=== retry helper ===")

(defn create-with-retry!
  "Attempt to create a page, retrying on network errors.
   API errors (bad token, invalid content) are not retried."
  [client token title nodes & {:keys [retries] :or {retries 3}}]
  (loop [n retries]
    (let [{:keys [ok result error data]}
          (telegraph/safe-create-page! client token title nodes)]
      (cond
        ok
        result

        (= :telegraph/api-error (:type data))
        (throw (ex-info error data))

        (pos? n)
        (do (println "  network error, retrying..." )
            (Thread/sleep 500)
            (recur (dec n)))

        :else
        (throw (ex-info (str "failed after " retries " retries: " error)
                        (or data {})))))))

(let [page (create-with-retry! client token "Retry Demo"
                                [(node/p "Created via the retry helper.")])]
  (println "✅ Created via retry!")
  (println "URL:" (:url page)))

;; 5. Bulk create with error collection ----------------------------------------
(println "\n=== bulk create, collect results ===")

(def articles
  [{:title "Bulk Article 1" :nodes [(node/p "First bulk article.")]}
   {:title "Bulk Article 2" :nodes [(node/p "Second bulk article.")]}
   {:title "Bulk Article 3" :nodes [(node/p "Third bulk article.")]}])

(def results
  (mapv (fn [{:keys [title nodes]}]
          (assoc (telegraph/safe-create-page! client token title nodes)
                 :title title))
        articles))

(println "Results:")
(doseq [{:keys [ok title result error]} results]
  (if ok
    (println " ✅" title "->" (:url result))
    (println " ❌" title ":" error)))
