(ns tasks
  (:require
    [clojure.java.shell :as shell]
    ))

(defn files->server [amount prefix version]
     (loop [s (range 1 (+ 1 amount))
               jar (apply str "target/uberjar/liberty-monitoring-" version "-SNAPSHOT-standalone.jar")]

                (if (nil? s)
                    (println "DONE " s )
                    (do (println "Deploying -> " prefix s)
                       (shell/sh "scp" jar (apply str prefix (first s) ":~/monitoring/"))
                       (shell/sh "scp" (apply str "./deploy/config/" prefix ".edn") 
                                       (apply str prefix (first s) ":~/monitoring/config.edn"))
                       (shell/sh "scp" (apply str "./deploy/start.sh") 
                                       (apply str prefix (first s) ":~/monitoring/"))
                       (shell/sh "scp" (apply str "./deploy/liberty-monitoring.service") 
                                       (apply str prefix (first s) ":~/monitoring/"))

                       (recur (next s) 
                              jar)))))