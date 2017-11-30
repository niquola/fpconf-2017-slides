(ns fpconf.cors
  (:require
   [unifn.core :as u]))

(defn allow [origin resp]
  (merge-with
    merge resp
    {:headers
     {"Access-Control-Allow-Origin" origin
      "Access-Control-Allow-Credentials" "true"
      "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type, X-total-count"}}))

(defmethod u/*fn
  :cors-preflight
  [{{meth :request-method hs :headers} :request :as args}]
  (when (= :options meth)
    (let [headers (get hs "access-control-request-headers")
          origin (get hs "origin")
          meth  (get hs "access-control-request-method")]
      {::u/status :stop
       :response
       {:status 200
        :body {:message "preflight complete"}
        :headers {"Access-Control-Allow-Headers" headers
                  "Access-Control-Allow-Methods" meth
                  "Access-Control-Allow-Origin" origin
                  "Access-Control-Allow-Credentials" "true"
                  "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type, X-total-count"}}})))

(defmethod u/*fn
  :cors-allow
  [{{hs :headers} :request :as args}]
  (let [origin (get hs "origin")]
    {:response
     {:headers {"Access-Control-Allow-Origin" origin
                "Access-Control-Allow-Credentials" "true"
                "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type, X-total-count"}}}))
