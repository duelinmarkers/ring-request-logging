; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging
  (:require [clojure.tools.logging :as log]))

(defn- wrap-param-logging [app]
  (fn [req]
    (log/debug :params (:params req))
    (app req)))

(defn- to-vec
  "Converts coll to a vector if it's not already. Surely there's already something for this!"
  [coll]
  (if (vector? coll) coll (vec coll)))

(defn- inner-wrap [app param-middleware]
  ((apply comp (conj (to-vec param-middleware) wrap-param-logging)) app))

(defn wrap-request-logging [app & {:keys [param-middleware] :or {param-middleware []}}]
  (let [param-wrapped-app (inner-wrap app param-middleware)]
    (fn [req]
      (log/info "Request start:" (:request-method req) (:uri req) (:query-string req))
      (try
        (let [res (param-wrapped-app req)]
          (if (:aleph.http/ignore res)
            (log/info "Request is async")
            (log/info "Request end:" (:uri req) (:status res)))
          res)
        (catch Throwable t
          (log/error t "Unhandled throwable")
          (throw t))))))
