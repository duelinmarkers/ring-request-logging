; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging-test
  (:use clojure.test)
  (:require [com.duelinmarkers.ring-request-logging :refer (wrap-request-logging)]
            [clojure.tools.logging :as logging])
  (:import (clojure.tools.logging.impl Logger LoggerFactory)))

(defn args-collector [atm] (fn [& args] (swap! atm #(conj % args))))

(defn success-ring-app [req] {:status 200})

(def fake-logger (reify Logger
                   (enabled? [_ _] true)
                   (write! [_ _ _ _] (throw (UnsupportedOperationException.)))))

(def fake-logger-factory (reify LoggerFactory
                           (name [_] "fake-logger-factory")
                           (get-logger [_ logger-ns] fake-logger)))

(deftest test-wrap-request-logging
  (binding [logging/*logger-factory* fake-logger-factory]

    (testing "Logs inbound request at info level"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging success-ring-app) {:request-method :get :uri "/index.html" :query-string "foo=bar&baz=bat"})
          (is (= [fake-logger :info nil "Request start: :get /index.html foo=bar&baz=bat"] (first @log*-args))))))

    (testing "Logs outbound response status at info level"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging success-ring-app) {:request-method :get :uri "/index.html"})
          (is (= [fake-logger :info nil "Request end: /index.html 200"] (last @log*-args))))))

    (testing "Logs that an aleph request is being handled asynchronously in place of an outbound response status"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging (constantly {:aleph.http/ignore true})) {:request-method :get})
          (is (= [fake-logger :info nil "Request is async"] (last @log*-args))))))))
