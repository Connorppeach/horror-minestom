(ns minestorm.generators
  (:gen-class)
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
           [noise FastNoiseLite FastNoiseLite$NoiseType FastNoiseLite$FractalType]
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
                                        ;java 
           [java.util List]
           
           )
  )


(def noiseS (FastNoiseLite.))
(def noiseS2 (FastNoiseLite.))

(def noiseP (FastNoiseLite.))
(def mbiome nil)


(defn gen-tree
  [x y z unit start top bottom biome]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range -4 3)
                     x2 (range -2 3)
                     z2 (range -2 3)
                     :when (and (< (if (< z2 0) (- z2) z2) (- y2)) (< (if (< x2 0) (- x2) x2) (- y2)))]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y y2 8)) (float (+ z z2)) top))
             (doseq [y (range y (+ y 5))]
               (.setBlock ^Block$Setter setter (float x) (float y) (float z) bottom)
               )
             )
           )))
(defn gen-grass
  [x y z unit]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (.setBlock ^Block$Setter setter (float x) (float (+ y 1)) (float z) Block/SHORT_GRASS)))))


(defn fillstock
  [mmap unit x z]
  (let [height2 (.GetNoise ^FastNoiseLite noiseS x z)
        interest (.GetNoise ^FastNoiseLite noiseS (* x 40) (* z 40))]
    (doseq [y (range -64  (+ 120 (- (* 10 height2) 2)))]
      (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/PLAINS))
    (doseq [y (range -64 (+ 100 (- (* 10 height2) 2)))]
      (cond
        (= (int y) -64) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/BEDROCK)
        (= (int y) (int (+ 100  (- (* 10 height2) 2)))) (do
                                                          (cond
                                                            (> interest 0.95)
                                                            (gen-tree x y z unit ^Point (:start mmap) Block/OAK_LEAVES Block/OAK_LOG Biome/PLAINS)
                                                            (> interest 0.7)
                                                            (gen-grass x y z unit))
                                                          (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap)))
        :else
        (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (long x) (long y) (long z) (:lower mmap))))))

(defn fillcorruption
  [mmap unit x z]
  (let [height2  (.GetNoise ^FastNoiseLite noiseS x z)
        holes (java.lang.Math/abs (.GetNoise ^FastNoiseLite noiseS2 x z))
        interest (.GetNoise ^FastNoiseLite noiseS (* x 40) (* z 40))]
    (doseq [y (range -64 (+ 120 (- (* 10 height2) 2)))]
      (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) mbiome))

    (doseq [y (range -64 (+ 100  (- (* 10 height2) 2)))]
      (let [yrand (* (- (* y holes) 200) 0.8)]
        (cond
          (= (int y) -64) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/BEDROCK)
          (and (> (* 105 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.9))) nil
          (and (> (* 108 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.8))) (if (> (java.lang.Math/abs (.GetNoise ^FastNoiseLite noiseS (* 10 x) (* 10 y) (* 10 z))) 0.1)
                                                                                                                   (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/OBSIDIAN))
          
          (and (> (* 111 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.7))) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/OBSIDIAN)
          
          (= (int y) (int (+ 100 (- (* 10 height2) 2))))
          (do
            (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap))
            (cond
              (> interest 0.95)
              (gen-tree x y z unit ^Point (:start mmap) Block/OAK_LEAVES Block/OAK_LOG mbiome)
              
              (> interest 0.7)
              (gen-grass x y z unit)
              
              )
            (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap)))
          :else
          (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (long x) (long y) (long z) (:lower mmap)))))))
(defn init
  []
  (.SetNoiseType ^FastNoiseLite noiseS  FastNoiseLite$NoiseType/OpenSimplex2)
  (.SetFractalType ^FastNoiseLite noiseS  FastNoiseLite$FractalType/DomainWarpProgressive)
  (.SetSeed ^FastNoiseLite noiseS 123)
  (.SetFrequency ^FastNoiseLite noiseS 0.01)

  (.SetFractalType ^FastNoiseLite noiseP  FastNoiseLite$FractalType/DomainWarpProgressive)
  (.SetNoiseType ^FastNoiseLite noiseP  FastNoiseLite$NoiseType/OpenSimplex2)

  (.SetFrequency ^FastNoiseLite noiseP 0.005)
  (def b ^Biome$Builder (Biome/builder))
  (def mb (do 
            (.downfall ^Biome$Builder b 0.3)
            (.temperature ^Biome$Builder b 0.3)
            (.build ^Biome$Builder b)))

  (def mbiome (.register (MinecraftServer/getBiomeRegistry) "corruption" mb))
  )

(def temptable
  [; ifn takes in x y z height2 height3 interest
   {:temp 2 ; marsh
    :humidity 2
    :ifn #(fillstock (assoc % :upper Block/GRASS_BLOCK :lower Block/DIRT) %2 %3 %4)}
   {:temp 5 ; corruption
    :humidity 5
    :ifn #(fillcorruption (assoc % :upper Block/GRASS_BLOCK :lower Block/DEEPSLATE) %2 %3 %4)}])


(defn mkgen []
  (reify
    net.minestom.server.instance.generator.Generator
    (generate [this unit]
      (let [start ^Point (.absoluteStart ^GenerationUnit unit)
            size ^Point (.size ^GenerationUnit unit)]
        
        (doseq [x (range 0 (.blockX ^Point size))
                z (range 0 (.blockZ ^Point size))]
          (let [temperature (java.lang.Math/abs (* 10 (.GetNoise ^FastNoiseLite noiseP (/ (+ ^Point (.blockX ^Point start) x) 10) (/ (+ ^Point (.blockZ ^Point start) z) 10))))
                humidity (java.lang.Math/abs (* 10 (.GetNoise ^FastNoiseLite noiseP (/ (+ ^Point (.blockX ^Point start) x) 10) (/ (+ ^Point (.blockZ ^Point start) z) 10))))]
            (let [biome (loop [i 0.0 closest 1024.0 closestindex 0.0]
                          (if (< i (count temptable))
                            (let [x2 (nth temptable i)]
                              (if (< (+ (java.lang.Math/pow (- temperature (:temp x2)) 2) (java.lang.Math/pow (- humidity (:humidity x2)) 2)) closest  )
                                (recur (+ i 1) (+ (java.lang.Math/pow (- temperature (:temp x2)) 2) (java.lang.Math/pow (- humidity (:humidity x2)) 2)) i)
                                (recur (+ i 1) closest closestindex)))
                            {:biome (nth temptable closestindex) :dist closest}))]
              ((:ifn (:biome biome)) {:temp temperature :humidity humidity :start start :size size :dist (:dist biome)} ^GenerationUnit unit (+ ^Point (.blockX start) x) (+ ^Point (.blockZ start) z)))))))))
