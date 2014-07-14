(ns compojure.api.edn
  (:require [ring.util.http-response :refer [content-type bad-request!]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [compojure.api.response-negotiation :as rn]))

(def edn-mime "application/edn")

(defn edn-request?
  "Checks from request content-type weather it's EDN."
  [{:keys [content-type]}]
  (and
    content-type
    (not (empty? (re-matches #"application/(vnd.+)?edn" content-type)))))

(defn read-edn-body [body encoding opts]
  (try
    (with-open [in (-> body (io/reader :encoding encoding) (java.io.PushbackReader.))]
      (edn/read opts in))
    (catch RuntimeException e
      (bad-request! {:type "edn-parse-exception" :message (.getMessage e)}))))

(defn edn-request-support [handler {edn-opts :edn-opts :or {edn-opts {}}}]
  (fn [{:keys [character-encoding content-type body] :as request}]
    (let [request (if-not (and body (edn-request? request))
                    request
                    (let [edn (read-edn-body body (or character-encoding "utf-8") edn-opts)
                          request (assoc request :body        edn
                                                 :body-params edn
                                                 :edn-params  edn)]
                      (if (map? edn)
                        (update-in request [:params] merge edn)
                        request)))
          request (update-in request [:meta :consumes] conj edn-mime)]
      (handler request))))

(defn- serializable?
  "Predicate that returns true whenever the response body is EDN serializable."
  [{:keys [body] :as response}]
  (and response
       (not (instance? java.io.File body))
       (not (instance? java.io.InputStream body))))

(defn ->edn-response [response]
  (-> response
      (content-type "application/edn; charset=utf-8")
      (update-in [:body] pr-str)))

(defn edn-response-support [handler _]
  (fn [request]
    (let [request  (update-in request [:meta :produces] conj edn-mime)
          response (handler request)]
      (if (and (rn/should-response-in? edn-mime request)
               (serializable? response))
        (->edn-response response)
        response))))

(defn edn-support [handler & [opts]]
  (-> handler
      (edn-response-support opts)
      (edn-request-support opts)))
