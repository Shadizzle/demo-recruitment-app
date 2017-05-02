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

(defn id->el [id]
  (.getElementById js/document id))

(defn tag-favourite [user]
  (let [fav-set (s/get-collection "favourites")
        is-fav? #(fav-set (:id %))]
    (conj user (if (is-fav? user)
                   [:favourite true]
                   [:favourite false]))))

(defn make-url [{:keys [followers location language]} page]
  (str "https://api.github.com/search/users"
       "?per_page=100"
       "&page=" page
       "&q=type:user"
       (if-not (blank? followers) (str " followers:" followers))
       (if-not (blank? location) (str " location:" location))
       (if-not (blank? language) (str " language:" language))))

;; Actions

(defn get-users [query page]
  (http/get (make-url query page)
    {:with-credentials? false
     :channel (chan 1 (map #(get-in % [:body :items])))}))

(defn reset-users! []
  (go
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (reset! gh-users (map tag-favourite new-users)))))

(defn into-users! []
  (go
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (swap! gh-users #(into (vec %) (map tag-favourite) new-users)))))

(defn update-favourites! [f & args]
  (apply s/update-collection-as! #{} "favourites" f args))

(defn favourite-user! [user]
  (update-favourites! conj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn unfavourite-user! [user]
  (update-favourites! disj (:id user))
  (swap! gh-users #(map tag-favourite %)))

(defn update-query! []
  (let [new-query {:followers (.-value (id->el "followers-filter"))
                   :location (.-value (id->el "location-filter"))
                   :language (.-value (id->el "language-filter"))}]
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
  (if (empty? @gh-users)
    [:div.loading-spinner [:span.fa.fa-spinner.fa-spin]]
    (let [show-all (not @only-favourites)]
      [:div.user-list
        [:table.table.table-striped
          [:tbody
            (->> @gh-users
              (into []
                (comp
                  (map-indexed #(^{:key %1} [user-row %2]))
                  (filter #(or show-all (:favourite %))))))]]
        (when (and show-all (= (mod (count @gh-users) 100) 0)) ;FIXME
          [:div.col-md-offset-2
            [:button.btn.btn-default.col-md-8
              {:on-click #(update-page!)}
              "Mas"]])])))

(defn favourites-filter []
  [:div.checkbox.favourites-filter
    [:input#favourites-filter
      {:type "checkbox"
       :value @only-favourites
       :on-click #(swap! only-favourites not)}]
    [:label {:for "favourites-filter"}
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
  (let [{:keys [followers location language]} @gh-query]
    (set! (.-value (id->el "followers-filter")) followers)
    (set! (.-value (id->el "location-filter")) location)
    (set! (.-value (id->el "language-filter")) language)))

(defonce users-initialised (reset-users!))

(r/render-component [root] (id->el "app"))
