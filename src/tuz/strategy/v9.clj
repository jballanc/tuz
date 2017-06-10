(ns tuz.strategy.v9
  (:require
   [clojure.core.match :refer [match]]
   [tuz.util.game :as game]))

(def bot-name "tuz-v9")

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
  "Return the direction to travel to reach the nearest border site"
  [{:keys [x y]} [center-x center-y] game-map]
  (let [diff-x (- x center-x)
        dist-x (game/single-dimension-distance x center-x
                                               (count (first game-map)))
        x-dir (match [(= dist-x (Math/abs diff-x)) (pos? diff-x)]
                     [true true] :east
                     [true false] :west
                     [false true] :west
                     [false false] :east)
        diff-y (- y center-y)
        dist-y (game/single-dimension-distance y center-y
                                               (count game-map))
        y-dir (match [(= dist-y (Math/abs diff-y)) (pos? diff-y)]
                     [true true] :south
                     [true false] :north
                     [false true] :north
                     [false false] :south)]
    (if (zero? (+ dist-x dist-y))
      (rand-nth [:north :south :east :west])
      (if (> (/ dist-x (+ dist-x dist-y)) (rand))
        x-dir
        y-dir))))

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
      target
      (cond
        (or (> strength (:strength target)) (and (> strength 127) (> (:production target) production)))
        ;; ...and we are strong or the site is desirable: attack
        [site (:direction target)]

        ;; ...otherwise: stay put
        :default
        [site :still])

      ;; We're strong enough to move...
      (> strength (* 5 production))
      [site (away-from site center game-map)]

      ;; We need to grow...stay put!
      :default
      [site :still])))

(defn move
  "Takes a 2D vector of sites and returns a list of [site, direction] pairs"
  [my-id game-map]
  (let [my-sites (->> game-map
                      flatten
                      (filter #(= (:owner %) my-id)))
        com (center-of-mass my-sites)]
    (map #(move-site %1 game-map com) my-sites)))
