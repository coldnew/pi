(ns pi.components.message
  (:require [pi.util :as util]
            [om.core         :as om
                             :include-macros true]
            [om.dom          :as dom
                             :include-macros true]
            ))

(defn message-view [message owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div #js {:className "row message"}
        (dom/div #js {:className "row top-row"}
          (dom/div #js {:className "col-xs-8 col-md-8"}
                   (get message :msg))
          (dom/div #js {:className "col-xs-4 col-md-4"}
                   (util/format-timestamp (get message :time))))
        (dom/div #js {:className "row bottom-row"}
          (dom/div #js {:className "col-xs-6 col-md-2"}
                   (get message :author))
          (dom/div #js {:className "col-xs-6 col-md-2 col-md-offset-8"}
                   (util/format-km (get message :distance))))))))
