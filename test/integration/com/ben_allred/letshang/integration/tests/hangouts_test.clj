(ns com.ben-allred.letshang.integration.tests.hangouts-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.integration.utils.fixtures :as fixtures]
    [com.ben-allred.letshang.integration.utils.helpers :as h]))

(use-fixtures :once #'fixtures/with-db)

(use-fixtures :each (#'fixtures/with-seed))

(defn ^:private modifier [expectation token hangouts old-name]
  (let [hangout-id (:id (colls/find (comp #{old-name} :name) hangouts))]
    (is hangout-id)
    (is (-> (h/update-hangout token hangout-id {:name "A new name"})
            (:name)
            (cond->>
              (= :can expectation) (= "A new name")
              (not= :can expectation) (not= "A new name"))))
    (is (-> (h/fetch-hangout token hangout-id)
            (:name)
            (cond->>
              (= :can expectation) (= "A new name")
              (= :can't expectation) (= old-name)
              (= :not-found expectation) (nil?))))
    (when (= :can expectation)
      (h/update-hangout token hangout-id {:name old-name}))))

(defn ^:private fetch-associates [token emails]
  (->> (h/fetch-associates token)
       (filter (comp emails :email))
       (sort-by :email)
       (map :id)))

(defn ^:private fetch-invitees [token hangout-id]
  (->> (h/fetch-invitations token hangout-id)
       (map :user-id)
       (set)))

(defn ^:private fetch-moment [token hangout-id window]
  (some-> (h/fetch-moments token hangout-id)
          (->> (colls/find (comp window :window)))
          (select-keys #{:window :date})
          (update :date (comp #(.toLocalDate %) #'dates/->internal))))

(defn ^:private fetch-location [token hangout-id location]
  (-> (h/fetch-locations token hangout-id)
      (->> (colls/find (comp location :name)))))

(deftest ^:integration user-hangouts-test
  (testing "hanging out"
    (let [all-hangouts (reduce #(colls/assoc-by :id %2 %1)
                               (h/fetch-hangouts (h/login "user3@example.test"))
                               (h/fetch-hangouts (h/login "user1@example.test")))]
      (testing "when logged in as user1"
        (let [token (h/login "user1@example.test")]
          (testing "has the correct hangouts"
            (let [hangouts (h/fetch-hangouts token)]
              (is (= 3 (count hangouts)))
              (is (= #{"User 1 hangout 1" "User 1 hangout 2" "User 2 hangout 1"}
                     (into #{} (map :name) hangouts)))

              (testing "can modify User 1 hangout 1"
                (modifier :can token all-hangouts "User 1 hangout 1"))

              (testing "can modify User 1 hangout 2"
                (modifier :can token all-hangouts "User 1 hangout 2"))

              (testing "cannot modify User 2 hangout 1"
                (modifier :can't token all-hangouts "User 2 hangout 1"))

              (testing "cannot modify User 3 hangout 1"
                (modifier :not-found token all-hangouts "User 3 hangout 1"))))

          (testing "has the correct known associates"
            (let [associates (h/fetch-associates token)]
              (is (= 3 (count associates)))
              (is (= #{"user2@example.test" "user3@example.test" "user5@example.test"}
                     (into #{} (map :email) associates)))))))

      (testing "When logged in as user2"
        (let [token (h/login "user2@example.test")]
          (testing "has the correct hangouts"
            (let [hangouts (h/fetch-hangouts token)]
              (is (= 2 (count hangouts)))
              (is (= #{"User 1 hangout 1" "User 2 hangout 1"}
                     (into #{} (map :name) hangouts)))

              (testing "cannot modify User 1 hangout 1"
                (modifier :can't token all-hangouts "User 1 hangout 1"))

              (testing "cannot modify User 1 hangout 2"
                (modifier :not-found token all-hangouts "User 1 hangout 2"))

              (testing "can modify User 2 hangout 1"
                (modifier :can token all-hangouts "User 2 hangout 1"))

              (testing "cannot modify User 3 hangout 1"
                (modifier :not-found token all-hangouts "User 3 hangout 1"))))

          (testing "has the correct known associates"
            (let [associates (h/fetch-associates token)]
              (is (= 3 (count associates)))
              (is (= #{"user1@example.test" "user3@example.test" "user5@example.test"}
                     (into #{} (map :email) associates)))))))

      (testing "When logged in as user3"
        (let [token (h/login "user3@example.test")]
          (testing "has the correct hangouts"
            (let [hangouts (h/fetch-hangouts token)]
              (is (= 2 (count hangouts)))
              (is (= #{"User 1 hangout 1" "User 3 hangout 1"}
                     (into #{} (map :name) hangouts)))

              (testing "cannot modify User 1 hangout 1"
                (modifier :can't token all-hangouts "User 1 hangout 1"))

              (testing "cannot modify User 1 hangout 2"
                (modifier :not-found token all-hangouts "User 1 hangout 2"))

              (testing "can modify User 2 hangout 1"
                (modifier :not-found token all-hangouts "User 2 hangout 1"))

              (testing "cannot modify User 3 hangout 1"
                (modifier :can token all-hangouts "User 3 hangout 1"))))

          (testing "has the correct known associates"
            (let [associates (h/fetch-associates token)]
              (is (= 4 (count associates)))
              (is (= #{"user1@example.test" "user2@example.test" "user4@example.test" "user5@example.test"}
                     (into #{} (map :email) associates)))))))

      (testing "When logged in as user4"
        (let [token (h/login "user4@example.test")]
          (testing "has the correct hangouts"
            (let [hangouts (h/fetch-hangouts token)]
              (is (= 1 (count hangouts)))
              (is (= #{"User 3 hangout 1"}
                     (into #{} (map :name) hangouts)))

              (testing "cannot modify User 1 hangout 1"
                (modifier :not-found token all-hangouts "User 1 hangout 1"))

              (testing "cannot modify User 1 hangout 2"
                (modifier :not-found token all-hangouts "User 1 hangout 2"))

              (testing "can modify User 2 hangout 1"
                (modifier :not-found token all-hangouts "User 2 hangout 1"))

              (testing "cannot modify User 3 hangout 1"
                (modifier :can't token all-hangouts "User 3 hangout 1"))))

          (testing "has the correct known associates"
            (let [associates (h/fetch-associates token)]
              (is (= 1 (count associates)))
              (is (= #{"user3@example.test"}
                     (into #{} (map :email) associates)))))))

      (testing "When logged in as user5"
        (let [token (h/login "user5@example.test")]
          (testing "has the correct hangouts"
            (let [hangouts (h/fetch-hangouts token)]
              (is (= 2 (count hangouts)))
              (is (= #{"User 1 hangout 1" "User 2 hangout 1"}
                     (into #{} (map :name) hangouts)))

              (testing "cannot modify User 1 hangout 1"
                (modifier :can't token all-hangouts "User 1 hangout 1"))

              (testing "cannot modify User 1 hangout 2"
                (modifier :not-found token all-hangouts "User 1 hangout 2"))

              (testing "can modify User 2 hangout 1"
                (modifier :can't token all-hangouts "User 2 hangout 1"))

              (testing "cannot modify User 3 hangout 1"
                (modifier :not-found token all-hangouts "User 3 hangout 1"))))

          (testing "has the correct known associates"
            (let [associates (h/fetch-associates token)]
              (is (= 3 (count associates)))
              (is (= #{"user1@example.test" "user2@example.test" "user3@example.test"}
                     (into #{} (map :email) associates))))))))))

(deftest ^:integration invitations-test
  (testing "invitations"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open invitations"
        (let [hangout-id (->> {:name "A brand new hangout" :others-invite? true}
                              (h/create-hangout token)
                              (:id))]

          (testing "and when inviting a user"
            (let [[user-2-id user-3-id] (fetch-associates token #{"user2@example.test" "user3@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user can invite another user"
                (h/suggest-who (h/login "user2@example.test") hangout-id [user-3-id])

                (is (->> (fetch-invitees token hangout-id)
                         (= #{user-2-id user-3-id}))))))))

      (testing "when creating a hangout with closed invitations"
        (let [hangout-id (->> {:name "A brand new hangout" :others-invite? false}
                              (h/create-hangout token)
                              (:id))]
          (testing "and when inviting a user"
            (let [[user-2-id user-3-id] (fetch-associates token #{"user2@example.test" "user3@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user cannot invite another user"
                (h/suggest-who (h/login "user2@example.test") hangout-id [user-3-id])

                (is (->> (fetch-invitees token hangout-id)
                         (= #{user-2-id})))))))))))

(deftest ^:integration moments-test
  (testing "moments"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open moments"
        (let [hangout-id (->> {:name "A brand new hangout" :when-suggestions? true}
                              (h/create-hangout token)
                              (:id))]
          (testing "the creator can suggest a moment"
            (h/suggest-when token hangout-id {:window :morning :date (dates/->inst (dates/today))})

            (is (-> (fetch-moment token hangout-id #{:morning})
                    (= {:window :morning :date (dates/today)}))))

          (testing "and when inviting a user"
            (let [[user-2-id] (fetch-associates token #{"user2@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user can suggest a moment"
                (h/suggest-when (h/login "user2@example.test") hangout-id {:window :any-time :date (dates/->inst (dates/today))})

                (is (-> (fetch-moment token hangout-id #{:any-time})
                        (= {:window :any-time :date (dates/today)}))))))))

      (testing "when creating a hangout with closed moments"
        (let [hangout-id (->> {:name "A brand new hangout" :when-suggestions? false}
                              (h/create-hangout token)
                              (:id))]
          (testing "the creator can suggest a moment"
            (h/suggest-when token hangout-id {:window :morning :date (dates/->inst (dates/today))})

            (is (-> (fetch-moment token hangout-id #{:morning})
                    (= {:window :morning :date (dates/today)}))))

          (testing "and when inviting a user"
            (let [[user-2-id] (fetch-associates token #{"user2@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user cannot suggest a moment"
                (h/suggest-when (h/login "user2@example.test") hangout-id {:window :any-time :date (dates/->inst (dates/today))})

                (is (nil? (fetch-moment token hangout-id #{:any-time})))))))))))

(deftest ^:integration locations-test
  (testing "locations"
    (let [token (h/login "user1@example.test")]
      (testing "when creating a hangout with open locations"
        (let [hangout-id (->> {:name "A brand new hangout" :where-suggestions? true}
                              (h/create-hangout token)
                              (:id))]
          (testing "the creator can suggest a location"
            (h/suggest-where token hangout-id {:name "An awesome place"})

            (is (fetch-location token hangout-id #{"An awesome place"})))

          (testing "and when inviting a user"
            (let [[user-2-id] (fetch-associates token #{"user2@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user can suggest a location"
                (h/suggest-where (h/login "user2@example.test") hangout-id {:name "A place to go"})

                (is (fetch-location token hangout-id #{"A place to go"})))))))

      (testing "when creating a hangout with closed locations"
        (let [hangout-id (->> {:name "A brand new hangout" :where-suggestions? false}
                              (h/create-hangout token)
                              (:id))]
          (testing "the creator can suggest a location"
            (h/suggest-where token hangout-id {:name "An awesome place"})

            (is (fetch-location token hangout-id #{"An awesome place"})))

          (testing "and when inviting a user"
            (let [[user-2-id] (fetch-associates token #{"user2@example.test"})]
              (h/suggest-who token hangout-id [user-2-id])

              (testing "the invited user cannot suggest a location"
                (h/suggest-where (h/login "user2@example.test") hangout-id {:name "A place to go"})

                (is (nil? (fetch-location token hangout-id #{"A place to go"})))))))))))
