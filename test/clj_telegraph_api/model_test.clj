(ns clj-telegraph-api.model-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-telegraph-api.model :as model]))

(deftest kebab->snake-test
  (is (= "short_name"     (model/kebab->snake :short-name)))
  (is (= "author_name"    (model/kebab->snake :author-name)))
  (is (= "access_token"   (model/kebab->snake :access-token)))
  (is (= "return_content" (model/kebab->snake :return-content))))

(deftest filter-attrs-string-keys-test
  (testing "keeps allowed attrs, drops disallowed"
    (is (= {"href" "https://x.com"}
           (model/filter-attrs "a" {"href" "https://x.com" "onclick" "evil()"})))
    (is (= {"src" "img.jpg"}
           (model/filter-attrs "img" {"src" "img.jpg" "onerror" "evil()"}))))
  (testing "returns empty map for unknown tag"
    (is (= {} (model/filter-attrs "script" {"src" "evil.js"})))))

(deftest filter-attrs-keyword-keys-test
  (testing "accepts keyword keys, always returns string keys"
    (is (= {"href" "https://x.com"}
           (model/filter-attrs "a" {:href "https://x.com" :onclick "evil()"})))
    (is (= {"src" "img.jpg" "alt" "photo"}
           (model/filter-attrs "img" {:src "img.jpg" :alt "photo" :onerror "x"})))))

(deftest valid-node-test
  (is (model/text-node? "hello"))
  (is (not (model/text-node? {:tag "p"})))
  (is (model/element-node? {:tag "p"}))
  (is (model/valid-nodes? ["hello" {:tag "p" :children ["world"]}]))
  (is (not (model/valid-nodes? []))))
