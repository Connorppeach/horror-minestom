(ns minestorm.debug
  (:gen-class)
  (:require [minestorm.core :as core]
            [clj-async-profiler.core :as prof]
  ))

(defn main []
  (prof/serve-ui 8080)
  (core/-main))
