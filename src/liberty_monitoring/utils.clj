(ns liberty-monitoring.utils
	(:gen-class)
	(:import [java.lang Runtime])
	(:require [clojure.java.shell :as shell]
		      [clojure.java.io :as io]
		      [clojure.string :as str]
     	      [clojure.edn :as edn]
     	      [riemann.client :as r]
     	      [cheshire.core :as json])
	(:use [clojure.walk :only [keywordize-keys] ]))


(defn ubuntu? 
  "Returns true if Ubuntu, "
  []
  (try 
    (if (= "Ubuntu" (-> (shell/sh "lsb_release" "-si") :out str/trim-newline ))
      true
      false)

  (catch java.io.IOException e
    false)))


(defn default-iface 
  "Parses output of 'ip -json route and gets default interface;"
  [] 
  (if (ubuntu?)
    (let [routes (-> (shell/sh "ip" "-json" "route") 
                   :out
                 json/parse-string
                 keywordize-keys)]
        (:dev (nth (filter #(= (:dst %) "default") routes) 0)))
     "ens3"))

(defn storage? [conf]
  (if (not (empty? (filter #(= "storage" %) (:type conf))))
    true
    false))

(defn vpn-node? [conf]
  (if (not (empty? (filter #(= "vpn" %) (:type conf))))
    true
    false))

(defn web? [conf]
  (if (not (empty? (filter #(= "web" %) (:type conf))))
    true
    false))


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


(defn event->riemann
  "Sends events to Riemann. 
   Riemann supposed to be main arbitrary event processing system."
  [event client]
  (-> client
      (r/send-event event)
      (deref 5000 ::timeout)))