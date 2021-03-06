(ns pi.components.local
  (:require-macros
            [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!]]
            [clojure.string :refer [blank?]]
            [pi.util :as util]
            [pi.handlers.chsk :refer [chsk chsk-send! chsk-state]]
            [pi.components.nav :refer [navbar]]
            [pi.components.message :refer [message-view]]
            ))

(defn handle-change [e owner]
  (om/set-state! owner :post (.. e -target -value)))

(defn locateMe [locate]
  (if (.hasOwnProperty js/navigator "geolocation")
    (.getCurrentPosition js/navigator.geolocation
                         #(put! locate (util/parse-location %)))))

(defn submit-post [user owner]
  (let [msg  (om/get-state owner :post)
        post {:msg msg
              :uid (get @user :uid)
              :location (get @user :location)}]
    (when msg
      (chsk-send! [:submit/post post])
      (om/set-state! owner :post "")
      )))

(defn meta-enter? [k]
  (and (.-metaKey k) (= (.-key k) "Enter")))

(defn new-post [user owner]
  (reify
    om/IInitState
    (init-state [_]
      {:post ""})
    om/IRenderState
    (render-state [this {:keys [post] :as state}]
      (let [username     (get user :uid)
            has-access   (-> username blank? not)
            has-location (-> user :location :latitude)
            usable       (and has-access has-location)
            submittable  (and usable (-> post blank? not))]
        (dom/div #js {:className "new-post"}
          (dom/textarea #js {:ref "new-post"
                             :className "form-control"
                             :placeholder "What's happening?"
                             :disabled (not usable)
                             :rows "3"
                             :value post
                             :onKeyDown #(if (and submittable
                                                  (meta-enter? %))
                                           (submit-post user owner))
                             :onChange #(handle-change % owner)})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "pull-left"} (count post))
            (dom/div #js {:className "pull-right"}
              (dom/button #js {:type "button"
                               :disabled (not submittable)
                               :className "btn btn-primary"
                               :onTouch #(submit-post user owner)
                               :onClick #(submit-post user owner)}
                          "Submit"))))))))

(defn local-view
  "Refreshes approximately every minute by sending the latest location
  to the server, which motivates the server to send back the messages
  within range of that location.
  
  Clients also receive real-time updates of new posts based on users'
  last known location."
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:locate (chan)})

    om/IWillMount
    (will-mount [_]
      (let [locate (om/get-state owner :locate)]
        (go-loop []
          (let [new-loc (<! locate)
                old-loc (get-in @app [:user :location])]
            (when-not (= old-loc new-loc)
              (om/transact! app :user #(assoc % :location new-loc))
              (chsk-send! [:update/location
                           {:uid      (-> @app :user :uid)
                            :location (-> @app :user :location)}])))
          (recur)))
      (let [locate (om/get-state owner :locate)]
        (locateMe locate)
        (js/setInterval #(locateMe locate) 60000)))

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "container"}
        (om/build navbar (select-keys app [:user :nav]))
        (dom/h4 nil (util/display-location (-> app :user :location)))
        (om/build new-post (get app :user)) ;{:init-state (:user app)})
        (apply dom/div #js {:className "message-list"}
               (om/build-all message-view (get app :messages)))))))
