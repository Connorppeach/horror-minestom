(ns minestorm.objects
  ;(:require );[thi.ng.math.noise :as n]
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.instance.block Block Block$Setter]
           [net.minestom.server.coordinate Vec Pos Point]
           [net.minestom.server.instance.generator UnitModifier GenerationUnit]
           [net.minestom.server.world.biome Biome Biome$Builder]
           [net.minestom.server.timer TaskSchedule]
                                        ;fastnoise
           [noise FastNoiseLite FastNoiseLite$NoiseType FastNoiseLite$FractalType]
           )
  )
(def noiseS3 (FastNoiseLite.))

(defn bush
  [x y z unit start top biome]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range -3 2)
                     x2 (range -1 2)
                     z2 (range -1 2)
                     :when (and (< (abs z2) (- y2)) (< (abs x2) (- y2)))
                     ]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y 3 y2)) (float (+ z z2)) top))
             
             )
           )))

(defn bush
  [x y z unit start top biome]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range -3 2)
                     x2 (range -1 2)
                     z2 (range -1 2)
                     :when (and (< (abs z2) (- y2)) (< (abs x2) (- y2)))
                     ]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y 3 y2)) (float (+ z z2)) top))
             
             )
           )))

(defn baloon
  [x y z unit start biome]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range -6 4)
                     z2 (range -3 4)
                     x2 (range -3 4)
                     :when (and (> (+ 1 (abs x2)) (- y2)) (> (+ 1 (abs z2)) (- y2))
                                (< (- (abs z2) 3) (- y2)) (< (- (abs x2) 3) (- y2)) )
                     ]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y 10 y2)) (float (+ z z2)) Block/RED_CONCRETE))
             
             )
           )))

(defn tree
  [x y z unit top bottom height biome]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range -4 3)
                     x2 (range -2 3)
                     z2 (range -2 3)
                     :when (and (< (if (< z2 0) (- z2) z2) (- y2)) (< (if (< x2 0) (- x2) x2) (- y2)))]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y y2 height 8)) (float (+ z z2)) top))
             (doseq [y (range y (+ y height 5))]
               (.setBlock ^Block$Setter setter (float x) (float y) (float z) bottom)
               )
             )
           )))


(defn rock
  [x y z unit size]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y2 (range (- size) (+ size 1))
                     x2 (range (- size) (+ size 1))
                     z2 (range (- size) (+ size 1))
                     :when (> (+ (/ (+ 1 (.GetNoise noiseS3 x y z)) 4)
                                 (/ (- 12 (+ (abs x2) (abs y2) (abs z2))) 24)) 0.5)]
               (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y y2)) (float (+ z z2)) Block/DEAD_TUBE_CORAL_BLOCK))
             
             )
           )))
(defn pillar
  [x y z unit height top]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (doseq [y (range y (+ y height))]
               (.setBlock ^Block$Setter setter (float x) (float y) (float z) top)
               )
             )
           )))
(defn grass
  [x y z unit]
  (.fork ^GenerationUnit unit
         (reify
           java.util.function.Consumer
           (accept [this setter]
             (.setBlock ^Block$Setter setter (float x) (float (+ y 1)) (float z) Block/SHORT_GRASS)))))
