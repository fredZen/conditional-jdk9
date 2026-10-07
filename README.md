# A proof of concept for including Java 9 methods in cljc.java-time

This is an attempt a exploring options to get access to Java 9 time methods in cljc.java-time when running on JDK 9+ without breaking the library on JDK 8.

[Relevant issue](https://github.com/henryw374/cljc.java-time/issues/35)

## Context

- In the [2020 state of Clojure survey](https://clojure.org/news/2020/02/20/state-of-clojure-2020) more than 55 % of respondents reported targeting Java 8.
- In the [2024 state of Clojure survey](https://clojure.org/news/2024/12/02/state-of-clojure-2024) 9.40 of respondents reported targeting Java 8.
- Java 8 support was officially [EOLed in Clojure 1.12](https://clojure.org/news/2024/09/05/clojure-1-12-0#_1_1_java_8_compatiblity_eol_notice). That is, while Clojure 1.12 supports Java 8, future versions very likely won’t.
- Clojure 1.13 alpha 5 [targets Java 17](https://clojure.org/news/2026/07/21/clojure-1-13-alpha5#_java_17_baseline).
- Anecdotically, my previous client was in a situation where, at the same time, mainstream servers were upgrading through Java 11 LTS, Java 14 LTS, and then Java 17 LTS, while another set of machines was stuck on Java. Cljc.java-time contributor @irigarae [is in a similar situation](https://github.com/henryw374/cljc.java-time/pull/34#issuecomment-6041933389)
- Cljc.java-time maintainer @henryw374 [has expressed the wish](https://github.com/henryw374/cljc.java-time/issues/35#issuecomment-6037137237) to keep the library backwards compatible with JDK8

## Brainstorming options

Roughly sorted from most invasive to least invasive.

- Just introduce the new methods to the library and let it break for JDK 8 users – they can stay on an older release of the library
- Fork the library – create cljc.java9-time for JDK 9+ users
- Duplicate the namespaces – have `cljc.java-time.jdk9.xxx` namespaces that contain the same functions as the corresponding `cljc.java-time.xxx` namesapce, plus the extra functions from JDK 9 (alternatively, just put the extra functions there, but that seems inconvenient to use)
- Keep JDK 9 and non-JDK 9 vars in the same namespace, but make the JDK 9 vars conditional on the host JVM (this was the idea suggested by @henryw374 in his comment)
- Provide polyfills (mirroring front-end terminology) for the missing functions in JDK 8
- Do nothing for now – maybe the extra methods are not that important (as evidenced by the library working without them for so long)

## POC implementations

For our proof of concept, we'll be implementing two of the options: duplicating the namespaces, and conditionally defining vars.


We’ll be implementing a stripped down version of the `cljc.java-time.duration` namespace.

The `of-days` function stands for methods that have stayed the same between JDK 8 and JDK 9.  
The `to-days-part` function stands for methods that exist in JDK 9 but not in JDK 8.  
The `divided-by` functions stands for (and is in fact the only) java.time method that has gained an additional signature in JDK 9.

There are three implementations of that namespace.

The `lib/separate-namespaces` directory models adding JDK 9 methods by duplicating namespaces.  
The `lib/reflection` directory models conditionally defining vars for JDK 9 methods. It uses reflection to detect if we a method exists on the host JDK or not.  
The `lib/try-catch` directory models an alternative implementation of conditional vars. It catches `IllegalArgumentException`s from trying to wrap non-existant methods.

There are three sample clients.

The `app/jdk8` directory contains an app meant to run on JDK 8.  
The `app/jdk9-joint-namespaces` directory contains an app that uses JDK 9 features. It is meant to work with the `reflection` or `try-catch` versions of the library.  
The `app/jdk9-separate-namepaces` directory also contains an app that uses JDK 9 features, but this one is meant to work with the `separate-namespaces` version of the library.

### Running the POCs

Caveat: Make sure to use a JDK 8 or a JDK 9 or above depending on the feature you are trying to test.
I use [ASDF](https://asdf-vm.com/) to switch between JDK versions, use what works for you.

``` shell
$ cd app/jdk8 && clj -M:run:+reflection 43 21
$ cd app/jdk8 && clj -M:run:+separate-namespaces 43 21
$ cd app/jdk8 && clj -M:run:+try-catch 43 21
$ cd app/jdk9-joint-namespaces && clj -M:run:+reflection 43 21
$ cd app/jdk9-joint-namespaces && clj -M:run:+try-catch 43 21
$ cd app/jdk9-separate-namespaces && clj -M:run:+separate-namespaces 43 21
```


### POC 1 – Conditional vars using reflection

In the implementation in `lib/reflection`  we use the `clojure.reflect/reflect` function to inspect the class being wrapped.
We check if `:members` contains an item with the expected `:name` (and possibly `:parameter-type`) before calling `defn`.

If needs be, this could also get implemented by directly calling the Java reflection API instead of the Clojure API.

### POC 2 – Contitional vars using exceptions

In the implementation in `lib/try-catch` we wrap JDK 9 specific `defn`s in a `try` `catch` block. If the function being wrapped doesn’t exist on the host JDK, the JVM throws an `IllegalArgumentException`, which we catch to abort the `defn` (in the case of `to-days-part`) or provide a more limited implementation (in the case of `divided-by`).

This doesn’t read super well in my opinion, because it visually interferes with the unrelated `(throw (IllegalArgumentException. "no corresponding java.time method with these args"))`, but that’s not necessarily a show stopper for generated code.

### POC 3 – Split namespaces

The implementation in `lib/separate-namespaces` is very straightforward.  
The `cljc.java-time.duration` namespace only contains JDK 8 compatible wrappers.  
The `cljc.java-time.jdk9.duration` namespace contains all JDK 9 compatible wrappers (including wrappers for functions that already existed in JDK 8).

## Closing thoughts

### Split namespaces vs conditional vars – which approach is cleaner?

The split namespaces approach creates additional clutter for the client, the conditionnal vars approach looks neater.

However, the conditional vars approach creates an additional element of surprise for developers: depending on when / where you look at a namespace, it may contain different namespaces.  
For an application that AOT-compiles cljc.java-time, the contents of the namespace would be determined by the JDK used at compile time.  
For non-AOT-compiled apps, the contents of the namespace would be determined by the JDK used at runtime.

Of course, it is generally adviseable to practice good discipline with JDK versions from development to production, but conditional vars put even more pressure on that. I believe that it is safer to have separate namespaces and make programmers expicitly opt in to JDK 9 specific functions.

### Discriminating between JDK 8 methods and JDK 9 specific methods at generation time

In order to generate specific namespaces for JDK 8 and JDK 9+, we need to be able to know which methods are only available on JDK 9+.

One way to achieve this would be to run the generation process twice, once on a version 8 JDK, generating the jdk 8 namespace, and once on a recent JDK, generating the jdk 9 namespace.

Another approach would be to run the generation process only once, on a recent JDK, and have an EDN file with a hard coded list of methods that are specific to the JDK 9. There are less than 20 such functions (see appendix).

Similarly, if a conditional var approach is chosen, we need a way to decide which vars to wrap in conditions (`try` `catch` blocs of `if` expressions with reflection).

The hard coded list mentioned for split namespaces would do the job here too.

While ugly, another approach is possible: just wrap all vars in a conditional. Most conditionals will just evaluate to `if true` regardless of the JDK version.

### Changes to the ClojureScript side

I haven’t looked into the ClojureScript side of things yet, but I presume the new functions from JDK 9 would need to be implemented there in some way.

## Appendices

## What changed in the java.time API in JDK 9+

- A number of new methods were introduced: `Clock.tickMillis`, `Duration.toDaysPart`, `Duration.toHoursPart`, `Duration.toMillisPart`, `Duration.toMinutesPart`, `Duration.toNanos`, `Duration.toSecondsPart`, `Duration.truncatedTo`, `LocalDate.datesUntil`, `LocalDate.ofInstant`, `LocalDate.toEpochSecond`, `LocalTime.ofInstant`, `LocalTime.toEpochSecond`, `OffsetTime.toEpochSecond`, `DateTimeFormatterBuilder.appendGenericZoneText`
- the `Duration.dividedBy` method gained an additional signature
- the return type of `LocalDate.getEra` was changed from the interface `Era` to the class `IsoEra`
- the `LocalDate.EPOCH` constant was introduced
