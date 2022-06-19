(ns city.core
  (:import (org.lwjgl.glfw GLFW)
           (org.lwjgl.opengl GL GL45)))

(defn compiler-shader [shader-type source]
  (let [shader (GL45/glCreateShader
                 ({:vertex GL45/GL_VERTEX_SHADER
                   :fragment GL45/GL_FRAGMENT_SHADER} shader-type))]
    (GL45/glShaderSource shader source)
    (GL45/glCompileShader shader)
    (if (= (GL45/glGetShaderi shader GL45/GL_COMPILE_STATUS) GL45/GL_TRUE)
      shader
      (let [info-log-length (GL45/glGetShaderi shader
                                               GL45/GL_INFO_LOG_LENGTH)]
        (println "Failed to compile shader:")
        (println (GL45/glGetShaderInfoLog shader info-log-length))
        (GL45/glDeleteShader shader)
        nil))))

(defn ui [window-promise]
  (GLFW/glfwInit)
  (let [window (GLFW/glfwCreateWindow 800 600 "City" 0 0)]
    (deliver window-promise window)
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

(defn start-ui-thread []
  (let [window (promise)]
    (.start (Thread. #(ui window)))
    window))

(defn -main [& args]
  (start-ui-thread))
