; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging
  (:require [clojure.tools.logging :refer (info debug)]))

(defn wrap-request-logging [app & options]
  (fn [req]
    (info "Request start:" (:request-method req) (:uri req) (:query-string req))
    (let [res (app req)]
      (if (:aleph.http/ignore res)
        (info "Request is async")
        (info "Request end:" (:uri req) (:status res)))
      res)))
