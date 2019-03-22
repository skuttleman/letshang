(ns com.ben-allred.letshang.integration.tests.hangouts-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.integration.utils.fixtures :as fixtures]
    [com.ben-allred.letshang.integration.utils.helpers :as h]
    [com.ben-allred.letshang.integration.utils.http :as test.http]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.dates :as dates]))

(use-fixtures :once #'fixtures/with-db)

(use-fixtures :each (#'fixtures/with-seed))

(defn ^:private modifier [expectation token hangouts old-name]
  (let [hangout-id (:id (colls/find (comp #{old-name} :name) hangouts))]
    (is (-> hangout-id
            (->> (str "/api/hangouts/"))
            (test.http/patch token {:body {:data {:name "A new name"}}})
            (second)
            (get-in [:data :name])
            (cond->>
              (= :can expectation) (= "A new name")
              (not= :can expectation) (not= "A new name"))))
    (is (-> hangout-id
            (->> (str "/api/hangouts/"))
            (test.http/get! token)
            (get-in [:data :name])
            (cond->>
              (= :can expectation) (= "A new name")
              (= :can't expectation) (= old-name)
              (= :not-found expectation) (nil?))))
    (when (= :can expectation)
      (-> hangout-id
          (->> (str "/api/hangouts/"))
          (test.http/patch token {:body {:data {:name old-name}}})))))

(deftest ^:integration user-hangouts-test
  (testing "hanging out"
    (testing "when logged in as user1"
      (let [token (h/login "user1@example.test")]
        (testing "has the correct hangouts"
          (let [hangouts (:data (test.http/get! "/api/hangouts" token))]
            (is (= 3 (count hangouts)))
            (is (= #{"User 1 hangout 1" "User 1 hangout 2" "User 2 hangout 1"}
                   (into #{} (map :name) hangouts)))

            (testing "can modify User 1 hangout 1"
              (modifier :can token hangouts "User 1 hangout 1"))

            (testing "can modify User 1 hangout 2"
              (modifier :can token hangouts "User 1 hangout 2"))

            (testing "cannot modify User 2 hangout 1"
              (modifier :can't token hangouts "User 2 hangout 1"))

            (testing "cannot modify User 3 hangout 1"
              (modifier :not-found token hangouts "User 3 hangout 1"))))

        (testing "has the correct known associates"
          (let [associates (:data (test.http/get! "/api/users/associates" token))]
            (is (= 3 (count associates)))
            (is (= #{"user2@example.test" "user3@example.test" "user5@example.test"}
                   (into #{} (map :email) associates)))))))

    (testing "When logged in as user2"
      (let [token (h/login "user2@example.test")]
        (testing "has the correct hangouts"
          (let [hangouts (:data (test.http/get! "/api/hangouts" token))]
            (is (= 2 (count hangouts)))
            (is (= #{"User 1 hangout 1" "User 2 hangout 1"}
                   (into #{} (map :name) hangouts)))

            (testing "cannot modify User 1 hangout 1"
              (modifier :can't token hangouts "User 1 hangout 1"))

            (testing "cannot modify User 1 hangout 2"
              (modifier :not-found token hangouts "User 1 hangout 2"))

            (testing "can modify User 2 hangout 1"
              (modifier :can token hangouts "User 2 hangout 1"))

            (testing "cannot modify User 3 hangout 1"
              (modifier :not-found token hangouts "User 3 hangout 1"))))

        (testing "has the correct known associates"
          (let [associates (:data (test.http/get! "/api/users/associates" token))]
            (is (= 3 (count associates)))
            (is (= #{"user1@example.test" "user3@example.test" "user5@example.test"}
                   (into #{} (map :email) associates)))))))

    (testing "When logged in as user3"
      (let [token (h/login "user3@example.test")]
        (testing "has the correct hangouts"
          (let [hangouts (:data (test.http/get! "/api/hangouts" token))]
            (is (= 2 (count hangouts)))
            (is (= #{"User 1 hangout 1" "User 3 hangout 1"}
                   (into #{} (map :name) hangouts)))

            (testing "cannot modify User 1 hangout 1"
              (modifier :can't token hangouts "User 1 hangout 1"))

            (testing "cannot modify User 1 hangout 2"
              (modifier :not-found token hangouts "User 1 hangout 2"))

            (testing "can modify User 2 hangout 1"
              (modifier :not-found token hangouts "User 2 hangout 1"))

            (testing "cannot modify User 3 hangout 1"
              (modifier :can token hangouts "User 3 hangout 1"))))

        (testing "has the correct known associates"
          (let [associates (:data (test.http/get! "/api/users/associates" token))]
            (is (= 4 (count associates)))
            (is (= #{"user1@example.test" "user2@example.test" "user4@example.test" "user5@example.test"}
                   (into #{} (map :email) associates)))))))

    (testing "When logged in as user4"
      (let [token (h/login "user4@example.test")]
        (testing "has the correct hangouts"
          (let [hangouts (:data (test.http/get! "/api/hangouts" token))]
            (is (= 1 (count hangouts)))
            (is (= #{"User 3 hangout 1"}
                   (into #{} (map :name) hangouts)))

            (testing "cannot modify User 1 hangout 1"
              (modifier :not-found token hangouts "User 1 hangout 1"))

            (testing "cannot modify User 1 hangout 2"
              (modifier :not-found token hangouts "User 1 hangout 2"))

            (testing "can modify User 2 hangout 1"
              (modifier :not-found token hangouts "User 2 hangout 1"))

            (testing "cannot modify User 3 hangout 1"
              (modifier :can't token hangouts "User 3 hangout 1"))))

        (testing "has the correct known associates"
          (let [associates (:data (test.http/get! "/api/users/associates" token))]
            (is (= 1 (count associates)))
            (is (= #{"user3@example.test"}
                   (into #{} (map :email) associates)))))))

    (testing "When logged in as user5"
      (let [token (h/login "user5@example.test")]
        (testing "has the correct hangouts"
          (let [hangouts (:data (test.http/get! "/api/hangouts" token))]
            (is (= 2 (count hangouts)))
            (is (= #{"User 1 hangout 1" "User 2 hangout 1"}
                   (into #{} (map :name) hangouts)))

            (testing "cannot modify User 1 hangout 1"
              (modifier :can't token hangouts "User 1 hangout 1"))

            (testing "cannot modify User 1 hangout 2"
              (modifier :not-found token hangouts "User 1 hangout 2"))

            (testing "can modify User 2 hangout 1"
              (modifier :can't token hangouts "User 2 hangout 1"))

            (testing "cannot modify User 3 hangout 1"
              (modifier :not-found token hangouts "User 3 hangout 1"))))

        (testing "has the correct known associates"
          (let [associates (:data (test.http/get! "/api/users/associates" token))]
            (is (= 3 (count associates)))
            (is (= #{"user1@example.test" "user2@example.test" "user3@example.test"}
                   (into #{} (map :email) associates)))))))))

(deftest ^:integration invitations-test
  (testing "invitations"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open invitations"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name           "A brand new hangout"
                                                                     :others-invite? true}}})))]

          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  [user-2-id user-3-id] (->> associates
                                             (filter (comp #{"user2@example.test" "user3@example.test"} :email))
                                             (sort-by :email)
                                             (map :id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})

              (testing "the invited user can invite another user"
                (let [token (h/login "user2@example.test")]
                  (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                                   token
                                   {:body {:data {:invitation-ids [user-3-id]}}}))

                (is (->> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                         (:data)
                         (:invitations)
                         (map :user-id)
                         (set)
                         (= #{user-2-id user-3-id}))))))))

      (testing "when creating a hangout with closed invitations"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name           "A brand new hangout"
                                                                     :others-invite? false}}})))]
          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  [user-2-id user-3-id] (->> associates
                                             (filter (comp #{"user2@example.test" "user3@example.test"} :email))
                                             (sort-by :email)
                                             (map :id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})
              (testing "the invited user cannot invite another user"
                (let [token (h/login "user2@example.test")]
                  (is (-> (str "/api/hangouts/" hangout-id "/suggestions/who")
                          (test.http/post token {:body {:data {:invitation-ids [user-3-id]}}})
                          (http/client-error?))))))))))))

(deftest ^:integration moments-test
  (testing "moments"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open moments"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name              "A brand new hangout"
                                                                     :when-suggestions? true}}})))]
          (testing "the creator can suggest a moment"
            (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/when")
                             token
                             {:body {:data {:window :morning :date (dates/->inst (dates/today))}}})
            (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                    (get-in [:data :moments])
                    (->> (colls/find (comp #{:morning} :window)))
                    (select-keys #{:window :date})
                    (update :date (comp #(.toLocalDate %) #'dates/->internal))
                    (= {:window :morning :date (dates/today)}))))

          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  user-2-id (->> associates
                                 (colls/find (comp #{"user2@example.test"} :email))
                                 (:id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})

              (testing "the invited user can suggest a moment"
                (let [token (h/login "user2@example.test")]
                  (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/when")
                                   token
                                   {:body {:data {:window :any-time :date (dates/->inst (dates/today))}}}))

                (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                        (get-in [:data :moments])
                        (->> (colls/find (comp #{:any-time} :window)))
                        (select-keys #{:window :date})
                        (update :date (comp #(.toLocalDate %) #'dates/->internal))
                        (= {:window :any-time :date (dates/today)}))))))))

      (testing "when creating a hangout with closed moments"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name              "A brand new hangout"
                                                                     :when-suggestions? false}}})))]
          (testing "the creator can suggest a moment"
            (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/when")
                             token
                             {:body {:data {:window :morning :date (dates/->inst (dates/today))}}})
            (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                    (get-in [:data :moments])
                    (->> (colls/find (comp #{:morning} :window)))
                    (select-keys #{:window :date})
                    (update :date (comp #(.toLocalDate %) #'dates/->internal))
                    (= {:window :morning :date (dates/today)}))))

          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  user-2-id (->> associates
                                 (colls/find (comp #{"user2@example.test"} :email))
                                 (:id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})
              (testing "the invited user cannot suggest a moment"
                (let [token (h/login "user2@example.test")]
                  (is (-> (str "/api/hangouts/" hangout-id "/suggestions/when")
                          (test.http/post token {:body {:data {:window :any-time :date (dates/->inst (dates/today))}}})
                          (http/client-error?))))))))))))

(deftest ^:integration locations-test
  (testing "locations"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open locations"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name               "A brand new hangout"
                                                                     :where-suggestions? true}}})))]
          (testing "the creator can suggest a location"
            (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/where")
                             token
                             {:body {:data {:name "An awesome place"}}})
            (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                    (get-in [:data :locations])
                    (->> (colls/find (comp #{"An awesome place"} :name))))))

          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  user-2-id (->> associates
                                 (colls/find (comp #{"user2@example.test"} :email))
                                 (:id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})

              (testing "the invited user can suggest a location"
                (let [token (h/login "user2@example.test")]
                  (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/where")
                                   token
                                   {:body {:data {:name "A place to go"}}}))

                (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                        (get-in [:data :locations])
                        (->> (colls/find (comp #{"A place to go"} :name))))))))))

      (testing "when creating a hangout with closed locations"
        (let [hangout-id (:id (:data (test.http/post! "/api/hangouts"
                                                      token
                                                      {:body {:data {:name               "A brand new hangout"
                                                                     :where-suggestions? false}}})))]
          (testing "the creator can suggest a location"
            (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/where")
                             token
                             {:body {:data {:name "An awesome place"}}})
            (is (-> (test.http/get! (str "/api/hangouts/" hangout-id) token)
                    (get-in [:data :locations])
                    (->> (colls/find (comp #{"An awesome place"} :name))))))

          (testing "and when inviting a user"
            (let [associates (:data (test.http/get! "/api/users/associates" token))
                  user-2-id (->> associates
                                 (colls/find (comp #{"user2@example.test"} :email))
                                 (:id))]
              (test.http/post! (str "/api/hangouts/" hangout-id "/suggestions/who")
                               token
                               {:body {:data {:invitation-ids [user-2-id]}}})
              (testing "the invited user cannot suggest a location"
                (let [token (h/login "user2@example.test")]
                  (is (-> (str "/api/hangouts/" hangout-id "/suggestions/where")
                          (test.http/post token {:body {:data {:name "A place to go"}}})
                          (http/client-error?))))))))))))
