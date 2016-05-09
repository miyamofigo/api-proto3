(ns api-proto3.utils.extern.springframeworks 
  (:import 
    org.springframework.security.core.Authentication
    [org.springframework.security.core.context
      SecurityContextHolder SecurityContext]))


(defn get-context [] (SecurityContextHolder/getContext))

(defn get-current-user-from-ctx 
  [^SecurityContext ctx]
  (.. ctx getAuthentication getPrincipal getUsername))

(defn set-context! [^SecurityContext ctx]
  (SecurityContextHolder/setContext ctx))

(defn set-auth! [^SecurityContext ctx, ^Authentication auth]
  (. ctx setAuthentication auth))

