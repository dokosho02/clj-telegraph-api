(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib      'org.clojars.dokosho02/clj-telegraph-api)
(def version  "0.1.6")
(def class-dir "target/classes")
(def jar-file  (format "target/%s-%s.jar" (name lib) version))

(defn- basis []
  (b/create-basis {:project "deps.edn"}))

(defn clean
  "Delete the target/ directory."
  [_]
  (b/delete {:path "target"}))

(defn jar
  "Build the library jar.
   Usage: clojure -T:build jar"
  [_]
  (let [basis (basis)]
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  ["src"]
                  :pom-data  [[:description "Idiomatic Clojure client for the Telegraph API"]
                              [:url "https://github.com/dokosho02/clj-telegraph-api"]
                              [:licenses
                               [:license
                                [:name "MIT License"]
                                [:url "https://opensource.org/licenses/MIT"]]]
                              [:developers
                               [:developer
                                [:name "dokosho02"]]]
                              [:scm
                               [:url "https://github.com/dokosho02/clj-telegraph-api"]
                               [:connection "scm:git:git://github.com/dokosho02/clj-telegraph-api.git"]
                               [:developerConnection "scm:git:ssh://git@github.com/dokosho02/clj-telegraph-api.git"]
                               [:tag (str "v" version)]]]})
    (b/copy-dir {:src-dirs   ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file  jar-file}))
  (println (str "Built: " jar-file)))

(defn install
  "Install the jar to the local Maven repository (~/.m2).
   Usage: clojure -T:build install"
  [_]
  (jar nil)
  (b/install {:basis     (basis)
              :lib       lib
              :version   version
              :jar-file  jar-file
              :class-dir class-dir})
  (println (str "Installed: " lib " " version " -> ~/.m2")))

(defn deploy
  "Deploy the jar to Clojars.
   Requires CLOJARS_USERNAME and CLOJARS_PASSWORD env vars.
   Usage: clojure -T:build deploy"
  [_]
  (jar nil)
  ;; deps-deploy is pulled in at runtime — add to :build alias if needed:
  ;;   slipset/deps-deploy {:mvn/version "0.2.2"}
  (let [dd (requiring-resolve 'deps-deploy.deps-deploy/deploy)]
    (dd {:installer :remote
         :artifact  jar-file
         :pom-file  (b/pom-path {:lib       lib
                                 :class-dir class-dir})
         :repository {"clojars" {:url      "https://repo.clojars.org"
                                 :username (System/getenv "CLOJARS_USERNAME")
                                 :password (System/getenv "CLOJARS_PASSWORD")}}}))
  (println (str "Deployed: " lib " " version " -> Clojars")))
