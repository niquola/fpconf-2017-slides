(ns future-app.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync] :as rf]
            [future-app.events]
            [future-app.subs]
            [clojure.string :as str]))

(def ReactNative (js/require "react-native"))

(defn c [prop]
  (r/adapt-react-class (aget ReactNative prop)))

(def app-registry (.-AppRegistry ReactNative))
(def btn (c "Button"))


(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
(def list-view (r/adapt-react-class (.-ListView ReactNative)))

(defn alert [title]
      (.alert (.-Alert ReactNative) title))


(def styles
  {
   :text {:text-align "center"
          :font-weight "bold"}

   :touch {:background-color "#999" :padding 10 :border-radius 5}

   :blue {:color "red"
          :background-color "#f1f1f1"
          :padding 10
          :font-size 32}

   :main {:background-color "#f1f1f1"
          :padding-vertical 30
          :padding-horizontal 10
          :flex 1
          :flex-direction "column"
          :justify-content "flex-start"
          :align-items "flex-start"}

   :scroll {:backgroundColor "#f1f1f1"
            :paddingVertical 30
            :paddingHorizontal 10
            :flex 1
            :flexDirection "column"
            :justifyContent "flex-start"
            :alignItems "flex-start"}

   })

(defn app-text [x]
  [text {:style {:font-family "Helvetica Neue"}} x])

(defn title [x]
  [text {:style {:font-family "HelveticaNeue-CondensedBold"
                 :font-size 18
                 :color "#573"}} x])

(defn small-text [x]
  [text {:style {:font-family "Helvetica Neue"
                 :font-size 11}} x])

(defn by-line [ttle author text]
  [touchable-opacity
   {:style {}
    :on-press #(.log js/console "On press " %)}
   [view {:style {:padding 5 
                  :padding-vertical 10
                  :flex-direction "row"}}
    [image {:source {:uri "https://croisant.net/assets/2015/05/cloje-icon-200.png"}
            :style {:width 50 :height 50
                    :border-radius 25
                    :background-color "white"}}]
    [view {:style {:flex-direction "column"
                   :margin-left 10}}
     [title ttle]
     [small-text author]
     [app-text text]
     [btn {:title "read more" 
           :color "green" :on-press #(.log js/console "hi")}]
     ]]])

(defn navigate-to [href]
  ;; (.log js/console "Nav to" href)
  (rf/dispatch (into [:navigate-to] href)))

(defn link [href txt]
  [touchable-opacity {:on-press  #(navigate-to href)}
   [text {:style {:color "blue"
                  :font-size 14
                  :margin-top 10
                  :margin-bottom 10}} txt]])

(rf/reg-event-db
 :navigate-to
 (fn [db [_ route params]]
   ;; (.log js/console "navigate to " (pr-str route) (pr-str params))
   (assoc db :route {:route route :params params})))

(defn animate [op]
  (when-not (> @op 1) 
    (reset! op (+ @op @op))
    (js/setTimeout #(animate op) 50)))

(defn fade-view [_]
  (let [op (r/atom 0.1)]
    (animate op)
    (fn [& cmps]
      #_(into [view {:style (:main styles)}] cmps)
      (into [view {:style (assoc (:main styles) :opacity @op)}] cmps))))

(defn xhr [{uri :uri action :action}]
  ;; (.log js/console "xhr " uri)
  (->
   (js/fetch uri)
   (.then (fn [res]
            ;; (.log js/console "res " res)
            (-> (.json res)
                (.then (fn [x]
                         ;; (.log js/console "json " action x)
                         (when action (rf/dispatch
                                       [action (js->clj x :keywordize-keys true)])))))))))

(rf/reg-fx :xhr xhr)

(rf/reg-event-fx
 :load-tables
 (fn [coef _]
   ;; (.log js/console "load tables")
   {:xhr {:uri "http://localhost:8889/tables"
          :action :tables-loaded}}))

(rf/reg-event-db
 :tables-loaded
 (fn [db [_ data]]
   (assoc db :tables data)))

(rf/reg-sub :tables (fn [db _] (:tables db)))

(defn index [params]
  (rf/dispatch [:load-tables])
  (let [tables (rf/subscribe [:tables])]
    (fn []
      [fade-view
       [title "Tables"]
       [scroll-view {:contentContainerStyle (:scroll styles)}
        (for [t @tables]
          ^{:key (:table_name t)}[link [:tables {:table_name (:table_name t)}] (:table_name t)])]])))

(defn tables [params]
  (fn []
    [fade-view
     [title (str "Table: " (:table_name params))]
     [link [:index {}] "Back"]
     [text "...."]]))

(defn not-found [params]
  (fn []
    [fade-view
     [text "Not found " (pr-str params)]
     [touchable-opacity {:on-press  #(rf/dispatch [:navigate-to :index {}])}
      [text  "Index"]]]))

(def routes
  {:index index
   :tables tables})

(rf/reg-sub
 :current-route
 (fn [db _] (get db :route)))

;; (rf/dispatch [:navigate-to :index {}])
 ;; (rf/dispatch [:navigate-to :tables {:table_name "table"}])

(defn app-root []
  (let [route (subscribe [:current-route])]
    (fn []
      (if-let [cmp (get routes (or (:route @route) :index))]
        [view {:style (:main styles)
               :on-magic-tap (.log js/console "magic")
               :on-layout (.log js/console "layout")}
         #_[text (pr-str @route)]
         [cmp (assoc (:params @route) :route @route)]]
        [not-found @route]))))

(rf/reg-event-db
 :init
 (fn [db _]
   (assoc db
          :tables [{:table_name "tables"}]
          :current-route {:route :index})))

(defn init []
  (dispatch-sync [:init])
  (.registerComponent app-registry "FutureApp" #(r/reactify-component app-root)))
