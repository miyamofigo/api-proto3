(ns api-proto3.handler
  (:require [clojure.string :refer [split upper-case lower-case starts-with?]] 
            [clojure.core.async :refer [chan <!! >!!]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [oauth2.handler :refer [spring-jetty-run]]
            [api-proto3.utils.tmp :refer :all])
  (:import [java.util Calendar Optional]
           javax.persistence.EntityExistsException 
           [api_proto3 User UserBasicInfo UserContactInfo UserDetailInfo UserDetailInfo$Sex UserService]
           com.miyamofigo.java8.nursery.Result))

(def ^:dynamic *factory* (atom nil)) 
(def ^:dynamic *ctrs* (atom []))

;handler

; register ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ignored #{"id" "username" "email" "hash" "age" "created" "updated"})
(defonce special {"birthdate" ["year" "month" "day_of_month"]})
(defonce entity-classes ["User" "UserBasicInfo" "UserContactInfo" "UserDetailInfo"])
(defonce entity-classes ["User" "UserBasicInfo" "UserContactInfo" "UserDetailInfo"])
(defonce creater-prefix "creat-") 

(defn entity-class? [fieldnam] (do-entity-pred fieldnam entity-classes))

(defn gen-creator-args [clazz ignored]
  (->> (get-cl-fieldnames clazz ignored) (apply eval-field-names entity-class? special) first)) 

(defmacro defcreator [clazz ignored func]
  (let [args# (->> (gen-creator-args (eval clazz) (eval ignored))
                   (map javaFieldName->clj-str)
                   (concat (list :user :email :hash))) 
        nam# (-> clazz str split-n-last-withA lc-first-char (->> (str creater-prefix)) symbol)]
    `(defn ~nam# [field-map#] (creat-entity ~func field-map# ~@args#))))

(defcreator User ignored User/createUser) 

(defn build-creat-user-args [info username hash*] (assoc info :user username :hash hash*))

(defn register-user! [info username hash* serv]
  (-> info (build-creat-user-args username hash*) creat-user (save! serv)))

(defn get-serv [req] (-> req :services :user-service))

(defn fetch-args-from-req [req] (vector (get-hash req) (get-serv req)))

(defn register-user-helper [req username] 
  (apply register-user! (req :params) username (fetch-args-from-req req)))

(defonce result-base {:status nil :body nil})

(defn gen-register-success [username] (assoc result-base :status 201 :body {:user username})) 

(defonce register-user-error-msg "The user you tried to regiter may have existed already.") 

(defn gen-register-failure [] (assoc result-base :status 400 :body {:error register-user-error-msg}))

(defn gen-register-response [^Result result] 
  (try (-> result unwrap get-username gen-register-success) (catch RuntimeException e (gen-register-failure))))
  
(defn register [req] (->> (get-current-user) (register-user-helper req) gen-register-response)) 

; fetch  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *mappers* (atom {}))
(defonce trimmed [:id :created :updated :hash])

(defmappers *mappers* User UserBasicInfo UserContactInfo UserDetailInfo)

(defn dbtype? [x] (recur-pred! x string? integer? nil?))  

(defn completely-mapped? [m] (every? dbtype? (vals m)))

(defmacro defmapperfetcher [mappers]
  `(defn fetch-mapper [obj#] 
     (if (nil? obj#) 
       nil
       (loop [seq# @~mappers]
         (if (empty? seq#)
           nil 
           (let [[k# v#] (first seq#)] 
             (if (instance? k# obj#) v# (recur (rest seq#)))))))))

(defmapperfetcher *mappers*)

(defn map! [^java.lang.Object o] (if-let [mapper-var (fetch-mapper o)] (mapper-var o)))

(defn not-map? [x] (not (map? x)))

(defn no-more-map? [x] (recur-pred! x not-map? completely-mapped?))

(defn get-year [^Calendar cal] (.get cal Calendar/YEAR))  

(defn get-month [^Calendar cal] (.get cal Calendar/MONTH))  

(defn get-day-of-month [^Calendar cal] (.get cal Calendar/DAY_OF_MONTH))  

(defn cal->date [^Calendar cal] 
  {:year (get-year cal), 
   :month (get-month cal), 
   :day_of_month (get-day-of-month cal)})

(swap! *mappers* assoc Calendar cal->date)

(defn do-check-n-map [maybe-map]
  (if (no-more-map? maybe-map) 
    maybe-map
    (loop [src maybe-map, res {}]
      (if (empty? src) 
        res 
        (let [[k v] (first src)]
          (recur (dissoc src k) (if-let [mapper-var (fetch-mapper v)]
                                  (merge res (mapper-var v)) 
                                  (let [new-v (enum->int v)] 
                                    (assoc res k (if (dbtype? new-v) new-v nil)))))))))) 

(defn translate [obj]
  (loop [maybe-m (map! obj)]
    (let [res (do-check-n-map maybe-m)] 
      (if (= maybe-m res) 
        res 
        (recur res))))) 

(defn trim [map*] (apply dissoc map* trimmed))
        
(defonce fetch-user-error-message "Your information has been deleted by someone..") 

(defn fetch-user [req]
  (let [userv (get-in req [:services :user-service]),
        result (->> req get-hash (find-user-by-hash userv))]
    (if (ok? result) 
      {:status 201 :body (-> result unwrap translate trim)}
      {:status 400 :body fetch-user-error-message})))

; delete  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce delete-user-success-message "Your account has been deleted successfully.") 
(defonce delete-user-error-message "Failed to delete your account or something wrong happens.") 

(defn delete [^UserService u, ^String hash+] (.delete u hash+))

(defn delete-user [req]
  (let [userv (get-in req [:services :user-service]),
        result (-> req get-hash delete)]
    (if (ok? result)
      {:status 201 :body delete-user-success-message}  
      {:status 400 :body delete-user-error-message})))

;middleware

(defn wrap-with-headers [handler] (fn [request] (-> request replace-headers handler)))  

(defn creat-userv [] (when-let [factory @*factory*] (creat-userv* factory)))

(defn gen-servs [] (for [creator @*ctrs*] (-> (creator) get+)))

(defn add-services-helper [request] (apply add-services request (gen-servs)))

(defn close-all-servs [service-map] 
  (loop [servs (vals service-map)] 
    (when-not (empty? servs) 
      (do (-> servs first close-serv) (-> servs rest recur)))))
 
(defn wrap-services [handler] 
  (fn [request] 
    (let [newreq (-> request add-services-helper), 
          response (handler newreq)]
      (do (->> newreq :services close-all-servs) response))))

;app

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/register" req (register req))
  (GET "/fetch" req (fetch-user req))
  (GET "/delete" req (delete-user req))
  (GET "/display" req (str req))
  (route/not-found "Not Found"))

(def app (-> app-routes wrap-services wrap-with-headers 
           (wrap-defaults api-defaults) wrap-json-params wrap-json-response))
 
;driver 

(defn set-usfactory! [factory] (reset! *factory* factory))

(defn init [] (do (->> (creat-usfactory) set-usfactory!) (swap! *ctrs* conj creat-userv))) 

(defn -main [] (do (init) (spring-jetty-run app {:servname "oauth3" :port 7777 :host "localhost"})))
