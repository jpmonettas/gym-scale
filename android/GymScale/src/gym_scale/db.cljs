(ns gym-scale.db
  (:require [clojure.spec.alpha :as s]))

(s/def :scale/last-weight number?)
(s/def :scale/connected? boolean?)

(s/def :screen/current #{:logo :user-select-1 :user-select-2 :user-check})

(s/def :user/id number?)
(s/def :user/name string?)

(s/def :gym/user-data (s/keys :req [:user/id
                                    :user/name]))

(s/def :gym/users (s/map-of :user/id :gym/user-data))

(s/def ::db (s/keys :req [:scale/last-weight
                          :scale/connected?
                          :gym/users]))

(def initial-db
  #_{:scale/last-weight 0 ;; in grams
     :scale/connected? false
     :screen/current :logo
     :gym/users {}}
  {:scale/last-weight 0
   :scale/connected? true
   :screen/current :logo
   :gym/users {}})
