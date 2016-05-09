(ns api-proto3.orm-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]
            [ring.mock.request :as mock]
            [api-proto3.handler :refer :all]
            [api-proto3.settings :refer [PERSISTENCE_UNIT]]
            [api-proto3.utils.tmp :refer :all])
  (:import [java.util Date Calendar List]
           api_proto3.User
           api_proto3.UserBasicInfo
           api_proto3.UserDetailInfo
           api_proto3.UserDetailInfo$Sex
           api_proto3.UserContactInfo))

(defmacro assert-gs-helper [obj# val# meth#]
  (if (some #(same-symbol? meth# %) ["getCreated" "getUpdated"]) 
    `(is (. ~val# after (. ~obj# ~meth#)))
    `(is (= ~val# (. ~obj# ~meth#)))))

(defmacro assert-getters! [obj# & args]
  (when-not (empty? args)
    (let [[[val# meth#] new_args#] (split-at 2 args)]
      (if (same-symbol? meth# "getBirthDate")
       `(do (is (= ~val# (.. ~obj# ~meth# (get ~(first new_args#)))))
            (assert-getters! ~obj# ~@(rest new_args#))) 
       `(do (assert-gs-helper ~obj# ~val# ~meth#)
            (assert-getters! ~obj# ~@new_args#))))))

(deftest test-basic-info
  (testing "testing UserBasicInfo table.."
    (with-emf
      (with-em em
        (with-transaction
          (doto em
            (. persist (UserBasicInfo. "user11" "user3@gmail.com" (gen-hash "user11")))
            (. persist (UserBasicInfo. "user22" "user4@gmail.com" (gen-hash "user22"))))))
      (with-em em
        (with-transaction
          (let [qlist (.. em (createQuery "from UserBasicInfo") getResultList)
                user-basic-info1 (->> qlist first (cast UserBasicInfo))
                user-basic-info2 (->> qlist rest first (cast UserBasicInfo))]
            (is (= "user11" (. user-basic-info1 getUserName))) 
            (is (= "user22" (. user-basic-info2 getUserName)))
            (is (. (Date.) after (. user-basic-info1 getCreated)))
            (is (. (Date.) after (. user-basic-info2 getCreated)))))))))

(deftest test-detail-info
  (testing "testing UserDetailInfo table.."
    (with-emf
      (with-em em
        (with-transaction
          (let [user1 (UserBasicInfo. "namenamename" 
                        "namenamename@gmail.com" (gen-hash "namenamename"))
                user2 (UserBasicInfo. "emanemaneman"
                        "emanemaneman@gmail.com" (gen-hash "emanemaneman"))]
          (doto em
            (. persist 
              (UserDetailInfo. 
                user1 "Name" "Namename" 1111 Calendar/MARCH 22 UserDetailInfo$Sex/Male)) 
            (. persist
              (UserDetailInfo. 
                user2 "名前" "次郎" 1222 Calendar/FEBRUARY 11 UserDetailInfo$Sex/Male))))))
      (with-em em
        (with-transaction
          (let [users (.. em (createQuery "from UserDetailInfo") getResultList)
                user1 (->> users first (cast UserDetailInfo))
                user2 (->> users rest first (cast UserDetailInfo))]
            (assert-getters! user1
              (.. user1 getUserBasicInfo getId) getId 
              "Name"                           getFirstname
              "Namename"                        getFamilyname 
              1111                              getBirthDate  Calendar/YEAR
              Calendar/MARCH                    getBirthDate  Calendar/MONTH
              22                                getBirthDate  Calendar/DAY_OF_MONTH 
              222                               getAge
              UserDetailInfo$Sex/Male           getSex 
              (Date.)                           getCreated)
            (assert-getters! user2
              (.. user2 getUserBasicInfo getId) getId  
              "名前"                            getFirstname 
              "次郎"                            getFamilyname 
              1222                              getBirthDate  Calendar/YEAR
              Calendar/FEBRUARY                 getBirthDate  Calendar/MONTH
              1                                 getBirthDate  Calendar/DAY_OF_MONTH 
              111                               getAge
              UserDetailInfo$Sex/Male           getSex 
              (Date.)                           getCreated)
            (refresh-transaction! em)
            (doto user2 (.. getBirthDate (set Calendar/YEAR 1111)) (->> (. em persist)))
            (refresh-transaction! em)
            (is (.after (Date.) 
                  (->> (-> em (.createQuery "from UserDetailInfo") .getResultList rest first) 
                    (cast UserDetailInfo) 
                    .getUpdated)))))))))

(deftest test-contact-info
  (testing "testing UserContactInfo table.."
    (let [ch (chan 10)]
      (with-emf
        (with-em em
          (with-transaction
            (. em persist 
              (UserContactInfo.  
                (UserBasicInfo. "mmmmmm" "mmmmmmm@gmail" (gen-hash "mmmmmm"))
                "0000" "Aaaa 0000" "Tokyo" "Tokyo" "+80333333333")))
          (with-transaction
            (let [user (->> (.. em (createQuery "from UserContactInfo") getResultList)
                         first (cast UserContactInfo))]
              (assert-getters! user 
                "0000"         getPostalId 
                "Aaaa 0000"    getAddress 
                "Tokyo"        getCity
                "Tokyo"        getState 
                "+803333333"   getPhoneNum 
                (Date.)           getCreated)
              (>!! ch (.. user getUserBasicInfo getId)) 
              (refresh-transaction! em)
              (doto user
                (. setPostalId "0000") (. setAddress "ああああ　１１１１")
                (. setCity "千代田区") (. setState "東京都") (. setPhoneNum "000000000")))) 
          (with-transaction
            (let [user (->> (.. em (createQuery "from UserContactInfo") getResultList)
                         first (cast UserContactInfo))]
              (assert-getters! user
                "00000" getPostalId "あああああ　１１１１１" getAddress
                "東京" getCity "東京都" getState
                "000000000" getPhoneNum (Date.) getUpdated)
              (is (= (<!! ch) (.. user getUserBasicInfo getId))))))))))

(deftest test-user
  (testing "testing User table.."
    (with-emf
      (with-em em
        (with-transaction
          (. em persist
            (User/createUser
              "aaaaaaaaaa" "bbbbb@gmail" (gen-hash "aaaaaaa")
              "CCCCCCC" "KKKKKK" 7777 Calendar/JULY 4 UserDetailInfo$Sex/Male
              "000000" "bbbbbb 333333" "Tokyo" "Tokyo" "+80333333")))
        (with-transaction
          (let [user (->> (.. em (createQuery "from User") getResultList)
                       first (cast User))
                userb (. user getUserBasicInfo) 
                userc (. user getUserContactInfo)
                userd (. user getUserDetailInfo)]
            (assert-getters! userb
              "jjjjjjjjjj"            getUserName 
              "jjjjjjjjjj@gmail"      getEmail
              (gen-hash "jjjjjjjjjj") getHash)
            (assert-getters! userc
              (. userb getId)   getId 
              "0000000"         getPostalId
              "PPPPP 11111111"  getAddress 
              "Toooooo"         getCity 
              "Kyyyy"           getState 
              "+80333333333"    getPhoneNum)
            (assert-getters! userd
              (. userb getId)         getId 
              "OOOOOOOOo"             getFirstname 
              "KKKKKKKk"              getFamilyname
              1111                    getBirthDate Calendar/YEAR 
              Calendar/MARCH          getBirthDate Calendar/MONTH
              3                       getBirthDate Calendar/DAY_OF_MONTH 
              UserDetailInfo$Sex/Male getSex)))))))

