(ns demo-recruitment-app.storage
  (:require [cljs.reader :refer [read-string]]))

(def *ns-separator* "/")
(def *item-ns* "item")
(def *coll-ns* "coll")

(defn set-item* [key val]
  (.setItem (.-localStorage js/window) key (str val)))

(defn set-item! [key val]
  (set-item* (str *item-ns* *ns-separator* key) val))

(defn set-collection! [key val]
  (set-item* (str *coll-ns* *ns-separator* key) val))

(defn get-item* [key]
  (if-let [item (.getItem (.-localStorage js/window) key)]
    (read-string item)
    nil))

(defn get-item [key]
  (get-item* (str *item-ns* *ns-separator* key)))

(defn get-collection [key]
  (get-item* (str *coll-ns* *ns-separator* key)))

(defn remove-item* [key]
  (.removeItem (.-localStorage js/window) key))

(defn remove-item! [key]
  (remove-item* (str *item-ns* *ns-separator* key)))

(defn remove-collection! [key]
  (remove-item* (str *coll-ns* *ns-separator* key)))

(defn update-item! [key f & args]
  (as-> (get-item key) item
    (apply f item args)
    (set-item! key item)))

(defn update-collection! [key f & args]
  (as-> (get-collection key) coll
    (apply f coll args)
    (set-collection! key coll)))
