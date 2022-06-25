(ns city.core
  (:import (java.nio FloatBuffer)
           (org.joml Matrix4f)
           (org.lwjgl BufferUtils)
           (org.lwjgl.glfw GLFWFramebufferSizeCallbackI)
           (org.lwjgl.opengl GL))
  (:require [city.ecs :as ecs]
            [city.gl :as gl]
            [city.glfw :as glfw]
            [clojure.string :as s]))

(def INITIAL-VIEWPORT-SIZE {:width 800 :height 600})

(def globals (atom nil))

(def command (atom nil))
(def viewport-size (atom INITIAL-VIEWPORT-SIZE))

(defn compile-shader [shader-type source]
  (let [shader (gl/create-shader
                 ({:vertex gl/VERTEX-SHADER
                   :fragment gl/FRAGMENT-SHADER} shader-type))]
    (gl/shader-source shader source)
    (gl/compile-shader shader)
    (if (= (gl/get-shaderi shader gl/COMPILE-STATUS) gl/TRUE)
      shader
      (let [info-log-length (gl/get-shaderi shader gl/INFO-LOG-LENGTH)]
        (println "Failed to compile shader:")
        (println (gl/get-shader-info-log shader info-log-length))
        (gl/delete-shader shader)
        nil))))

(defn link-program [vs fs]
  (let [program (gl/create-program)]
    (gl/attach-shader program vs)
    (gl/attach-shader program fs)
    (gl/link-program program)
    (if (= (gl/get-programi program gl/LINK-STATUS) gl/TRUE)
      program
      (let [info-log-length (gl/get-programi program gl/INFO-LOG-LENGTH)]
        (println "Failed to link program:")
        (println (gl/get-program-info-log program info-log-length))
        (gl/detach-shader program fs)
        (gl/detach-shader program vs)
        (gl/delete-program program)))))

(defn make-default-shader-program []
  (let [vertex-shader
          (compile-shader :vertex
            (s/join "\n"
              ["#version 450 core"
               "in vec3 in_position;"
               "uniform mat4 uni_mvp;"
               "void main()"
               "{"
               "  gl_Position = uni_mvp * vec4(in_position, 1.0);"
               "}"]))
        fragment-shader
          (compile-shader :fragment
            (s/join "\n"
              ["#version 450 core"
               "out vec4 out_color;"
               "uniform vec4 uni_albedo;"
               "void main()"
               "{"
               "  out_color = uni_albedo;"
               "}"]))
        program (link-program vertex-shader fragment-shader)]
    {:id program
     :in-position (gl/get-attrib-location program "in_position")
     :uni-mvp (gl/get-uniform-location program "uni_mvp")
     :uni-albedo (gl/get-uniform-location program "uni_albedo")}))

(defn make-mesh [{:keys [primitives position indices]}]
  (let [position-buffer (gl/gen-buffers)
        index-buffer (gl/gen-buffers)]
    (gl/bind-buffer gl/ARRAY-BUFFER position-buffer)
    (gl/buffer-data gl/ARRAY-BUFFER position gl/STATIC-DRAW)
    (gl/bind-buffer gl/ARRAY-BUFFER 0)
    (gl/bind-buffer gl/ELEMENT-ARRAY-BUFFER index-buffer)
    (gl/buffer-data gl/ELEMENT-ARRAY-BUFFER indices gl/STATIC-DRAW)
    (gl/bind-buffer gl/ELEMENT-ARRAY-BUFFER 0)
    {:position-buffer position-buffer
     :index-buffer index-buffer
     :primitives ({:triangles gl/TRIANGLES} primitives)
     :index-num (count indices)}))

(defn draw-mesh [mesh position-attr]
  (gl/enable-vertex-attrib-array position-attr)
  (gl/bind-buffer gl/ARRAY-BUFFER (mesh :position-buffer))
  (gl/vertex-attrib-pointer position-attr 3 gl/FLOAT false 0 0)
  (gl/bind-buffer gl/ARRAY-BUFFER 0)
  (gl/bind-buffer gl/ELEMENT-ARRAY-BUFFER (mesh :index-buffer))
  (gl/draw-elements (mesh :primitives)
                    (mesh :index-num)
                    gl/UNSIGNED-INT 0)
  (gl/bind-buffer gl/ELEMENT-ARRAY-BUFFER 0)
  (gl/disable-vertex-attrib-array position-attr))

(defn send-command [cmd]
  (reset! command cmd))

