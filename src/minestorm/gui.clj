(ns minestorm.gui
  (:require [minestorm.db :as db])

  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.entity Player]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.instance.block Block BlockFace]
           [net.minestom.server.item ItemStack Material]
           [net.minestom.server.inventory Inventory InventoryType]
           [net.kyori.adventure.text Component]
           [net.kyori.adventure.text.format TextColor]))
(defn emptyrow
  [^Inventory p ^Integer row ^Material mat]
  (doseq [i (range (* row 9) (+ (* row 9) 9))]
    (.setItemStack p i (-> (ItemStack/builder mat) (.customName
                                                                                 (Component/text "")) .build))))
(defn tntpowergui
  [exitfn]
  (let [setupgui (fn* [^Inventory p]
                      (emptyrow p 0 Material/RED_STAINED_GLASS_PANE)
                      (emptyrow p 2 Material/RED_STAINED_GLASS_PANE)

                      (.setItemStack p 26 (-> (ItemStack/builder Material/DARK_OAK_DOOR)
                                              (.customName
                                               (Component/text "Exit" (TextColor/color 200 0 0)))
                                              .build))
                      (.setItemStack p 12 (-> (ItemStack/builder Material/STONE)
                                              (.customName
                                               (Component/text "little tnt" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 13 (-> (ItemStack/builder Material/LAPIS_BLOCK)
                                              (.customName
                                               (Component/text "medium tnt" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 14 (-> (ItemStack/builder Material/GOLD_BLOCK)
                                              (.customName
                                               (Component/text "medium large tnt" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 15 (-> (ItemStack/builder Material/EMERALD_BLOCK)
                                              (.customName
                                               (Component/text "medium and a little bit larger tnt" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 16 (-> (ItemStack/builder Material/DIAMOND_BLOCK)
                                              (.customName
                                               (Component/text "large tnt" (TextColor/color 120 0 0)))
                                              .build))
                      (.update p))
        p (proxy [Inventory] [InventoryType/CHEST_3_ROW "Main menu"]
            (leftClick [^Player pl ^Integer slot]
              (cond
                (= slot 26) (exitfn pl)
                (= slot 12) (db/set-prop! (.getUsername pl) :power 5)
                (= slot 13) (if (> (db/get-prop (.getUsername pl) :level) 5)
                              (db/set-prop! (.getUsername pl) :power 6)
                              (.sendMessage pl (Component/text "need to be at least level 5" (TextColor/color 120 0 0))))
                (= slot 14) (if (> (db/get-prop (.getUsername pl) :level) 10)
                              (db/set-prop! (.getUsername pl) :power 7)
                              (.sendMessage pl (Component/text "need to be at least level 10" (TextColor/color 120 0 0))))
                (= slot 15) (if (> (db/get-prop (.getUsername pl) :level) 20)
                              (db/set-prop! (.getUsername pl) :power 8)
                              (.sendMessage pl (Component/text "need to be at least level 20" (TextColor/color 120 0 0))))
                (= slot 16) (if (> (db/get-prop (.getUsername pl) :level) 30)
                              (db/set-prop! (.getUsername pl) :power 9)
                              (.sendMessage pl (Component/text "need to be at least level 30" (TextColor/color 120 0 0)))))
              false
              )
            (middleClick [^Player pl ^Integer slot] false)
            (rightClick [^Player pl ^Integer slot] false)
            (doubleClick [^Player pl ^Integer slot] false)
            (shiftClick [^Player pl ^Integer slot] false)
            (drop [^Player pl ^Boolean all ^Integer slot ^Integer button] (setupgui this) false)

            )]
    (setupgui p)
    
    p
    ))

(defn settingsgui
  [exitfn]
  (let [setupgui (fn* [^Inventory p]
                      (emptyrow p 0 Material/YELLOW_STAINED_GLASS_PANE)
                      (emptyrow p 1 Material/YELLOW_STAINED_GLASS_PANE)
                      (emptyrow p 2 Material/YELLOW_STAINED_GLASS_PANE)

                      (.setItemStack p 26 (-> (ItemStack/builder Material/DARK_OAK_DOOR)
                                              (.customName
                                               (Component/text "Exit" (TextColor/color 200 0 0)))
                                              .build))
                      (.setItemStack p 1 (-> (ItemStack/builder Material/GLASS)
                                              (.customName
                                               (Component/text "No tnt visible(if you have a tato computer)" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 10 (-> (ItemStack/builder Material/SAND)
                                              (.customName
                                               (Component/text "Falling block tnt(can be choppy, but is the only method for bedrock)" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 19 (-> (ItemStack/builder Material/COMMAND_BLOCK)
                                              (.customName
                                               (Component/text "Full display entity tnt(the preffered way to play)" (TextColor/color 120 0 0)))
                                              .build))

                      (.update p))
        interactfn (fn [^Player pl ^Integer slot] (cond
                (= slot 26) (exitfn pl)
                (= slot 1)  (db/set-prop! (.getUsername pl) :blockstyle "nil")
                (= slot 10) (db/set-prop! (.getUsername pl) :blockstyle "falling")
                (= slot 19) (db/set-prop! (.getUsername pl) :blockstyle "display")
              
              ) false)
        p (proxy [Inventory] [InventoryType/CHEST_3_ROW "Settings"]
            (leftClick [^Player pl ^Integer slot] (interactfn pl slot))
              
            (middleClick [^Player pl ^Integer slot] (interactfn pl slot) false)
            (rightClick [^Player pl ^Integer slot] (interactfn pl slot) false)
            (doubleClick [^Player pl ^Integer slot] (interactfn pl slot) false)
            (shiftClick [^Player pl ^Integer slot] (interactfn pl slot) false)
            (drop [^Player pl ^Boolean all ^Integer slot ^Integer button] (setupgui this) (interactfn pl slot) false)

            )]
    (setupgui p)
    
    p
    ))

(defn tntexplgui
  [exitfn]
  (let [setupgui (fn* [^Inventory p]
                      (emptyrow p 0 Material/BLUE_STAINED_GLASS_PANE)
                      (emptyrow p 2 Material/BLUE_STAINED_GLASS_PANE)

                      (.setItemStack p 26 (-> (ItemStack/builder Material/DARK_OAK_DOOR)
                                              (.customName
                                               (Component/text "Exit" (TextColor/color 200 0 0)))
                                              .build))
                      (.setItemStack p 12 (-> (ItemStack/builder Material/TNT)
                                              (.customName
                                               (Component/text "regular tnt" (TextColor/color 0 0 120)))
                                              .build))
                      (.setItemStack p 13 (-> (ItemStack/builder Material/LIGHTNING_ROD)
                                              (.customName
                                               (Component/text "lightning tnt" (TextColor/color 0 0 120)))
                                              .build))
                      (.setItemStack p 14 (-> (ItemStack/builder Material/SLIME_BLOCK)
                                              (.customName
                                               (Component/text "bouncy tnt" (TextColor/color 0 0 120)))
                                              .build))
                      (.setItemStack p 15 (-> (ItemStack/builder Material/COMMAND_BLOCK)
                                              (.customName
                                               (Component/text "recursive tnt" (TextColor/color 0 0 120)))
                                              .build))
                      (.update p))
        p (proxy [Inventory] [InventoryType/CHEST_3_ROW "Tnt menu"]
            (leftClick [^Player pl ^Integer slot]
              (cond
                (= slot 26) (exitfn pl)
                (= slot 12) (db/set-prop! (.getUsername pl) :dropstyle "reg")
                (= slot 13) (db/set-prop! (.getUsername pl) :dropstyle "lightning")
                (= slot 14) (db/set-prop! (.getUsername pl) :dropstyle "bounce")
                (= slot 15) (if (> (db/get-prop (.getUsername pl) :level) 1)
                              (db/set-prop! (.getUsername pl) :dropstyle "recurse")
                              (.sendMessage pl (Component/text "need to be at least level 40" (TextColor/color 120 0 0)))))
              false
              )
            (middleClick [^Player pl ^Integer slot] false)
            (rightClick [^Player pl ^Integer slot] false)
            (doubleClick [^Player pl ^Integer slot] false)
            (shiftClick [^Player pl ^Integer slot] false)
            (drop [^Player pl ^Boolean all ^Integer slot ^Integer button] (setupgui this) false)

            )]
    (setupgui p)
    
    p
    ))


(defn mainmenu
  [exitfn]
  (let [setupgui (fn* [^Inventory p]
                      (emptyrow p 0 Material/GRAY_STAINED_GLASS_PANE)
                      (emptyrow p 2 Material/GRAY_STAINED_GLASS_PANE)

                      (.setItemStack p 26 (-> (ItemStack/builder Material/DARK_OAK_DOOR)
                                              (.customName
                                               (Component/text "Exit" (TextColor/color 200 0 0)))
                                              .build))
                      (.setItemStack p 12 (-> (ItemStack/builder Material/TNT)
                                              (.customName
                                               (Component/text "Change TNT power" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 13 (-> (ItemStack/builder Material/GRASS_BLOCK)
                                              (.customName
                                               (Component/text "Change exploded block behavior" (TextColor/color 0 0 120)))
                                              .build))                      
                      (.setItemStack p 14 (-> (ItemStack/builder Material/COMMAND_BLOCK)
                                              (.customName
                                               (Component/text "Settings" (TextColor/color 0 0 120)))
                                              .build))                      
                      (.update p))
        p (proxy [Inventory] [InventoryType/CHEST_3_ROW "Main menu"]
            (leftClick [^Player pl ^Integer slot]
              (cond
                (= slot 26) (exitfn pl)
                (= slot 12) (.openInventory pl (tntpowergui #(.closeInventory %)))
                (= slot 13) (.openInventory pl (tntexplgui #(.closeInventory %)))
                (= slot 14) (.openInventory pl (settingsgui #(.closeInventory %))))
                false
              )
            (middleClick [^Player pl ^Integer slot] false)
            (rightClick [^Player pl ^Integer slot] false)
            (doubleClick [^Player pl ^Integer slot] false)
            (shiftClick [^Player pl ^Integer slot] false)
            (drop [^Player pl ^Boolean all ^Integer slot ^Integer button] (setupgui this) false)

            )]
    (setupgui p)
    
    p
    ))




(defn blockselectors
  [screen]
  (let [blocks (sort #(compare (String/valueOf %) (String/valueOf %2)) (remove #(nil? (.material (.registry %))) (.toArray (Block/values))))
        setupgui (fn* [^Inventory p]
                      
                      (doseq [i (range 52)]
                        (if (not (nil? (.material (.registry (nth blocks (+ (* screen 52) i))))))
                          (.setItemStack p i (ItemStack/of
                                              (.material (.registry (nth blocks (+ (* screen 52) i))))
                                              ))))
                      (.setItemStack p 52 (-> (ItemStack/builder Material/BOOK)
                                              (.customName
                                               (Component/text "back" (TextColor/color 120 0 0)))
                                              .build))
                      (.setItemStack p 53 (-> (ItemStack/builder Material/BOOK)
                                              (.customName
                                               (Component/text "next" (TextColor/color 0 0 120)))
                                              .build))                      
                      (.update p))
        handlefn (fn* [^Player pl ^Integer slot] (cond
                                                   (= 52 slot) (do (.openInventory pl (blockselectors (max 0 (- screen 1)))) false)
                                                   (= 53 slot) (do (.openInventory pl (blockselectors (min (- (int (/ (count blocks) 52)) 1) (+ screen 1)))) false)
                                                   (> 53 slot) (do (.addItemStack (.getInventory pl) (ItemStack/of (.material (.registry (nth blocks (+ (* screen 52) slot)))))) true)
                                                   :else false))
        p (proxy [Inventory] [InventoryType/CHEST_6_ROW "Block Gui"]
            (leftClick [^Player pl ^Integer slot]
              (handlefn pl slot)
              )
            (middleClick [^Player pl ^Integer slot] (handlefn pl slot))
            (rightClick [^Player pl ^Integer slot] (handlefn pl slot))
            (doubleClick [^Player pl ^Integer slot] (handlefn pl slot))
            (shiftClick [^Player pl ^Integer slot] (handlefn pl slot))
            (drop [^Player pl ^Boolean all ^Integer slot ^Integer button] (handlefn pl slot))

            )]
    (setupgui p)
    
    p
    ))
