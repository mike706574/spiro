(ns bottle.messaging.connector
  (:require [clamq.activemq :as amq]
            [clamq.rabbitmq :as rmq]
            [clojure.spec.alpha :as s]))

(s/def :bottle/event-broker-path string?)
(s/def :bottle/event-endpoint string?)
(s/def :bottle/event-content-type #{"application/edn"
                                    "application/transit+json"
                                    "application/transit+msgpack"})

(s/def :bottle/messaging-config (s/keys :req [:bottle/eventbroker-type
                                              :bottle/event-broker-path
                                              :bottle/event-content-type
                                              :bottle/event-endpoint]))

(defn connector
  [config]
  (case (:bottle/broker-type config)
    :active-mq amq/activemq-connection
    :rabbit-mq rmq/rabbitmq-connection))