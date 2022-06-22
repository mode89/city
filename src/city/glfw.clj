(ns city.glfw
  (:use [city.gl :only (wrap-constants wrap-methods)]))

(wrap-constants "GLFW_" org.lwjgl.glfw.GLFW)
(wrap-methods "glfw" org.lwjgl.glfw.GLFW)
