(ns instastalker.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.set :as set]
            [me.raynes.fs :as fs])
  (:gen-class))

(def db-path (fs/expand-home "~/.instastalker.db"))

(defn load-db []
  (try
    (read-string (slurp db-path))
    (catch java.io.FileNotFoundException e #{})))

(defn parse-posts [body]
  (when-let [arr (some-> (re-find #"<script type=\"text/javascript\">window\._sharedData = (.*)</script>" body)
                         (get 1)
                         (json/read-str)
                         (get-in ["entry_data" "ProfilePage" 0 "graphql" "user" "edge_owner_to_timeline_media" "edges"]))]
    (map #(when-let [node (get % "node")]
            {:id (get node "id") :url (get node "display_url")}) arr)))

(defn get-posts [profile]
  (some-> (str "https://www.instagram.com/" profile)
          (client/get)
          (get :body)
          (parse-posts)))

(defn send-telegram [text chat-id bot-token]

  (client/post (str  "https://api.telegram.org/bot" bot-token "/sendMessage")
               {:form-params {:chat_id chat-id :text text}}))

(defn send-posts [posts char-id bot-token]
  (doseq [post posts]
    (send-telegram (get post :url) char-id bot-token)))

(defn dump-db [posts]
  (spit db-path (pr-str posts)))

(defn -main
  [& args]
  (let [[profile chat-id] args
        bot-token (or (System/getenv "TELEGRAM_BOT_TOKEN")
                      (throw (Exception. "Missing TELEGRAM_BOT_TOKEN")))
        db (load-db)
        new-posts (filter #(not (contains? db (get % :id))) (get-posts profile))]
    (when (not (empty? new-posts))
      (send-posts new-posts chat-id bot-token))
    (dump-db (clojure.set/union db (apply hash-set (map #(get % :id) new-posts))))))
