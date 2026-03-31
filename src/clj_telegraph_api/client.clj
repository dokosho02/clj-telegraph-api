(ns clj-telegraph-api.client
  "Low-level HTTP transport for the Telegraph API.
   Provides post! (form-encoded) and get! (query-string) over hato."
  (:require [hato.client :as hato]
            [cheshire.core :as json]))

(def ^:private api-base "https://api.telegra.ph")

(defn make-client
  "Return a Telegraph client configuration map.

   Options (all optional):
     :base-url   — API base URL (default: https://api.telegra.ph)
     :timeout    — connect + request timeout in ms (default: 10000)
     :proxy-host — HTTP proxy hostname
     :proxy-port — HTTP proxy port (integer)
     :insecure?  — disable TLS verification (default: false)"
  ([] (make-client {}))
  ([{:keys [base-url timeout proxy-host proxy-port insecure?]
     :or   {base-url api-base timeout 10000}}]
   (cond-> {:base-url base-url :timeout timeout}
     proxy-host (assoc :proxy-host proxy-host)
     proxy-port (assoc :proxy-port proxy-port)
     insecure?  (assoc :insecure? true))))

(defn- base-opts
  "Build hato options shared by all requests."
  [client]
  (cond-> {:as              :stream
           :connect-timeout (:timeout client)
           :request-timeout (:timeout client)}
    (:proxy-host client) (assoc :proxy-host (:proxy-host client))
    (:proxy-port client) (assoc :proxy-port (:proxy-port client))
    (:insecure?  client) (assoc :insecure? true)))

(defn- parse-body
  "Parse the JSON response body and return :result, or throw on API error."
  [resp endpoint params]
  (let [body (-> resp :body slurp (json/parse-string true))]
    (if (:ok body)
      (:result body)
      (throw (ex-info (str "Telegraph API error: " (:error body))
                      {:type     :telegraph/api-error
                       :error    (str (:error body))
                       :endpoint endpoint
                       :params   params})))))

(defn post!
  "POST form-encoded params to endpoint.
   Returns :result on success; throws ExceptionInfo on failure."
  [client endpoint params]
  (let [url  (str (:base-url client) endpoint)
        opts (assoc (base-opts client)
                    :form-params  params
                    :content-type :x-www-form-urlencoded)]
    (try
      (parse-body (hato/post url opts) endpoint params)
      (catch clojure.lang.ExceptionInfo e (throw e))
      (catch Exception e
        (throw (ex-info (str "Telegraph HTTP error: " (ex-message e))
                        {:type    :telegraph/http-error
                         :endpoint endpoint
                         :params  params
                         :cause   e}))))))

(defn get!
  "GET with query-string params to endpoint.
   Returns :result on success; throws ExceptionInfo on failure."
  [client endpoint params]
  (let [url  (str (:base-url client) endpoint)
        opts (assoc (base-opts client) :query-params params)]
    (try
      (parse-body (hato/get url opts) endpoint params)
      (catch clojure.lang.ExceptionInfo e (throw e))
      (catch Exception e
        (throw (ex-info (str "Telegraph HTTP error: " (ex-message e))
                        {:type     :telegraph/http-error
                         :endpoint endpoint
                         :params   params
                         :cause    e}))))))
