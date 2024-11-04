(ns minestorm.pack
  (:require [clojure.java.io :as io])
  (:import [net.minestom.server MinecraftServer]
           [net.worldseed.multipart ModelEngine] ;model reader
           [net.worldseed.resourcepack PackBuilder]
           [java.nio.file Path]
           [java.io File FileInputStream InputStreamReader Reader]
           [java.nio.charset Charset]
           [java.util Collection]
           [java.util.concurrent.atomic AtomicReference]
           [java.net URI]
           [net.minestom.server.item ItemStack Material]
           [org.apache.commons.io FileUtils]
           [org.zeroturnaround.zip ZipUtil]
           [java.nio.file Paths]
           ))
(set! *warn-on-reflection* true)

(defn mk []
  (let [basepath (Path/of (.toURI (io/resource "")))
        template (Path/of (.toURI (io/resource "pack_template" )))
        bbmodel (Path/of (.toURI (io/resource "bbmodel" )))
        zippath (.resolve basepath "pack.zip")
        modelpath (.resolve basepath "models")]
    (def server (MinecraftServer/init))
    (ModelEngine/setModelMaterial Material/SLIME_BALL)
    (FileUtils/deleteDirectory (.toFile (.resolve basepath "pack")))
    (FileUtils/copyDirectory (.toFile template) (.toFile (.resolve basepath "pack")))
    (let [config (PackBuilder/Generate bbmodel (.resolve basepath "pack") modelpath)]
      (FileUtils/writeStringToFile (.toFile (.resolve basepath "model_mappings.json")), (.modelMappings config), (Charset/defaultCharset))
      (let [mappingsData (InputStreamReader. (FileInputStream. (.toFile (.resolve basepath "model_mappings.json"))))]
        (ModelEngine/loadMappings mappingsData modelpath)
        (ZipUtil/pack (.toFile (.resolve basepath "pack")) (.toFile zippath))
        (System/exit 0)))))

(defn init []
  (ModelEngine/setModelMaterial Material/SLIME_BALL)
  (let [basepath (Path/of (.toURI (File. "./test")))
        modelpath (.resolve basepath "models")]
    (let [mappingsData (InputStreamReader. (FileInputStream. (.toFile (.resolve basepath "model_mappings.json"))))]
      (ModelEngine/loadMappings mappingsData modelpath))))
