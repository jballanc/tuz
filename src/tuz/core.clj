(ns tuz.core
  (:require
   [tuz.util.game :as game]
   [tuz.util.io :as io])
  (:gen-class))

(defn run-strategy
  "Requires the namespace for the specified strategy and runs the game"
  [version]
  (let [strategy (symbol (str "tuz.strategy." version))
        {:keys [my-id productions width height game-map]} (io/get-init!)]
      (require strategy)
      (println (var-get (ns-resolve strategy 'bot-name)))
      (doseq [turn (range)]
        (let [game-map (io/create-game-map width height productions (io/read-ints!))]
          (io/send-moves! ((ns-resolve strategy 'move) my-id game-map))))))

(defn -main
  "Run the game with the specified version of the strategy"
  [strategy]
  (run-strategy strategy))
