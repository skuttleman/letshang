(ns com.ben-allred.letshang.integration.tests.websocket-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.integration.utils.fixtures :as fixtures]
    [com.ben-allred.letshang.integration.utils.helpers :as h]))

(use-fixtures :once
              (#'fixtures/with-app)
              #'fixtures/with-db)

(use-fixtures :each
              (#'fixtures/with-seed))

(deftest ^:integration subscriptions-test
  (testing "subscriptions"
    (testing "when subscribing to an owned hangout"
      (let [token (h/login "user1@example.test")
            [user-6-id :as token-6] (h/login "user6@example.test")
            ws (h/ws-connect token)
            hangout-id (->> token
                            (h/fetch-hangouts)
                            (colls/find (comp #{"user1@example.test"} :email :creator))
                            (:id))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])
        (is (= [:subscriptions/subscribed [:hangout hangout-id]] (async/<!! (h/with-timeout ws))))

        (testing "and when creating a message"
          (let [message (h/create-message token hangout-id {:body "This is a message"})]

            (testing "receives the message via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:messages/new {:data message}]}]
                       msg))))))

        (testing "and when updating the hangout"
          (let [hangout (h/update-hangout token hangout-id {:name "User 1 hangout 1 - new name"})]

            (testing "receives the hangout via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:hangout/updated {:data hangout}]}]
                       msg))))

            (testing "and when another user is not invited"
              (let [ws-2 (h/ws-connect token-6)]

                (testing "and when updating invitees"
                  (h/suggest-who token hangout-id #{user-6-id})

                  (testing "receives the invitation via websocket"
                    (let [msg (async/<!! (h/with-timeout ws-2))]
                      (is (= [:ws/message {:topic [:user user-6-id] :body [:hangouts/invited {:data hangout}]}]
                             msg)))))

                (async/close! ws-2)))))

        (testing "and when responding to an invitation"
          (let [invitation-id (->> (h/fetch-invitations token-6 hangout-id)
                                   (filter (comp #{user-6-id} :user-id))
                                   (first)
                                   (:invitation-id))
                response (h/respond-who token-6 invitation-id :positive)]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id]
                                     :body  [:hangout.invitation/response {:data response}]}]
                       msg))))))

        (testing "and when suggesting a location"
          (let [{location-id :id :as location} (h/suggest-where token-6 hangout-id {:name "Number 6's place"})]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id]
                                     :body  [:hangout.suggestion/location {:data location}]}]
                       msg))))

            (testing "and when responding to the location"
              (let [response (h/respond-where token-6 location-id :neutral)]

                (testing "receives the update via websocket"
                  (let [msg (async/<!! (h/with-timeout ws))]
                    (is (= [:ws/message {:topic [:hangout hangout-id]
                                         :body  [:hangout.location/response {:data response}]}]
                           msg))))))))

        (testing "and when suggesting a moment"
          (let [{moment-id :id :as moment} (h/suggest-when token-6 hangout-id {:window :morning :date (dates/today)})]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:hangout.suggestion/moment {:data moment}]}]
                       msg))))

            (testing "and when responding to the moment"
              (let [response (h/respond-when token-6 moment-id :neutral)]

                (testing "receives the update via websocket"
                  (let [msg (async/<!! (h/with-timeout ws))]
                    (is (= [:ws/message {:topic [:hangout hangout-id]
                                         :body  [:hangout.moment/response {:data response}]}]
                           msg))))))))

        (async/close! ws)))

    (testing "when subscribing to an unowned hangout"
      (let [token (h/login "user1@example.test")
            [user-6-id :as token-6] (h/login "user6@example.test")
            ws (h/ws-connect token)
            [hangout-id creator-token] (->> token
                                            (h/fetch-hangouts)
                                            (colls/find (comp not #{"user1@example.test"} :email :creator))
                                            ((juxt :id (comp h/login :email :creator))))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])
        (is (= [:subscriptions/subscribed [:hangout hangout-id]] (async/<!! (h/with-timeout ws))))

        (testing "and when creating a message"
          (let [message (h/create-message token hangout-id {:body "This is a message"})]

            (testing "receives the message via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:messages/new {:data message}]}]
                       msg))))))

        (testing "and when updating the hangout"
          (let [hangout (h/update-hangout creator-token hangout-id {:name "User 1 hangout 1 - new name"})]

            (testing "receives the hangout via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:hangout/updated {:data hangout}]}]
                       msg))))

            (testing "and when another user is not invited"
              (let [ws-2 (h/ws-connect token-6)]

                (testing "and when updating invitees"
                  (h/suggest-who creator-token hangout-id #{user-6-id})

                  (testing "receives the invitation via websocket"
                    (let [msg (async/<!! (h/with-timeout ws-2))]
                      (is (= [:ws/message {:topic [:user user-6-id] :body [:hangouts/invited {:data hangout}]}]
                             msg)))))

                (async/close! ws-2)))))

        (testing "and when responding to an invitation"
          (let [invitation-id (->> (h/fetch-invitations token-6 hangout-id)
                                   (filter (comp #{user-6-id} :user-id))
                                   (first)
                                   (:invitation-id))
                response (h/respond-who token-6 invitation-id :positive)]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id]
                                     :body  [:hangout.invitation/response {:data response}]}]
                       msg))))))

        (testing "and when suggesting a location"
          (let [{location-id :id :as location} (h/suggest-where creator-token hangout-id {:name "Number 6's place"})]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id]
                                     :body  [:hangout.suggestion/location {:data location}]}]
                       msg))))

            (testing "and when responding to the location"
              (let [response (h/respond-where token-6 location-id :neutral)]

                (testing "receives the update via websocket"
                  (let [msg (async/<!! (h/with-timeout ws))]
                    (is (= [:ws/message {:topic [:hangout hangout-id]
                                         :body  [:hangout.location/response {:data response}]}]
                           msg))))))))

        (testing "and when suggesting a moment"
          (let [{moment-id :id :as moment} (h/suggest-when creator-token
                                                           hangout-id
                                                           {:window :morning :date (dates/today)})]

            (testing "receives the update via websocket"
              (let [msg (async/<!! (h/with-timeout ws))]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:hangout.suggestion/moment {:data moment}]}]
                       msg))))

            (testing "and when responding to the moment"
              (let [response (h/respond-when token-6 moment-id :neutral)]

                (testing "receives the update via websocket"
                  (let [msg (async/<!! (h/with-timeout ws))]
                    (is (= [:ws/message {:topic [:hangout hangout-id]
                                         :body  [:hangout.moment/response {:data response}]}]
                           msg))))))))

        (async/close! ws)))

    (testing "when subscribing to an unauthorized hangout"
      (let [user-1-token (h/login "user1@example.test")
            user-4-token (h/login "user4@example.test")
            ws (h/ws-connect user-4-token)
            hangout-id (->> user-1-token
                            (h/fetch-hangouts)
                            (colls/find (comp #{"User 1 hangout 1 - new name"} :name))
                            (:id))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])
        (is (= [:subscriptions/unsubscribed [:hangout hangout-id]] (async/<!! (h/with-timeout ws))))

        (testing "and when updating the hangout"
          (h/create-message user-1-token hangout-id {:body "This is a message"})
          (h/update-hangout user-1-token hangout-id {:name "User 1 hangout 1 - new name"})
          (h/suggest-where user-1-token hangout-id {:name "Number 6's place"})
          (h/suggest-when user-1-token hangout-id {:window :morning :date (dates/today)})

          (testing "receives no messages"
            (is (nil? (async/<!! (h/with-timeout ws))))))

        (async/close! ws)))))
