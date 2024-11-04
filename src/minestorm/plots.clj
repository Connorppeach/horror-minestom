(ns minestorm.plots
  (:require [minestorm.imanager :as iman]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :refer [vec3 xyz]]
            [thi.ng.geom.voxel.svo :as svo]
            [thi.ng.geom.voxel.isosurface :as iso]
            [thi.ng.geom.mesh.io :as mio]
            [thi.ng.math.core :as m]
            [thi.ng.math.noise :as n]
            [minestorm.generators :as generators]
            )
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.instance.block.rule BlockPlacementRule BlockPlacementRule$UpdateState BlockPlacementRule$PlacementState]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.entity EntityDespawnEvent]
           [net.minestom.server.instance.block Block BlockFace]
           [net.minestom.server.coordinate Vec Pos Point]
           [net.minestom.server.event.player PlayerMoveEvent PlayerBlockPlaceEvent PlayerBlockBreakEvent]
           [net.hollowcube.polar PolarLoader]
           [net.minestom.server.utils MathUtils]
                                        ; entitys
           [net.minestom.server.entity Entity Player ]

           [net.minestom.server.instance.generator UnitModifier GenerationUnit]
           
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]

           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [java.io File]
           ))


(set! *warn-on-reflection* true)




(defn mkworld
  [^InstanceManager iManager ^String username ^java.util.UUID owner]
  (let [instance (.createInstanceContainer ^InstanceManager iManager)
        ]
    (iman/assocInstance username instance)
    (.setChunkLoader  ^InstanceContainer instance ^IChunkLoader (PolarLoader. (.toPath (File. (str "./data/plots/" (.toString owner) ".polar")))))
    (.setWorldBorder instance (net.minestom.server.instance.WorldBorder. 64 0 0 0 0))
    (.setChunkSupplier ^Instance instance
                       (reify
                         net.minestom.server.utils.chunk.ChunkSupplier
                         (createChunk [this instance chunkx chunkz]
                           
                           (net.minestom.server.instance.LightingChunk. instance chunkx chunkz))))
    (.setGenerator ^Instance instance
                   (reify
                     net.minestom.server.instance.generator.Generator
                     (generate [this unit]
                       (generators/plotworld unit)
                       )))

    (.addListener (.eventNode instance) PlayerBlockPlaceEvent
                  (reify
                    java.util.function.Consumer
                    (accept [this event]
                      (let [event ^PlayerBlockPlaceEvent event
                            sender ^Player (.getPlayer ^PlayerBlockPlaceEvent event)
                            uuid (.getUuid sender)
                            blockpos (.getBlockPosition event)]
                        (.consumeBlock ^PlayerBlockPlaceEvent event false)
                        
                        (if (or (not (.equals owner uuid)) (> (abs (.x blockpos)) 31) (> (abs (.z blockpos)) 31))
                          (.setCancelled ^PlayerBlockPlaceEvent event true)
                          )
                        ))))
    (.addListener (.eventNode instance) PlayerBlockBreakEvent
                  (reify
                    java.util.function.Consumer
                    (accept [this event]
                      (let [event ^PlayerBlockBreakEvent event
                            sender ^Player (.getPlayer  event)
                            uuid (.getUuid sender)
                            blockpos (.getBlockPosition event)]
                        (if (or (not (.equals owner uuid)) (> (abs (.x blockpos)) 31) (> (abs (.z blockpos)) 31))
                          (.setCancelled  event true)
                          )
                        ))))

    (.addListener (.eventNode instance) EntityDespawnEvent
                  (reify
                    java.util.function.Consumer
                    (accept [this event]
                      (let [event ^EntityDespawnEvent event
                            instance (.getInstance event)]
                        (if (= 0 (.size (.getPlayers instance)))
                          (do (.get (.saveChunksToStorage instance))
                              (println "unloading instance")
                              (iman/removeInstance username)
                              (.unregisterInstance iManager instance))
                          )
                        ))))

    
    (.addListener (.eventNode instance) net.minestom.server.event.player.PlayerChunkUnloadEvent
                  (reify
                    java.util.function.Consumer
                    (accept [this event]
                      (let [event ^net.minestom.server.event.player.PlayerChunkUnloadEvent event]
                        (if (not= nil (.getInstance event))
                          (let [chunk (.getChunk ^InstanceContainer (.getInstance event) (.getChunkX event) (.getChunkZ event))
                                instance ^InstanceContainer (.getInstance event)]
                            (if (and (not= instance nil) (not= chunk nil) (= (.size ^java.util.Set (.getViewers chunk)) 0))
                              (do (.get (.saveChunkToStorage instance chunk))
                                  (.unloadChunk instance chunk)))))))))
    instance))
