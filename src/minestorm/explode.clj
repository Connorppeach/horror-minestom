(ns minestorm.explode
  (:require [minestorm.constants :as consts]
            [minestorm.db :as db]
            [clojure.core.reducers :as reducers]
            );
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Vec Pos Point]
           [net.minestom.server.instance.batch BatchOption AbsoluteBlockBatch]
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
           [net.minestom.server.entity.metadata.other FallingBlockMeta]
           [dev.emortal.rayfast.vector Vector3d]
           [net.worldseed.multipart ModelEngine]
                                        ;java 
           
           ))

(set! *warn-on-reflection* true)

(defn ^Entity getentity
  [map]
  ^Entity (first map))
(defn mkblock
  [^Instance i ^Pos pos ^Integer b]
  [
   (let [block (proxy  [Entity]
                   [EntityType/FALLING_BLOCK])]

     (.updateViewableRule block
                          (reify
                            java.util.function.Predicate
                            (test [this t]
                              (if (instance? Player t)
                                (.equals "falling" (db/get-prop (.getUsername ^Player t) :blockstyle))))))

     (.setBlock ^FallingBlockMeta (.getEntityMeta block ) (Block/fromBlockId b))

     (.setInstance block i pos)
     (.setNoGravity block true)
     block
     )
   (let [p ^Entity (proxy  [Entity]
                       [EntityType/BLOCK_DISPLAY])]
     (.updateViewableRule p
                          (reify
                            java.util.function.Predicate
                            (test [this t]
                              (if (instance? Player t)
                                (.equals "display" (db/get-prop (.getUsername ^Player t) :blockstyle))))))
     (.setNoGravity  ^Entity p true)
     (.setBlockState ^BlockDisplayMeta (.getEntityMeta p) (Block/fromBlockId b))

     (.get (.setInstance  ^Entity p  ^Instance i ^Pos pos))
     (.setPosRotInterpolationDuration ^BlockDisplayMeta (.getEntityMeta p) 1)
     p)]
  )

