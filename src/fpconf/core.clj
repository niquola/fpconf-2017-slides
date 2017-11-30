(ns fpconf.core)

;; Hashmap is a peril for data 
;; names as first class

(def data
  {:name {:given "Nikolai"
          :family "Ryzhikov"}
   :birthDate "1980"
   :password "somehash"
   :labels #{:GithubUser :Developer :HockeyPlayer :Clojurinas}})

{:namespaced/key 1
 :another/key 2
 ::key 3}

(get data :name)

(:name data)

(get-in data [:name :given])

(select-keys data [:name :labels])

(assoc-in data [:name :middle] "Nikolaevich")

(keys data)

(map first data)

(map second data)
(update data :name (fn [x] (assoc x :middle "Nikolaevich")))
(update data :name assoc :middle "Nikolaevich")


(def v [1 3 3])

(nth v 2)
(conj v 4)
(reduce + 0 v)
(reduce * 1 v)

;; fn


(defn myfn [a b]
  (str a b))

(map (fn [x] (inc x)) [1 2 3])


'(-> (a) (b) (c))
'(-> (a) b c)

;; (c (b (a)))

'(-> (a) (b 1) (c 2))
;; (c (b (a) 1) 2)



(->> (range 100) 
     (filter odd?)
     (map inc)
     (take 4))

;; java interop

(-> (java.util.Date.)
    .getMonth)




