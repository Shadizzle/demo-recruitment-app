(ns demo-recruitment-app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as r]
            [demo-recruitment-app.storage :as s]
            [cljs.core.async :refer [chan <!]]))

(enable-console-print!)

(defn on-js-reload [])

(defonce gh-users (r/atom []))
(defonce gh-query (r/atom {}))
(defonce favourites (r/atom false))

(defn get-users [q]
  (let [response-chan (chan 1 (map #(get-in % [:body :items])))
        url "https://api.github.com/search/users?per_page=100&q=type:user"]
    (http/get url {:with-credentials? false :channel response-chan})))

(defn tag-favourite [user]
  (let [fav-set (s/get-favourites)
        is-fav? #(fav-set (:id %))]
    (conj user (if (is-fav? user)
                   [:favourite true]
                   [:favourite false]))))

(defn reset-users! [users query]
  (go
    (let [new-users (<! (get-users @query))]
      (reset! users (map tag-favourite new-users)))))

(defn into-users! [users query]
  (go
    (let [new-users (<! (get-users @query))]
      (swap! users #(into % (map tag-favourite) new-users)))))

(defonce _ (reset-users! gh-users gh-query))

(defn favourite-user [user]
  (s/update-favourites! conj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn unfavourite-user [user]
  (s/update-favourites! disj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn favourite-star [user]
  (if (:favourite user)
    [:span.fa.fa-star {:on-click #(unfavourite-user user)}]
    [:span.fa.fa-star-o {:on-click #(favourite-user user)}]))

(defn user-row [user]
  [:tr
    [:td [:a {:href (:html_url user)} (:login user)]]
    [:td [favourite-star user]]])

(defn user-list []
  (if (seq @gh-users)
    [:table.table.table-striped.user-list
      [:tbody
        (let [show-all (not @favourites)]
          (for [user @gh-users
                :when (or show-all
                          (:favourite user))]
            ^{:key (:id user)} [user-row user]))]]
    [:div.loading-spinner [:span.fa.fa-spinner.fa-spin]]))

(defn favourites-filter []
  [:div.checkbox
    [:label {:for "favourites-filter"}
      [:input#favourites-filter
        {:type "checkbox"
         :value @favourites
         :on-click #(swap! favourites not)}]
      "Mostrar solo favoritos"]])

(defn filter-list []
  [:div.filter-list
    [favourites-filter]])

(defn root []
  [:div.container
    [:h1 "Usuarios"]
    [:div.row
      [:div.col-md-8 [user-list]]
      [:div.col-md-3 [filter-list]]]])

(r/render-component [root]
  (.getElementById js/document "app"))
