(ns instastalker.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:gen-class))

(defn posts [resp]
  (as-> resp v
    (get v :body)
    (re-find #"<script type=\"text/javascript\">window\._sharedData = (.*)</script>" v)
    (some-> v
            (get 1)
            (json/read-str)
            (get "entry_data")
            (get "ProfilePage")
            (get 0)
            (get "graphql")
            (get "user")
            (get "edge_owner_to_timeline_media")
            (get "edges")
            )
    (some->> v
             (map #(when-let [v (get % "node")]
                     {:id (get v "id") :url (get v "display_url")})))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (client/get "https://www.instagram.com/ori_levi_ganani/")
  (println "Hello, World!"))
