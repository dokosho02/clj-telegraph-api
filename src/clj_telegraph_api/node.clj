(ns clj-telegraph-api.node
  "Telegraph Node constructors and HTML-to-Node conversion.

  A Telegraph Node is one of:
    - a String                               (text node)
    - {:tag \"p\" :children [Node ...]}        (element, no attrs)
    - {:tag \"a\" :attrs {\"href\" \"...\"} ...}   (element with attrs)

  All attrs maps use string keys. Only Telegraph-allowed attributes
  are kept; everything else is silently dropped."
  (:require [clj-telegraph-api.model :as model]
            [clojure.string :as str])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document Element TextNode Node)))

;; --- Low-level constructor ---------------------------------------------------

(defn element
  "Build a Telegraph element node map.
   attrs may use keyword or string keys; only allowed attrs are kept.

   (element \"p\" [\"hello\"])
   => {:tag \"p\" :children [\"hello\"]}

   (element \"a\" {:href \"https://x.com\"} [\"label\"])
   => {:tag \"a\" :attrs {\"href\" \"https://x.com\"} :children [\"label\"]}"
  ([tag]                  (element tag {} []))
  ([tag children-or-attrs]
   (if (map? children-or-attrs)
     (element tag children-or-attrs [])
     (element tag {} children-or-attrs)))
  ([tag attrs children]
   (let [filtered (model/filter-attrs tag attrs)]
     (cond-> {:tag tag}
       (seq filtered) (assoc :attrs filtered)
       (seq children) (assoc :children (vec children))))))

;; --- Semantic helpers --------------------------------------------------------

(defn p          "Paragraph."              [& ch] (element "p"          (vec ch)))
(defn h3         "Heading level 3."        [& ch] (element "h3"         (vec ch)))
(defn h4         "Heading level 4."        [& ch] (element "h4"         (vec ch)))
(defn b          "Bold."                   [& ch] (element "b"          (vec ch)))
(defn strong     "Strong."                 [& ch] (element "strong"     (vec ch)))
(defn em         "Emphasis / italic."      [& ch] (element "em"         (vec ch)))
(defn i          "Italic."                 [& ch] (element "i"          (vec ch)))
(defn s          "Strikethrough."          [& ch] (element "s"          (vec ch)))
(defn u          "Underline."              [& ch] (element "u"          (vec ch)))
(defn code       "Inline code."            [t]    (element "code"       [t]))
(defn pre        "Preformatted / block."   [t]    (element "pre"        [t]))
(defn blockquote "Block quote."            [& ch] (element "blockquote" (vec ch)))
(defn aside      "Aside / caption."        [& ch] (element "aside"      (vec ch)))
(defn figure     "Figure wrapper."         [& ch] (element "figure"     (vec ch)))
(defn figcaption "Figure caption."         [& ch] (element "figcaption" (vec ch)))
(defn li         "List item."              [& ch] (element "li"         (vec ch)))
(defn ul         "Unordered list."         [& it] (element "ul"         (vec it)))
(defn ol         "Ordered list."           [& it] (element "ol"         (vec it)))
(defn br         "Line break."             []     {:tag "br"})
(defn hr         "Horizontal rule."        []     {:tag "hr"})

(defn link
  "Anchor node.
   (link \"Click here\" \"https://example.com\")"
  [text href]
  (element "a" {:href href} [text]))

(defn img
  "Image node. src must be an absolute URL.
   (img \"https://example.com/photo.jpg\")
   (img \"https://example.com/photo.jpg\" \"alt text\")"
  ([src]     (element "img" {:src src}          []))
  ([src alt] (element "img" {:src src :alt alt} [])))

(defn iframe
  "Video embed node. Wrap src in a figure as required by Telegraph.
   Use a youtu.be short URL or full YouTube URL for YouTube embeds.
   (iframe \"https://youtu.be/VIDEO_ID\")"
  [src]
  {:tag      "figure"
   :children [{:tag "iframe" :attrs {"src" src}}]})

;; --- HTML -> Node conversion -------------------------------------------------

(defn- absolute-url
  "Resolve url against base. Returns url unchanged when already absolute
   or when base is blank."
  [url base]
  (if (or (str/blank? base)
          (str/blank? url)
          (str/starts-with? url "http://")
          (str/starts-with? url "https://")
          (str/starts-with? url "//"))
    url
    (str (.resolve (java.net.URI. base) url))))

(defn- el-attrs
  "Extract jsoup element attributes as a string-key map."
  [^Element el]
  (into {} (for [a (.attributes el)] [(.getKey a) (.getValue a)])))

(declare node->tg)

(defn- el->tg [^Element el base]
  (let [tag (str (.tagName el))
        ch  (->> (.childNodes el) (map #(node->tg % base)) (remove nil?) vec)]
    (cond
      (contains? model/void-tags tag)
      (let [raw   (cond-> (el-attrs el)
                    (and (= tag "img") (seq (get (el-attrs el) "src")))
                    (update "src" absolute-url base))
            attrs (model/filter-attrs tag raw)]
        (cond-> {:tag tag} (seq attrs) (assoc :attrs attrs)))

      (contains? model/allowed-tags tag)
      (let [raw   (cond-> (el-attrs el)
                    (and (= tag "a")      (seq (get (el-attrs el) "href"))) (update "href" absolute-url base)
                    (and (= tag "iframe") (seq (get (el-attrs el) "src")))  (update "src"  absolute-url base)
                    (and (= tag "video")  (seq (get (el-attrs el) "src")))  (update "src"  absolute-url base))
            attrs (model/filter-attrs tag raw)]
        (cond-> {:tag tag}
          (seq attrs) (assoc :attrs attrs)
          (seq ch)    (assoc :children ch)))

      (seq ch)
      (if (= 1 (count ch)) (first ch) {:tag "p" :children ch})

      :else nil)))

(defn- node->tg [^Node n base]
  (cond
    (instance? TextNode n)
    (let [t (.text ^TextNode n)] (when-not (str/blank? t) t))
    (instance? Element n) (el->tg ^Element n base)
    :else nil))

(defn html->nodes
  "Convert an HTML string to a Telegraph Node vector.

   Options:
     :base-url — base URL used to resolve relative href/src values.

   Unsupported tags (div, span, section, etc.) are transparent: their
   children are hoisted up. Script and style content is dropped entirely.

   Example:
     (html->nodes \"<p>Hello <b>world</b></p>\")
     ;=> [{:tag \"p\" :children [\"Hello \" {:tag \"b\" :children [\"world\"]}]}]"
  ([html] (html->nodes html {}))
  ([html {:keys [base-url] :or {base-url ""}}]
   (let [^Document doc (if (seq base-url)
                         (Jsoup/parse html ^String base-url)
                         (Jsoup/parseBodyFragment html))]
     (->> (.childNodes (.body doc))
          (map #(node->tg % base-url))
          (remove nil?)
          vec))))
