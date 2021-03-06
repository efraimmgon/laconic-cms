(ns laconic-cms.db.sql.user
  (:require
    [clojure.spec.alpha :as s]
    [laconic-cms.db.core :as db]
    [laconic-cms.db.pathom :refer [parser]]
    [laconic-cms.db.sql.common :as common]
    [next.jdbc :as jdbc]))

;;; ---------------------------------------------------------------------------
;;; Profile

(defn get-user-by-id [id]
  (let [user (parser {[:users/id id] common/user-query})]
    (when (s/valid? :users/User user)
      user)))

(defn get-users []
  (:users/list
    (parser {[:users/all] 
             [{:users/list common/user-query}]})))

(defn get-user-by-username [username]
  (let [user (parser {[:users/username username] common/user-query})]
    (when (s/valid? :users/User user)
      user)))

(defn create-profile!
  "Creates a new profile record."
  [params]
  (db/insert! "profile" params))

(defn update-profile!
  "Updates an existing profile record."
  [params]
  (db/update! "profile" 
              (dissoc params :profile/id) 
              (select-keys params [:profile/id])))

;;; ---------------------------------------------------------------------------
;;; User

(defn create-user!
  "Creates a new user record."
  [params]
  (db/insert! "users" params))

(defn create-user-with-profile!
  "Creates a new user and profile record."
  [params]
  (jdbc/with-transaction [tx db/*db*]
    (binding [db/*db* tx]
      (let [user-id (-> params (select-keys common/user-columns) create-user! :users/id)
            profile-params (select-keys params common/profile-columns)]
        (-> profile-params
            (assoc :profile/user-id user-id)
            create-profile!)
        {:users/id user-id}))))

(defn update-user!
  "Updates an existing user record."
  [params]
  (db/update! "users" 
              (dissoc params :users/id) 
              (select-keys params [:users/id])))


(defn update-user-with-profile!
  "Updates an existing user and profile record."
  [{:users/keys [profile] :as params}]
  (jdbc/with-transaction [tx db/*db*]
    (binding [db/*db* tx]
      (let [uret (-> params 
                     (dissoc :users/profile)
                     update-user!)]
        (when (seq profile)
          [uret (update-profile! profile)])))))

(defn get-user
  "Retrives a user record given the user-id."
  [user-id]
  (db/execute-one!
    ["SELECT * FROM users u WHERE u.id = ?" user-id]))
               
  
(defn delete-user!
  "Deletes a user record given the id."
  [id]
  (db/delete! "users" {:id id}))

  
(comment
  (->> (get-users)
       (every? :users/profile))
  
  
  (s/explain :users/User (parser {[:users/id id] common/user-query}))
  (db/parser [{[:by-id 1] [:users/id 10]}])
  (db/parser [{:users/all 123}]))
