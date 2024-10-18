(ns minestorm.steve
  (:require [minestorm.stevemob :as model])

  (:import [java.util List]
           [java.time Duration]
           [net.minestom.server.utils.time TimeUnit Cooldown]
           [net.worldseed.multipart.animations AnimationHandler]
           [net.minestom.server.entity.ai GoalSelector]
           [net.worldseed.multipart GenericModelImpl]
           [net.minestom.server MinecraftServer]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.entity EntityCreature EntityType Player]
           [net.minestom.server.instance Instance]
           [net.worldseed.multipart.animations AnimationHandler AnimationHandlerImpl]
           [net.minestom.server.entity.ai.target ClosestEntityTarget]
           [java.util.function Predicate]
           [net.minestom.server.entity.attribute Attribute]
           ))

(set! *warn-on-reflection* true)


(defn mkmovgoal
  [^EntityCreature entity ^AnimationHandler animationHandler]
  (let [
        target (atom nil)
        targetpos (atom nil)
        lastpos (atom nil)
        forceEnd (atom false)
        pathDuration (Cooldown. (Duration/of 20 TimeUnit/SERVER_TICK))
        p (proxy [GoalSelector] [entity]

      (shouldStart []
        (reset! target (let [^GoalSelector this this]
                         (proxy-super findTarget)))
          (cond 
                (nil? @target) false
                (not (= (.getPlaying animationHandler) "idle")) false
                (and @lastpos (.samePoint (.getPosition ^EntityCreature @target) @lastpos)) false
                :else true
                )
        
        )
      (start []
        (.setTarget entity ^EntityCreature @target)
        (.stopRepeat animationHandler "idle")
        (.playRepeat animationHandler "walk")
        (let [nav (.getNavigator entity)]
          (reset! targetpos (.getPosition ^EntityCreature @target))
          (if (or (= (.getPathPosition nav) nil) (not (.samePoint (.getPathPosition nav) @lastpos)))
            (.setPathTo nav @targetpos)
            (.reset (.getNavigator entity))
            
            ))
        
        )
      (tick [^Long time]

        (if (not @forceEnd)
          (if (not (nil? (.getTarget entity)))
            (let [tpos (.getPosition (.getTarget entity))]
              (if (and @lastpos (> (.distance tpos @lastpos) 0.3))
                (do (reset! lastpos tpos)
                    (.setPathTo (.getNavigator entity) tpos)
                                        ;(println tpos)
                    ;(println (.getPathPosition (.getNavigator entity)))
                    )
                (reset! lastpos tpos)
                ))
            )))
      (shouldEnd []
        (< (.distance (.getPosition entity) @lastpos) 3))
      (end []
        (.setPathTo (.getNavigator entity) nil)
        (.stopRepeat animationHandler "walk")
        (.playRepeat animationHandler "idle")
        (reset! forceEnd false))
      )]
    p
    ))

(defn mk
  [instance pos name]
  (let [model ^GenericModelImpl (model/mk instance pos name)
        f (.init model ^Instance instance ^Pos pos)
        animationHandler ^AnimationHandlerImpl (AnimationHandlerImpl. model)
        p ^EntityCreature (proxy [EntityCreature] [EntityType/ZOMBIE]
            (updateNewViewer
              [^Player player]
              (let [^EntityCreature this this]
                (proxy-super updateNewViewer player))
              (.addViewer model player))
            (tick
              [^Long time]
              (let [^EntityCreature this this]
                (proxy-super tick time)
                (.setPosition model (.getPosition this))
                (.setGlobalRotation model (.yaw (.getPosition this)))
              ))
            (updateOldViewer
              [^Player player]
              (let [^EntityCreature this this]
                (proxy-super updateOldViewer player))
              (.removeViewer model player))
            (remove
              []
              (let [^EntityCreature this this]
                (proxy-super remove))
              (.destroy model)
              (.destroy animationHandler))
            )]
    (.setInvisible p true)
    (.setBaseValue (.getAttribute p Attribute/GENERIC_MOVEMENT_SPEED) 0.05)
    (.addAIGroup p
                 (List/of (mkmovgoal p animationHandler))
                 (List/of (ClosestEntityTarget. ^EntityCreature p 32.0 ^Predicate (reify Predicate
                                                       (test [this t]
                                                         (instance? Player t))))))
    (.playRepeat animationHandler "idle")
    (.join (.setInstance p ^Instance instance ^Pos pos ))
    ))
