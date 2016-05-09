(ns api-proto3.settings)

(defonce SALT_BITS 128)
(defonce SALT_BYTES (/ SALT_BITS 8))  
(defonce SALT "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
(defonce CRYPTO_COST 4) 
(defonce PERSISTENCE_UNIT "api_proto3.jpa")

