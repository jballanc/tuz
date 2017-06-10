(ns tuz.strategy.v1
  (:require
   [tuz.util.game :as game]))

(def bot-name "tuz-v1")

(defn move-site
  "After 5 turns of production, move north or west"
  [site game-map]
  (if (< (:strength site) (* 5 (:production site)))
    [site :still]
    [site (rand-nth [:north :west])]))

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))]
    (map #(move-site %1 game-map) my-sites)))
