(ns compojure.api.response-negotiation-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [compojure.api.response-negotiation :refer :all]
            [compojure.api.json :refer [json-mime]]
            [compojure.api.edn :refer [edn-mime]]))

(testable-privates compojure.api.response-negotiation
                   parse-mime-and-q-param
                   accepts-in-pref-order)

(tabular parse-mime-and-q-param
  (fact (parse-mime-and-q-param ?item) => ?resp)
  ?item                   ?resp
  "a/b"                   [1.0 "a/b"]
  "a/b; x=y"              [1.0 "a/b"]
  "a/b; q=0.5"            [0.5 "a/b"]
  "a/b;q=0.5"             [0.5 "a/b"]
  "a/b;q=0.5 "            [0.5 "a/b"]
  "a/b; q = 0.5"          [1.0 "a/b"]
  "a/b; x=y;q=0.5;q=w"    [0.5 "a/b"]
  "a/b; x=y;q=0.5 ;q=w"   [0.5 "a/b"]
  "a/b; x=y; q=0.5 ;q=w"  [0.5 "a/b"]
  "a/b; x=y; q=0.5;q=w"   [0.5 "a/b"]
  "a/b; q=1.x.3"          [1.0 "a/b"])

(tabular accepts-in-pref-order
  (fact (accepts-in-pref-order ?accept) => ?resp)
  ?accept          ?resp
  nil              nil
  "a/a"            ["a/a"]
  "a/a, b/b"       (contains ["a/a" "b/b"])
  " a/a  , b/b "   (contains ["a/a" "b/b"])
  "c/c; q=0.3, d/d; q=0.1, a/a, b/b; q=0.5"  ["a/a" "b/b" "c/c" "d/d"])

(defn ->req [accept content-type produces]
  {:headers      {"accept" accept}
   :content-type content-type
   :meta         {:produces produces}})

(tabular response-format
  (fact (response-format (->req ?accept ?content-type ?produces)) => ?response-format)
  ?accept         ?content-type      ?produces                ?response-format
  json-mime       ..anything..       [json-mime edn-mime]     json-mime
  nil             json-mime          [json-mime edn-mime]     json-mime
  nil             nil                [json-mime edn-mime]     json-mime
  "*/*"           json-mime          [json-mime edn-mime]     json-mime
  "*/*"           nil                [json-mime edn-mime]     json-mime
  edn-mime        json-mime          [json-mime edn-mime]     edn-mime)

(fact should-response-in?

  (should-response-in? ..format.. ..request..) => truthy
  (provided
    (response-format ..request..) => ..format..)

  (should-response-in? ..format.. ..request..) => falsey
  (provided
    (response-format ..request..) => ..other-format..))
