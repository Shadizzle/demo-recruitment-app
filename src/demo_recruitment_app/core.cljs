(ns demo-recruitment-app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as r]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn on-js-reload [])

(defonce gh-users (r/atom []))

(go
  (let [response (<! (http/get "https://api.github.com/search/users?q=type:user&per_page=100"
                               {:with-credentials? false}))
        users (:items (:body response))]
    (swap! gh-users #(into % users))))

(defn user-list [users]
  (js/console.log (str users))
  (if (seq users)
    (into [:ul] (map #(vector :li (:login %)) users))
    [:span "waiting"]))

(defn root []
  [user-list @gh-users])

(r/render-component [root]
  (.getElementById js/document "app"))
