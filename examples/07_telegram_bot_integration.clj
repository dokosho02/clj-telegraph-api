;; examples/07_telegram_bot_integration.clj — Telegram bot + Telegraph
;;
;; A polling bot: user sends a URL, bot fetches it, publishes to Telegraph,
;; and replies with the Instant View link.
;;
;; Extra deps required in deps.edn:
;;   com.github.marksto/clj-tg-bot-api {:mvn/version "0.9.2"}
;;   com.github.oliyh/martian-clj-http {:mvn/version "0.1.28"}
;;
;; Usage:
;;   clojure -M examples/07_telegram_bot_integration.clj BOT_TOKEN TELEGRAPH_TOKEN

(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node]
         '[marksto.clj-tg-bot-api.core :as tg]
         '[hato.client :as hato])

(def bot-token (or (first  *command-line-args*)
                   (throw (Exception. "Usage: ... BOT_TOKEN TELEGRAPH_TOKEN"))))
(def tph-token (or (second *command-line-args*)
                   (throw (Exception. "Usage: ... BOT_TOKEN TELEGRAPH_TOKEN"))))

(def tg-client  (tg/->client {:bot-token bot-token}))
(def tph-client (telegraph/make-client))

(let [me (tg/make-request! tg-client :get-me)]
  (println (str "✅ Logged in as @" (:username me) " (" (:first_name me) ")")))

(defn url? [s]
  (and (string? s)
       (or (clojure.string/starts-with? s "http://")
           (clojure.string/starts-with? s "https://"))))

(defn reply! [chat-id text]
  (tg/make-request! tg-client :send-message
                    {:chat-id    chat-id
                     :text       text
                     :parse-mode "HTML"}))

(defn handle-url! [chat-id url]
  (reply! chat-id "⏳ Fetching article...")
  (let [{:keys [ok result error]}
        (try
          (let [html  (:body (hato/get url {:as              :string
                                             :connect-timeout 10000
                                             :request-timeout 10000}))
                title (or (second (re-find #"<title[^>]*>([^<]+)</title>" html))
                          "Untitled")
                nodes (telegraph/html->nodes html {:base-url url})]
            (telegraph/safe-create-page! tph-client tph-token title nodes))
          (catch Exception e
            {:ok false :error (ex-message e)}))]
    (if ok
      (reply! chat-id (str "✅ <b>" (:title result) "</b>\n\n"
                            "<a href=\"" (:url result) "\">Open Instant View</a>"))
      (reply! chat-id (str "❌ Failed: " error)))))

(defn handle-update! [update]
  (let [chat-id (get-in update [:message :chat :id])
        text    (get-in update [:message :text])]
    (when chat-id
      (cond
        (= text "/start")
        (reply! chat-id "👋 Send me any article URL and I will create an Instant View page.")

        (url? text)
        (handle-url! chat-id text)

        (some? text)
        (reply! chat-id "Please send a valid URL (starting with http:// or https://)")))))

(println "Bot polling... (Ctrl+C to stop)")
(loop [offset 0]
  (let [updates (try
                  (tg/make-request! tg-client :get-updates
                                    {:offset          offset
                                     :timeout         30
                                     :allowed-updates ["message"]})
                  (catch Exception e
                    (println "getUpdates error:" (ex-message e))
                    (Thread/sleep 3000)
                    []))]
    (doseq [u updates]
      (try (handle-update! u)
           (catch Exception e
             (println "Handler error:" (ex-message e)))))
    (recur (if (seq updates)
             (-> updates last :update_id inc)
             offset))))
