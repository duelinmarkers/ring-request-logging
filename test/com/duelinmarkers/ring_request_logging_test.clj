; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging-test
  (:use clojure.test
        [com.duelinmarkers.ring-request-logging :only (wrap-request-logging)])
  (:require [clojure.tools.logging :as logging])
  (:import (clojure.tools.logging.impl Logger LoggerFactory)))

(defn args-collector [atm] (fn [& args] (swap! atm #(conj % args))))

(def success-response {:status 200 :body "Success!"})

(defn success-ring-app [req] success-response)

(def fake-logger (reify
                   Logger
                   (enabled? [_ _] true)
                   Object
                   (toString [_] "fake-logger")))

(def fake-logger-factory (reify LoggerFactory
                           (get-logger [_ logger-ns] fake-logger)))

(deftest basic-functionality
  (binding [logging/*logger-factory* fake-logger-factory]

    (testing "Logs inbound request at info level"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging success-ring-app) {:request-method :get :uri "/index.html" :query-string "foo=bar&baz=bat"})
          (is (= [fake-logger :info nil "Request start: :get /index.html foo=bar&baz=bat"] (first @log*-args))))))

    (testing "Logs entire request map at :trace level"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging success-ring-app) {:request-method :get :uri "/index.html"})
          (is (= [fake-logger :trace nil "Request map: {:request-method :get, :uri \"/index.html\"}"] (fnext @log*-args))))))

    (testing "Applies given :param-middleware to extract :params and logs params at debug level"
      (let [log*-args (atom [])
            first-param-middleware (fn [app] (fn [req] (app (assoc req :params {"p1" "value"}))))
            second-param-middleware (fn [app option] (fn [req] (app (update-in req [:params option] clojure.string/reverse))))
            wrapped-app (wrap-request-logging
                          success-ring-app
                          :param-middleware #(-> % (second-param-middleware "p1") first-param-middleware))]
        (with-redefs [logging/log* (args-collector log*-args)]
          (is (= success-response (wrapped-app {:request-method :get})))
          (is (= [fake-logger :debug nil ":params {p1 eulav}"] (fnext (next @log*-args)))))))

    (testing "Logs outbound response status at info level"
      (let [log*-args (atom [])]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging success-ring-app) {:request-method :get :uri "/index.html"})
          (is (= [fake-logger :info nil "Request end: /index.html 200"] (last @log*-args))))))

    (testing "Logs and rethrows an unhandled throwable by default"
      (let [log*-args (atom [])
            unhandled-throwable (Throwable. "it's me!")
            app (fn [_] (throw unhandled-throwable))]
        (with-redefs [logging/log* (args-collector log*-args)]
          (is (thrown-with-msg? Throwable #"it's me!" ((wrap-request-logging app) {:request-method :get})))
          (is (= [fake-logger :error unhandled-throwable "Unhandled throwable"] (last @log*-args))))))))

(deftest param-filtering
  (binding [logging/*logger-factory* fake-logger-factory]

    (testing "Filters specified :params"
      (let [log*-args (atom [])
            raw-params {"p1" "value" "p2" {"nested" "secret"} "p3" "also secret"}
            param-middleware (fn [app] (fn [req] (app (assoc req :params raw-params))))
            wrapped-app (wrap-request-logging success-ring-app
                                              :param-middleware param-middleware
                                              :filter-params ["p3" "extra" ["p2" "nested"] ["extra" "vec"]])]
        (with-redefs [logging/log* (args-collector log*-args)]
          (wrapped-app {:request-method :get})
          (is (= [fake-logger :debug nil ":params {p1 value, p2 {nested [FILTERED]}, p3 [FILTERED]}"]
                 (fnext (next @log*-args)))))))))

(deftest error-fn-option
  (binding [logging/*logger-factory* fake-logger-factory]

    (testing "After logging, uses provided :error-fn for an unhandled throwable"
      (let [log*-args (atom [])
            req {:request-method :get}
            unhandled-throwable (Throwable. "it's me!")
            app (fn [_] (throw unhandled-throwable))
            custom-error-handler (fn [& args] (is (= [req unhandled-throwable] args))
                                   {:status 500 :body "Oops"})
            wrapped-app (wrap-request-logging app :error-fn custom-error-handler)]
        (with-redefs [logging/log* (args-collector log*-args)]
          (is (= {:status 500 :body "Oops"} (wrapped-app req)))
          (is (= [fake-logger :error unhandled-throwable "Unhandled throwable"] (last @log*-args))))))))

(deftest aleph-support
  (binding [logging/*logger-factory* fake-logger-factory]

    (testing "Logs that an aleph request is being handled asynchronously in place of an outbound response status"
      (let [log*-args (atom [])
            app (constantly {:aleph.http/ignore true})]
        (with-redefs [logging/log* (args-collector log*-args)]
          ((wrap-request-logging app) {:request-method :get})
          (is (= [fake-logger :info nil "Request is async"] (last @log*-args))))))))
