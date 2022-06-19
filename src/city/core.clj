(ns city.core
  (:import (org.lwjgl.glfw GLFW)
           (org.lwjgl.opengl GL GL45)))

(defn -main [& args]
  (GLFW/glfwInit)
  (let [window (GLFW/glfwCreateWindow 800 600 "City" 0 0)]
    (GLFW/glfwMakeContextCurrent window)
    (GLFW/glfwSwapInterval 1)
    (GL/createCapabilities)
    (GL45/glClearColor 0.1 0.1 0.5 1.0)
    (while (not (GLFW/glfwWindowShouldClose window))
      (GLFW/glfwPollEvents)
      (GL45/glClear (bit-or GL45/GL_COLOR_BUFFER_BIT
                            GL45/GL_DEPTH_BUFFER_BIT))
      (GLFW/glfwSwapBuffers window))
    (GLFW/glfwDestroyWindow window)
    (GLFW/glfwTerminate)))
