(defproject api-proto3 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.371"]
                 [org.clojure/tools.trace "0.7.8"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [oauth2 "0.1.0-SNAPSHOT"]
                 [com.h2database/h2 "1.2.145"]
                 [org.bouncycastle/bcprov-ext-jdk15on "1.54"]
                 [org.hibernate/hibernate-core "5.1.0.Final"]
                 [org.hibernate/hibernate-entitymanager "5.1.0.Final"]
                 [org.hibernate/hibernate-hikaricp "5.1.0.Final"]
                 [org.springframework/spring-web "4.2.4.RELEASE"]
                 [org.springframework.security/spring-security-core
                   "4.0.3.RELEASE"]
                 [org.springframework.session/spring-session-data-redis
                   "1.0.2.RELEASE"]
                 [org.springframework.data/spring-data-redis
                   "1.6.2.RELEASE"]
                 [commons-logging/commons-logging "1.2"]
                 [org.apache.commons/commons-pool2 "2.4.2"]
                 [org.slf4j/slf4j-api "1.7.18"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [ch.qos.logback/logback-core "1.1.3"]
                 [com.zaxxer/HikariCP "2.4.4"]
                 [org.apache.logging.log4j/log4j-core "2.5"]
                 [org.jboss.logging/jboss-logging "3.3.0.Final"]
                 [com.miyamofigo/java8.nursery "0.2.0-SNAPSHOT"]
                 [simple-mailservice-demo/simple-mailservice-demo "0.1.0-SNAPSHOT"]]
  :java-source-paths ["src/java" "test/java"]
  :main api-proto3.handler
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
