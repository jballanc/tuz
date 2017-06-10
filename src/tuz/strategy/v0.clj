(ns tuz.strategy.v0
  (:require
   [tuz.util.game :as game]))

(def bot-name "tuz-v0")

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))]
    (map vector my-sites (repeatedly #(rand-nth game/directions)))))
