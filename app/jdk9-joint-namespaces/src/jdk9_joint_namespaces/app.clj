(ns jdk9-joint-namespaces.app
  (:require
   [cljc.java-time.duration :as duration]))

(defn -main [& args]
  (let [[n1 n2] (map #(Long/parseLong %) args)]
    (println (str (duration/to-days-part (duration/of-days n1))))
    (println (str (duration/divided-by (duration/of-days n1) n2)))
    (println (str (duration/divided-by (duration/of-days n1) (duration/of-days n2))))))
