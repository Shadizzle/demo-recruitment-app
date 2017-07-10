(ns demo-recruitment-app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [reagent.core :as r]
            [demo-recruitment-app.storage :as s]
            [clojure.string :refer [blank?]]
            [cljs.core.async :refer [chan <!]]))

(enable-console-print!)

;; Todo List

; Favouriting
; Followers as num field and dropdown for operator (ie. > < <= >= etc.)

;; State

(defonce app-state (r/atom {:users []
                            :only-favourites false
                            :active-query-count 0
                            :query {:followers ""
                                    :location ""
                                    :language ""
                                    :page 1}}))

(def gh-users (r/cursor app-state [:users]))
(def gh-query (r/cursor app-state [:query]))
(def gh-query-page (r/cursor gh-query [:page]))
(def only-favourites (r/cursor app-state [:only-favourites]))
(def active-query-count (r/cursor app-state [:active-query-count]))

;; Transformations (pure)

(defn make-url [{:keys [followers location language]} page]
  (str "https://api.github.com/search/users"
       "?per_page=100"
       "&page=" page
       "&q=type:user"
       (if-not (blank? followers) (str " followers:" followers))
       (if-not (blank? location) (str " location:" location))
       (if-not (blank? language) (str " language:" language))))

;; Actions (impure)

(defn id->el [id]
  (.getElementById js/document id))

(defn get-users [query page]
  (http/get (make-url query page)
    {:with-credentials? false
     :channel (chan 1 (map #(get-in % [:body :items])))}))

(defn reset-users! []
  (go
    (swap! active-query-count inc)
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (reset! gh-users new-users)
      (swap! active-query-count dec))))

(defn into-users! []
  (go
    (swap! active-query-count inc)
    (let [new-users (<! (get-users @gh-query @gh-query-page))]
      (swap! gh-users #(into (vec %) new-users))
      (swap! active-query-count dec))))

; TODO: stub
(defn favourite-user! [user])

; TODO: stub
(defn unfavourite-user! [user])

(defn update-query! []
  (let [new-query {:followers (.-value (id->el "followers-filter"))
                   :location (.-value (id->el "location-filter"))
                   :language (.-value (id->el "language-filter"))}]
    (when (not= @gh-query new-query)
      (reset! gh-query new-query)
      (reset! gh-query-page 1)
      (reset-users!))))

(defn inc-page! []
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
  (if-let [users (seq @gh-users)]
    [:div.user-list
      [:table.table.table-striped
        (into [:tbody]
              (map-indexed
                (fn [idx user] ^{:key idx} [user-row user]))
              users)]]))

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

(defn root []
  [:div.container
    [:h1 "Usuarios"]
    [:div.row
      [:div.col-md-8
        (if (= 0 @active-query-count)
          [user-list]
          [:div.loading-spinner [:span.fa.fa-spinner.fa-spin]])]
      [:div.col-md-3.sticky [query-filters]]]])

;; Run

(defn on-js-reload []
  (let [{:keys [followers location language]} @gh-query]
    (set! (.-value (id->el "followers-filter")) followers)
    (set! (.-value (id->el "location-filter")) location)
    (set! (.-value (id->el "language-filter")) language)))

(defonce *app-init* (reset-users!))

(r/render-component [root] (id->el "app"))
