(ns liberty-monitoring.core
  (:gen-class)
  (:import [java.lang Runtime])
  (:require [riemann.client :as r]
            [clojure.string :as str]
            [liberty-monitoring.events :as e]
            [liberty-monitoring.metrics :as m]
            [liberty-monitoring.utils :as u]
            [clojure.core.async :as async :refer [go go-loop <! >! chan]]))


(defn- -main
  "Liberty monitoring core entry point."
  []
  (println "Hello, Fucking World!")

  (let [ch (chan)
        conf (u/load-edn "./config.edn")
        client (r/tcp-client (:riemann-host conf) (:riemann-port conf))
        iface (keyword (u/default-iface))]


    (go-loop [e (<! ch)]
            (println "Event for sending ---> " e)
            (u/event->riemann e client)
            (recur (<! ch)))      

    (loop [conn  (m/connections)
           idata (m/ifaces)
           capacity  (m/disk-capacity)]

      (go (>! ch (e/heartbeat conf 1)))

      (cond 
        (u/vpn-node? conf) (do 
                              (go (>! ch (e/connections conf (count conn))))

                              (go (>! ch (e/bytes-rx conf (-> idata iface :rx-bytes))))

                              (go (>! ch (e/bytes-tx conf (-> idata iface :tx-bytes))))))

      (cond
        (u/storage? conf) (do (go (>! ch (e/disk-capacity conf capacity)))))

      (Thread/sleep (:timeout conf))
      (recur (m/connections)
             (m/ifaces)
             (m/disk-capacity)))))


;(-main)
