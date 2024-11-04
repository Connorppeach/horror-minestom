(ns minestorm.biomes
  (:require [minestorm.objects :as objs]
            [minestorm.constants :as consts]
            );[thi.ng.math.noise :as n]
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
           [com.github.fastnoise FastNoise FastNoise$OutputMinMax FastNoise$Metadata]
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
           [net.minestom.server.registry DynamicRegistry DynamicRegistry$Key]
                                        ;java 
           [java.util List]
           )
  )
(def noiseS2 (FastNoise. "OpenSimplex2"))

(def noiseSpikes (FastNoise. "Simplex"))


(defn ^Float getnoise
  ([^FastNoise noise x z seed]
   ;(println (.genSingle2D noise x z seed))
   (/ (+ (.genSingle2D noise x z seed) 1) 2)
   )
  ([^FastNoise noise x y z seed]
   (.genSingle3D noise x y z seed)
   )
  ([^FastNoise noise x y z w seed]
   (.genSingle4D noise x y z w seed)
   ))




(def mbiome nil)
(defn init
  []
  (def b ^Biome$Builder (Biome/builder))
  (def mb (do 
            (.downfall ^Biome$Builder b 0.3)
            (.temperature ^Biome$Builder b 0.3)
            (.build ^Biome$Builder b)))

  (def mbiome (.register (MinecraftServer/getBiomeRegistry) "corruption" mb))
)




(defn mountainfn [mmap x y z ^GenerationUnit unit]
  (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/STONE)
  )

(defn plainsfn [mmap x y z ^GenerationUnit unit]
  
  (cond (= y (- (int (first (:blend mmap))) 1))
        (cond (< 0.98 (getnoise noiseS2 x z (+ 213 @consts/seed)))
              (objs/tree x y z unit Block/OAK_LEAVES Block/OAK_LOG 0 Biome/DEEP_COLD_OCEAN)
              :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/GRASS_BLOCK))
        :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/DIRT)
        )
  )

(defn corruptionfn [mmap x y z ^GenerationUnit unit]
  
  (cond (= y (- (int (first (:blend mmap))) 1))
        (cond (< 0.98 (getnoise noiseS2 x z (+ 213 @consts/seed)))
              (objs/tree x y z unit Block/OAK_LEAVES Block/OAK_LOG 0 mbiome)
              :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/GRASS_BLOCK))
        :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/DIRT)
        )
  )

(defn desertfn [mmap x y z ^GenerationUnit unit]
  ;(println (- y (+ (int (first (:blend mmap))) 1)))
  (cond (= y (- (int (first (:blend mmap))) 1))
        (cond (< 0.97 (getnoise noiseS2 x z (+ 213 @consts/seed)))
              (objs/pillar x (+ y 1) z unit (rand-int 4) Block/CACTUS)
              :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/SAND))
        :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/SANDSTONE)
        )
  )
(defn oceanfn [mmap x y z ^GenerationUnit unit]
  (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/DEEP_COLD_OCEAN)
  ;(println (- y (+ (int (first (:blend mmap))) 1)))
  (cond (< y -2)
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/SAND)
        :else
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/WATER)
        )
  )


(defn junglefn [mmap x y z ^GenerationUnit unit]
  ;(println (- y (+ (int (first (:blend mmap))) 1)))
  (cond (= y (- (int (first (:blend mmap))) 1))
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/GREEN_CONCRETE)
        :else
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/DIRT)
        )
  )


(defn deepdesertfn [mmap x y z ^GenerationUnit unit]
  ;(println (- y (+ (int (first (:blend mmap))) 1)))
  (cond (= y (- (int (first (:blend mmap))) 1))
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/YELLOW_CONCRETE)
        :else
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/DIRT)
        )
  )
