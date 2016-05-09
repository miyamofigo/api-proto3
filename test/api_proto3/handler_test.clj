(ns api-proto3.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [api-proto3.handler :refer :all]
            [api-proto3.utils.tmp :refer :all])
  (:import java.util.Calendar
           org.springframework.security.core.context.SecurityContextHolder
           org.springframework.security.core.Authentication
           org.springframework.security.core.userdetails.UserDetails
           api_proto3.User
           api_proto3.UserBasicInfo
           api_proto3.UserDetailInfo
           api_proto3.UserDetailInfo$Sex))

(defn setup! [] (init))

(defn close-usf! [] (when-let [factory (deref *factory*)] (close-usfactory* factory)))

(defn clear-usf! [] (set-usfactory! nil))

(defn teardown! [] (do (close-usf!) (clear-usf!)))

(defn handler-test-fixture [f] (setup!) (f) (teardown!))

(use-fixtures :once handler-test-fixture)

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World")))) 

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(def username "user")
(def credential "Basic XXXXXXXXXXXXXXXXX")

(defn build-udmock [user] 
  (reify UserDetails
    (getUsername [this] user)))

(defn build-authmock [detail cred]
  (reify Authentication
    (getCredentials [this] [cred])
    (getDetails [this] [detail])
    (getPrincipal [this] detail)))

(defn gen-mock-auth [user cred]
  (build-authmock (build-udmock user) cred))

(defn set-mock-auth! [user cred]
  (let [auth (gen-mock-auth user cred)] 
    (-> (doto (get-context) (set-auth! auth)) set-context!)))

(defn build-mock-request [method url] 
  (-> (keyword method) (mock/request url) 
    (assoc-in [:headers :authorization] credential) add-services-helper))

(defn build-mock-register-request [method url user] 
  (-> (build-mock-request method url) (assoc :params user)))

(defn close-all-servs-in-req [req] (-> req :services close-all-servs))

(defn get-user-detail-info [^User u] (.getUserDetailInfo u))

(defn get-class [obj] (.getClass obj))

(defmulti get-sex get-class)

(defmethod get-sex UserDetailInfo [obj] (.getSex obj))

(defmethod get-sex User [obj] (-> obj get-user-detail-info get-sex))

(def users
  {:becker 
    {:email "ooOOOOooo@gmail.com", :firstname "Boris", :familyname "Becker",
     :year 1967, :month Calendar/NOVEMBER, :day_of_month 22, :sex UserDetailInfo$Sex/Male,
     :postal-id "8888888", :address "3-4-6 MinamiAoyama",:city "Minato", :state "Tokyo",
     :phone-num "09077778888"}
   :murley
    {:email "ooooooooo@gmail.com", :firstname "Andy", :familyname "Murley",
     :year 1977, :month Calendar/JUNE, :day_of_month 6, :sex UserDetailInfo$Sex/Male,
     :postal-id "1234567", :address "3-4-6 KitaAoyama", :city "Minato", :state "Tokyo",
     :phone-num "09015288888"}})

(deftest test-register
  (testing "testing utils"
    (let [req (build-mock-request :get "/")]
      (is (= credential (get-auth req)))
      (set-mock-auth! username credential)
      (is (= username (get-current-user)))
      (is (= (gen-hash "XXXXXXXXXXXXXXXXXX")) (get-hash req))
      (close-all-servs-in-req req)))
  (testing "testing register"
    (let [req (build-mock-register-request :get "/register" (users :becker))] 
      (set-mock-auth! username credential)
      (is (= {:status 201 :body {:user username}} (register req)))
      (is (= {:status 401 :body {:error register-user-error-msg}}))
      (close-all-servs-in-req req)))
  (testing "testing fetch-user"
    (let [req (build-mock-request :get "/fetch"),
          user0 (-> req get-serv (find-user-by-hash (get-hash req)) unwrap)] 
      (set-mock-auth! username credential)
      (let [res (fetch-user req)]
        (is (= 201 (res :status)))
        (is (= 0 (.ordinal UserDetailInfo$Sex/Male))) 
        ;(is (= 111 (get-sex user0)))  
        ;(is (= 0 (res :body)))
        (is (= "Boris" (get-in res [:body :firstname])))))))
