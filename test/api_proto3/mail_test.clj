(ns api-proto3.mail-test
  (:require 
    [clojure.test :refer :all]
    [api-proto3.utils.tmp :refer :all])
  (:import 
    org.springframework.mail.SimpleMailMessage
    [org.springframework.mail.demo MailServiceBootstrap SimpleMailServiceFactory SimpleMailService]))

(deftest mail-test
  (testing "simple mail service"
    (let [f (create-msfactory), serv (create-mail-service f), mail (create-smm serv "jjjjjj@i.softbank.jp")]
      (is (ok? (send-mail serv mail))))))
