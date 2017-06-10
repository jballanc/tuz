(ns tuz.strategy.v5
  (:require
   [tuz.util.game :as game]))

(def bot-name "tuz-v5")

(defn get-neighbors
  "Find the neighbors of a site on the map and annotate them with the relative
  direction from the site"
  [site game-map]
  (map (fn [direction]
         (assoc (game/adjacent-site game-map site direction)
                :direction direction))
       game/cardinal-directions))

(defn move-site
  "Multifaceted strategy based on neighboring sites:
    * If neighbor is non-self and weaker: take it
    * If neighbor is non-self and we're already > 50% max strength, attack
    * If not, stay at current site until strength is 3x production or half max
    * Otherwise move north or west"
  [{:keys [strength production owner] :as site} game-map]
  (let [neighbors (get-neighbors site game-map)
        enemies (filter #(not= (:owner %) owner) neighbors)
        target (first (sort-by :strength enemies))]
    (cond
      ;; Weakest neighbor is weaker than us...take it!
      (and target
           (or (> strength (:strength target))
               (> strength 200)))
      [site (:direction target)]

      ;; We're strong enough to move...
      (or (> strength 127) (> strength (* 3 production)))
      [site (rand-nth [:north :west])]

      ;; We need to grow...stay put!
      :default
      [site :still])))

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))]
    (map #(move-site %1 game-map) my-sites)))
