(ns liberty-monitoring.metrics 
	(:gen-class)
	(:import [java.lang Runtime])
    (:require [clojure.string :as str]
              [clojure.java.shell :as shell]
              [liberty-monitoring.utils :as u])
    (:use [clojure.walk :only [keywordize-keys] ]))


(defn ifaces-file []
  (if (u/ubuntu?)
    "/proc/net/dev"
    "resources/dev"))


(defn ifaces
  "Parses /proc/net/dev and returns a map of interface names to a map of
   interface data." 
  []
  (let [interfaces (->> (str/split (slurp (java.io.FileReader. (ifaces-file))) #"\n")
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
                                 :tx-comppressed] %) interfaces)))))


(defn conn-cmd []
  (if (u/ubuntu?) 
    (->(shell/sh "/usr/sbin/ip" "-o" "xfrm" "pol") :out)
    (slurp (java.io.FileReader. "resources/connections"))))


(defn connections []
  "Parses output of 'ip -o xfrm pol' command - the same way 'ipsec show does'
   except ip addresses; only reqid and priority; Returns ({:reqid X :priority Y})"
  []
    (->> (str/split (conn-cmd) #"\n")
                    (map #(str/split % #"\s|\t"))
                    (filter #(and (some (fn [x](= x "dir")) %)
                                  (some (fn [x](= x "out")) %)
                                  (not (some (fn [x] (= x "ipv6-icmp")) %))))
                    (map #(remove (fn [x] (and (not (= "priority" x)) 
                                               (not (= "reqid" x))
                                               (nil? (re-matches #"\d+" x))))%))))

(defn disk-stats-raw []
	(->> (str/split (-> (shell/sh "df" "-P") :out) #"\n")
		(map #(str/split % #"\s|\t"))
		(map #(remove (fn [x] (empty? x)) %))))

(defn disk-keys [k]
	(->> k
		(map #(keyword (str/lower-case %)))
		drop-last))

(defn disk-stats []
	(let [d (disk-stats-raw)]
		(->> (map #(zipmap (disk-keys (first d)) %) (next d))
			(filter #(= "/" (:mounted %)))
			first)))

(defn disk-capacity []
	(->> (disk-stats) 
		 :capacity
         (remove #(= \% %))
         (apply str)))
