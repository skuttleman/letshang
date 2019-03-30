(ns com.ben-allred.letshang.integration.tests.websocket-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.letshang.common.utils.colls :as colls]
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
            ws (h/ws-connect token)
            hangout-id (->> token
                            (h/fetch-hangouts)
                            (colls/find (comp #{"user1@example.test"} :email :creator))
                            (:id))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])

        (testing "and when creating a message"
          (let [message (h/create-message token hangout-id {:body "This is a message"})]

            (testing "receives the message via websocket"
              (let [msg (async/<!! ws)]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:messages/new message]}]
                       msg))))))

        (async/close! ws)))

    (testing "when subscribing to an unowned hangout"
      (let [token (h/login "user1@example.test")
            ws (h/ws-connect token)
            hangout-id (->> token
                            (h/fetch-hangouts)
                            (colls/find (comp not #{"user1@example.test"} :email :creator))
                            (:id))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])

        (testing "and when creating a message"
          (let [message (h/create-message token hangout-id {:body "This is a message"})]

            (testing "receives the message via websocket"
              (let [msg (async/<!! ws)]
                (is (= [:ws/message {:topic [:hangout hangout-id] :body [:messages/new message]}]
                       msg))))))

        (async/close! ws)))

    (testing "when subscribing to an unauthorized hangout"
      (let [user-1-token (h/login "user1@example.test")
            user-4-token (h/login "user4@example.test")
            ws (h/ws-connect user-4-token)
            hangout-id (->> user-1-token
                            (h/fetch-hangouts)
                            (colls/find (comp #{"User 1 hangout 1"} :name))
                            (:id))]
        (async/>!! ws [:subscriptions/subscribe [:hangout hangout-id]])

        (testing "and when creating a message"
          (h/create-message user-4-token hangout-id {:body "This is a message"})

          (testing "receives no message"
            (is (nil? (async/<!! (h/with-timeout ws))))))

        (async/close! ws)))))
