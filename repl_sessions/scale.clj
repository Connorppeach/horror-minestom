(ns test
  (:require [minestorm.core :as core])
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
                                        ;noise
           )
  )

(defn scale
  [player size]
  (.setBaseValue (.getAttribute player  Attribute/GENERIC_SCALE) size)

  (.setBaseValue (.getAttribute player Attribute/GENERIC_FLYING_SPEED) size)

  ;(.setBaseValue (.getAttribute player Attribute/GENERIC_JUMP_STRENGTH) (/ size 2.2))

  ;(.setBaseValue (.getAttribute player Attribute/GENERIC_KNOCKBACK_RESISTANCE) size)

  ;(.setBaseValue (.getAttribute player Attribute/GENERIC_MAX_HEALTH) (* size 20))

  ;(.setBaseValue (.getAttribute player Attribute/GENERIC_MOVEMENT_SPEED) (/ size 10))

  ;(.setBaseValue (.getAttribute player Attribute/GENERIC_STEP_HEIGHT) (/ size 10))

  ;(.setBaseValue (.getAttribute player Attribute/PLAYER_SNEAKING_SPEED) size))
  )
(.setGameMode (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) "LittleTiger4635") net.minestom.server.entity.GameMode/CREATIVE)
(scale (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) "LittleTiger4635") 0.1)
