(ns clj-telegraph-api.model
  "Data constants, validators, and coercion helpers for the Telegraph API."
  (:require [clojure.string :as str]))

;; Tags accepted by Telegraph's content renderer.
(def allowed-tags
  #{"a" "aside" "b" "blockquote" "br" "code" "em" "figcaption"
    "figure" "h3" "h4" "hr" "i" "iframe" "img" "li" "ol" "p"
    "pre" "s" "strong" "u" "ul" "video"})

;; Self-closing tags that have no children.
(def void-tags #{"br" "hr" "img"})

;; Block-level tags; used when hoisting children of unsupported elements.
(def block-tags
  #{"aside" "blockquote" "figure" "li" "ol" "p" "pre" "ul" "h3" "h4"})

;; Per-tag attribute allowlists (string keys).
(def allowed-attrs
  {"a"      #{"href"}
   "img"    #{"src" "alt"}
   "iframe" #{"src" "width" "height" "frameborder" "allowfullscreen"}
   "video"  #{"src" "autoplay" "loop"}})

;; --- Validators --------------------------------------------------------------

(defn text-node?    [n] (string? n))
(defn element-node? [n] (and (map? n) (string? (:tag n))))
(defn valid-node?   [n] (or (text-node? n) (element-node? n)))
(defn valid-nodes?  [ns] (and (sequential? ns) (seq ns) (every? valid-node? ns)))
(defn valid-access-token? [t] (and (string? t) (seq t)))

;; --- Coercion ----------------------------------------------------------------

(defn kebab->snake
  "Convert a keyword (or string) with hyphens to an underscore string.
   :author-name => \"author_name\""
  [k]
  (str/replace (name k) "-" "_"))

(defn filter-attrs
  "Return only the allowed attributes for tag from attrs.
   Accepts keyword or string keys; always returns a string-key map."
  [tag attrs]
  (let [allowed (get allowed-attrs tag #{})]
    (into {}
          (for [[k v] attrs
                :let  [ks (name k)]
                :when (contains? allowed ks)]
            [ks v]))))
