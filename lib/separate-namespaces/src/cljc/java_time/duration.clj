(ns cljc.java-time.duration
  (:import
   (java.time Duration)))

(defn of-days
  {:arglists '(["long"])}
  (^Duration [^long days]
   (Duration/ofDays days)))

(defn divided-by
  {:arglists '(["java.time.Duration" "long"])}
  (^Duration [^Duration this ^long divisor]
   (.dividedBy this divisor)))
