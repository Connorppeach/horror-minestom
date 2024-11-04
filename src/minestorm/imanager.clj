(ns minestorm.imanager
  (:import
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.entity EntityType Entity Player Player$Hand EntityCreature]
           [java.util UUID]
           ))

(def instancemap (atom {}))

(defn assocInstance
  [^String name ^Instance instance]
  (swap! instancemap assoc name instance))

(defn getInstance
  [^String name]
  (get @instancemap name))


(defn removeInstance
  [^String name]
  (swap! instancemap dissoc name))
