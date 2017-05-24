(ns spiro.server.system
  (:require [manifold.bus :as bus]
            [spiro.api.event :as event-api]
            [spiro.server.connection :as conn]
            [spiro.server.handler :as handler]
            [spiro.server.service :as service]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defn system
  [config]
  (log/info "Building system.")
  (log/merge-config!
   {:appenders {:spit (appenders/spit-appender
                       {:fname "/home/mteaster/wut.log"})}})
  {:event-bus (bus/event-bus)
   :connections (atom {})
   :events (ref {})
   :conn-manager (conn/manager)
   :event-manager (event-api/manager)
   :handler-factory (handler/factory)
   :app (service/aleph-service config)})
