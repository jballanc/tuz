(ns tuz.util.io
  (:require
   [clojure.string :as str]
   [tuz.util.game :as game]))

(defn create-game-map
  "Parses the run-length encoded format of the map into a 2D vector of Sites"
  [width height productions compressed-map]
  (let [total-size (* width height)
        potential-owner-run-pairs (partition 2 compressed-map)
        owner-pair-count (->> potential-owner-run-pairs
                              (map first)
                              (reductions + 0)
                              (take-while #(not= total-size %))
                              (count))
        owners (->> potential-owner-run-pairs
                    (take owner-pair-count)
                    (mapcat #(repeat (first %) (second %))))
        strengths (->> compressed-map
                       (drop (* 2 owner-pair-count)))
        flat-sites (map game/->Site
                        (cycle (range width))
                        (mapcat #(repeat width %) (range))
                        productions
                        strengths
                        owners)]
    (mapv vec (partition width flat-sites))))

(defn read-ints!
  "Reads a sequence of space-delimited integers from *in*"
  []
  (map #(Integer/parseInt %)
       (str/split (read-line) #" ")))

(defn get-init!
  "Reads all the initialization data provided by the Halite environment process"
  []
  (let [[my-id] (read-ints!)
        [width height] (read-ints!)
        productions (read-ints!)
        game-map (create-game-map width height productions (read-ints!))]
    {:my-id my-id
     :width width
     :height height
     :productions productions
     :game-map game-map}))


(def direction->int (zipmap game/directions (range)))

(defn- format-moves-for-output [moves]
  (->> moves
       (mapcat (fn [[{:keys [x y]} direction]] (list x y (direction->int direction))))
       (str/join " ")))

(defn send-moves!
  "Submits a list of [site, direction] pairs to the Halite enviroment process"
  [moves]
  (println (format-moves-for-output moves)))