(defn setblockpos
  [blocks ^Pos pos]
  (doseq [i (map #(.teleport ^Entity % pos) blocks)]
    (.join  ^java.util.concurrent.CompletableFuture i))
  blocks
  )
(defn removeblock
  [blocks]
  (doseq [i blocks]
    (.remove ^Entity i))
  nil
  )

(defn deletefn
  [mentry iters]
  nil)
(defn bouncefn
  [mentry iters]
  (.teleport  ^Entity (getentity (:entity mentry)) (.add (.getPosition ^Entity (getentity (:entity mentry))) 0.0 (+ (float (:y (:velocity mentry))) 1) 0.0))
  
  (if (nil? (:bcount mentry))
    (assoc-in (assoc mentry :bcount 1) [:velocity :y] 1)
    (if (> (:bcount mentry) 0)
      (assoc-in (update mentry :bcount - 0.5) [:velocity :y] (/ (:bcount mentry) 2))
      nil)
    ))



(defn lightningfn [entry iters]
  (if (> iters 20)
    (let [lightning (Entity. EntityType/LIGHTNING_BOLT)]
      (.setSilent lightning true)
      (.setInstance lightning (.getInstance ^Entity (getentity (:entity entry))) (.getPosition ^Entity (getentity (:entity entry))))

      (.scheduleTask (.scheduler lightning)
                     (reify Runnable
                       (run [this]
                         (.remove lightning)
                         )) (TaskSchedule/tick 2) (TaskSchedule/stop))
      
      )) nil)

(defn stockmove
  [mentry iters]
  (if (< 400 iters)
    (removeblock (:entity mentry))
    (do (setblockpos (:entity mentry)
                     (.withView
                      (.add (.getPosition ^Entity (getentity (:entity mentry))) (float (:x (:velocity mentry))) (float (:y (:velocity mentry))) (float (:z (:velocity mentry))))
                      (+ (.yaw (.getPosition ^Entity (getentity (:entity mentry)))) (:yaw (:velocity mentry))) (+ (:pitch (:velocity mentry)) (.pitch (.getPosition ^Entity (getentity (:entity mentry)))))))
        (update-in mentry [:velocity :y] #(max -1 (- % 0.05))))))

(defn dloop
  [mmap speed iterations movfn colfn scheduler]
  (let [mvec (loop [mentryi mmap
                    nextgen (transient [])] 
               (if (not (empty? mentryi))
                 (let [mentry (first mentryi)
                       nextentry (if (not (nil? (.getInstance  ^Entity (getentity (:entity mentry)))))
                                   (if (and (not (.isAir (.getBlock (.getInstance  ^Entity (getentity (:entity mentry))) (.getPosition  ^Entity (getentity (:entity mentry)))))))
                                     (or (colfn mentry iterations) (removeblock (:entity mentry)))
                                     (movfn mentry iterations))
                                   (removeblock (:entity mentry)))]
                   (recur (next mentryi)
                          (if (nil? nextentry)
                            nextgen
                            (conj! nextgen nextentry)))
                   )
                 (persistent! nextgen)
                 ))]
    (if (not (nil? (getentity (:entity (first mvec)))))
      (.scheduleTask ^net.minestom.server.timer.Scheduler scheduler
                     (reify Runnable
                       (run [this]
                         (dloop mvec speed (+ iterations 1) movfn colfn scheduler)
                         )) (TaskSchedule/tick speed) (TaskSchedule/stop)))))


(defn circle
  [r]
  (let [r (min r 18)]
    (loop [pointlist (transient [])
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
        (persistent! pointlist)
        (recur
         (loop [list [{:x @x :z @y} {:x (- @x) :z @y} {:x @x :z (- @y)} {:x (- @x) :z (- @y)}  {:x @y :z @x} {:x (- @y) :z @x} {:x @y :z (- @x)} {:x (- @y) :z (- @x)}]
                plist pointlist] 
           (if (not (empty? list))
             (recur (next list)
                    (conj! plist (first list)))
             plist)
           )
         x y P
         )
        )
      )
    ))
(defn circle3d
  [r]
  (loop [pointlist (transient [])
         y (- r)]
    (if (> y r)
      (persistent! pointlist)
      (recur
       (loop
           [plist (map #(assoc % :y y)
                       (apply vector (circle (- (abs r) (abs y)))))
            pointlist pointlist]
         (if (not (empty? plist))
           (recur
            (next plist)
            (conj! pointlist (first plist)))
           pointlist))
       (+ y 1.9)))
    ))


(defn replaceloop
  [mmap speed iterations waittime ^net.minestom.server.timer.Scheduler scheduler]
  (let [mvec  (loop [mentryi mmap
                     nextgen (transient [])] 
                (if (not (empty? mentryi))
                  (let [mentry (first mentryi)
                        nextentry (if (and (> iterations waittime) (= 0 (rand-int 2)))
                                    (do (.setBlock ^Instance (:instance mentry) (float (:x mentry)) (float (:y mentry)) (float (:z mentry)) ^Block (:old mentry)) nil)
                                    mentry)]
                    (recur (next mentryi)
                           (if (nil? nextentry)
                             nextgen
                             (conj! nextgen nextentry)))
                    )
                  (persistent! nextgen)
                  ))]
    (if (not (nil? (first mvec)))
      (.scheduleTask scheduler
                     (reify Runnable
                       (run [this]
                         (replaceloop mvec speed (+ iterations 1) waittime scheduler)
                         )) (TaskSchedule/tick speed) (TaskSchedule/stop)))))


(defn explodefn
  [mentry iters]

  (let [point (.getPosition ^Entity (getentity (:entity mentry)))
                                        ;batch (AbsoluteBlockBatch. (-> (BatchOption.)
                                        ;                               (.setUnsafeApply true)
                                        ;                               ))
        setblocks (atom (hash-set))
        blocks2 (loop [rpos (circle3d (+ @consts/power (:power mentry)))
                       blocks (transient [])
                       entities (transient [])]
                  (if (not (empty? rpos))
                    (let [iterator (GridCast/createGridIterator (.x point) (.y point) (.z point) (:x (first rpos)) (:y (first rpos)) (:z (first rpos)) 1.0 (+ @consts/power (:power mentry)))
                          result
                          (loop [power (+ @consts/power (:power mentry))
                                 entities entities
                                 blocks blocks
                                 ]
                            (if (and (< 0 power) (.hasNext iterator))
                              (let [n ^Vector3d (.next iterator)]
                                (let [old (.getBlock (.getInstance ^Entity (getentity (:entity mentry))) (float (.x n)) (float (.y n)) (float (.z n)))]
                                  (if (not (.isAir old))
                                    (recur (long (- power (+ (rand-int 2) (.explosionResistance (.registry old)))))
                                           (if (= (rand-int @consts/yeild) 0)
                                             (conj! entities
                                                    (let [pos ^Pos (.getPosition ^Entity (getentity (:entity mentry)))
                                                          instance ^Instance (.getInstance ^Entity (getentity (:entity mentry)))]
                                                      
                                                      
                                                      
                                                      {:entity (mkblock instance (Pos. (float (.x n)) (float (.y n)) (float (.z n)) (- (rand-int 360) 180) (- (rand-int 360) 180))
                                                                        (.id (.getBlock instance (float (.x n)) (float (.y n)) (float (.z n))))                                                                                                     ) :velocity {:x (/ (- (.x n) (.x pos)) 10) :y (/ (+ 5 (- (.y n) (.y pos))) 5) :z (/ (- (.z n) (.z pos)) 10) :pitch (- 3 (rand-int 6)) :yaw (- 3 (rand-int 6))}})
                                                    )
                                             entities)
                                           (conj! blocks  (let [instance (.getInstance ^Entity (getentity (:entity mentry)))
                                                                bmap {:x (.x n) :y (.y n) :z (.z n) :instance instance :old old}]
                                                            
                                                            (.setBlock instance (float (.x n)) (float (.y n)) (float (.z n)) Block/AIR)
                                                            bmap)))
                                    
                                    (recur (long (- power (+ (rand-int 2)))) entities blocks)
                                    )
                                  )) [blocks entities]))]
                      (recur (next rpos)
                             (first result)
                             (second result)
                             )
                      
                      )
                    
                    [(persistent! blocks) (persistent! entities)]))
        ]

                                        ;(.unsafeApply batch (.getInstance ^Entity (:entity mentry))
                                        ;        (reify
                                        ;          java.lang.Runnable
                                        ;          (run [this]
                                        ;(println blocks2)
                                        ;(println (first blocks2))
    (let [
          blocks (first blocks2)
          entities (second  blocks2)

          ]
      (.playSound (.getInstance ^Entity (getentity (:entity (first entities))))
                  (net.kyori.adventure.sound.Sound/sound (net.kyori.adventure.key.Key/key "entity.generic.explode") net.kyori.adventure.sound.Sound$Source/BLOCK 5.0 1.0)
                  (.x point) (.y point) (.z point))
      ((:totalfn mentry) (count blocks))
      (replaceloop blocks 2 0 16 (.scheduler (.getInstance ^Entity (getentity (:entity (first entities))))))
      (dloop entities 1 0 stockmove (:deletefn mentry) (.scheduler (.getInstance ^Entity (getentity (:entity (first entities))))))
                                        ;(println blocks)
      )
                                        ;
                                        ;
                                        ;            )))
    ) nil)



(defn summon-tnt
  [instance pos d endfn power dropstyle]

  (let [p (mkblock instance pos (.id Block/TNT))]
    
    (dloop [{:entity p :velocity d :totalfn endfn :power (int power)
             
             :deletefn (cond (= dropstyle "reg") deletefn
                             (= dropstyle "bounce") bouncefn
                             (= dropstyle "lightning") lightningfn
                             (= dropstyle "recurse") deletefn
                             )}] 1 0 stockmove explodefn
           (.scheduler ^Instance instance))))
