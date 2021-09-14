(ns gym-scale.db)

(def initial-db
  {:scale/last-weight 0
   :scale/status :unknown ;; :unknown, :uncalibrated, :ready
   })
