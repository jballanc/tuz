(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]])

(task-options!
 pom {:version "0.4.0"
      :description "A Bot to play the Halite game."
      :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}}
 aot {:all true})

(require
 '[clojure.java.io :as io]
 '[clojure.string :as str])

(defn find-latest
  "Finds the latest strategy version based on the fileset"
  [fileset]
  (let [version-file-pat #"tuz/strategy/v(\d+)\.clj"]
    (->> fileset input-files
         (map :path)
         (filter #(re-matches version-file-pat %))
         (map #(str/replace % version-file-pat "$1"))
         (map #(Integer/parseInt %))
         (sort >)
         first)))

(defn render-main-ns
  "Reads the main namespace file template, substitutes in the correct version,
  and writes the result to the provided directory"
  [main-ns version outdir]
  (let [rendered-ns (-> (tmp-file main-ns)
                        slurp
                        (str/replace #"\{\{.*\}\}" (str version)))]
    (io/make-parents outdir)
    (spit (io/file outdir "MyBot.clj") rendered-ns)))

(defn run-version
  "Generates a function that will run the game for the version specified,
  pitting it against the next previous version"
  [version]
  (let [jvm-cmd (fn [v] (str "java -jar target/tuz.jar v" v " 2> v" v "-err.log"))
        run-fn  (boot.util/sh "./halite" "-d" "30 30"
                              (jvm-cmd version)
                              (jvm-cmd (dec version)))]
    (if (= 0 (run-fn))
      (boot.util/info "Finished running game.\n")
      (boot.util/fail "Problem running game! (Check logs...)\n"))))

(deftask ^:private render-latest-main
  "Finds the MyBot.clj file and updates the `-main` function to run only the
  latest version of the strategy"
  []
  (with-pre-wrap fileset
    (let [version (find-latest fileset)
          outdir (tmp-dir!)
          main-ns (->> fileset input-files
                       (filter #(re-matches #"MyBot.clj" (:path %)))
                       first)]
      (render-main-ns main-ns version outdir)
      (boot.util/info "Rendered main namespace...\n")
      (-> fileset
          (add-source outdir)
          (commit!)))))

(deftask run-game
  "Runs the game pitting the specified version of the strategy against the next
  most recent version"
  [v version STRATEGY int "Latest version to run"]
  (fn [next-task]
    (fn [fileset]
      (boot.util/info "Running game with version " version)
      (run-version version)
      (next-task fileset))))

(deftask run-latest
  "Runs the latest version of the game"
  []
  (fn [next-task]
    (fn [fileset]
      (let [version (find-latest fileset)]
        (boot.util/info "Running game with version %d...\n" version)
        (run-version version))
      (next-task fileset))))

(deftask dev-build
  "Builds the project in preparation for running locally"
  []
  (comp
   (pom :project 'tuz)
   (aot)
   (uber)
   (jar :main 'tuz.core :file "tuz.jar")
   (target)))

(deftask dev
  "Build the project and run the game locally"
  []
  (comp
   (dev-build)
   (run-latest)))

(deftask release
  "Builds and prepares the project to be uploaded for competiton"
  []
  (comp
   (pom :project 'MyBot)
   (render-latest-main)
   (aot)
   (uber)
   (jar :main 'MyBot :file "MyBot.jar")
   (zip :file "MyBot.zip")
   (target)))
