(ns gym-scale.main
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]
            [gym-scale.events]
            [gym-scale.subs]
            [gym-scale.views :as views]))

(defn ^:export -main [& args]
  (dispatch [:app/init])
  (r/as-element [views/main-screen]))
