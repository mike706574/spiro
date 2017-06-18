(ns bottle.client
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [bottle.message :as message]
            [bottle.user-manager :as user-manager]))

(def content-type "application/transit+json")

(defn receive!
  ([conn]
   (receive! conn 100))
  ([conn timeout]
   (let [out @(s/try-take! conn :drained timeout :timeout)]
     (if (contains? #{:drained :timeout} out) out (message/decode content-type out)))))

(defn flush!
  [conn]
  (loop [out :continue]
    (when (not= out :timeout)
      (recur @(s/try-take! conn :drained 10 :timeout)))))

(defn send!
  [conn message]
  (s/put! conn (message/encode content-type message)))

(defn parse
  [response]
  (let [response-content-type (get-in response [:headers "content-type"])]
    (if (and (contains? response :body) (= response-content-type content-type))
      (update response :body (comp (partial message/decode content-type)))
      response)))

(defn connect!
  ([ws-url]
   (connect! ws-url nil))
  ([ws-url category]
   (let [url (if category
               (str ws-url "/" (name category))
               ws-url)
         conn @(http/websocket-client url)]
     conn)))

(defn transit-get
  [url]
  (parse @(http/get url
                    {:headers {"Content-Type" content-type
                               "Accept" content-type}
                     :throw-exceptions false})))

(defn transit-post
  [url body]
  (parse @(http/post url
                     {:headers {"Content-Type" content-type
                                "Accept" content-type}
                      :body (message/encode content-type body)
                      :throw-exceptions false})))

(defn add-user!
  [system username password]
  (user-manager/add! (:user-manager system) {:bottle/username username
                                             :bottle/password password}))

(defprotocol Client
  (authenticate [this credentials])
  (events [this])
  (events-by-category [this category])
  (create-event [this event]))

(defrecord ServiceClient [url content-type token]
  Client
  (authenticate [this credentials]
    (let [response @(http/post (str url "/api/tokens")
                               {:headers {"Content-Type" content-type
                                          "Accept" content-type}
                                :body (message/encode content-type credentials)
                                :throw-exceptions false})]
      (when (= (:status response) 201)
        (assoc this :token (-> response :body slurp)))))

  (events [this]
    (parse @(http/get (str url "/api/events")
                      {:headers {"Content-Type" content-type
                                 "Accept" content-type
                                 "Authorization" (str "Token " token)}
                       :throw-exceptions false})))

  (events-by-category [this category]
    (parse @(http/get (str url "/api/events")
                      {:headers {"Content-Type" content-type
                                 "Accept" content-type
                                 "Authorization" (str "Token " token)}
                       :query-params {"category" (name category)}
                       :throw-exceptions false})))
  (create-event [this event]
    (parse @(http/post (str url "/api/events")
                       {:headers {"Content-Type" content-type
                                  "Accept" content-type
                                  "Authorization" (str "Token " token)}
                        :body (message/encode content-type event)
                        :throw-exceptions false}))))

(defn client
  [{:keys [url content-type]}]
  (map->ServiceClient {:url url
                       :content-type content-type}))
