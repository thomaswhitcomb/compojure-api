(ns compojure.api.response-negotiation-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [compojure.api.response-negotiation :refer :all]))

(testable-privates compojure.api.response-negotiation parse-mime-and-q-param)

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

(testable-privates compojure.api.response-negotiation accepts-in-pref-order)

(tabular accepts-in-pref-order
  (fact (accepts-in-pref-order ?accept) => ?resp)
  ?accept          ?resp
  nil              nil
  "a/a"            ["a/a"]
  "a/a, b/b"       (contains ["a/a" "b/b"])
  " a/a  , b/b "   (contains ["a/a" "b/b"])
  "c/c; q=0.3, d/d; q=0.1, a/a, b/b; q=0.5"  ["a/a" "b/b" "c/c" "d/d"])

(defn ->req [accept produces]
  {:headers {"accept" accept}
   :meta    {:produces produces}})

(tabular should-response-in?
  (fact (should-response-in? ?offer (->req ?accept ?produces) ?default) => ?should)
  ?offer  ?accept        ?produces  ?default  ?should
  "a"     "a"            ["a"]      false     true
  "a"     "a, b"         ["a"]      false     true
  "a"     "a;q=0.5, b"   ["a"]      false     true
  "a"     "a;q=0.5, b"   ["a" "b"]  false     false
  "b"     "a;q=0.5, b"   ["a" "b"]  false     true
  "a"     nil            ["a" "b"]  false     false
  "a"     nil            ["a" "b"]  true      true)
