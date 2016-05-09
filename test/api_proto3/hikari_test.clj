(ns api-proto3.hikari-test
  (:require
    [clojure.test :refer :all]
    [api-proto3.utils.tmp :refer :all])
  (:import 
    [java.sql Connection SQLException]
    [java.util ArrayList List Properties]
    javax.persistence.EntityManager
    javax.persistence.EntityManagerFactory
    com.zaxxer.hikari.HikariDataSource
    com.zaxxer.hikari.pool.HikariPool
    [org.hibernate Session SessionFactory]
    org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
    org.hibernate.hikaricp.internal.HikariCPConnectionProvider
    org.hibernate.internal.SessionFactoryImpl
    org.hibernate.jpa.HibernateEntityManagerFactory
    org.hibernate.service.internal.SessionFactoryServiceRegistryImpl
    [org.hibernate.service.spi SessionFactoryServiceRegistry Wrapped]
    api_proto3.TestElf))

(defn cast-to-hemf [^EntityManagerFactory emf] (cast HibernateEntityManagerFactory emf))

(defn cast-to-sf-impl [^SessionFactory factory] (cast SessionFactoryImpl factory)) 

(defn cast-to-servreg-impl [^SessionFactoryServiceRegistry reg] (cast SessionFactoryServiceRegistryImpl reg))

(defn get-session-factory [^HibernateEntityManagerFactory emf] (. emf getSessionFactory))

(defn get-properties [^SessionFactoryImpl sf] (. sf getProperties))

(defn get-property [^Properties p, ^String s] (. p get s))

(defn get-service-registry [^SessionFactoryImpl sf] (. sf getServiceRegistry))

(defn get-service [^SessionFactoryServiceRegistryImpl reg, ^java.lang.Class role] (. reg getService role))

(defonce conn-provider-key "hibernate.connection.provider_class")

(defonce conn-provider-val "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")

(defonce max-lifetime-key "hibernate.hikari.maxLifetime")

(defn unwrap* [^Wrapped w, ^java.lang.Class c] (. w unwrap c))

(defn get-pool [^HikariDataSource ds] (TestElf/getPool ds)) 

(defn get-total-conns [^HikariPool pool] (. pool getTotalConnections))

(def ^:dynamic *emf* (atom nil))

(def ^:dynamic *ems* (atom []))

(defonce not-emf-created "an entityManagerFactory has not been created.") 

(defn get-emf [] (if-let [emf @*emf*] emf (throw (Exception. not-emf-created))))

(defn get-session [^EntityManager em] (. em getSession))

(defn get-conn-from-session [^Session s] (. s connection))

(defn get-conn [^EntityManager em] (-> em get-session get-conn-from-session))

(defn sleep [ms] (Thread/sleep ms))

(defn close-conn [^Connection conn] (. conn close))

(defn close-all-ems [ems] 
  (loop [ems ems] 
    (when (not (empty? ems)) 
      (-> (doto ems (-> first close-em)) rest recur))))

(defn creat-em-with-swap [emf] (doto (-> emf creat-em) (->> (swap! *ems* conj))))

(defn str->int [^String s] (Integer/parseInt s))

(defn valid? [^Connection conn, timeout] (. conn isValid timeout))

(defn setup! [] (->> (creat-emf) (reset! *emf*)))

(defn teardown! [] 
  (do (doto *ems* (-> deref close-all-ems) (reset! [])) (-> (get-emf) close-emf) (reset! *emf* nil)))

(defn hikaricp-test-fixture [f] (setup!) (f) (teardown!))

(use-fixtures :once hikaricp-test-fixture)

(deftest hikaricp-connection-provider-test
  (testing "hikaricp connection provider"
    (let [factory (-> (get-emf) cast-to-hemf get-session-factory cast-to-sf-impl),
          provider (-> factory get-service-registry cast-to-servreg-impl (get-service ConnectionProvider)),
          lifetime (-> factory get-properties (get-property max-lifetime-key) str->int),
          em (doto (-> (get-emf) creat-em) (->> (swap! *ems* conj))),
          conn (doto (get-conn em) close-conn)]
      (is (= conn-provider-val (-> factory get-properties (get-property conn-provider-key))))
      (is (not (nil? (unwrap* provider HikariCPConnectionProvider))))
      (is (<= (-> provider (unwrap* HikariDataSource) get-pool get-total-conns) 5)) 
      (do (-> lifetime (+ 5000) sleep) 
          (is (not (= conn (-> (get-emf) creat-em-with-swap get-conn))))
          (is (not (valid? conn 0))))))) 
      
