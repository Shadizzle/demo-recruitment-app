(ns demo-recruitment-app.storage)

(def *ns-separator* "/")
(def *item-ns* "item")
(def *coll-ns* "coll")

(defn set-item* [key val]
  (.setItem (.-localStorage js/window) key (.stringify js/JSON val)))

(defn set-item! [key val]
  (set-item* (str *item-ns* *ns-separator* key) val))

(defn set-collection! [key val]
  (set-item* (str *coll-ns* *ns-separator* key) (apply array val)))

(defn get-item* [key]
  (.parse js/JSON (.getItem (.-localStorage js/window) key)))

(defn get-item [key]
  (get-item* (str *item-ns* *ns-separator* key)))

(defn get-collection [key]
  (get-item* (str *coll-ns* *ns-separator* key)))

(defn get-collection-as [dest key]
  (into dest (get-collection key)))

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

(defn update-collection-as! [dest key f & args]
  (as-> (get-collection-as key dest) coll
    (apply f coll args)
    (set-collection! key coll)))
