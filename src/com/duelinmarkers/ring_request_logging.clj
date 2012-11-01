; Copyright © 2012 John D. Hume
; Distributed under the Eclipse Public License, the same as Clojure.
(ns com.duelinmarkers.ring-request-logging
  (:require [clojure.tools.logging :as log]))

(defn wrap-request-logging [app & options]
  (fn [req]
    (log/info "Request start:" (:request-method req) (:uri req) (:query-string req))
    (try
      (let [res (app req)]
        (if (:aleph.http/ignore res)
          (log/info "Request is async")
          (log/info "Request end:" (:uri req) (:status res)))
        res)
      (catch Throwable t
        (log/error t "Unhandled throwable")
        (throw t)))))
