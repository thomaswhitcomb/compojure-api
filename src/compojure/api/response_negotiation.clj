(ns compojure.api.response-negotiation
  (:require [clojure.string :as s]))

;;
;; Negotiate reponse format using HTTP Accept header and available providers.
;;
;; See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1
;;

(defn- parse-mime-and-q-param
  "Parses the 'q' param from Accept header item. If 'q' parameter
   is not specified it defaults to 1.0 (as per HTTP spec).
   Returns a vector with two elements, first is the 'q' value,
   second is the MIME."
  [item]
  (let [mime    (->> item (re-find #"^([^\s;]+)") second)
        q-param (->> item (re-find #";\s*q=([\d\.]+)") second)
        q       (if q-param
                  (try (Double/parseDouble q-param) (catch NumberFormatException _ 1.0))
                  1.0)]
    [q mime]))

(defn- accepts-in-pref-order
  "Parse HTTP Accept header, return accepted MIME types in preferred order, most
   preferred format first"
  [accept]
  (when accept
    (->> (-> accept (s/trim) (s/split #"\s*,\s*"))
         (map parse-mime-and-q-param)
         (sort-by first >)
         (map second))))

(defn should-response-in?
  "Determined is the response should be formatted in 'offer-format' format.
   Parameters:
     offer-format:     mime-type caller offers, forexample \"application/json\"
     request:          HTTP request
     default-format?:  true if caller is the default (used when Accept is missing)
   Returns true if response should be formatted to 'offer-format'."
  [offer-format request default-format?]
  (let [accept   (-> request (get-in [:headers "accept"]))
        produces (-> request (get-in [:meta :produces]) set)]
    (or (and (s/blank? accept) default-format?) 
        (= offer-format (some produces (accepts-in-pref-order accept))))))