(defn indevfn [mmap x y z ^GenerationUnit unit]
  
  (cond (= y (- (int (first (:blend mmap))) 1))
        (cond (< 0.95 (+ 213 @consts/seed))
              (objs/tree x y z unit Block/RED_MUSHROOM_BLOCK Block/MUSHROOM_STEM (rand-int 20) Biome/DEEP_COLD_OCEAN)
              :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/GRASS_BLOCK))
        :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/DIRT)
        )
  )

(defn hellfn [mmap x y z ^GenerationUnit unit]
  ;(println (- y (+ (int (first (:blend mmap))) 1)))
  (let [n (+ 213 @consts/seed)]
    (cond (= y (- (int (first (:blend mmap))) 1))
        (cond (< 0.995 n) (objs/rock x y z unit (+ (rand-int 5) 2))
              (< 0.9 n)
              (do (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float (+ y 1)) (float z) Block/FIRE)
                  (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/NETHERRACK))
              (< 0.6 n)
              (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/NETHERRACK)
              :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/RED_CONCRETE))
        :else (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/NETHERRACK)
        ))
  )

(def windhills (fn [x y z unit]
                   (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/FROZEN_RIVER)

                 ))
(def desert (fn [x y z unit]
                   (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/DESERT)

                 ))
(def ocean (fn [x y z unit]
                   (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/LUKEWARM_OCEAN)

                 ))
(def plains (fn [x y z unit]
                   (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/PLAINS)

                 ))


(def biomes
  [{:startheightfn (fn [x y n] 1); mountains
    :heightfn #(do (+ 10 (* 20 %3)))
    :fillfn mountainfn
    :bweight 0.2
    :biome windhills
    :temp 0
    :name "mountains"
    :humidity 0
    }
   {:startheightfn (fn [x y n] 1); desert
    :heightfn #(do (+ 0 (* 10 %3)))
    :fillfn desertfn
    :bweight 1
    :temp 0
    :biome desert
    :humidity 1
    :name "desert"
    }
   {:startheightfn (fn [x y n] 1) ; indev
    :heightfn (fn [x y n] (+ 0 (* (getnoise noiseSpikes (/ x 50) (/ y 50) 20) (+ 213 @consts/seed))))
    :fillfn indevfn
    :bweight 1
    :biome windhills
    :temp 0
    :humidity 2
    :name "indev"
    }


   {:startheightfn (fn [x y n] 1) ; jungle
    :heightfn #(do (+ 0 (* 10 %3)))
    :fillfn junglefn
    :bweight 1
    :biome windhills
    :temp 1
    :humidity 0
    :name "jungle"
    }
   {:startheightfn (fn [x y n] 1); plains
    :heightfn #(do %3 (* 10 %3))
    :fillfn plainsfn
    :bweight 1
    :biome plains
    :temp 1 
    :humidity 1
    :name "plains"
    }
   {:startheightfn (fn [x y n] 20) ; ocean
    :heightfn (fn [x y n] 0)
    :bweight 1
    :fillfn oceanfn
    :biome ocean
    :temp 1 
    :humidity 2
    :name "ocean"
    }


   {:startheightfn (fn [x y n] 1) ; deep desert
    :heightfn #(do (+ 0 (* 10 %3)))
    :fillfn deepdesertfn
    :biome desert
    :bweight 1
    :temp 2 
    :humidity 0
    :name "deep desert"
    }
   {:startheightfn (fn [x y n] 1); corruption
    :heightfn #(do (+ 0 (* 10 %3)))
    :biome (fn [x y z unit] (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) mbiome))
    :fillfn corruptionfn
    :bweight 1
    :temp 2
    :humidity 1
    :name "corruption"
    }
   {:startheightfn (fn [x y n] 1) ; hell
    :heightfn #(do (+ 0 (* 10 %3)))
    :fillfn hellfn
    :biome windhills
    :bweight 1
    :temp 2 
    :humidity 2
    :name "hell"
    }

   ])
(def maxtemp (apply max (mapv #(:temp %) biomes)))
(def maxhumidity (apply max (mapv #(:humidity %) biomes)))

