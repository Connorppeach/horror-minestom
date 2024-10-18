(ns minestorm.stevemob
  (:import [net.worldseed.multipart GenericModelImpl]
           [net.minestom.server.instance Instance]
           [net.minestom.server.coordinate Pos]))

(set! *warn-on-reflection* true)

(defn mk
  [instance pos name]
  (proxy [GenericModelImpl] []
    (init [^Instance instance ^Pos pos]
      (let [^GenericModelImpl this this]
        (proxy-super init instance pos (float 1))))
    (getId [] name)))
