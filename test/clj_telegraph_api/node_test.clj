(ns clj-telegraph-api.node-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-telegraph-api.node :as node]))

;; --- helpers -----------------------------------------------------------------

(deftest p-test
  (is (= {:tag "p" :children ["hello"]} (node/p "hello")))
  (is (= {:tag "p" :children ["a" "b"]} (node/p "a" "b"))))

(deftest h3-test (is (= {:tag "h3" :children ["H"]} (node/h3 "H"))))
(deftest h4-test (is (= {:tag "h4" :children ["H"]} (node/h4 "H"))))

(deftest link-test
  (is (= {:tag "a" :attrs {"href" "https://x.com"} :children ["Click"]}
         (node/link "Click" "https://x.com"))))

(deftest img-test
  (is (= {:tag "img" :attrs {"src" "https://x.com/img.jpg"}}
         (node/img "https://x.com/img.jpg")))
  (is (= {:tag "img" :attrs {"src" "https://x.com/img.jpg" "alt" "photo"}}
         (node/img "https://x.com/img.jpg" "photo"))))

(deftest br-test (is (= {:tag "br"} (node/br))))
(deftest hr-test (is (= {:tag "hr"} (node/hr))))

(deftest blockquote-test
  (is (= {:tag "blockquote" :children ["q"]} (node/blockquote "q"))))

(deftest code-test (is (= {:tag "code" :children ["x"]} (node/code "x"))))
(deftest pre-test  (is (= {:tag "pre"  :children ["x"]} (node/pre "x"))))

(deftest ul-test
  (is (= {:tag "ul" :children [{:tag "li" :children ["a"]}
                                {:tag "li" :children ["b"]}]}
         (node/ul (node/li "a") (node/li "b")))))

(deftest figure-test
  (is (= {:tag "figure"
          :children [{:tag "img" :attrs {"src" "https://x.com/p.jpg"}}
                     {:tag "figcaption" :children ["Cap"]}]}
         (node/figure (node/img "https://x.com/p.jpg") (node/figcaption "Cap")))))

(deftest iframe-test
  (testing "iframe is wrapped in a figure node"
    (let [n (node/iframe "https://youtu.be/VIDEO_ID")]
      (is (= "figure" (:tag n)))
      (is (= "iframe" (:tag (first (:children n)))))
      (is (= "https://youtu.be/VIDEO_ID"
             (get-in n [:children 0 :attrs "src"])))))
  (testing "non-youtube src is passed through unchanged"
    (let [n (node/iframe "https://vimeo.com/embed/123")]
      (is (= "figure" (:tag n)))
      (is (= "https://vimeo.com/embed/123"
             (get-in n [:children 0 :attrs "src"]))))))

;; --- html->nodes -------------------------------------------------------------

(deftest html->nodes-basic-test
  (testing "paragraph"
    (is (= [{:tag "p" :children ["Hello"]}]
           (node/html->nodes "<p>Hello</p>"))))

  (testing "bold inside p"
    (is (= [{:tag "p" :children ["Hello " {:tag "b" :children ["world"]}]}]
           (node/html->nodes "<p>Hello <b>world</b></p>"))))

  (testing "heading"
    (is (= [{:tag "h3" :children ["Title"]}]
           (node/html->nodes "<h3>Title</h3>"))))

  (testing "link href preserved"
    (let [a (get-in (node/html->nodes "<p><a href=\"https://x.com\">Click</a></p>")
                    [0 :children 0])]
      (is (= "a" (:tag a)))
      (is (= "https://x.com" (get-in a [:attrs "href"])))))

  (testing "img src preserved"
    (let [img (first (node/html->nodes "<img src=\"https://x.com/img.jpg\">"))]
      (is (= "img" (:tag img)))
      (is (= "https://x.com/img.jpg" (get-in img [:attrs "src"])))))

  (testing "unsupported tag hoists children"
    (let [nodes (node/html->nodes "<div><p>text</p></div>")]
      (is (some #(= "p" (:tag %)) nodes))))

  (testing "script content is dropped"
    (let [nodes (node/html->nodes "<p>ok</p><script>evil()</script>")]
      (is (every? #(not= "script" (:tag %)) nodes))))

  (testing "onclick attr is stripped"
    (let [a (first (node/html->nodes "<a href=\"https://x.com\" onclick=\"evil()\">x</a>"))]
      (is (nil? (get-in a [:attrs "onclick"])))))

  (testing "onerror attr is stripped"
    (let [img (first (node/html->nodes "<img src=\"https://x.com/img.jpg\" onerror=\"evil()\">"))]
      (is (nil? (get-in img [:attrs "onerror"]))))))

(deftest html->nodes-base-url-test
  (testing "resolves relative img src"
    (let [img (first (node/html->nodes "<img src=\"/photo.jpg\">"
                                        {:base-url "https://example.com"}))]
      (is (= "https://example.com/photo.jpg" (get-in img [:attrs "src"])))))

  (testing "resolves relative href"
    (let [a (first (node/html->nodes "<a href=\"/about\">About</a>"
                                      {:base-url "https://example.com"}))]
      (is (= "https://example.com/about" (get-in a [:attrs "href"])))))

  (testing "absolute URL is unchanged"
    (let [img (first (node/html->nodes "<img src=\"https://cdn.example.com/img.jpg\">"
                                        {:base-url "https://example.com"}))]
      (is (= "https://cdn.example.com/img.jpg" (get-in img [:attrs "src"]))))))
