(ns com.ben-allred.letshang.common.utils.colors
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.preds :as preds]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(def ^:private colors
  (let [m (-> [:black :red :green :yellow :blue :magenta :cyan :white]
              (zipmap (map (partial + 30) (range))))]
    (fn [color]
      (get m color (:white m)))))

(def ^:private attributes
  (let [m (-> [:normal :bright :dim :italic :underline nil nil :invert]
              (zipmap (range))
              (dissoc nil))]
    (fn [attribute]
      (get m attribute (:normal m)))))

(declare colorfully)

(def ^:private default-schema
  {:hash        {:color :white :attribute :dim}
   :fn          {:color :white :attribute :italic}
   :bracket     {:color :white}
   :keyword     {:color :magenta}
   :symbol      {:color :yellow}
   :typed-type  {:color :cyan}
   :typed-value {:color :cyan :attribute :bright}
   :regex       {:color :green}
   :pr-str      {:color :green}
   :number      {:color :cyan}
   :boolean     {:color :blue :attribute :bright}
   :nil         {:color :red}
   :default     {:color :white :attribute :italic}})

(defn ^:private css->str [css]
  (->> css
       (map (fn [[property value]] (str (name property) ": " value)))
       (string/join ";")))

(def ^:private invert
  (let [m {:white  :black
           :black  :white
           :cyan   :darkcyan
           :yellow :darkorange}]
    (fn [style]
      (-> style
          (maps/update-maybe :color #(m % %))
          (maps/update-maybe :background-color #((dissoc m :yellow) % %))))))

(defn ^:private style->css [{:keys [attribute] :or {attribute :normal} :as style}]
  (-> style
      (update :color #(or % :white))
      (cond->
        (= :bright attribute) (assoc :font-weight :bold)
        (= :dim attribute) (assoc :opacity "0.5")
        (= :italic attribute) (assoc :font-style :italic)
        (= :underline attribute) (assoc :text-decoration :underline)
        (= :invert attribute) (-> (set/rename-keys {:color :background-color})
                                  (assoc :color :white)))
      (dissoc :attribute :trim?)
      (invert)
      (maps/update-all keywords/safe-name)))

(defn with-style
  ([[message style]] (with-style message style))
  ([message {:keys [color attribute trim?] :as style}]
   (let [msg (if (string? message) message (pr-str message))]
     #?(:clj  (format (str "\u001b[%d;%dm%s\u001b[m" (when-not trim? " ") "\u001b[m")
                      (attributes attribute)
                      (colors color)
                      (strings/maybe-pr-str message))
        :cljs [(str "%c" (string/trim (strings/maybe-pr-str message)) (when-not trim? " "))
               (css->str (style->css style))]))))

(defn ^:private surround
  ([begin end schema value style]
   (surround begin end colorfully schema value style))
  ([begin end mapper schema value style]
   (conj (into [[begin style]] (mapcat (partial mapper schema) value))
         [end style])))

(defn ^:private colorize* [f _ value css]
  [[(f value) css]])

(defn ^:private colorize-re* [re schema value & styles]
  (->> value
       (pr-str)
       (re-matches re)
       (rest)
       (interleave styles)
       (into (colorize* str schema "#" (:hash schema))
             (comp (partition-all 2) (map reverse)))))

(def ^:private colorize-fn
  (partial colorize* (constantly "#[fn]")))

(def ^:private colorize-map
  (partial surround "{" "}" (fn [schema [k v]] (into (colorfully schema k) (colorfully schema v)))))

(def ^:private colorize-set
  (partial surround "#{" "}"))

(def ^:private colorize-list
  (partial surround "(" ")"))

(def ^:private colorize-vector
  (partial surround "[" "]"))

(def ^:private colorize-typed-pr-str
  (partial colorize-re* #"#([a-z]+) (.+)"))

(def ^:private colorize-regex
  (partial colorize-re* #"#(.+)"))

(def ^:private colorize-symbol
  (partial colorize* (partial str "'")))

(def ^:private colorize-pr-str
  (partial colorize* pr-str))

(def ^:private colorize-basic
  (partial colorize* str))

(def ^:private pred-colorize-mappings
  [#{fn?} [colorize-fn [:fn]]
   #{map?} [colorize-map [:bracket]]
   #{set?} [colorize-set [:bracket]]
   #{list? seq?} [colorize-list [:bracket]]
   #{vector?} [colorize-vector [:bracket]]
   #{symbol?} [colorize-symbol [:symbol]]
   #{uuid? inst?} [colorize-typed-pr-str [:typed-type :typed-value]]
   #{preds/regexp?} [colorize-regex [:regex]]
   #{keyword?} [colorize-basic [:keyword]]
   #{number?} [colorize-basic [:number]]
   #{boolean?} [colorize-basic [:boolean]]
   #{string?} [colorize-pr-str [:pr-str]]
   #{nil?} [colorize-pr-str [:nil]]
   #{some?} [colorize-pr-str [:default]]])

(defn ^:private colorfully [schema value]
  (loop [[[preds [f style-keys]] & more] (partition 2 pred-colorize-mappings)]
    (cond
      ((apply some-fn preds) value) (apply f schema value (map schema style-keys))
      (seq more) (recur more)
      :else (throw (ex-info (str "Cannot colorize: " (pr-str value)) value)))))

(defn ^:private space-n-style [styles]
  (loop [result "" [[message style] & more] styles]
    (let [[[next] :as nx] more
          style (assoc style :trim? (or (nil? nx)
                                        (contains? #{nil "{" "#{" "[" "(" "#"} message)
                                        (contains? #{"}" "]" ")"} next)))
          next-result (str result (with-style message style))]
      (if (empty? more)
        next-result
        (recur next-result more)))))

(defn ^:private trim-the-fat [[msg css-strs]]
  (-> msg
      (string/replace #"((%c#\s+)|([\{\[\(]\s+)|(\s+%c[\}\]\)]))" (comp string/trim second))
      (string/trim)
      (cons css-strs)))

(defn colorized? [value]
  (::colorized? (meta value)))

(defn prep-cljs [value]
  (->> value
       (reduce (fn [[s css] [s' css' :as current]]
                 (if (colorized? current)
                   [(str s s') (apply conj css (rest current))]
                   [(str s s') (conj css css')]))
               ["" []])
       (trim-the-fat)))

(defn colorize
  ([value] (colorize default-schema value))
  ([schema value]
    #?(:clj  (->> value
                  (colorfully schema)
                  (space-n-style)
                  (string/trim))
       :cljs (-> value
                 (->>
                   (colorfully schema)
                   (map with-style))
                 (prep-cljs)
                 (vary-meta assoc ::colorized? true)))))
