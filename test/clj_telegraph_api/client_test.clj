(ns clj-telegraph-api.client-test
  (:require [clojure.test :refer [deftest is]]
            [clj-telegraph-api.client :as client]
            [hato.client :as hato]
            [cheshire.core :as json]))

(defn- json-stream [m]
  (java.io.ByteArrayInputStream.
   (.getBytes (json/generate-string m) "UTF-8")))

(deftest make-client-defaults-test
  (let [c (client/make-client)]
    (is (= "https://api.telegra.ph" (:base-url c)))
    (is (= 10000 (:timeout c)))))

(deftest make-client-opts-test
  (let [c (client/make-client {:base-url   "http://local"
                                :timeout    5000
                                :proxy-host "127.0.0.1"
                                :proxy-port 1080})]
    (is (= "http://local" (:base-url c)))
    (is (= 5000 (:timeout c)))
    (is (= "127.0.0.1" (:proxy-host c)))))

(deftest post-success-test
  (with-redefs [hato/post (fn [_ _]
                            {:body (json-stream {:ok true :result {:url "https://telegra.ph/T"}})})]
    (is (= {:url "https://telegra.ph/T"}
           (client/post! (client/make-client) "/createPage" {})))))

(deftest post-api-error-test
  (with-redefs [hato/post (fn [_ _]
                            {:body (json-stream {:ok false :error "UNAUTHORIZED"})})]
    (try
      (client/post! (client/make-client) "/ep" {})
      (is false "should throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :telegraph/api-error (:type (ex-data e))))))))

(deftest post-http-error-test
  (with-redefs [hato/post (fn [_ _] (throw (java.io.IOException. "refused")))]
    (try
      (client/post! (client/make-client) "/ep" {})
      (is false "should throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :telegraph/http-error (:type (ex-data e))))))))

(deftest get-success-test
  (with-redefs [hato/get (fn [_ _]
                           {:body (json-stream {:ok true :result {:views 42}})})]
    (is (= {:views 42}
           (client/get! (client/make-client) "/getViews/Some-Path" {})))))

(deftest get-api-error-test
  (with-redefs [hato/get (fn [_ _]
                           {:body (json-stream {:ok false :error "PAGE_NOT_FOUND"})})]
    (try
      (client/get! (client/make-client) "/getViews/bad" {})
      (is false "should throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :telegraph/api-error (:type (ex-data e))))))))
