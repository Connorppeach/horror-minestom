(ns minestorm.core
  (:gen-class)
  (:require [minestorm.generators :as gen]
            [minestorm.commands :as cmds]
            [minestorm.constants :as consts]
            [minestorm.pack :as pack]
            [minestorm.steve :as steve]
            )
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Vec Pos Point]
                                        ; entitys
           [net.minestom.server.entity EntityType Entity Player EntityCreature]
           
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]
           [net.minestom.server.timer TaskSchedule]
           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
           [net.worldseed.multipart ModelEngine]
                                        ;java 
           
           ))

(set! *warn-on-reflection* true)



(defn deletefn
  [mentry iters]
  nil)
(defn bouncefn
  [mentry iters]
  (.teleport  ^Entity (:entity mentry) (.add (.getPosition ^Entity (:entity mentry)) 0.0 (+ (float (:y (:velocity mentry))) 1) 0.0))
  
  (if (nil? (:bcount mentry))
    (assoc-in (assoc mentry :bcount 2) [:velocity :y] 1)
    (if (> (:bcount mentry) 0)
      (assoc-in (update mentry :bcount - 0.5) [:velocity :y] (/ (:bcount mentry) 2))
      nil)
    ))
(defn stockmove
  [mentry iters]
  (.setVelocity ^Entity (:entity mentry) (Vec. 0.0))
  (.teleport  ^Entity (:entity mentry)
              (.withView
               (.add (.getPosition ^Entity (:entity mentry)) (float (:x (:velocity mentry))) (float (:y (:velocity mentry))) (float (:z (:velocity mentry))))
               (+ (.yaw (.getPosition ^Entity (:entity mentry))) (:yaw (:velocity mentry))) (+ (:pitch (:velocity mentry)) (.pitch (.getPosition ^Entity (:entity mentry))))))
  (update-in mentry [:velocity :y] #(max -1 (- % 0.05))))
(defn dloop
  [mmap speed iterations movfn colfn]
  (let [mvec (vec (remove nil? (for [mentry mmap] 
                                 (do 
                                   (if (not (.isAir (.getBlock (.getInstance  ^Entity (:entity mentry)) (.getPosition  ^Entity (:entity mentry)))))
                                     (or (colfn mentry iterations) (.remove ^Entity (:entity mentry)))
                                     (movfn mentry iterations))))))]
    (if (not (nil? (:entity (first mvec))))
      (.scheduleTask (.scheduler ^Entity (:entity (first mvec)))
                     (reify Runnable
                       (run [this]
                         (dloop mvec speed iterations movfn colfn)
                         )) (TaskSchedule/tick speed) (TaskSchedule/stop)))))
(defn circle
  [r]
  (let [r (min r 18)]
  (loop [pointlist '()
         x (atom (long r))
         y (atom (long 0))
         P (atom (long (- r 1)))]
    (reset! y (+ @y 1))
    (if (<= @P 0)
      (reset! P (+ (* (+ @P 2) @y) 1))
      (do
        (reset! x (- @x 1))
         (reset! P (+ (* (- (* (+ @P 2) @y) 2) @x) 1)))
      )
    (if (< @x @y)
      (rest pointlist)
      (recur
       (list*
        {:x @x :z @y} {:x (- @x) :z @y} {:x @x :z (- @y)} {:x (- @x) :z (- @y)}  {:x @y :z @x} {:x (- @y) :z @x} {:x @y :z (- @x)} {:x (- @y) :z (- @x)}
        pointlist
        )
       x y P
       )
      )
    )
  ))
(defn circle3d
  [r]
  (loop [pointlist '()
         y (- r)]
    (if (> y r)
      pointlist
      (recur
       (list* (map #(assoc % :y y)
                   (apply vector (circle (- (abs r) (abs y))))) pointlist)
       (+ y 1.4)))
    ))

(defn explodefn
  [mentry iters]
  (let [point (.getPosition ^Entity (:entity mentry))]
    (dloop (remove nil? (vec (flatten (for [current (circle3d @consts/power)
                                            rpos current]
                                        (let [iterator (GridCast/createGridIterator (.x point) (.y point) (.z point) (:x rpos) (:y rpos) (:z rpos) 1.0 @consts/power)]
                                          (loop [power @consts/power vlist []]
                                            (if (and (< 0 power) (.hasNext iterator))
                                              (let [n ^Vector3d (.next iterator)]
                                                (let [old (.getBlock (.getInstance ^Entity (:entity mentry)) (float (.x n)) (float (.y n)) (float (.z n)))]
                                                  (recur (long (- power (+ (rand-int 2) (.explosionResistance (.registry old)))))
                                                         (conj vlist
                                                               (if (not (.isAir old))
                                                                 (do (let [e ^Entity (proxy  [Entity]
                                                                                         [EntityType/BLOCK_DISPLAY])
                                                                           pos ^Pos (.getPosition ^Entity (:entity mentry))
                                                                           instance ^Instance (.getInstance ^Entity (:entity mentry))]
                                                                       (.setNoGravity  ^Entity e true)
                                                                       (.setPosRotInterpolationDuration ^BlockDisplayMeta (.getEntityMeta e) 2)
                                                                       (.setBlockState ^BlockDisplayMeta (.getEntityMeta e) old)
                                                                       (.setInstance  ^Entity e  ^Instance instance (Pos. (float (.x n)) (float (.y n)) (float (.z n)) (- (rand-int 360) 180) (- (rand-int 360) 180)))
                                                                       (.setBlock instance (float (.x n)) (float (.y n)) (float (.z n)) Block/AIR)
                                                                       (.scheduleTask (.scheduler instance)
                                                                                      (reify Runnable
                                                                                        (run [this]
                                                                                          (.setBlock instance (float (.x n)) (float (.y n)) (float (.z n)) old)

                                                                                          )) (TaskSchedule/tick (+ 40 (rand-int 10))) (TaskSchedule/stop))
                                                                       {:entity e :velocity {:x (/ (- (.x n) (.x pos)) 10) :y (/ (+ 5 (- (.y n) (.y pos))) 5) :z (/ (- (.z n) (.z pos)) 10) :pitch (- 3 (rand-int 6)) :yaw (- 3 (rand-int 6))}}))))))) vlist)))
                                        )))) 1 0 stockmove bouncefn)) nil)

(defn summon-tnt
  [instance pos d]

  (let [p ^Entity (proxy  [Entity]
                      [EntityType/BLOCK_DISPLAY])]
    (.setNoGravity  ^Entity p true)
    (.setBlockState ^BlockDisplayMeta (.getEntityMeta p) Block/TNT)
    (.setInstance  ^Entity p  ^Instance instance ^Pos pos)
    (.setPosRotInterpolationDuration ^BlockDisplayMeta (.getEntityMeta p) 2)
    (dloop [{:entity p :velocity d}] 1 0 stockmove explodefn)))



(defn -main
  "main"
  [& args]
  (def server (MinecraftServer/init))
  (def iManager (MinecraftServer/getInstanceManager))
  (def instance (.createInstanceContainer ^InstanceManager iManager))
  
  (gen/init)

                                        ;pack
  (pack/init)

  
  (def gEventHandler (MinecraftServer/getGlobalEventHandler))
  (.setChunkLoader  ^InstanceContainer instance ^IChunkLoader (AnvilLoader. "worlds/test"))
  (.setChunkSupplier ^Instance instance
                     (reify
                       net.minestom.server.utils.chunk.ChunkSupplier
                       (createChunk [this instance chunkx chunky]
                         (net.minestom.server.instance.LightingChunk. instance chunkx chunky))))
  (.setGenerator ^Instance instance
                 (gen/mkgen))

  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.AsyncPlayerConfigurationEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (.setSpawningInstance ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event ^Instance instance)
                    (.setRespawnPoint (.getPlayer ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event) ^Point (Pos. 0.0 160.0 0.0)))))


  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerChunkUnloadEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (let [event ^net.minestom.server.event.player.PlayerChunkUnloadEvent event]
                      (if (not= nil (.getInstance event))
                        (let [chunk (.getChunk ^InstanceContainer (.getInstance event) (.getChunkX event) (.getChunkZ event))
                              instance ^InstanceContainer (.getInstance event)]
                          (if (and (not= instance nil) (not= chunk nil) (= (.size ^java.util.Set (.getViewers chunk)) 0))
                            (.saveChunkToStorage instance chunk)
                            (.unloadChunk instance chunk))))))))

  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerSpawnEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (.setGameMode ^Player (.getPlayer ^net.minestom.server.event.player.PlayerSpawnEvent event) net.minestom.server.entity.GameMode/CREATIVE)
                    (.sendResourcePacks
                     ^Player (.getPlayer ^net.minestom.server.event.player.PlayerSpawnEvent event)
                     (let [b ^ResourcePackRequest$Builder (ResourcePackRequest/resourcePackRequest)]
                       (.required b true)
                       (.replace b true)
                       (.packs b ^ResourcePackInfoLike [(.get (.computeHashAndBuild (.uri (ResourcePackInfo/resourcePackInfo) (java.net.URI/create "https://github.com/Connorppeach/deconstruct-minestom/releases/download/v0.0.1/pack.zip"))))])
                       ^ResourcePackRequest (.build b)
                       )
                     ))))

  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerHandAnimationEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (let [sender ^Player (.getPlayer ^net.minestom.server.event.player.PlayerHandAnimationEvent event)]
                      (if (= Material/TNT (.material (.getItemInMainHand ^Player (.getPlayer ^net.minestom.server.event.player.PlayerHandAnimationEvent event) )))
                        (summon-tnt ^Instance (.getInstance sender) ^Pos (.getPosition sender)
                                    (let [pos (.direction (.getPosition sender))]
                                      {:x (.x pos) :y (+ (.y pos) 0.5) :z (.z pos) :pitch (- 3 (rand-int 6)) :yaw (- 3 (rand-int 6))})))))))
  

  
  (cmds/init instance)
                                        ;(net.minestom.server.extras.velocity.VelocityProxy/enable "hXt2TN42ucml"); REPLACE ME WITH YOUR OWN KEY TO USE VELOCITY
  (.start ^MinecraftServer server "0.0.0.0" 25565)
  )

