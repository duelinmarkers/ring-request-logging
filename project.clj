(defproject com.duelinmarkers/ring-request-logging "0.1.0"
  :description "Ring middleware to log requests and responses"
  :url "http://github.com/duelinmarkers/ring-request-logging"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.4" :exclusions [org.clojure/clojure]]]
  :profiles {:clojure13 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
