(ns api-proto3.service-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [api-proto3.settings :refer :all]
            [api-proto3.utils.tmp :refer :all]
            [api-proto3.handler :refer :all])
  (:import 
    [java.util Optional Calendar]
    [api_proto3 UserServiceFactory UserServiceFactoryImpl UserService UserServiceImpl 
      User UserDetailInfo UserDetailInfo$Sex UserBasicInfo]))

(declare *usfactory*)

(defn test-creat-userv [] (when-let [factory @*usfactory*] (creat-userv* factory)))

(defn test-close-usfactory [] (when-let [factory @*usfactory*] (close-usfactory* factory)))

(def ^:dynamic *usfactory* (atom nil))

(defn setup! [] (->> (creat-usfactory) (reset! *usfactory*)))

(defn teardown! [] (do (test-close-usfactory) (reset! *usfactory* nil)))

(defn service-test-fixture [f] (setup!) (f) (teardown!))

(use-fixtures :once service-test-fixture)

(defonce info 
  {:email "ooOOOOooo@gmail.com",  
   :firstname "Boris", :familyname "Becker",
   :year 1967, :month Calendar/NOVEMBER, :day_of_month 22, 
   :sex UserDetailInfo$Sex/Male,
   :postal-id "8888888", :address "3-4-6 MinamiAoyama",
   :city "Minato", :state "Tokyo",
   :phone-num "09077778888"})

(defonce user "user")

(defonce hash* "hassssssssssssssssssssh")

(deftest service-test
  (testing "user service factory and user service"
    (let [serv (-> (test-creat-userv) get+)]
      (-> info (assoc :user user :hash hash*) creat-user (save! serv))
      (let [user0 (-> serv (find-user-by-id 1) unwrap)
            user1 (-> serv (find-user-by-hash hash*) unwrap)]
        (do (is (= (get-username user0) user)) (is (= (get-firstname user0) "Boris")))
        (do (is (= (get-username user1) user)) (is (= (get-firstname user1) "Boris")))
        (is (ok? (delete serv hash*)))
        (is (err? (delete serv hash*)))
        (is (err? (find-user-by-hash serv hash*)))
        (close-serv serv)))))
 
