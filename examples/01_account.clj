;; examples/01_account.clj — Account management
;;
;; Demonstrates: create-account!, get-account!, edit-account!,
;;               safe-create-account!, revoke-token!
;;
;; Usage:
;;   clojure -M examples/01_account.clj YOUR_SHORT_NAME

(require '[clj-telegraph-api.core :as telegraph])

(def short-name (or (first *command-line-args*) "ExampleBot"))
(def client     (telegraph/make-client))

;; 1. Create account -----------------------------------------------------------
(println "\n=== create-account! ===")
(def account
  (telegraph/create-account! client short-name
                              {:author-name "Clojure Example"
                               :author-url  "https://clojure.org"}))
(println "short_name  :" (:short_name  account))
(println "access_token:" (:access_token account))
(println "auth_url    :" (:auth_url    account))

(def token (:access_token account))

;; 2. Get account info ---------------------------------------------------------
(println "\n=== get-account! ===")
(println "full info:" (telegraph/get-account! client token))

(println "\n=== get-account! with :fields ===")
(println "selected fields:"
         (telegraph/get-account! client token
                                  {:fields [:short-name :page-count :author-name]}))

;; 3. Edit account -------------------------------------------------------------
(println "\n=== edit-account! ===")
(def updated (telegraph/edit-account! client token {:author-name "Updated Author"}))
(println "author_name after edit:" (:author_name updated))

;; 4. safe- variant (intentional error) ----------------------------------------
(println "\n=== safe-create-account! (bad input) ===")
(let [{:keys [ok error]} (telegraph/safe-create-account! client "")]
  (println "ok?   :" ok)
  (println "error :" error))

;; 5. Revoke token -------------------------------------------------------------
(println "\n=== revoke-token! ===")
(def new-account (telegraph/revoke-token! client token))
(println "new access_token:" (:access_token new-account))
(println "Note: the old token is now invalid.")
