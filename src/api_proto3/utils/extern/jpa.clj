(ns api-proto3.utils.extern.jpa 
  (:import 
    java.sql.Connection
    [javax.persistence 
      EntityManager EntityManagerFactory Persistence 
      Query TypedQuery]))

(defn ^EntityManagerFactory creat-emf 
  [punit] (Persistence/createEntityManagerFactory punit)) 

(defn ^EntityManager creat-em 
  [^EntityManagerFactory emf] (. emf createEntityManager))

(defn begin-transaction! [^EntityManager em] (.. em getTransaction begin))

(defn commit-transaction! [^EntityManager em] (.. em getTransaction commit))

(defn refresh-transaction! [^EntityManager em] (doto (. em getTransaction) .commit .begin))

(defn ^Query creat-query [^EntityManager em ^String qstr] (. em createQuery qstr))

(defn ^TypedQuery create-tquery 
  [^EntityManager em, ^String qstr, ^Class cls] (. em createQuery qstr cls))

(defn ^Query set-string [^Query q, ^String name*, ^String str*] 
  (. q setParameter name* str*))

(defn ^java.lang.Object get-single-result [^Query q] (. q getSingleResult))

(defn close-emf [^EntityManagerFactory emf] (. emf close))

(defn close-em [^EntityManager em] (. em close))

(defn ^Connection get-conn-from-em [^EntityManager em, ^java.lang.Class c] (. em unwrap c))  
