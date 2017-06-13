(ns bottle.event-manager
  (:require [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [bottle.util :as util]
            [taoensso.timbre :as log]))

(defprotocol EventManager
  "Manages events."
  (events [this] "Retrieve all events.")
  (store [this data] "Get the next event identifier."))

(defrecord RefEventManager [counter events]
  EventManager
  (events [this]
    @events)
  (store [this data]
    (dosync
     (let [id (str (alter counter inc))
           event (assoc data
                        :bottle/id id
                        :bottle/time (java.util.Date.)
                        :bottle/closed? false)]
       (log/debug (str "Storing event " event "."))
       (alter events assoc id event)
       event))))

(defn event-manager [config]
  (component/using (map->RefEventManager {:counter (ref 0)})
    [:events]))