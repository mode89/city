(ns city.ecs)

(defmulti update-system (fn [system world] (class system)))

(defn make-world []
  (atom {:entities {}
         :systems []}))

(defn update-world [world]
  (reduce #(update-system %2 %1) world (world :systems)))

(defn add-system! [world system]
  (swap! world update :systems conj system))
