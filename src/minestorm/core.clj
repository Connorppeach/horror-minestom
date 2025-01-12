(ns minestorm.core
  (:gen-class)
  (:require [minestorm.commands :as cmds]
            [minestorm.constants :as consts]
            [minestorm.pack :as pack]
            [minestorm.db :as db]
            [minestorm.mainworld :as mworld]
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





(def server (atom nil))
(def iManager (atom nil))
(def instance (atom nil))
(defn -main
  "main"
  [& args]
  (reset! server (MinecraftServer/init))
  (reset! iManager (MinecraftServer/getInstanceManager))
  (reset! instance (mworld/mkworld @iManager "./data/worlds/main"))
  (db/initdb)
  (println "server has started up")
  (pack/init)
  (if (nil? (System/getenv "FABRIC_PROXY_SECRET"))
    (MojangAuth/init)
    (VelocityProxy/enable (System/getenv "FABRIC_PROXY_SECRET")))
  (def gEventHandler (MinecraftServer/getGlobalEventHandler))
  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.AsyncPlayerConfigurationEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (.setSpawningInstance ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event ^Instance @instance)
                    (.setRespawnPoint (.getPlayer ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event) ^Point (Pos. 0.0 160.0 0.0)))))


  
  (def cooldowns (atom {}))
  
  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerSpawnEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (let [player ^Player  (.getPlayer ^net.minestom.server.event.player.PlayerSpawnEvent event)]
                    (.setGameMode  player net.minestom.server.entity.GameMode/SURVIVAL)
                     (db/add-prop-if-nil! (.getUsername player) :level 0)
                     (if (= (first (.getUsername player)) \.)
                       (db/add-prop-if-nil! (.getUsername player) :blockstyle "falling")
                       (db/add-prop-if-nil! (.getUsername player) :blockstyle "display"))
                     (db/add-prop-if-nil! (.getUsername player) :power 3)
                     (db/add-prop-if-nil! (.getUsername player) :banned false)
                     (db/add-prop-if-nil! (.getUsername player) :trust 0.17)
                     (db/add-prop-if-nil! (.getUsername player) :blocks-broken 0)
                     (db/add-prop-if-nil! (.getUsername player)  :dropstyle "reg")
                     (if (db/get-prop (.getUsername player) :banned)
                       (.kick player "banned for something;  \nTODO: add reason"))
                     (.setLevel player (db/get-prop (.getUsername player) :level))
                     (.setExp player (- (db/get-prop (.getUsername player) :level) (int (db/get-prop (.getUsername player) :level))))
                     (.setItemInHand player Player$Hand/MAIN (ItemStack/of Material/TNT))
                     (.addItemStack (.getInventory player) (ItemStack/of Material/NETHER_STAR))
                     (.addItemStack (.getInventory player) (ItemStack/of Material/BOOK))
                     (.setInstantBreak player true)
                     (reset! cooldowns (assoc @cooldowns (keyword (.getUsername player)) (System/currentTimeMillis)))
                     (.setAllowFlying player true)
                    (.sendResourcePacks player
                     (let [b ^ResourcePackRequest$Builder (ResourcePackRequest/resourcePackRequest)]
                       (.required b true)
                       (.replace b true)
                       (.packs b ^ResourcePackInfoLike [(.get (.computeHashAndBuild (.uri (ResourcePackInfo/resourcePackInfo) (java.net.URI/create "https://github.com/Connorppeach/deconstruct-minestom/raw/refs/heads/main/test/pack.zip"))))]);https://github.com/Connorppeach/deconstruct-minestom/releases/download/v0.0.1/pack.zip 
                       ^ResourcePackRequest (.build b)
                       )
                     )))))




  
  (defn rotatebyplacement
    [block]
    (let [p 
        (proxy [BlockPlacementRule] [block]
          (blockUpdate [^BlockPlacementRule$UpdateState b] (.currentBlock b))
          (blockPlace [^BlockPlacementRule$PlacementState b]
            (if (not (or (= (String/valueOf (.blockFace b)) "TOP") (= (String/valueOf (.blockFace b)) "BOTTOM")))
              (.withProperty (.block b) "facing" (.toLowerCase (String/valueOf (.blockFace b))))
              (.withProperty (.block b) "facing" (.toLowerCase (String/valueOf (MathUtils/getHorizontalDirection (.yaw (.playerPosition b)))))))))]
      (.registerBlockPlacementRule (MinecraftServer/getBlockManager) p)))
  
  (doseq [block [Block/ACACIA_STAIRS Block/ANDESITE_STAIRS Block/ANDESITE_STAIRS Block/POLISHED_BLACKSTONE_BRICK_STAIRS Block/POLISHED_BLACKSTONE_STAIRS Block/POLISHED_DEEPSLATE_STAIRS
                 Block/POLISHED_DIORITE_STAIRS Block/POLISHED_GRANITE_STAIRS Block/POLISHED_TUFF_STAIRS Block/PRISMARINE_BRICK_STAIRS Block/PRISMARINE_STAIRS Block/PURPUR_STAIRS Block/QUARTZ_STAIRS Block/RED_NETHER_BRICK_STAIRS Block/RED_SANDSTONE_STAIRS Block/SANDSTONE_STAIRS Block/SMOOTH_QUARTZ_STAIRS Block/SMOOTH_RED_SANDSTONE_STAIRS Block/SMOOTH_SANDSTONE_STAIRS Block/SPRUCE_STAIRS Block/STONE_BRICK_STAIRS Block/STONE_STAIRS Block/TUFF_BRICK_STAIRS Block/TUFF_STAIRS Block/WARPED_STAIRS Block/WAXED_CUT_COPPER_STAIRS Block/WAXED_EXPOSED_CUT_COPPER_STAIRS Block/WAXED_OXIDIZED_CUT_COPPER_STAIRS Block/WAXED_WEATHERED_CUT_COPPER_STAIRS Block/WEATHERED_CUT_COPPER_STAIRS Block/OAK_STAIRS Block/COBBLESTONE_STAIRS Block/BRICK_STAIRS Block/STONE_BRICK_STAIRS]]
    (rotatebyplacement block))
  (cmds/init @iManager)
  (nrepl-server/start-server :bind "0.0.0.0" :port 7889)
                                        ;(net.minestom.server.extras.velocity.VelocityProxy/enable "hXt2TN42ucml"); REPLACE ME WITH YOUR OWN KEY TO USE VELOCITY
    (.start ^MinecraftServer @server "0.0.0.0" 25575)
  
  )

