(ns compojure.api.middleware-test
  (:require [midje.sweet :refer :all]
            [cheshire.core :as cheshire]
            [compojure.api.middleware :as mw]
            [ring.util.http-response :refer [ok]]
            [compojure.api.json :refer [json-mime]]
            [compojure.api.edn :refer [edn-mime]])
  (:import [java.io ByteArrayInputStream]))

(defn ->req [req-type accept]
  {:body          (-> ((condp = req-type
                         :json cheshire/encode
                         :edn  pr-str)
                        {:foo "foo"})
                      (.getBytes "utf-8")
                      (ByteArrayInputStream.))
   :content-type  (condp = req-type :json json-mime :edn edn-mime)
   :headers       {"accept" accept}})

(defn response-type? [expected {{:strs [Content-Type]} :headers}]
  (= Content-Type expected))

(def json-response? (partial response-type? "application/json; charset=utf-8"))
(def edn-response?  (partial response-type? "application/edn; charset=utf-8"))

(let [handler (mw/api-middleware (constantly (ok {:bar "bar"})))]
  (tabular response-negotiation
    (fact (handler (->req ?req-fmt ?accept)) => ?resp)
    ?req-fmt  ?accept               ?resp
    :json     nil                   json-response?
    :json     "application/json"    json-response?
    :edn      "application/json"    json-response?
    :edn      nil                   edn-response?
    :edn      "application/edn"     edn-response?
    :json     "application/edn"     edn-response?))
