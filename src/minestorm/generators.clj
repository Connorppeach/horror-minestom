(ns minestorm.generators
  (:require [minestorm.biomes :as bio]
            [minestorm.constants :as consts]
            [minestorm.objects :as objs]);[thi.ng.math.noise :as n]
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block Block$Setter]
           [net.minestom.server.coordinate Vec Pos Point]
                                        ; entitys
           [net.minestom.server.entity EntityType Entity Player EntityCreature]
           [net.minestom.server.entity.ai EntityAIGroupBuilder ]
           [net.minestom.server.entity.ai.goal  RandomLookAroundGoal RandomStrollGoal MeleeAttackGoal]
           [net.minestom.server.entity.ai.target LastEntityDamagerTarget ClosestEntityTarget]
           
           [net.minestom.server.instance.generator UnitModifier GenerationUnit]
           [net.minestom.server.event EventNode]
           [net.minestom.server.world.biome Biome Biome$Builder]
           [net.minestom.server.command.builder CommandExecutor Command CommandContext]
           [net.minestom.server.command CommandSender]
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]
           [net.minestom.server.timer TaskSchedule]
           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [com.github.fastnoise FastNoise FastNoise$OutputMinMax FastNoise$Metadata FloatArray]
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
                                        ;java 
           [java.util List]
           )
  )

(set! *warn-on-reflection* true)



(def mbiome nil)








(defn ^Float getnoise
  ([^FastNoise noise x z seed]
   ;(println (/ (+ (.genSingle2D noise x z seed) 1) 2))
    (/ (+ (.genSingle2D noise x z seed) 1) 2)
   )
  ([^FastNoise noise x y z seed]
   (.genSingle3D noise x y z seed)
   )
  ([^FastNoise noise x y z w seed]
   (.genSingle4D noise x y z w seed)
   ))
;(def biome-noisep1 (FastNoise. "OpenSimplex2S"))
(def biome-noise (FastNoise. "Simplex"))
(def perlin (FastNoise. "Perlin"))



(defn init
  []
  (bio/init)
  )
(defn genunit [unit]
  (let [start ^Point (.absoluteStart ^GenerationUnit unit)
            size ^Point (.size ^GenerationUnit unit)]
        
        (doseq [x (range (.blockX ^Point size))
                z (range (.blockZ ^Point size))]
          (let [rx (float (+ ^Point (.blockX ^Point start) x))
                rz (float (+ ^Point (.blockZ ^Point start) z))
                bnoise (* 1 (getnoise perlin (* rx 0.01) (* rz 0.01) (+ 13122 @consts/seed)))
                temperature (* bio/maxtemp (getnoise biome-noise (* rx 0.003) (* rz 0.003) (+ 45324 @consts/seed)))
                humidity (*  bio/maxhumidity (getnoise biome-noise (* rx 0.003) (* rz 0.003) (+ 19232 @consts/seed)))
                ;e (println (str "temp: " temperature
                ;                "\n"
                ;                "humidity: " humidity))
                biomeblend (loop
                               [totalweight 0.0
                                totalheight 0.0
                                biome bio/biomes
                                weightsources (transient [])
                                ]
                             (if (not (empty? biome))
                               (do
                                 ;(println (first biome))
                                 (let [dist (+ (java.lang.Math/pow (- (:temp (first biome)) temperature) 2) (java.lang.Math/pow (- (:humidity (first biome)) humidity) 2))
                                       startheight ((:startheightfn (first biome)) rx rz dist)
                                       influence (max 0 (- (* startheight 1) (* startheight dist dist)));2.83
                                       ;f (println (str influence ": " (:name (first biome))))
                                       ;startheight 
                                       weight influence
                                       height (* weight ((:heightfn (first biome)) rx rz bnoise))
                                       ];(/ (- influence (- startheight (* bnoise (:bweight (first biome))))) influence)
                                   (recur (+ totalweight weight)
                                          (+ totalheight height)
                                          (next biome)
                                          (conj! weightsources [weight biome])
                                          )
                                     )
                                   
                                   )
                                 
                               [  (/ totalheight totalweight)
                                (sort #(compare (first %2) (first %)) (persistent! weightsources))])
                               )
                topbiome (first (second (first (second biomeblend))))
                fillfn (:fillfn topbiome)
                ]
            ;(println (* (first biomeblend) 255))
            (doseq [y (range -64 (int (first biomeblend)))]
              (fillfn {:blend biomeblend} rx y rz unit))
            (doseq [y (range -64 300)]
              ((:biome topbiome) rx y rz unit))
            )
            )
          
          ))

(defn plotworld [^GenerationUnit unit]
  
  (let [start ^Point (.absoluteStart ^GenerationUnit unit)
        size ^Point (.size ^GenerationUnit unit)]
    (if (and (< (if (neg? (.blockX start)) (- (- (.blockX start)) 1) (.blockX start)) 32) (< (if (neg? (.blockZ start)) (- (- (.blockZ start)) 1) (.blockZ start)) 32))
      (genunit unit))))




(defn mkgen []
  (reify
    net.minestom.server.instance.generator.Generator
    (generate [this unit]
      (genunit unit))))



