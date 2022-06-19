(ns city.core
  (:import (org.lwjgl.glfw GLFW)
           (org.lwjgl.opengl GL GL45)))

(def command (atom nil))

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

(defn link-program [vs fs]
  (let [program (GL45/glCreateProgram)]
    (GL45/glAttachShader program vs)
    (GL45/glAttachShader program fs)
    (GL45/glLinkProgram program)
    (if (= (GL45/glGetProgrami program GL45/GL_LINK_STATUS) GL45/GL_TRUE)
      program
      (let [info-log-length (GL45/glGetProgrami program
                                                GL45/GL_INFO_LOG_LENGTH)]
        (println "Failed to link program:")
        (print (GL45/glGetProgramInfoLog program info-log-length))
        (GL45/glDetachShader program fs)
        (GL45/glDetachShader program vs)
        (GL45/glDeleteProgram program)))))

(defn send-command [cmd]
  (reset! command cmd))

(defn tick [window]
  (when (some? @command)
    (@command)
    (reset! command nil))
  (GLFW/glfwPollEvents)
  (GL45/glClear (bit-or GL45/GL_COLOR_BUFFER_BIT
                        GL45/GL_DEPTH_BUFFER_BIT))
  (GLFW/glfwSwapBuffers window))

(defn ui [window-promise]
  (GLFW/glfwInit)
  (let [window (GLFW/glfwCreateWindow 800 600 "City" 0 0)]
    (try
      (deliver window-promise window)
      (GLFW/glfwMakeContextCurrent window)
      (GLFW/glfwSwapInterval 1)
      (GL/createCapabilities)
      (GL45/glClearColor 0.1 0.1 0.5 1.0)
      (while (not (GLFW/glfwWindowShouldClose window))
        (tick window))
      (finally
        (GLFW/glfwDestroyWindow window)
        (GLFW/glfwTerminate)))))

(defn start-ui-thread []
  (let [window (promise)]
    (.start (Thread. #(ui window)))
    window))

(defn -main [& args]
  (start-ui-thread))
