(ns liberty-monitoring.core
  (:gen-class)
  (:require [riemann.client :as r]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))


(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))


(defn ifaces-data
  "Parses /proc/net/dev and returns a map of interface names to a map of
   interface data." 
  [file]
  (let [ifaces (->> (str/split (slurp (java.io.FileReader. file)) #"\n")
                    (map #(str/split % #" "))
                    (mapv #(filter (fn [x] (not (str/blank? x))) %))
                    (drop 2))]

    (into {} (map #(hash-map (keyword (nth (str/split (:interface %) #":") 0)) %)
                  (map #(zipmap [:interface
                                 :rx-bytes
                                 :rx-packets
                                 :rx-errs
                                 :rx-drop
                                 :rx-fifo
                                 :rx-frame
                                 :rx-compressed
                                 :rx-multicast
                                 :tx-bytes
                                 :tx-packets
                                 :tx-errs
                                 :tx-drop
                                 :tx-fifo
                                 :tx-colls
                                 :tx-carrier
                                 :tx-comppressed] %) ifaces)))))

(defn send->riemann
  "Sends events to Riemann. 
   Riemann supposed to be main arbitrary event processing system."
  [event client]
  (-> client
      (r/send-event event)
      (deref 5000 ::timeout)))


(defn connects-event
  "We need to build the event, for different events we are going
  to have different attributes, so we need to build the event"
  [config metric]
  {:service "monitoring.devices.connects"
   :tags ["connects" (:env config)]
   :metric  (:metric metric)})


(defn bytes-rx-event
  "We need to build the event, for different events we are going
  to have different attributes, so we need to build the event"
  [config metric]
  {:service "monitoring.bytes.recieved"
   :tags ["bytes-rx" (:env config)]
   :metric (Long/parseLong metric)})


(defn bytes-tx-event
  "We need to build the event, for different events we are going
  to have different attributes, so we need to build the event"
  [config metric]
  {:service "monitoring.bytes.transmited"
   :tags ["bytes-tx" (:env config)]
   :metric (Long/parseLong metric)})


(defn- -main
  "Liberty monitoring core entry point."
  []
  (println "Hello, Fucking World!")

  (let [config (load-edn "./config.edn")
        client (r/tcp-client (:riemann-host config) (:riemann-port config))
        iface (keyword (:iface config))]
    (loop [event-connects (connects-event config (load-edn (:connects-path config)))
           ifaces (ifaces-data (:ifaces-path config))]
      
      (println "Sent event connects: " event-connects)
      (send->riemann event-connects client)

      (println "Sent event traffic rx: " (bytes-rx-event config(-> iface ifaces :rx-bytes)))
      (send->riemann (bytes-rx-event config (-> iface ifaces :rx-bytes)) client)

      (println "Sent event traffic tx: " (bytes-tx-event config (-> iface ifaces :tx-bytes)))
      (send->riemann (bytes-tx-event config (-> iface ifaces :tx-bytes)) client)


      (Thread/sleep (:timeout config))
      (recur (connects-event config (load-edn (:connects-path config)))
             (ifaces-data (:ifaces-path config))))))
