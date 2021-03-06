;; Verbs
;; =================
;; Submit (new data)
;; Update (user state)
;;
(ns pi.server.chsk 
  (:require [com.stuartsierra.component :as component]
            [clojure.core.cache :as cache]
            [taoensso.sente :as s]
            [clojure.string :refer [blank?]]
            [clojure.core.async :refer [<! <!! chan go go-loop thread]]
            [pi.data.core :refer [all-msgs all-users all-votes
                                  next-mid next-vid
                                  local-messages local-users]]
            ;; too much about the data model is leaking out
            [pi.util :as util]
            ))

(defn connected-users
  "Get all connected users."
  ([]
   (dosync
     (map #(get @all-users %) (:any @connected-uids)))))

(defmulti event-msg-handler :id)
(defn     event-msg-handler*
  "This is supposed to be for logging and error handling, but I have not
  found the extra level of indirection useful thusfar."
  [{:as ev-msg :keys [id ?data event]}]
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (println "Unhandled event:" event)
    (when-not (:dummy-reply-fn (meta ?reply-fn))
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/uidport-open [ev-msg] nil)
(defmethod event-msg-handler :chsk/uidport-close [ev-msg] nil)
(defmethod event-msg-handler :chsk/ws-ping [ev-msg] nil)

(defn valid-user? [uid]
  (complement
    (or (nil? uid) (blank? uid))))
;; User updates their current location, which is tracked to support
;; real time updates and push notifications.
(defmethod event-msg-handler :update/location
  [{:keys [event ring-req]}]
  (let [uid (-> ring-req :session :uid)]
    (if (valid-user? uid)
      (let [{:keys [uid location]} (last event)
            user  (get @all-users uid)
            user* (assoc user :location location)
            msgs  (local-messages location @all-msgs)]
        (dosync
          (ref-set all-users (assoc @all-users uid user*))
          (chsk-send! uid [:swap/posts msgs]))))))

(defmethod event-msg-handler :submit/post
  [{:keys [event ring-req]}]
  (let [uid  (-> ring-req :session :uid)
        post (last event)]
    (when (and (:msg post) (= uid (:uid post))
               (valid-user? uid))
      (let [data (merge post {:time (util/now)
                              :mid (next-mid)})]
        (dosync
          (ref-set all-msgs (conj @all-msgs data))
          (doseq [user (local-users (:location data)
                                    @all-msgs
                                    (connected-users))]
            (chsk-send! (:uid user) [:new/post data])))))))

(defmethod event-msg-handler :submit/vote
  [{:keys [event ring-req]}]
  (let [uid   (-> ring-req :session :uid)
        user  (get @all-users uid)
        vote  (last event)
        vote* {:vid      (next-vid)
               :mid      (:mid vote)
               :uid      uid
               :time     (util/now)
               :location (:location user)}]
    (dosync
      (ref-set all-votes (conj @all-votes vote*))
      (doseq [user (local-users (:location vote*)
                                @all-msgs
                                (connected-users))]
        (chsk-send! (:uid user) [:new/vote vote*])))))

(defmethod event-msg-handler :submit/comment
  [{:keys [event ring-req]}]
  nil)

(defmethod event-msg-handler :update/teleport-location
  [{:keys [event ring-req]}]
  (let [uid (-> ring-req :session :uid)]
    (if (valid-user? uid)
      (let [place    (last event)
            location (<!! (util/geocode place))
            messages (local-messages location @all-msgs)]
        (chsk-send! uid [:swap-teleport/posts {:place    place
                                               :location location
                                               :messages messages}])))))

(defn stop-server! [router]
  (when-let [stop-f @router]
    (println "stopping chsk server")
    (stop-f)))

(defrecord ChskServer [;ring-ajax-post
                       ;ring-ajax-get-or-ws-handshake
                       ch-chsk
                       chsk-send!
                       connected-uids
                       router
                       handler]
  component/Lifecycle
  (start [this]
    (println "starting chsk server")
    (stop-server! server)
    (let [{:keys [ch-recv send-fn
                  ;ajax-post-fn
                  ;ajax-get-or-ws-handshake-fn
                  connected-uids]}
          (s/make-channel-socket! {})]
      (assoc this
     ;  :ring-ajax-post ajax-post-fn
     ;  :ring-ajax-get-or-ws-handshake ring-ajax-get-or-ws-handshake-fn
             :ch-chsk ch-recv
             :chsk-send! send-fn
             :connected-uids connected-uids
             :router (atom (s/start-chsk-router! ch-recv handler)))))

  (stop [this]
    (assoc this :router (stop-server! server))))

(defn chsk-server []
  (map->ChskServer {:handler event-msg-handler*}))
