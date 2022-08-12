(ns liberty-monitoring.core

  (:gen-class)
  (:require [riemann.client :as r]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure-csv.core :as csv]))


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

(defn take-csv
  "Takes file name and reads data."
  [source]
  (with-open [file (io/reader source)]
    (-> file
        (slurp)
        (csv/parse-csv))))

(defn parse-ifaces
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (let [d (take-csv source)
        ens3 (str/split (nth (nth d 2) 0) #" ")] 

    (into {} (map #(hash-map :interface (nth % 0)
                             :rx-bytes (nth % 1)
                             :rx-packets (nth % 2)
                             :rx-errs (nth % 3)
                             :rx-drop (nth % 4)
                             :rx-fifo (nth % 5)
                             :rx-frame (nth % 6)
                             :rx-compressed (nth % 7)
                             :rx-multicast (nth % 8)
                             :tx-bytes (nth % 9)
                             :tx-packets (nth % 10)
                             :tx-errs (nth % 11)
                             :tx-drop (nth % 12)
                             :tx-fifo (nth % 13)
                             :tx-colls (nth % 14)
                             :tx-carrier (nth % 15)
                             :tx-compressed (nth % 16)) (vector (filter #(not (str/blank? %)) ens3))))))


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
   :metric (:metric metric) 
   })


(defn bytes-rx-event
  "We need to build the event, for different events we are going
  to have different attributes, so we need to build the event"
  [config metric]
  {:service "monitoring.bytes.recieved"
   :tags ["bytes-rx" (:env config)]
   :metric (Long/parseLong metric) 
   })

(defn bytes-tx-event
  "We need to build the event, for different events we are going
  to have different attributes, so we need to build the event"
  [config metric]
  {:service "monitoring.bytes.transmited"
   :tags ["bytes-tx" (:env config)]
   :metric (Long/parseLong metric) 
   })


(defn- -main
  "Liberty monitoring core entry point."
  []
  (println "Hello, Fucking World!")

  (let [config (load-edn "./config.edn")
        client (r/tcp-client (:riemann-host config) (:riemann-port config))]
    (loop [event-connects (connects-event config (load-edn (:connects-path config)))
           ifaces (parse-ifaces (:traffic-path config))]
      
      (println "BYTES: " (:tx-bytes ifaces) (:rx-bytes ifaces))
      
      (println "Sent event connects: " event-connects)
      (send->riemann event-connects client)
            
      (println "Sent event traffic rx: " (bytes-rx-event config (:rx-bytes ifaces)))
      (send->riemann (bytes-rx-event config (:rx-bytes ifaces)) client)
      
      (println "Sent event traffic tx: " (bytes-tx-event config (:tx-bytes ifaces)))
      (send->riemann (bytes-tx-event config (:tx-bytes ifaces)) client)
      
      
      (Thread/sleep 30000)
      (recur (connects-event config (load-edn (:connects-path config)))
             (parse-ifaces (:traffic-path config))))))




;(-main)

;(def config (load-edn "./config.edn")) 
;(def client (r/tcp-client "localhost" 5555))
;(def ifaces (parse-ifaces (:traffic-path config)))
;(type (:tx-bytes ifaces))
;(send->riemann (bytes-tx-event config (:tx-bytes ifaces)) client)
;
;(Long/parseLong (:tx-bytes ifaces))