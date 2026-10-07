(ns cljc.java-time.jdk9.duration
  (:import
   (java.time Duration)))

(defn of-days
  {:arglists '(["long"])}
  (^Duration [^long days]
   (Duration/ofDays days)))

(defn to-days-part
  {:arglists '(["java.time.Duration"])}
  (^Integer [^Duration this]
   (.toDaysPart this)))

(defn divided-by
  {:arglists '(["java.time.Duration" "java.time.Duration"] ["java.time.Duration" "long"])}
  ([^Duration this arg0]
   (cond
     (instance? Duration arg0)
     (let [divisor ^Duration arg0]
       (.dividedBy this divisor))
     (instance? Long arg0)
     (let [divisor (long arg0)]
       (.dividedBy this divisor))
     :else (throw (IllegalArgumentException. "no corresponding java.time method with these args")))))

