# ring-request-logging

A [ring](https://github.com/mmcgrana/ring) middleware for logging requests.

Logging is done using [clojure.tools.logging](https://github.com/clojure/tools.logging),
so you can use any logging framework it supports.

## Usage

Usage is just like any standard ring middleware.

    (require '[com.duelinmarkers.ring-request-logging :refer (wrap-request-logging)]))
    
    (defn ring-app [request] {:status 200 :body "Worst app ever!"})
    
    (def logged-app (wrap-request-logging ring-app))

## License

Copyright © 2012 John D. Hume

Distributed under the Eclipse Public License, the same as Clojure.
