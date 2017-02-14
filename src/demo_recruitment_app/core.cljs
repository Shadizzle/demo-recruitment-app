(ns demo-recruitment-app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as r]
            [demo-recruitment-app.storage :as s]
            [clojure.string :refer [blank?]]
            [cljs.core.async :refer [chan <!]]))

(enable-console-print!)

;; State

(defonce gh-users (r/atom []))
(defonce gh-query (r/atom {:followers ""
                           :location ""
                           :language ""}))
(defonce gh-query-page (r/atom 1))
(defonce only-favourites (r/atom false))

;; Helpers

(defn tag-favourite [user]
  (let [fav-set (s/get-favourites)
        is-fav? #(fav-set (:id %))]
    (conj user (if (is-fav? user)
                   [:favourite true]
                   [:favourite false]))))

(defn make-url [query page]
  (str "https://api.github.com/search/users?per_page=100&page=" page
       "&q=type:user"
       (if-not (-> query :followers blank?)
         (str " followers:" (:followers query)))
       (if-not (-> query :location blank?)
         (str " location:" (:location query)))
       (if-not (-> query :language blank?)
         (str " language:" (:language query)))))

;; Actions

(defn get-users [query page]
  (let [response-chan (chan 1 (map #(get-in % [:body :items])))
        url (make-url query page)]
    (http/get url {:with-credentials? false :channel response-chan})))

(defn reset-users! []
  (go
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (reset! gh-users (map tag-favourite new-users)))))

(defn into-users! []
  (go
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (swap! gh-users #(into (vec %) (map tag-favourite) new-users)))))

(defn favourite-user! [user]
  (s/update-favourites! conj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn unfavourite-user! [user]
  (s/update-favourites! disj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn update-query! []
  (let [followers-val (.-value (.getElementById js/document "followers-filter"))
        location-val (.-value (.getElementById js/document "location-filter"))
        language-val (.-value (.getElementById js/document "language-filter"))
        new-query {:followers followers-val
                   :location location-val
                   :language language-val}]
    (when (not= @gh-query new-query)
      (reset! gh-query new-query)
      (reset! gh-query-page 1)
      (reset-users!))))

(defn update-page! []
  (swap! gh-query-page inc)
  (into-users!))

;; Components

(defn favourite-star [user]
  (if (:favourite user)
    [:span.fa.fa-star {:on-click #(unfavourite-user! user)}]
    [:span.fa.fa-star-o {:on-click #(favourite-user! user)}]))

(defn user-row [user]
  [:tr
    [:td [:a {:href (:html_url user)} (:login user)]]
    [:td [favourite-star user]]])

(defn user-list []
  (if (seq @gh-users)
    [:div.user-list
      [:table.table.table-striped
        [:tbody
          (let [show-all (not @only-favourites)
                index (zipmap (map :id @gh-users) (range))]
            (for [user @gh-users
                  :when (or show-all
                            (:favourite user))]
              ^{:key (get index (:id user))} [user-row user]))]]
      (when (and (not @only-favourites)
                 (= (mod (count @gh-users) 100) 0)) ;FIXME
        [:div.col-md-offset-2
          [:button.btn.btn-default.col-md-8 {:on-click #(update-page!)}
            "Mas"]])]

    [:div.loading-spinner [:span.fa.fa-spinner.fa-spin]]))

(defn favourites-filter []
  [:div.checkbox.favourites-filter
    [:label {:for "favourites-filter"}
      [:input#favourites-filter
        {:type "checkbox"
         :value @only-favourites
         :on-click #(swap! only-favourites not)}]
      "Mostrar solo favoritos"]])

(defn query-filters []
  [:div.form-horizontal.query-filters
    [:div.form-group.followers-filter
      [:label.control-label.col-sm-4 {:for "followers-filter"}
        "Seguidores"]
      [:div.col-sm-8
        [:input#followers-filter.form-control]]]
    [:div.form-group.location-filter
      [:label.control-label.col-sm-4 {:for "location-filter"}
        "Ubicación"]
      [:div.col-sm-8
        [:input#location-filter.form-control]]]
    [:div.form-group.language-filter
      [:label.control-label.col-sm-4 {:for "language-filter"}
        "Lenguaje"]
      [:div.col-sm-8
        [:input#language-filter.form-control]]]
    [:button.btn.btn-default.col-sm-12 {:on-click #(update-query!)}
      "Filtrar"]])

(defn filter-list []
  [:div.filter-list
    [query-filters]
    [favourites-filter]])

(defn root []
  [:div.container
    [:h1 "Usuarios"]
    [:div.row
      [:div.col-md-8 [user-list]]
      [:div.col-md-3 [filter-list]]]])

;; Run

(defn on-js-reload []
  (set!
    (.-value (.getElementById js/document "followers-filter"))
    (:followers @gh-query))
  (set!
    (.-value (.getElementById js/document "location-filter"))
    (:location @gh-query))
  (set!
    (.-value (.getElementById js/document "language-filter"))
    (:language @gh-query)))

(defonce users-initialised (reset-users!))

(r/render-component [root]
  (.getElementById js/document "app"))
