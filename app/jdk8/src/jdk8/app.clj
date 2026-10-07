(ns jdk8.app
  (:require
   [cljc.java-time.duration :as duration]))

(defn -main [& args]
  (let [[n1 n2] (map #(Integer/parseInt %) args)]
    (println (str (duration/divided-by (duration/of-days n1) n2)))))
