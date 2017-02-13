(ns demo-recruitment-app.storage)

(defn set-favourites! [val]
  (.setItem (.-localStorage js/window) "favourites"
    (.stringify js/JSON (apply array val))))

(defn get-favourites []
  (set (.parse js/JSON (.getItem (.-localStorage js/window) "favourites"))))

(defn update-favourites! [f & fargs]
  (let [favourites (or (get-favourites) #{})]
    (set-favourites! (apply f favourites fargs))))

(defn remove-favourites! [key]
  (.removeItem (.-localStorage js/window) "favourites"))
