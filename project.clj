(defproject liberty-monitoring "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [riemann-clojure-client "0.5.4"]
                 [cheshire "5.11.0"]
                 [nrepl "1.0.0"]
                 [org.clojure/core.async "1.5.648"]]
  :main ^:skip-aot liberty-monitoring.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
