(ns fr.jeremyschoffen.prose.alpha.compilation.core
  (:require
    [net.cgrand.macrovich :as macro :include-macros true])
  #?(:cljs (:import [goog.string StringBuffer])))


;;----------------------------------------------------------------------------------------------------------------------
;; Basic compilation emit! mechanism
;;----------------------------------------------------------------------------------------------------------------------
(defprotocol Output
  (append! [this text]))


(def ^:dynamic *compilation-out*
  (reify Output
    (append! [_ _]
      (throw (ex-info "No compilation output provided." {})))))


(defn emit! [& args]
  (doseq [text args]
    (when text
      (append! *compilation-out* text))))


(macro/deftime
  (defmacro bind-output [out & body]
    `(binding [*compilation-out* ~out]
       ~@body)))


;;----------------------------------------------------------------------------------------------------------------------
;; Stringbuffer implementation
;;----------------------------------------------------------------------------------------------------------------------
(macro/case
  :clj (defn text-output []
         (let [builder (StringBuilder.)]
           (reify
             Object
             (toString [_]
               (str builder))
             Output
             (append! [_ text]
               (.append builder text)))))

  :cljs (defn text-output []
          (let [builder (StringBuffer.)]
            (specify! builder
              Output
              (append! [_ text]
                (.append builder text))))))


(macro/deftime
  (defmacro text-environment [& body]
    `(bind-output (text-output)
                  ~@body
                  (str *compilation-out*))))


;;----------------------------------------------------------------------------------------------------------------------
;; Generic compiler
;;----------------------------------------------------------------------------------------------------------------------
(declare emit-doc!)


(defn emit-seq! [ss]
  (doseq [s ss]
    (emit-doc! s)))


(defn special? [x]
  (and (map? x)
       (contains? x :type)))


(def tag? map?)


(def ^:dynamic *implementation* {:name ::default
                                 :default-emit-str! (fn [& args]
                                                      (throw (ex-info "No `:default-emit-str!` provided"
                                                                      {`*implementation* *implementation*})))
                                 :default-emit-tag! (fn [& args]
                                                      (throw (ex-info "No `:default-emit-tag!` provided"
                                                                      {`*implementation* *implementation*})))
                                 :default-emit-special! (fn [& args]
                                                          (throw (ex-info "No `:default-emit-special!` provided"
                                                                          {`*implementation* *implementation*})))})


(macro/deftime
  (defmacro with-implementation
    "Binds the dynamic var [[*implementation*]] to `i`."
    [i & body]
    `(binding [*implementation* ~i]
       ~@body)))


(defmulti emit-str! (fn [_] (:name *implementation*)))
(defmethod emit-str! :default [s] ((:default-emit-str! *implementation*) s))


(defmulti emit-tag! (fn [node] [(:name *implementation*) (:tag node)]))
(defmethod emit-tag! :default [s] ((:default-emit-tag! *implementation*) s))


(defmulti emit-special! (fn [node] [(:name *implementation*) (:type node)]))
(defmethod emit-special! :default [s] ((:default-emit-special! *implementation*) s))


(defn emit-doc! [node]
  "Emit a document to [[*compilation-out*]].
  The [[*implementation* also needs to be bound]]"
  (cond
    (special? node) (emit-special! node)
    (tag? node) (emit-tag! node)
    (sequential? node) (emit-seq! node)
    (string? node) (emit-str! node)
    :else (throw (ex-info "Can't compile." {:faulty-form node}))))