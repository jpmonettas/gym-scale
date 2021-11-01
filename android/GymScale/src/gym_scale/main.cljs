(ns gym-scale.main
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]
            [gym-scale.events]
            [gym-scale.subs]
            [gym-scale.dev]
            [gym-scale.views.user :as user-views]))

(defn ^:export -main [& args]
  (dispatch [:app/init])
  (r/as-element [user-views/main-screen]))
