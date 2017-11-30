(ns fpconf.web
  (:require [org.httpkit.server :as server]
            ))

(defn dispatch [req]
  {:status 200
   :body (pr-str req)})

(defn start []
  (server/run-server #'dispatch {:port 8889}))

(comment
  (def srv (start))
  ;; stop it
  (srv))

;; pipeline / middleware

;;-> parse-body
;;-> access control
;;-> process request

(dispatch {})

(require '[unifn.core :as u])

(defmethod u/*fn :index
  [{req :request :as ctx}]
  {:response {:status 200
              :body (pr-str ctx)}})

(u/*apply :index {:db "mydb"})

(defmethod u/*fn
  :env
  [ctx]
  {:env (reduce
         (fn [acc [k v]] (assoc acc (keyword k) v))
         {} (System/getenv))})

(u/*apply :env {})

(require '[fpconf.db :as db])

(defmethod u/*fn
  :db-connection
  [{{db-uri :DATABASE_URL} :env :as ctx}]
  {:db {:dbtype "postgresql"
        :connection-uri db-uri}})

(u/*apply :db-connection {:env {:DATABASE_URL "jdbc:postgresql://localhost:5679/postgres?stringtype=unspecified&user=postgres&password=secret"}})

(u/*apply [:env :db-connection] {})

(defmethod u/*fn
  :index
  [{db :db :as ctx}]
  {:response
   {:status 200
    :body (db/q db {:select [:*]
                    :from [:information_schema.tables]
                    :limit 10})}})


(def web-stack [:env :db-connection :index] )

(def ctx {:env {:DATABASE_URL "jdbc:postgresql://localhost:5679/postgres?stringtype=unspecified&user=postgres&password=secret"}})

(keys (u/*apply web-stack ctx))


(defn dispatch [req]
  (let [{resp :response} (u/*apply web-stack (assoc ctx :request req))]
    resp))

(dispatch {})

(require '[cheshire.core :as json])
(require '[clj-yaml.core :as yaml])

(defmethod u/*fn
  :format
  [{{body :body} :response :as ctx}]
  (when body
    {:response {:body (json/generate-string body)
                #_(yaml/generate-string body)}}))

(def web-stack [:env :db-connection :index :format])


(def routes {:GET {:action :index}
            "admin" {"users" {:GET {:action :get-users}}}
            [:table_name] {:GET {:action :select}}})


(require '[route-map.core :as routing])

(:match (routing/match [:GET "/admin/users"] routes))
(:match (routing/match [:GET "/admin/ups"] routes))

(:match (routing/match [:GET "/tables"] routes))
(:params (routing/match [:GET "/tables"] routes))

(defmethod u/*fn
  :resolve-route
  [{{uri :uri meth :request-method} :request :as ctx}]
  (if-let [m (routing/match [meth uri] routes)]
    {:current-route (:match m)
     :request {:route-params (:params m)}}
    {:current-route {:action :not-found}}))

(-> :resolve-route
    (u/*apply {:request {:uri "/admin/users" :request-method :get}})
    :current-route)

(defmethod u/*fn
  :dispatch
  [{route :current-route :as ctx}]
  (u/*apply (:action route)
            (assoc ctx :route-metadata route)))


(def web-stack [:env :db-connection :resolve-route :dispatch :format])

(select-keys
 (u/*apply web-stack (assoc ctx :request {:uri "/tables" :request-method :get}))
 [:response ::u/message ::u/status])

(defmethod u/*fn
  :select
  [{{{tbl :table_name} :route-params} :request
    d :db}]
  {:response {:body (db/q d {:select [:*] :from [(keyword (str "information_schema." tbl))]
                             :limit 100})}})

(->
 (u/*apply web-stack (assoc ctx :request {:uri "/tables" :request-method :get}))
 (select-keys [:response]))

(->
 (u/*apply web-stack (assoc ctx :request {:uri "/columns" :request-method :get}))
 (select-keys [:response]))



(require '[fpconf.cors :as cors])

(def web-stack [:env :db-connection
                :cors-preflight
                :cors-allow
                :resolve-route :dispatch :format])
