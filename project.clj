(defproject city "0.1.0"
  :dependencies [
    [org.clojure/clojure "1.11.1"]
    [org.joml/joml "1.10.4"]
    [org.lwjgl/lwjgl "3.3.1"]
    [org.lwjgl/lwjgl "3.3.1" :classifier "natives-linux"]
    [org.lwjgl/lwjgl-glfw "3.3.1"]
    [org.lwjgl/lwjgl-glfw "3.3.1" :classifier "natives-linux"]
    [org.lwjgl/lwjgl-opengl "3.3.1"]
    [org.lwjgl/lwjgl-opengl "3.3.1" :classifier "natives-linux"]
  ]
  :main city.core)
