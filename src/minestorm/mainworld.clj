(ns minestorm.mainworld
  (:require [minestorm.commands :as cmds]
            [minestorm.constants :as consts]
            [minestorm.pack :as pack]
            [minestorm.db :as db]
            [minestorm.steve :as steve]
            [nrepl.server :as nrepl-server]
            ;[cider.nrepl :as cider]
            )
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.instance.block.rule BlockPlacementRule BlockPlacementRule$UpdateState BlockPlacementRule$PlacementState]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block BlockFace]
           [net.minestom.server.coordinate Vec Pos Point]
           [net.minestom.server.event.player PlayerMoveEvent PlayerBlockPlaceEvent]
           [net.minestom.server.extras MojangAuth]
           [net.minestom.server.extras.velocity VelocityProxy]
           [net.minestom.server.utils MathUtils]
                                        ; entitys
           [net.minestom.server.entity EntityType Entity Player Player$Hand EntityCreature]
           
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]

           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
           [net.worldseed.multipart ModelEngine]
                                        ;java
           [net.minestom.server.network.packet.server.play SetCooldownPacket]
           [net.minestom.server.utils.time Cooldown]
           
           ))
(set! *warn-on-reflection* true)


(defn mkworld
  [^InstanceManager iManager ^String worldfolder]
  (let [instance (.createInstanceContainer ^InstanceManager iManager)]

  (.setChunkLoader  ^InstanceContainer instance ^IChunkLoader (AnvilLoader. worldfolder))

  (.setChunkSupplier ^Instance instance
                     (reify
                       net.minestom.server.utils.chunk.ChunkSupplier
                       (createChunk [this instance chunkx chunky]
                         (net.minestom.server.instance.LightingChunk. instance chunkx chunky))))
  (.setGenerator ^Instance instance
                 (reify
                   net.minestom.server.instance.generator.Generator
                   (generate [this unit]
                     (-> unit
                         .modifier
                         (.fillHeight 0 40 Block/STONE)))))


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
  
  instance
  ))
