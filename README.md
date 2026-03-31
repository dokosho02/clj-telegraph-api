# clj-telegraph-api

Idiomatic Clojure client for the [Telegraph API](https://telegra.ph/api).
Publish Instant View pages for Telegram from your Clojure application.

[![Clojure CI](https://img.shields.io/badge/clojure-1.12-blue)](https://clojure.org)

## Installation

```clojure
;; deps.edn
org.clojars.dokosho02/clj-telegraph-api {:mvn/version "0.1.6"}
```

Dependencies: `hato` `cheshire` `jsoup` `slf4j-nop`

## Quick start

```clojure
(require '[clj-telegraph-api.core :as telegraph]
         '[clj-telegraph-api.node :as node])

;; Create a client (plain map, no global state)
(def client (telegraph/make-client))

;; Create an account — do this once and store the token
(def account (telegraph/create-account! client "MyBot"))
(def token   (:access_token account))

;; Build content with node helpers
(def page
  (telegraph/create-page! client token "My Article"
    [(node/h3 "Introduction")
     (node/p  "Hello " (node/b "world") "!")
     (node/img "https://example.com/photo.jpg")
     (node/p  (node/link "Read more" "https://example.com"))]))

(println (:url page))
;; => https://telegra.ph/My-Article-01-01
;; Send this URL in Telegram to open as Instant View (mobile app only).
```

## API Reference

### Client

```clojure
(telegraph/make-client)
(telegraph/make-client {:timeout    15000
                         :proxy-host "127.0.0.1"
                         :proxy-port 1080
                         :insecure?  false})
```

### Account

```clojure
;; Create (returns map with :access_token — store it)
(telegraph/create-account! client "MyBot")
(telegraph/create-account! client "MyBot" {:author-name "Alice"
                                            :author-url  "https://alice.dev"})

;; Edit
(telegraph/edit-account! client token {:author-name "Bob"})

;; Get info
(telegraph/get-account! client token)
(telegraph/get-account! client token {:fields [:short-name :page-count]})

;; Revoke and replace token (old token is invalidated immediately)
(def new-account (telegraph/revoke-token! client token))
```

### Pages

```clojure
;; Create
(telegraph/create-page! client token "Title" nodes)
(telegraph/create-page! client token "Title" nodes
                         {:author-name "Alice" :return-content? true})

;; Edit (only pages created with this token)
(telegraph/edit-page! client token "My-Page-01-01" "New Title" new-nodes)

;; Get
(telegraph/get-page! client "My-Page-01-01")
(telegraph/get-page! client "My-Page-01-01" {:return-content? true})

;; List
(telegraph/get-page-list! client token)
(telegraph/get-page-list! client token {:offset 50 :limit 50})
```

### Views

```clojure
(telegraph/get-views! client "My-Page-01-01")
(telegraph/get-views! client "My-Page-01-01" {:year 2024 :month 6 :day 15})
```

### safe- variants

Every `!` function has a `safe-` counterpart that returns
`{:ok true :result ...}` or `{:ok false :error ...}` instead of throwing:

```clojure
(let [{:keys [ok result error]}
      (telegraph/safe-create-page! client token "Title" nodes)]
  (if ok
    (println "Published:" (:url result))
    (println "Error:" error)))
```

### Error handling

```clojure
(try
  (telegraph/create-page! client "bad-token" "Title" nodes)
  (catch clojure.lang.ExceptionInfo e
    (let [{:keys [type error endpoint]} (ex-data e)]
      (case type
        :telegraph/api-error  (println "API error:" error "at" endpoint)
        :telegraph/http-error (println "Network error:" (ex-message e))))))
```

`ex-data` keys: `:type` `:error` `:endpoint` `:params` (`:cause` for http errors)

### Node helpers

```clojure
(require '[clj-telegraph-api.node :as node])

;; Headings (Telegraph supports h3 and h4 only)
(node/h3 "Section")
(node/h4 "Subsection")

;; Inline
(node/p "text")
(node/p "Hello " (node/b "bold") " and " (node/em "italic"))
(node/b "bold")       (node/strong "strong")
(node/em "em")        (node/i "italic")
(node/s "strike")     (node/u "underline")
(node/code "inline")  (node/pre "code block")
(node/br)             (node/hr)

;; Links and media
(node/link "label" "https://example.com")
(node/img  "https://example.com/photo.jpg")
(node/img  "https://example.com/photo.jpg" "alt text")
(node/figure (node/img "https://example.com/p.jpg")
             (node/figcaption "Caption"))

;; YouTube / video embed — use youtu.be short URL
;; (node/iframe "https://youtu.be/VIDEO_ID")

;; Blocks
(node/blockquote "A quote.")
(node/aside "Sidebar note.")

;; Lists
(node/ul (node/li "item 1") (node/li "item 2"))
(node/ol (node/li "step 1") (node/li "step 2"))

;; HTML string to Telegraph Node vector
(telegraph/html->nodes "<p>Hello <b>world</b></p>")
(telegraph/html->nodes html {:base-url "https://example.com"})
```

## Examples

See [`examples/README.md`](examples/README.md) for runnable scripts covering
every API function.

## Project layout

```
src/clj_telegraph_api/
├── core.clj    — public API
├── client.clj  — HTTP transport (hato, post! and get!)
├── model.clj   — constants, validators, coercion
└── node.clj    — node constructors + html->nodes

test/clj_telegraph_api/
├── core_test.clj
├── client_test.clj
├── model_test.clj
└── node_test.clj

examples/
├── 01_account.clj
├── 02_node_helpers.clj
├── 03_pages.clj
├── 04_views.clj
├── 05_html_to_nodes.clj
├── 06_error_handling.clj
├── 07_telegram_bot_integration.clj
└── README.md
```

## Running tests

```bash
clojure -M:test
```

## License

MIT

## Build & Deploy

### Build the jar

```bash
clojure -T:build jar
# => target/clj-telegraph-api-0.1.6.jar
```

### Install to local Maven (~/.m2)

Useful for depending on it from other local projects before publishing.

```bash
clojure -T:build install
```

### Deploy to Clojars

Set your Clojars credentials as environment variables, then run deploy:

```bash
export CLOJARS_USERNAME=...
export CLOJARS_PASSWORD=...   # deploy token, not login password

clojure -T:build deploy
```


### Clean

```bash
clojure -T:build clean
```
