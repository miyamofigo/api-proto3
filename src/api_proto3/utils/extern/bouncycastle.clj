(ns api-proto3.utils.extern.bouncycastle
  (:import org.bouncycastle.util.encoders.Hex
           org.bouncycastle.crypto.generators.BCrypt))

(defn encode-bytes [b] (Hex/encode b))

(defn decode-bytes [b] (Hex/decode b))

(defn bcrypt-generate [auth salt cost] 
  (BCrypt/generate auth salt cost))
