(ns tid
  (:require [lambdaisland.cli :as cli]
            [tid.ednl :as ednl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as medley]
            [clojure.pprint :as pp]
            [babashka.fs :as fs]))

(defn config-file []
  (str (fs/xdg-config-home) "/tid/config.edn"))

(defn read-config []
  (when (fs/exists? (config-file))
    (edn/read-string (slurp (config-file)))))

(defn write-config [config]
  (io/make-parents (config-file))
  (spit (config-file) (pr-str config)))

(defn entries-file []
  (str (fs/xdg-data-home) "/tid/entries.ednl"))

(defn read-entries []
  (when (fs/exists? (entries-file))
    (vec (ednl/slurp (entries-file)))))

(defn write-entry [entry]
  (io/make-parents (entries-file))
  (ednl/spit (entries-file) [entry] {:append? true}))

(comment

  (write-config {:pc :dell-laptop})

  (read-config)

  (write-entry {:bar 321})

  (fs/delete-if-exists (entries-file))

  (mapv :task (distinct (read-entries)))

  {:action :clock/in
   :task "ABC-2135 Some task"
   :project :proj
   :pc :dell-laptop
   :time #inst "2025"}

  {:action :clock/out}

  {:action :clock/sleep}
  {:action :clock/wakeup}

  )

(defn now [] (java.util.Date.))

(defn tz [opts]
  )

(defn clock [{:keys [action task project]}]
  (let [{:keys [pc]} (read-config)
        entry (-> {:action (keyword "clock" action)
                   :pc pc
                   :ts (now)}
                  (medley/assoc-some :task task :project (keyword project)))]
    (write-entry entry)
    (println "Wrote entry:")
    (pp/pprint entry)))

(def cli
  {:name "tid"
   :commands ["tz" {:command tz
                    :flags []}
              "clock <action>" {:command clock
                                :flags ["-t, --task TASK" "The task"
                                        "-p, --project PROJECT" "The project"]}]
   :flags []})

(when (= *file* (System/getProperty "babashka.file"))
  (cli/dispatch cli))
