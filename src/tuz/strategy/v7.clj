(ns tuz.strategy.v7
  (:require
   [clojure.core.match :refer [match]]
   [tuz.util.game :as game]))

(def bot-name "tuz-v7")

(defn get-neighbors
  "Find the neighbors of a site on the map and annotate them with the relative
  direction from the site"
  [site game-map]
  (map (fn [direction]
         (assoc (game/adjacent-site game-map site direction)
                :direction direction))
       game/cardinal-directions))

(defn center-of-mass
  "Calculate the rudimentary center-of-mass for our sites"
  [sites]
  (let [total-mass (reduce (fn [sum {:keys [strength]}] (+ sum strength)) 0 sites)
        weighted-x (reduce (fn [sum {:keys [x strength]}] (+ sum (* strength x))) 0 sites)
        weighted-y (reduce (fn [sum {:keys [y strength]}] (+ sum (* strength y))) 0 sites)
        center-fn  (fn [dim] (-> dim
                                (Math/pow 2)
                                (/ (Math/pow total-mass 2))
                                (Math/sqrt)
                                (Math/round)))]
    [(center-fn weighted-x) (center-fn weighted-y)]))

(defn away-from
  "Picks a direction that points away from the center"
  [{:keys [x y]} [center-x center-y]]
  (let [diff-x (- x center-x)
        diff-y (- y center-y)]
    (match [(> diff-x diff-y) (pos? diff-x) (pos? diff-y)]
           [true true _] :east
           [true false _] :west
           [false _ true] :south
           [false _ false] :north)))

(defn move-site
  "Multifaceted strategy based on neighboring sites:
    * If neighbor is non-self...
    ** and weaker: take it
    ** and we are > 50% max strength: attack
    ** and we are a weakling: stay put
    * If not, stay at current site until strength is 3x production or half max
    * Otherwise move away from the center of mass"
  [{:keys [strength production owner] :as site} game-map center]
  (let [neighbors (get-neighbors site game-map)
        enemies (filter #(not= (:owner %) owner) neighbors)
        target (first (sort-by #(/ (:strength %) (inc (:production %))) enemies))]
    (cond
      ;; We have a neighbor...
      (and target (> strength (:strength target)))
      ;; ...and we are strong: attack
      [site (:direction target)]

      ;; We're strong enough to move...
      (or (> strength 127) (> strength (* 3 production)))
      [site (away-from site center)]

      ;; We need to grow...stay put!
      :default
      [site :still])))

(def turn (atom 0))

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))
        com (center-of-mass my-sites)]
    (spit "debug.log" (str "Turn: " (swap! turn inc) ", COM: " com "\n") :append true)
    (map #(move-site %1 game-map com) my-sites)))

