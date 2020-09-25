(ns fr.jeremyschoffen.prose.alpha.document.clojure-test
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [meander.epsilon :as m]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.document.clojure.env :as env]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]))


(defn load-doc [path]
  (-> path
      io/resource
      slurp
      reader/read-from-string))


(def doc (binding [env/*load-document* load-doc]
           (-> "clojure/master.tp"
               load-doc
               eval/eval-forms-in-temp-ns)))


(def ns-tags (filterv #(and (map? %)
                            (= :ns (:tag %)))
                       (tree-seq map?
                                 :content
                                 {:tag :doc :content doc})))

(deftest insert-require-dos
  (is  (m/match ns-tags
         [?x1 ..2 ?x2 ?x1]
         true

         _ false)))