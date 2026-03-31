;; examples/02_node_helpers.clj — All node helpers demo
;;
;; Builds a page using every node constructor and publishes it to Telegraph.
;; Paste the printed URL into Telegram to open as Instant View.
;;
;; Usage:
;;   clojure -M examples/02_node_helpers.clj YOUR_ACCESS_TOKEN

(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node])

(def token  (or (first *command-line-args*)
                (throw (Exception. "Usage: clojure -M examples/02_node_helpers.clj TOKEN"))))
(def client (telegraph/make-client))

(def content
  [(node/h3 "Text and Inline Elements")
   (node/p "Plain paragraph. "
           (node/b "Bold. ")
           (node/strong "Strong. ")
           (node/em "Emphasis. ")
           (node/i "Italic. ")
           (node/s "Strikethrough. ")
           (node/u "Underline. ")
           (node/code "inline-code") ".")
   (node/p (node/link "A hyperlink to clojure.org" "https://clojure.org"))

   (node/h3 "Headings")
   (node/h4 "This is h4 — the smallest heading Telegraph supports")

   (node/h3 "Image with Caption")
   (node/figure
    (node/img "https://www.clojure.org/images/clojure-logo-120b.png"
              "Clojure logo")
    (node/figcaption "The Clojure logo"))

   ;; failed
   ;; (node/h3 "YouTube Embed")
   ;; (node/iframe "https://youtu.be/ROaS9F4A2eQ")

   (node/h3 "Lists")
   (node/ul
    (node/li "Unordered item one")
    (node/li "Unordered item two")
    (node/li (node/link "Linked item" "https://telegra.ph/api")))
   (node/ol
    (node/li "Step one")
    (node/li "Step two")
    (node/li "Step three"))

   (node/h3 "Code Block")
   (node/pre "(defn greet [name]\n  (str \"Hello, \" name \"!\"))")

   (node/h3 "Blockquote and Aside")
   (node/blockquote "Any sufficiently advanced technology is indistinguishable from magic.")
   (node/aside "— Arthur C. Clarke")

   (node/hr)
   (node/p "End of node helpers demo.")])

(println "Publishing node helpers demo...")
(def page (telegraph/create-page! client token "Node Helpers Demo" content))
(println "\n✅ Published!")
(println "URL  :" (:url page))
(println "Path :" (:path page))
(println "\nSend the URL above in any Telegram chat to open as Instant View.")
