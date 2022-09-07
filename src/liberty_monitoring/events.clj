(ns liberty-monitoring.events 
	(:gen-class)
	(:import [java.lang Runtime]))


(defn connections
  "Contains metric of amount of devices are connected"
  [config metric]
  {:service "monitoring.devices.connects"
   :tags ["connects" (:env config)]
   :metric metric})


(defn heartbeat
  "Contains metric of service status"
  [config metric]
  {:service "monitoring.heartbeat"
   :tags ["status" (:env config)]
   :metric metric})


(defn bytes-rx
  "Contains metric of amount of recieved traffic in bytes"
  [config metric]
  {:service "monitoring.bytes.recieved"
   :tags ["bytes-rx" (:env config)]
   :metric (Long/parseLong metric)})


(defn bytes-tx
  "Contains metric of amount of transmitted traffic in bytes"
  [config metric]
  {:service "monitoring.bytes.transmited"
   :tags ["bytes-tx" (:env config)]
   :metric (Long/parseLong metric)})

(defn disk-capacity
  "Contains metric of disk capacity"
  [config metric]
  {:service "monitoring.disk.capacity"
   :tags ["capacity" (:env config)]
   :metric (Long/parseLong metric)})