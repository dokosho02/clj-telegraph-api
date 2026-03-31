(ns clj-telegraph-api.core
  "Public API for clj-telegraph-api.

  All network functions end with ! (bang).
  Every bang function has a safe- counterpart that returns
  {:ok true :result ...} or {:ok false :error ...} instead of throwing.

  Quick start:
    (require '[clj-telegraph-api.core :as telegraph]
             '[clj-telegraph-api.node :as node])

    (def client  (telegraph/make-client))
    (def account (telegraph/create-account! client \"MyBot\"))
    (def token   (:access_token account))

    (def page
      (telegraph/create-page! client token \"My Article\"
        [(node/h3 \"Hello\")
         (node/p  \"World\")]))
    (println (:url page))"
  (:require [clj-telegraph-api.client :as client]
            [clj-telegraph-api.model  :as model]
            [clj-telegraph-api.node   :as node]
            [cheshire.core            :as json]
            ;; [clojure.string           :as str]
            ))

(def make-client
  "Create a Telegraph client config map. See client/make-client."
  client/make-client)

(def html->nodes
  "Convert an HTML string to a Telegraph Node vector. See node/html->nodes."
  node/html->nodes)

(defn- wrap
  "Run f and return {:ok true :result ...} or {:ok false :error ...}."
  [f]
  (try {:ok true :result (f)}
       (catch clojure.lang.ExceptionInfo e
         {:ok false :error (ex-message e) :data (ex-data e)})
       (catch Exception e
         {:ok false :error (ex-message e)})))

;; --- Account -----------------------------------------------------------------

(defn create-account!
  "Create a new Telegraph account.

   short-name — display name shown on pages (1–32 chars).
   opts keys  — :author-name :author-url

   Returns a map with :access_token, :auth_url, :short_name etc.
   Store :access_token — it is the credential for all subsequent calls."
  ([c short-name] (create-account! c short-name {}))
  ([c short-name opts]
   (client/post! c "/createAccount"
                 (into {"short_name" short-name}
                       (for [[k v] opts :when (some? v)]
                         [(model/kebab->snake k) v])))))

(defn safe-create-account!
  "Like create-account! but returns {:ok bool ...} instead of throwing."
  ([c n]      (wrap #(create-account! c n)))
  ([c n opts] (wrap #(create-account! c n opts))))

(defn edit-account!
  "Edit account info. opts keys: :short-name :author-name :author-url"
  [c token opts]
  (client/post! c "/editAccountInfo"
                (into {"access_token" token}
                      (for [[k v] opts :when (some? v)]
                        [(model/kebab->snake k) v]))))

(defn safe-edit-account!
  [c token opts]
  (wrap #(edit-account! c token opts)))

(defn get-account!
  "Fetch account info.

   opts keys:
     :fields — vector of field keywords to return, e.g.
               [:short-name :author-name :author-url :auth-url :page-count]"
  ([c token] (get-account! c token {}))
  ([c token {:keys [fields]}]
   (client/post! c "/getAccountInfo"
                 (cond-> {"access_token" token}
                   fields (assoc "fields"
                                 (json/generate-string
                                  (mapv model/kebab->snake fields)))))))

(defn safe-get-account!
  ([c t]      (wrap #(get-account! c t)))
  ([c t opts] (wrap #(get-account! c t opts))))

(defn revoke-token!
  "Revoke access-token and return a new one.
   The old token is invalidated immediately."
  [c token]
  (client/post! c "/revokeAccessToken" {"access_token" token}))

(defn safe-revoke-token!
  [c token]
  (wrap #(revoke-token! c token)))

;; --- Pages -------------------------------------------------------------------

(defn create-page!
  "Publish a new Telegraph page.

   nodes — vector of Telegraph Nodes (use clj-telegraph-api.node helpers).
   opts keys:
     :author-name     — override account author name
     :author-url      — override account author URL
     :return-content? — include :content nodes in response (default false)

   Returns a map with :url, :path, :title, :views etc."
  ([c token title nodes] (create-page! c token title nodes {}))
  ([c token title nodes opts]
   (let [content-json (json/generate-string (vec nodes))
         params       (cond-> {"access_token"   token
                               "title"          (subs title 0 (min 256 (count title)))
                               "content"        content-json
                               "return_content" (str (boolean (:return-content? opts)))}
                        (:author-name opts) (assoc "author_name" (:author-name opts))
                        (:author-url  opts) (assoc "author_url"  (:author-url  opts)))]
     (client/post! c "/createPage" params))))

(defn safe-create-page!
  ([c t title nodes]      (wrap #(create-page! c t title nodes)))
  ([c t title nodes opts] (wrap #(create-page! c t title nodes opts))))

(defn edit-page!
  "Edit an existing Telegraph page. Only pages created with this token can be edited.

   path  — page path from create-page! :path, e.g. \"My-Article-01-01\"
   opts  — same keys as create-page!"
  ([c token path title nodes] (edit-page! c token path title nodes {}))
  ([c token path title nodes opts]
   (let [content-json (json/generate-string (vec nodes))
         params       (cond-> {"access_token"   token
                               "title"          (subs title 0 (min 256 (count title)))
                               "content"        content-json
                               "return_content" (str (boolean (:return-content? opts)))}
                        (:author-name opts) (assoc "author_name" (:author-name opts))
                        (:author-url  opts) (assoc "author_url"  (:author-url  opts)))]
     (client/post! c (str "/editPage/" path) params))))

(defn safe-edit-page!
  ([c t p title nodes]      (wrap #(edit-page! c t p title nodes)))
  ([c t p title nodes opts] (wrap #(edit-page! c t p title nodes opts))))

(defn get-page!
  "Fetch a page by path.
   opts keys: :return-content? — include :content nodes (default false)"
  ([c path]      (get-page! c path {}))
  ([c path opts] (client/post! c (str "/getPage/" path)
                               {"return_content" (str (boolean (:return-content? opts)))})))

(defn safe-get-page!
  ([c p]      (wrap #(get-page! c p)))
  ([c p opts] (wrap #(get-page! c p opts))))

(defn get-page-list!
  "List pages for this access token.
   opts keys: :offset (default 0) :limit 1–200 (default 50)
   Returns {:total_count N :pages [...]}"
  ([c token]      (get-page-list! c token {}))
  ([c token opts] (client/post! c "/getPageList"
                                {"access_token" token
                                 "offset"       (str (get opts :offset 0))
                                 "limit"        (str (get opts :limit  50))})))

(defn safe-get-page-list!
  ([c t]      (wrap #(get-page-list! c t)))
  ([c t opts] (wrap #(get-page-list! c t opts))))

;; --- Views -------------------------------------------------------------------

(defn get-views!
  "Get view count for path.
   opts keys (progressively finer granularity): :year :month :day :hour
   Returns {:views N}"
  ([c path]      (get-views! c path {}))
  ([c path opts] (client/get! c (str "/getViews/" path)
                              (into {}
                                    (for [[k v] opts :when (some? v)]
                                      [(model/kebab->snake k) (str v)])))))

(defn safe-get-views!
  ([c p]      (wrap #(get-views! c p)))
  ([c p opts] (wrap #(get-views! c p opts))))
