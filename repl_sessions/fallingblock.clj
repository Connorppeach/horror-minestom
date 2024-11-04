(ns test
  (:require [minestorm.core :as core]
            [minestorm.db :as db]
            [minestorm.explode :as expl])
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance]
           [net.minestom.server.entity Player]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance InstanceManager]
           [net.minestom.server.instance InstanceContainer]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.entity.attribute Attribute]
           [net.minestom.server.timer TaskSchedule]
           [net.minestom.server.entity Entity EntityType]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]
                                        ;noise
           )
  )




(def player ^Player (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) "femboywifey"))

(def block (mkblock (.getInstance player) (.getPosition player) 23))



(setblockpos block (.add ^Pos (.getPosition ^Player player) (double i) (double 0) (double 0)))




(db/get-prop "femboywifey" :blockstyle)


(db/set-prop! "femboywifey" :blockstyle "falling")


