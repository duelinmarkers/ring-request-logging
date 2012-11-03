; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging
  (:require [clojure.tools.logging :as log]))

(defn- filtered-params [params to-filter]
  (reduce
    #(if (vector? %2)
       (if (get-in %1 %2) (assoc-in %1 %2 "[FILTERED]") %1)
       (if (get %1 %2) (assoc %1 %2 "[FILTERED]") %1))
    params
    to-filter))

(defn- wrap-param-logging [app to-filter]
  (fn [req]
    (log/debug :params (filtered-params (:params req) to-filter))
    (app req)))

(defn- to-vec
  "Converts coll to a vector if it's not already. Surely there's already something for this!"
  [coll]
  (if (vector? coll) coll (vec coll)))

(defn wrap-request-logging
  "Wraps logging around a ring app.

  options:
  :param-middleware - a middleware \"wrap-\" fn that will be applied after the
    \"Request start\" message but before :params are logged. Examples might
    include ring's own wrap-params, wrap-keyword-params, wrap-nested-params,
    and wrap-multipart-params. To apply multiple param middlewares, compose
    them into one function, e.g., #(-> % wrap-keyword-params wrap-params)
  :error-fn - a (fn [ring-request-map unhandled-throwable]) ring-response-map).
    The default error-fn will re-raise unhandled-throwable to be caught by
    another middleware or the adapter."
  {:arglists '([app & options])}
  [app & {:keys [param-middleware
                 filter-params
                 error-fn]
          :or {filter-params []
               error-fn #(throw %2)}}]
  (let [param-wrapped-app (wrap-param-logging app filter-params)
        param-wrapped-app (if param-middleware (param-middleware param-wrapped-app) param-wrapped-app)]
    (fn [req]
      (log/info "Request start:" (:request-method req) (:uri req) (:query-string req))
      (log/trace "Request map:" (pr-str req))
      (try
        (let [res (param-wrapped-app req)]
          (if (:aleph.http/ignore res)
            (log/info "Request is async")
            (log/info "Request end:" (:uri req) (:status res)))
          res)
        (catch Throwable t
          (log/error t "Unhandled throwable")
          (error-fn req t))))))
