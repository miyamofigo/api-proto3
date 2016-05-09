(ns api-proto3.utils.tmp
  (:require 
    [clojure.string :refer [split upper-case lower-case starts-with?]]
    [api-proto3.settings :refer [SALT_BITS SALT_BYTES SALT CRYPTO_COST PERSISTENCE_UNIT]]
    [api-proto3.utils.extern.bouncycastle :refer [encode-bytes decode-bytes bcrypt-generate]]
    [api-proto3.utils.extern.jpa :as jpa]
    [api-proto3.utils.extern.springframeworks :as spring])
  (:import 
    java.util.Optional
    [java.util.function Predicate Consumer Supplier]
    java.security.SecureRandom java.nio.ByteBuffer
    org.springframework.mail.SimpleMailMessage
    com.miyamofigo.java8.nursery.Result
    [api_proto3 User UserService UserServiceFactory UserServiceImpl UserServiceFactoryImpl
      UserBasicInfo UserDetailInfo]
    [org.springframework.mail.demo MailServiceBootstrap SimpleMailServiceFactory SimpleMailService]))

;core-utils

(declare lc-first-char uc-first-char) 
(declare split-n-last-withA split-n-first-withA)

(defn subclass? [clazz base] (. base isAssignableFrom clazz)) 

(defn add-elm [set* elm] (conj set* elm))

