(ns tuz.strategy.v2
  (:require
   [tuz.util.game :as game]))

(def bot-name "tuz-v2")

(defn move-site
  "Randomly move after 5 turns of production"
  [site game-map]
  (if (< (:strength site) (* 5 (:production site)))
    [site :still]
    [site (rand-nth game/directions)]))

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))]
    (map #(move-site %1 game-map) my-sites)))
