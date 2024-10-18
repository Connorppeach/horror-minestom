(ns minestorm.commands
  (:gen-class)
  (:require [minestorm.constants :as consts]
            [minestorm.steve :as steve])

  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.entity Player]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.command.builder CommandExecutor Command CommandContext]
           [net.minestom.server.command CommandSender]))

(defn init
  [instance]
  (def cmanager (MinecraftServer/getCommandManager))
  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["size"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (let [sender ^Player  sender context ^CommandContext context]
                                                     (reset! consts/power (Integer/parseInt (nth (.split (.getInput context) " ") 1)))))))
               ^Command p))

  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["spawn"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (let [sender ^Player  sender]
                                                     (steve/mk ^Instance (.getInstance sender) ^Pos (.getPosition sender) (str (nth (.split (.getInput context) " ") 1) ".bbmodel"))))))
               ^Command p))

  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["save"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (.saveChunksToStorage ^Instance instance))))
               ^Command p)))
