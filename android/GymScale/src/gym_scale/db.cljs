(ns gym-scale.db
  (:require [clojure.spec.alpha :as s]))

(s/def :scale/last-weight number?)
(s/def :scale/connected? boolean?)

(s/def ::db (s/keys :req [:scale/last-weight
                             :scale/connected?]))

(def initial-db
  {:scale/last-weight 0
   :scale/connected? false})