(defn in-seq? [el seq*] (some #(= el %) seq*))  

(defn merge-pair [pair old]
  (-> pair first (->> (concat old)) (vector (fnext pair)))) 

(defn get-proj-name [] (-> *ns* str split-n-first-withA))

(defn compl-classpath [clname] (str (get-proj-name) clname))   

(defn- new-fieldset [clazz old]
  (-> (.getName clazz) split-n-last-withA lc-first-char (->> (add-elm old))))

(defn get-cl-fieldnames* [clazz]
  (for [field (.getDeclaredFields clazz)] (. field getName)))

(defn get-cl-fieldnames [clazz ignored]
  (let [new-ignored (new-fieldset clazz ignored)]
    (vector (get-cl-fieldnames* clazz) new-ignored))) 

(defonce jcls-ns-prefix "api_proto3.")
(defn get-cl-fieldnames-helper* [clname ignored]
  (-> clname (->> (str jcls-ns-prefix)) Class/forName (get-cl-fieldnames ignored)))

;;; get-cl-fieldnames-helper should be like below.
;;; BUT THIS PROJECt name is odd, so we cannot use java class namespace properly. 

(defn get-cl-fieldnames-helper [clname ignored]
  (-> clname compl-classpath Class/forName (get-cl-fieldnames ignored)))

(declare eval-field-name)

(defn eval-field-names [pred special fnames ignored]
  (loop [src fnames, res [], ignored ignored]
    (if (empty? src)
      [res, ignored]
      (let [field (first src),
            [res*, ignored*]
             (cond 
               (in-seq? field ignored) [res, ignored]
               (contains? special field) [(concat res (special field)), ignored]
               (pred field) (-> field 
                              (eval-field-name pred ignored special) 
                              (merge-pair res)) 
               :else (-> field 
                       (->> (conj (vec res))) 
                       (vector ignored)))] 
         (recur (rest src) res* ignored*)))))

(defn eval-field-name [fnames pred ignored special]
  (-> fnames uc-first-char (get-cl-fieldnames-helper* ignored)
    (->> (apply eval-field-names pred special))))

(defn expected [key* msg] (vector key* msg))

(defonce null-xcpt-msg 
  "Any operation cannot be executed with a null value.")

(defn non-nil?-n-? [src func & args]
  (cond 
    (nil? src) (expected :xcpt null-xcpt-msg)
    (empty? args) (func src)
    :else (apply func src args)))

(defn cast* {:static true} [x ^Class c] (. c (cast x)))

(defn non-nil?-n-cast [src dst-t] 
  (try (non-nil?-n-? src cast* dst-t)
       (catch ClassCastException e (expected :xcpt (.getMessage e)))))

(defn fetch-cl-name [instance] (.. instance getClass getName))

(defn comp-map-n-filt [mfunc ffunc] (comp (map mfunc) (filter ffunc)))

(defn get-mname [^java.lang.reflect.Method m] (. m getName))

(defonce getter-prefix "get")

(defn is-getter-name? [^String s] (starts-with? s getter-prefix))

(def getter-name-selector (comp-map-n-filt get-mname is-getter-name?))

(defn select-getter-names [meths] (sequence getter-name-selector meths))

(defn fetch-all-methods [^java.lang.Object inst] (.. inst getClass getMethods))

(defn fetch-getter-names [inst] (-> inst fetch-all-methods select-getter-names))

(defmacro do-getter [obj gtrnam] (let [gtr# (symbol gtrnam)] `(. ~obj ~gtr#)))

(declare getterName->clj-key)

(defmacro do-all-getters* [obj res & getters]
  (loop [obj# obj, res res, getters getters]  
    (if (empty? getters)
      res
      (let [gtr# (first getters)]
        (recur obj#
          (assoc res
             (getterName->clj-key gtr#)
             `(do-getter ~obj# ~gtr#))
          (rest getters))))))    

(defn do-all-getters-helper [res] (dissoc res :class)) 

(defmacro do-all-getters [obj res & getters]
  `(do-all-getters-helper (do-all-getters* ~obj ~res ~@getters)))

(defmacro class->mapper-sym [^java.lang.Class c] `(-> ~c new instance->mapper-sym)) 

(defmacro recur-pred! [x & forms]
  (let [gx (gensym)]
    `(let [~gx ~x]
       (or ~@(map (fn [f] 
                    (if (seq? f)
                      `(~(first f) ~gx ~@(next f))
                      `(~f ~gx)))
                  forms)))))

(defn is-present? [^Optional o] (. o isPresent))

(defn get+ [^Optional o] (. o get))

(defn is-empty? [^Optional o] (-> o is-present? not)) 

(defn empty+ [] (Optional/empty))

(defn eq?? [^Optional o1, ^Optional o2] (. o1 equals o2))

(defn f->pred [func] (reify Predicate (test [this v] (func v))))

(defn fetch-class [x & _] (class x))

(defn filter+ [^Optional o, pred] (. o filter (f->pred pred)))

(defn flatmap [^Optional o, func] (. o flatmap func))

(defn hashcode [^Optional o] (. o hashCode))

(defn f->consumer [func!] 
  (reify Consumer 
    (accept [this arg] (func! arg)) 
    (andThen [this after!] (fn [x] (-> x func! after!))))) 

(defn do-if-present [^Optional o, func!] (. o ifPresent (f->consumer func!)))

(defmulti map+ fetch-class)

(defmethod map+ Optional [o mapper] (. o map mapper))

(defn opt-of [x] (Optional/of x))

(defn opt-of-nullable [x] (Optional/ofNullable x))

(defmulti or-else fetch-class)

(defmethod or-else Optional [o, other] (. o orElse other)) 

(defn f->supplier [func] (reify Supplier (get [this] (func)))) 

(defn or-else-get [^Optional o, ^Supplier func] (. o orElseGet (f->supplier func)))

(defn or-else-throw [^Optional o, xcpt-supplier] (. o orElseThrow xcpt-supplier))

(defn ok? [^Result r] (. r isOk))

(defn err? [^Result r] (. r isErr))

(defn unwrap [^Result r] (. r unwrap))

(defmethod map+ Result [r mapper] (. r map mapper))

(defn map-err [^Result r, mapper] (. r mapErr mapper))

(defn and+ [^Result r, res] (.and r res))

(defn and-then [^Result r, func] (. r map func))

(defn or+ [^Result r, optb] (.or r optb))

(defmethod or-else Result [r func] (. r orElse func))

(defn unwrap-or [^Result r, optb] (. r unwrapOr optb)) 

(defn unwrap-or-else [^Result r, func] (. r unwrapOrElse func))

(defn unwrap-err [^Result r] (. r unwrapErr))

(defn get-method* [clazz name* & args] (.getMethod clazz name* (into-array java.lang.Class args)))

(defn ordinal [enum] (.ordinal enum))

(defn enum->int [maybe-e]
  (try 
    (if (-> maybe-e .getClass (get-method* "ordinal")) 
      (ordinal maybe-e) 
      maybe-e)
    (catch Exception e maybe-e)))

;string-utils

(defonce REG_SPACE #" ")
(defonce REG_APOSTROPHY #"\.")

(defn- split-n-? [src func & [delim]] 
  (-> src (split (if (nil? delim) REG_SPACE delim)) func))

(defn split-n-first [src delim] (split-n-? src first delim))
(defn split-n-first-withA [string] (split-n-first string REG_APOSTROPHY))

(defn split-n-last [src delim] (split-n-? src last delim))
(defn split-n-take-second [src] (split-n-? src fnext))
(defn split-n-last-withA [string] (split-n-last string REG_APOSTROPHY))

(defn uppercase? [^java.lang.Character c] (java.lang.Character/isUpperCase c)) 
(defn lowercase? [^java.lang.Character c] (java.lang.Character/isLowerCase c)) 

(defonce CHAR_SPACE \space)
(defonce CHAR_HYPHEN \-)

(defn vecc-with-prefix [prefix c] (vector prefix c))
(defn vecc-with-space [c] (vecc-with-prefix CHAR_SPACE c))
(defn vecc-with-hyp [c] (vecc-with-prefix CHAR_HYPHEN c))

(defn lower-case-char [^java.lang.Character c] (java.lang.Character/toLowerCase c))
(defn upper-case-char [^java.lang.Character c] (java.lang.Character/toUpperCase c))

(defn uc->lc->vec 
  ([c] (uc->lc->vec c vector))
  ([c func] (if (uppercase? c) (-> c lower-case-char func) (vector c)))) 

(defn uc->space+lc->vec [c] (uc->lc->vec c vecc-with-space))
(defn uc->hyp+lc->vec [c] (uc->lc->vec c vecc-with-hyp))

(defn op-first-char [op ^String s] (str (-> s first op) (subs s 1)))

(defn uc-first-char [^String s] (op-first-char upper-case-char s))
(defn lc-first-char [^String s] (op-first-char lower-case-char s))

(defn no-uppercase? [^String s] (not-any? uppercase? s))

(declare str-converter-skelton)

(defn javaStr-converter-helper
  ([^String s, str->carray] (javaStr-converter-helper s str->carray nil)) 
  ([^String s, str->carray, finalizer]
     (javaStr-converter-helper s str->carray (partial drop 1) finalizer))
  ([^String s, str->carray, char-op, finalizer]
     (str-converter-skelton s str->carray uc->hyp+lc->vec char-op finalizer)))

(defn ClassName->clj-str [^String s] (javaStr-converter-helper s char-array)) 

(defn getterName->clj-key [^String s] (javaStr-converter-helper s (partial drop 3) keyword)) 

(defn extra-op-helper [func arg] (if (nil? func) (identity arg) (func arg))) 

(defn str-converter-skelton 
  [^String s, str->carray, converter & [char-op finalizer]]
  (->> s str->carray (map converter) (apply concat)
    (extra-op-helper char-op) (apply str) (extra-op-helper finalizer))) 

(defn javaFieldName->clj-str [^String s] (javaStr-converter-helper s char-array nil nil))

(defn ClassName->clj-key [^String s] (-> s ClassName->clj-str keyword))

(defn drop-last-n-tostring [^String s, n] (->> s (drop-last n) (apply str)))

;http-utils

(declare gen-hash)

(defn- fetch-token-from-req [req]
  (-> req :headers :authorization split-n-take-second))

(defn- parse-headers [hdrs]
  (->> (for [[k v] hdrs] [(keyword k) v]) (apply concat) (apply assoc {})))

(defn replace-headers 
  ([req] (update req :headers parse-headers))
 ([req func] (replace-headers req func)))

(defn get-auth [req] (get-in req [:headers :authorization]))

(defn get-hash [req]
  (-> req get-auth split-n-take-second gen-hash))    

(defn get-context [] (spring/get-context))

(defn set-context! [ctx] (spring/set-context! ctx))

(defn get-current-user [] (spring/get-current-user-from-ctx (get-context)))

(defn set-auth! [ctx auth] (spring/set-auth! ctx auth))

;crypt-utils

(defmacro defrandgenerator [clazz] `(defn gen-rand [] (new ~clazz))) 
(defrandgenerator SecureRandom)

(defn- gen-salt [] (. (gen-rand) generateSeed SALT_BYTES))

(defn- set-salt-up [salt-str] (-> salt-str .getBytes decode-bytes))

(defn- inner-generate [auth]
  (bcrypt-generate auth (set-salt-up SALT) CRYPTO_COST))

(defn- str->bytes [src] (. src getBytes))

(defn gen-hash [auth] 
  (-> auth str->bytes inner-generate encode-bytes String.))

(defn- bb-wrap-bytes [b] (ByteBuffer/wrap b)) 

(defn- geti-from-bb [^ByteBuffer buf] (. buf getInt))

(defn bytes->int [b] (-> b bb-wrap-bytes geti-from-bb)) 

(def isize Integer/SIZE)

(defn- alloc-bb [i] (ByteBuffer/allocate i))

(defn- puti-into-bb [^ByteBuffer buf i] (. buf putInt i))

(defn- bb-fetch-array [^ByteBuffer buf] (. buf array))

(defn int->bytes [i] (-> isize alloc-bb (puti-into-bb i) bb-fetch-array))  

;orm-utils

(defmacro defemfcreater [punit] `(defn creat-emf [] (jpa/creat-emf ~punit)))

(defemfcreater PERSISTENCE_UNIT)

(defn creat-em [emf] (jpa/creat-em emf)) 

(defmacro with-emf [& body] `(doto (creat-emf) ~@body .close))

(defmacro with-em [emf# em# & body]
  `(let [~em# (creat-em ~emf#)] (doto ~em# ~@body .close)))

(defn begin-transaction! [em] (jpa/begin-transaction! em))

(defn commit-transaction! [em] (jpa/commit-transaction! em))

(defn refresh-transaction! [em] (jpa/refresh-transaction! em))

(defmacro with-transaction [em# & body]
  `(do (begin-transaction! ~em#) ~@body (commit-transaction! ~em#)))

(defn same-symbol? [obj symname] (= (str obj) symname))

(defmacro creat-entity [f fmap & fields]
  `(~f ~@(map (fn [field] `(~fmap ~(keyword field))) fields))) 

(defn creat-query [em qstr] (jpa/creat-query em qstr))

(defn create-tquery [em qstr clazz] (jpa/create-tquery em qstr clazz))

(defn set-string [query name* str*] (jpa/set-string query name* str*))

(defn get-single-result [query] (jpa/get-single-result query))

(defn close-emf [emf] (jpa/close-emf emf))

(defn close-em [em] (jpa/close-em em))

(defn get-conn-from-em [em cl] (jpa/get-conn-from-em em cl))

;service-utils

(defn creat-usfactory* [^String punit] (UserServiceFactoryImpl/createUserServiceFactory punit))

(defn creat-usfactory [] (creat-usfactory* PERSISTENCE_UNIT))

(defn creat-userv* [^UserServiceFactory usfactory] (. usfactory createUserService)) 

(defn close-usfactory* [^UserServiceFactory usfactory] (. usfactory close))

(defn close-serv [serv] (. serv close)) 

(defn save! [^User user, ^UserService serv] (. serv save user))

(defn find-user-by-id [^UserService serv, id] (. serv findById id))

(defn find-user-by-hash [^UserService serv, ^String hash*] (. serv findByHash hash*)) 

(defn get-username-from-ubinfo [^UserBasicInfo ubinfo] (. ubinfo getUserName))

(defn get-ubinfo [^User u] (. u getUserBasicInfo))

(defn get-username [^User u] (-> u get-ubinfo get-username-from-ubinfo))  

(defn get-udinfo [^User u] (. u getUserDetailInfo))

;(defn get-firstname-from-udinfo [^UserDetailInfo udinfo] (. udinfo getFirstName))
(defn get-firstname-from-udinfo [^UserDetailInfo udinfo] (. udinfo getFirstname))

(defn get-firstname [^User u] (-> u get-udinfo get-firstname-from-udinfo))

(defn get-uhash [^User u] (. u getHash))

(defn drop-last4-n-tostring [clname] (drop-last-n-tostring clname 4))

(defn gen-clname-key [instance] 
  (-> instance fetch-cl-name split-n-last-withA drop-last4-n-tostring ClassName->clj-key))

(defn add-service [request serv] (assoc-in request [:services (gen-clname-key serv)] serv))

(defn add-services [request & servs] 
  (loop [req request, servs servs] 
    (if (empty? servs) 
      req 
      (recur (->> servs first (add-service req)) 
             (rest servs)))))

(defn create-msfactory [] (MailServiceBootstrap/createMailServiceFactory))

(defn create-mail-service [^SimpleMailServiceFactory f] (.create f))

(defn create-smm [^SimpleMailService s, addr] (.create s addr))

(defn send-mail 
  ([^SimpleMailService s, ^SimpleMailMessage m] (.send s m))
  ([^SimpleMailService s, msg & messages]
    (-> msg vector (concat messages)
     (->> (into-array SimpleMailMessage (.send s)))))) 

;handler-utils

(defonce XCPT_MSG_SEP ": ")
(defonce XCPT_BASE_CLASS java.lang.Exception)
(defonce NOT_XCPT_MSG
  "this class is not a subclass of java.lang.Exception!")
(defonce REGISTER_ERR_MSGHDR "register-user! failed: ") 

(defn to-xcpt-msg-header [^java.lang.Class clazz]
  (-> (str clazz) split-n-last-withA 
    (->> (map uc->space+lc->vec) (apply concat) (drop 1) (apply str)) 
    (str XCPT_MSG_SEP)))

(defn exception? [clazz] (subclass? clazz java.lang.Exception)) 

(defn gen-xcpt-msg [clazz & msgs]
  (if (exception? clazz)
    (apply str (first msgs) (to-xcpt-msg-header clazz) (rest msgs))
    (throw (Exception. NOT_XCPT_MSG))))

(defn register-error [clazz e]
  (gen-xcpt-msg clazz REGISTER_ERR_MSGHDR (. e getMessage)))

(defmacro handle-exceptions [body & xcpts]
  (let [c (gensym)]
    `(let [~c (clojure.core.async/chan 10)]
       (try ~body (clojure.core.async/>!! ~c [:success, ""])
         ~@(map (fn [xcpt]
                  (let [e (gensym)]
                    `(catch ~xcpt ~e 
                       (clojure.core.async/>!! ~c
                         [:error, (register-error ~xcpt ~e)]))))
                xcpts))
       (clojure.core.async/<!! ~c))))

(defn do-entity-pred [nam classes]
  (if (no-uppercase? nam)
    false
    (let [clnam (uc-first-char nam)]
      (if (some #(= clnam %) classes) true false))))

(defonce mapper-suffix "->map")

(defn add-mapper-suffix [str*] (str str* mapper-suffix)) 

(defn instance->mapper-sym [^java.lang.Object i]
  (-> i fetch-cl-name split-n-last-withA 
    ClassName->clj-str add-mapper-suffix symbol))

(defmacro defmapper-from-inst [^java.lang.Object _i]
  (let [i (eval _i) 
        mapper# (instance->mapper-sym i) 
        gs# (fetch-getter-names i)]
    `(defn ~mapper# [obj#] (do-all-getters obj# {} ~@gs#))))

(defmacro defmapper [^java.lang.Class c]
  (let [c# (eval c)] `(defmapper-from-inst (new ~c#))))

(defmacro defmappers [mappers & classes]
  (when-not (empty? classes)
    `(do (defmapper ~(first classes)) 
         (swap! ~mappers assoc 
           ~(first classes) (-> ~(first classes) class->mapper-sym resolve))
         (defmappers ~mappers ~@(rest classes)))))
