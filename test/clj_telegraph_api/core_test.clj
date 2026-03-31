(ns clj-telegraph-api.core-test
  (:require [clojure.test :refer [deftest is]]
            [clj-telegraph-api.core   :as telegraph]
            [clj-telegraph-api.client :as client]
            [clj-telegraph-api.node   :as node]
            [clojure.string           :as str]
            [cheshire.core            :as json]))

(def ^:private fake-account
  {:short_name   "TestBot"
   :author_name  "Tester"
   :author_url   ""
   :access_token "abc123token"
   :auth_url     "https://edit.telegra.ph/auth/abc"})

(def ^:private fake-page
  {:path     "Test-Page-01-01"
   :url      "https://telegra.ph/Test-Page-01-01"
   :title    "Test Page"
   :views    0
   :can_edit true})

(defn- ok!   [result] (fn [_c _ep _p] result))
(defn- fail! [error]
  (fn [_c _ep _p]
    (throw (ex-info (str "Telegraph API error: " error)
                    {:type :telegraph/api-error :error error}))))

;; --- Account -----------------------------------------------------------------

(deftest create-account-test
  (with-redefs [client/post! (ok! fake-account)]
    (let [acc (telegraph/create-account! (telegraph/make-client) "TestBot")]
      (is (= "abc123token" (:access_token acc))))))

(deftest create-account-opts-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (is (= "Tester" (get params "author_name")))
                               fake-account)]
    (telegraph/create-account! (telegraph/make-client) "TestBot"
                               {:author-name "Tester"})))

(deftest safe-create-account-ok-test
  (with-redefs [client/post! (ok! fake-account)]
    (let [r (telegraph/safe-create-account! (telegraph/make-client) "TestBot")]
      (is (:ok r))
      (is (= fake-account (:result r))))))

(deftest safe-create-account-fail-test
  (with-redefs [client/post! (fail! "SHORT_NAME_INVALID")]
    (let [r (telegraph/safe-create-account! (telegraph/make-client) "")]
      (is (false? (:ok r)))
      (is (string? (:error r))))))

(deftest get-account-test
  (with-redefs [client/post! (ok! fake-account)]
    (is (= "TestBot"
           (:short_name (telegraph/get-account! (telegraph/make-client) "tok"))))))

(deftest get-account-fields-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (is (contains? params "fields"))
                               (is (= ["short_name" "page_count"]
                                      (json/parse-string (get params "fields"))))
                               fake-account)]
    (telegraph/get-account! (telegraph/make-client) "tok"
                            {:fields [:short-name :page-count]})))

(deftest revoke-token-test
  (let [new-acc (assoc fake-account :access_token "newtoken")]
    (with-redefs [client/post! (ok! new-acc)]
      (is (= "newtoken"
             (:access_token (telegraph/revoke-token! (telegraph/make-client) "old")))))))

;; --- Pages -------------------------------------------------------------------

(deftest create-page-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (let [content (get params "content")]
                                 (is (string? content))
                                 (when (string? content)
                                   (is (sequential? (json/parse-string content true)))))
                               fake-page)]
    (telegraph/create-page! (telegraph/make-client) "tok" "Title"
                            [(node/p "Hello world")])))

(deftest create-page-title-truncated-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (is (<= (count (get params "title")) 256))
                               fake-page)]
    (telegraph/create-page! (telegraph/make-client) "tok"
                            (apply str (repeat 300 "x"))
                            [(node/p "body")])))

(deftest create-page-opts-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (is (= "Alice" (get params "author_name")))
                               fake-page)]
    (telegraph/create-page! (telegraph/make-client) "tok" "Title"
                            [(node/p "body")] {:author-name "Alice"})))

(deftest create-page-return-content-test
  (with-redefs [client/post! (fn [_c _ep params]
                               (is (= "true" (get params "return_content")))
                               fake-page)]
    (telegraph/create-page! (telegraph/make-client) "tok" "Title"
                            [(node/p "body")] {:return-content? true})))

(deftest safe-create-page-test
  (with-redefs [client/post! (ok! fake-page)]
    (let [r (telegraph/safe-create-page! (telegraph/make-client) "tok" "T"
                                         [(node/p "b")])]
      (is (:ok r))
      (is (= fake-page (:result r))))))

(deftest edit-page-test
  (with-redefs [client/post! (fn [_c ep params]
                               (is (str/includes? ep "editPage"))
                               (is (string? (get params "content")))
                               fake-page)]
    (telegraph/edit-page! (telegraph/make-client) "tok" "Test-Page-01-01"
                          "New Title" [(node/p "new")])))

(deftest get-page-test
  (with-redefs [client/post! (ok! fake-page)]
    (is (= "Test Page"
           (:title (telegraph/get-page! (telegraph/make-client) "Test-Page-01-01"))))))

(deftest get-page-list-test
  (with-redefs [client/post! (ok! {:total_count 2 :pages [fake-page fake-page]})]
    (let [r (telegraph/get-page-list! (telegraph/make-client) "tok")]
      (is (= 2 (:total_count r))))))

;; --- Views -------------------------------------------------------------------

(deftest get-views-test
  (with-redefs [client/get! (ok! {:views 42})]
    (is (= 42 (:views (telegraph/get-views! (telegraph/make-client) "p"))))))

(deftest get-views-date-test
  (with-redefs [client/get! (fn [_c _ep params]
                              (is (= "2024" (get params "year")))
                              {:views 7})]
    (is (= 7 (:views (telegraph/get-views! (telegraph/make-client) "p"
                                           {:year 2024 :month 6}))))))

;; --- html->nodes re-export ---------------------------------------------------

(deftest html->nodes-reexport-test
  (is (= [(node/p "Hello")] (telegraph/html->nodes "<p>Hello</p>"))))