(defn tick [window]
  (when (some? @command)
    (@command)
    (reset! command nil))
  (glfw/poll-events)
  (gl/viewport 0 0 (@viewport-size :width) (@viewport-size :height))
  (gl/clear (bit-or gl/COLOR-BUFFER-BIT gl/DEPTH-BUFFER-BIT))
  (when (some? @globals)
    (let [program (@globals :program)
          aspect (/ (@viewport-size :width) (@viewport-size :height))
          model-mat (-> (Matrix4f.)
                        (.translate -0.25 0.0 0.0)
                        ; (.rotateZ 0.5)
                        (.rotateX 1.0)
                        ; (.scale 1.0 1.0 1.0)
                        )
          view-mat (-> (Matrix4f.)
                       (.setLookAt 0.0 0.0 2.0
                                0.0 0.0 0.0
                                0.0 1.0 0.0)
                       )
          projection-mat (-> (Matrix4f.)
                             ; (.setOrtho2D 0 (@viewport-size :width)
                             ;              0 (@viewport-size :height))
                             ; (.setOrtho2D 0 1 0 1)
                             (.setPerspective 1.0 aspect 0.01 100.0)
                             ; (.lookAt 0.0 0.0 2.0
                             ;          0.0 0.0 0.0
                             ;          0.0 1.0 0.0)
                             )
          mvp (-> projection-mat
                  (.mul view-mat)
                  (.mul model-mat)
                  )
          ]
      (gl/use-program (program :id))
      (gl/uniform-matrix-4fv (program :uni-mvp) false
                               (.get mvp (BufferUtils/createFloatBuffer 16)))
      (gl/uniform-4f (program :uni-albedo) 1.0 0.0 0.0 1.0)
      (draw-mesh (@globals :mesh) (program :in-position))
      (gl/use-program 0))))

(defrecord Position [x y z])
(defrecord Rotation [x y z])
(defrecord Mesh [primitives positions indices])

(defrecord RenderingSystem
  [default-shader
   viewport-size])

(defn make-rendering-system [window]
  (let [viewport-size (atom INITIAL-VIEWPORT-SIZE)]
    (glfw/set-framebuffer-size-callback window
      (reify GLFWFramebufferSizeCallbackI
        (invoke [this window width height]
          (reset! viewport-size {:width width :height height}))))
    (RenderingSystem.
      (make-default-shader-program)
      viewport-size)))

(defmethod ecs/update-system RenderingSystem [system world]
  (let [viewport-size (-> system :viewport-size deref)]
    (gl/viewport 0 0 (viewport-size :width) (viewport-size :height))
    (gl/clear-color 0.3 0.3 0.3 1.0)
    (gl/clear (bit-or gl/COLOR-BUFFER-BIT gl/DEPTH-BUFFER-BIT))
    world))

(defn ui [window-promise]
  (glfw/init)
  (let [window (glfw/create-window (INITIAL-VIEWPORT-SIZE :width)
                                   (INITIAL-VIEWPORT-SIZE :height)
                                   "City" 0 0)]
    (try
      (deliver window-promise window)
      (glfw/make-context-current window)
      (glfw/swap-interval 1)
      (GL/createCapabilities)
      (gl/clear-color 0.1 0.1 0.5 1.0)
      (let [world (ecs/make-world)]
        (ecs/add-system! world (make-rendering-system window))
        (while (not (glfw/window-should-close window))
          (glfw/poll-events)
          (reset! world (ecs/update-world @world))
          (glfw/swap-buffers window)))
      (finally
        (glfw/destroy-window window)
        (glfw/terminate)))))

(defn start-ui-thread []
  (let [window (promise)]
    (.start (Thread. #(ui window)))
    window))

(defn -main [& args]
  (start-ui-thread))

#_(def window (start-ui-thread))
#_ (compile-shader :vertex vertex-shader-source)
#_ (send-command #(gl/clear-color 0.2 0.2 0.2 1))
#_ (send-command #(gl/polygon-mode gl/FRONT-AND-BACK gl/FILL))
#_ (send-command #(gl/polygon-mode gl/FRONT-AND-BACK gl/LINE))
#_ (send-command
     #(do
        (let [program (make-default-shader-program)
              mesh (make-mesh {:primitives :triangles
                               :position (float-array [0.0 0.0 0.0
                                                       0.0 0.5 0.0
                                                       0.5 0.0 0.0
                                                       0.5 0.5 0.0])
                               :indices (int-array [0 1 2
                                                    1 2 3])})]
          (swap! globals assoc :program program :mesh mesh))))
