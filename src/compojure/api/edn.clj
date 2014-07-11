(ns compojure.api.edn
  (:require [ring.util.http-response :refer [content-type bad-request!]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn edn-request?
  "Checks from request content-type weather it's EDN."
  [{:keys [content-type]}]
  (and
    content-type
    (not (empty? (re-matches #"application/(vnd.+)?edn" content-type)))))

(defn read-edn-body [body encoding]
  (try
    (with-open [in (-> body (io/reader :encoding encoding) (java.io.PushbackReader.))]
      (edn/read in))
    (catch RuntimeException e
      (bad-request! {:type "edn-parse-exception" :message (.getMessage e)}))))

(defn edn-request-support [handler {:keys [keywords?] :or {keywords? true}}]
  (let [post-walk (if keywords? keywordize-keys identity)]
    (fn [{:keys [character-encoding content-type body] :as request}]
      (let [request (if-not (and body (edn-request? request))
                      request
                      (let [edn (-> body
                                    (read-edn-body (or character-encoding "utf-8"))
                                    (post-walk))
                            request (assoc request :body        edn
                                                   :body-params edn
                                                   :edn-params  edn)]
                        (if (map? edn)
                          (update-in request [:params] merge edn)
                          request)))
            request (update-in request [:meta :consumes] concat ["application/edn"])]
        (handler request)))))

(defn serializable?
  "Predicate that returns true whenever the response body is serializable."
  [{:keys [body] :as response}]
  (when response
    (not (or
           (instance? java.io.File body)
           (instance? java.io.InputStream body)))))

(defn ->edn [x]
  (prn-str x))

(defn ->edn-response [response]
  (-> response
      (content-type "application/edn; charset=utf-8")
      (update-in [:body] ->edn)))

(defn edn-response-support [handler opts]
  (fn [request]
    (let [request  (update-in request [:meta :produces] concat ["application/edn"])
          response (handler request)]
      (if false #_(serializable? response)
        (->edn-response response)
        response))))

(defn edn-support [handler & [opts]]
  (-> handler
      (edn-response-support opts)
      (edn-request-support opts)))
