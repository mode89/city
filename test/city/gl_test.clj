(ns city.gl-test
  (:require [clojure.test :refer :all]
            [city.gl :refer [make-wrapper-method-name]]))

(deftest make-wrapper-method-name-test
  (is (= "clear" (make-wrapper-method-name "Clear")))
  (is (= "polygon-mode" (make-wrapper-method-name "PolygonMode")))
  (is (= "tex-image-2d" (make-wrapper-method-name "TexImage2D"))))
