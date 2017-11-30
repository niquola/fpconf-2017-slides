(ns fpconf.ui
  (:require [hiccup.core :as hiccup]
            [garden.core :as garden]
            [garden.units :as u]))


(hiccup/html [:div {:attr "val"}])
(hiccup/html [:div {:attr "val"}
              [:h3 "Helo"]])


(garden/css
 [:.app {:font-size (u/px 30)
         :border {:top "1px solid #ddd"}}])

