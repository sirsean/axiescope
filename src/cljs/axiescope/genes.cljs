(ns axiescope.genes
  (:require
    [clojure.string :as string]
    [cljs-web3.core :as web3]
    [axiescope.axie :refer [attach-tank-body attach-dps-body attach-support-body
                            base-stats parts->stats
                            weighted-average]]
    ))

(defn genes->binary
  [genes]
  (let [gene-string (-> genes web3/to-big-number (.toString 2))]
    (str
      (string/join
        (repeat
          (- 256 (count gene-string))
          "0"))
      gene-string)))

(defn slice-genes
  [genes]
  (->> genes
       (partition-all 32)
       (map (partial apply str))))

(def class-gene-map
  {"0000" "beast"
   "0001" "bug"
   "0010" "bird"
   "0011" "plant"
   "0100" "aquatic"
   "0101" "reptile"
   "1000" "???"
   "1001" "???"
   "1010" "???"})

(defn group->class
  [group]
  (let [bin (subs group 0 4)]
    (get class-gene-map bin "?")))

(def region-gene-map
  {"00000" "global"
   "00001" "japan"})

(defn group->region
  [group]
  (let [bin (subs group 8 13)]
    (get region-gene-map bin "?")))

(def binary-traits
  {"beast" {"eyes" {"001000" {"global" "Puppy"}
                    "000010" {"global" "Zeal"
                              "mystic" "Calico Zeal"}
                    "000100" {"global" "Little Peas"
                              "xmas" "Snowflakes"}
                    "001010" {"global" "Chubby"}}
            "ears" {"001010" {"global" "Puppy"}
                    "000100" {"global" "Nut Cracker"}
                    "000010" {"global" "Nyan"
                              "mystic" "Pointy Nyan"}
                    "000110" {"global" "Innocent Lamb"
                              "xmas" "Merry Lamb"}
                    "001000" {"global" "Zen"}
                    "001100" {"global" "Belieber"}}
            "back" {"001000" {"japan" "Hamaya"
                              "global" "Risky Beast"}
                    "000100" {"global" "Hero"}
                    "000110" {"global" "Jaguar"}
                    "000010" {"mystic" "Hasagi"
                              "global" "Ronin"}
                    "001010" {"global" "Timber"}
                    "001100" {"global" "Furball"}}
            "horn" {"001000" {"japan" "Umaibo"
                              "global" "Pocky"}
                    "000100" {"global" "Imp"
                              "japan" "Kendama"}
                    "000110" {"global" "Merry"}
                    "000010" {"mystic" "Winter Branch"
                              "global" "Little Branch"}
                    "001010" {"global" "Dual Blade"}
                    "001100" {"global" "Arco"}}
            "tail" {"000100" {"global" "Rice"}
                    "000010" {"global" "Cottontail"
                              "mystic" "Sakura Cottontail"}
                    "000110" {"global" "Shiba"}
                    "001000" {"global" "Hare"}
                    "001010" {"global" "Nut Cracker"}
                    "001100" {"global" "Gerbil"}}
            "mouth" {"000100" {"global" "Goda"}
                     "000010" {"global" "Nut Cracker"
                               "mystic" "Skull Cracker"}
                     "001000" {"global" "Axie Kiss"}
                     "001010" {"global" "Confident"}}}
   "bug" {"mouth" {"001000" {"japan" "Kawaii"
                             "global" "Cute Bunny"}
                   "000010" {"global" "Mosquito"
                             "mystic" "Feasting Mosquito"}
                   "000100" {"global" "Pincer"}
                   "001010" {"global" "Square Teeth"}}
          "horn" {"001010" {"global" "Parasite"}
                  "000010" {"global" "Lagging"
                            "mystic" "Laggingggggg"}
                  "000110" {"global" "Caterpillars"}
                  "000100" {"global" "Antenna"}
                  "001000" {"global" "Pliers"}
                  "001100" {"global" "Leaf Bug"}}
          "tail" {"001000" {"global" "Gravel Ant"}
                  "000010" {"mystic" "Fire Ant"
                            "global" "Ant"}
                  "000100" {"global" "Twin Tail"}
                  "000110" {"global" "Fish Snack"
                            "japan" "Maki"}
                  "001010" {"global" "Pupae"}
                  "001100" {"global" "Thorny Caterpillar"}}
          "back" {"001000" {"global" "Sandal"}
                  "000010" {"global" "Snail Shell"
                            "mystic" "Starry Shell"}
                  "000100" {"global" "Garish Worm"
                            "xmas" "Candy Canes"}
                  "000110" {"global" "Buzz Buzz"}
                  "001010" {"global" "Scarab"}
                  "001100" {"global" "Spiky Wing"}}
          "ears" {"000010" {"global" "Larva"
                            "mystic" "Vector"}
                  "000110" {"global" "Ear Breathing"}
                  "000100" {"global" "Beetle Spike"}
                  "001000" {"global" "Leaf Bug"}
                  "001010" {"global" "Tassels"}
                  "001100" {"japan" "Mon"
                            "global" "Earwing"}}
          "eyes" {"000010" {"global" "Bookworm"
                            "mystic" "Broken Bookworm"}
                  "000100" {"global" "Neo"}
                  "001010" {"global" "Kotaro?"}
                  "001000" {"global" "Nerdy"}}}
   "aquatic" {"eyes" {"001000" {"global" "Gero"}
                      "000010" {"global" "Sleepless"
                                "mystic" "Insomnia"
                                "japan" "Yen"}
                      "000100" {"global" "Clear"}
                      "001010" {"global" "Telescope"}}
              "mouth" {"001000" {"global" "Risky Fish"}
                       "000100" {"global" "Catfish"}
                       "000010" {"global" "Lam"
                                 "mystic" "Lam Handsome"}
                       "001010" {"global" "Piranha"
                                 "japan" "Geisha"}}
              "horn" {"001100" {"global" "Shoal Star"}
                      "000110" {"global" "Clamshell"}
                      "000010" {"global" "Babylonia"
                                "mystic" "Candy Babylonia"}
                      "000100" {"global" "Teal Shell"}
                      "001000" {"global" "Anemone"}
                      "001010" {"global" "Oranda"}}
              "ears" {"000010" {"global" "Nimo"
                                "mystic" "Red Nimo"}
                      "000110" {"global" "Bubblemaker"}
                      "000100" {"global" "Tiny Fan"}
                      "001000" {"global" "Inkling"}
                      "001010" {"global" "Gill"}
                      "001100" {"global" "Seaslug"}}
              "tail" {"000010" {"global" "Koi"
                                "mystic" "Kuro Koi"
                                "japan" "Koinobori"}
                      "000110" {"global" "Tadpole"}
                      "000100" {"global" "Nimo"}
                      "001010" {"global" "Navaga"}
                      "001000" {"global" "Ranchu"}
                      "001100" {"global" "Shrimp"}}
              "back" {"000010" {"global" "Hermit"
                                "mystic" "Crystal Hermit"}
                      "000100" {"global" "Blue Moon"}
                      "000110" {"global" "Goldfish"}
                      "001010" {"global" "Anemone"}
                      "001000" {"global" "Sponge"}
                      "001100" {"global" "Perch"}}}
"bird" {"ears" {"001100" {"japan" "Karimata"
                          "global" "Risky Bird"}
                "000010" {"global" "Pink Cheek"
                          "mystic" "Heart Cheek"}
                "000100" {"global" "Early Bird"}
                "000110" {"global" "Owl"}
                "001010" {"global" "Curly"}
                "001000" {"global" "Peace Maker"}}
        "tail" {"001010" {"japan" "Omatsuri"
                          "global" "Granma's Fan"}
                "000010" {"global" "Swallow"
                          "mystic" "Snowy Swallow"}
                "000100" {"global" "Feather Fan"}
                "000110" {"global" "The Last One"}
                "001000" {"global" "Cloud"}
                "001100" {"global" "Post Fight"}}
        "back" {"000010" {"global" "Balloon"
                          "mystic" "Starry Balloon"}
                "000110" {"global" "Raven"}
                "000100" {"global" "Cupid"
                          "japan" "Origami"}
                "001000" {"global" "Pigeon Post"}
                "001010" {"global" "Kingfisher"}
                "001100" {"global" "Tri Feather"}}
        "horn" {"000110" {"global" "Trump"}
                "000010" {"global" "Eggshell"
                          "mystic" "Golden Shell"}
                "000100" {"global" "Cuckoo"}
                "001000" {"global" "Kestrel"}
                "001010" {"global" "Wing Horn"}
                "001100" {"global" "Feather Spear"
                          "xmas" "Spruce Spear"}}
        "mouth" {"000010" {"global" "Doubletalk"
                           "mystic" "Mr. Doubletalk"}
                 "000100" {"global" "Peace Maker"}
                 "001000" {"global" "Hungry Bird"}
                 "001010" {"global" "Little Owl"}}
        "eyes" {"000010" {"global" "Mavis"
                          "mystic" "Sky Mavis"}
                "000100" {"global" "Lucas"}
                "001010" {"global" "Robin"}
                "001000" {"global" "Little Owl"}}}
"reptile" {"eyes" {"001010" {"japan" "Kabuki"
                             "global" "Topaz"}
                   "000100" {"global" "Tricky"}
                   "000010" {"global" "Gecko"
                             "mystic" "Crimson Gecko"}
                   "001000" {"global" "Scar"
                             "japan" "Dokuganryu"}}
           "mouth" {"001000" {"global" "Razor Bite"}
                    "000100" {"global" "Kotaro"}
                    "000010" {"global" "Toothless Bite"
                              "mystic" "Venom Bite"}
                    "001010" {"global" "Tiny Turtle"
                              "japan" "Dango"}}
           "ears" {"001000" {"global" "Small Frill"}
                   "000110" {"global" "Curved Spine"}
                   "000100" {"global" "Friezard"}
                   "000010" {"global" "Pogona"
                             "mystic" "Deadly Pogona"}
                   "001010" {"global" "Swirl"}
                   "001100" {"global" "Sidebarb"}}
           "back" {"001000" {"global" "Indian Star"}
                   "000010" {"global" "Bone Sail"
                             "mystic" "Rugged Sail"}
                   "000100" {"global" "Tri Spikes"}
                   "000110" {"global" "Green Thorns"}
                   "001010" {"global" "Red Ear"}
                   "001100" {"global" "Croc"}}
           "tail" {"000100" {"global" "Iguana"}
                   "000010" {"global" "Wall Gecko"
                             "mystic" "Escaped Gecko"}
                   "000110" {"global" "Tiny Dino"}
                   "001000" {"global" "Snake Jar"
                             "xmas" "December Surprise"}
                   "001010" {"global" "Gila"}
                   "001100" {"global" "Grass Snake"}}
           "horn" {"000010" {"global" "Unko"
                             "mystic" "Pinku Unko"}
                   "000110" {"global" "Cerastes"}
                   "000100" {"global" "Scaly Spear"}
                   "001010" {"global" "Incisor"}
                   "001000" {"global" "Scaly Spoon"}
                   "001100" {"global" "Bumpy"}}}
"plant" {"tail" {"001000" {"global" "Yam"}
                 "000010" {"global" "Carrot"
                           "mystic" "Namek Carrot"}
                 "000100" {"global" "Cattail"}
                 "000110" {"global" "Hatsune"}
                 "001010" {"global" "Potato Leaf"}
                 "001100" {"global" "Hot Butt"}}
         "mouth" {"000100" {"global" "Zigzag"
                            "xmas" "Rudolph"}
                  "000010" {"global" "Serious"
                            "mystic" "Humorless"}
                  "001000" {"global" "Herbivore"}
                  "001010" {"global" "Silence Whisper"}}
         "eyes" {"000010" {"global" "Papi"
                           "mystic" "Dreamy Papi"}
                 "000100" {"global" "Confused"}
                 "001010" {"global" "Blossom"}
                 "001000" {"global" "Cucumber Slice"}}
         "ears" {"000010" {"global" "Leafy"
                           "mystic" "The Last Leaf"}
                 "000110" {"global" "Rosa"}
                 "000100" {"global" "Clover"}
                 "001000" {"global" "Sakura"
                           "japan" "Maiko"}
                 "001010" {"global" "Hollow"}
                 "001100" {"global" "Lotus"}}
         "back" {"000110" {"global" "Bidens"}
                 "000100" {"global" "Shiitake"
                           "japan" "Yakitori"}
                 "000010" {"global" "Turnip"
                           "mystic" "Pink Turnip"}
                 "001010" {"global" "Mint"}
                 "001000" {"global" "Watering Can"}
                 "001100" {"global" "Pumpkin"}}
         "horn" {"000100" {"global" "Beech"
                           "japan" "Yorishiro"}
                 "000110" {"global" "Rose Bud"}
                 "000010" {"global" "Bamboo Shoot"
                           "mystic" "Golden Bamboo Shoot"}
                 "001010" {"global" "Cactus"}
                 "001000" {"global" "Strawberry Shortcake"}
                 "001100" {"global" "Watermelon"}}}})

(defn get-part-name
  [class part region bin]
  (or
    (-> binary-traits (get class) (get part) (get bin) (get region))
    (-> binary-traits (get class) (get part) (get bin) (get "global"))
    "?"))

(defn group->parts
  [part group region]
  (let [d-class (get class-gene-map (subs group 2 6))
        d-bin (subs group 6 12)
        d (get-part-name d-class part region d-bin)
        r1-class (get class-gene-map (subs group 12 16))
        r1-bin (subs group 16 22)
        r1 (get-part-name r1-class part region r1-bin)
        r2-class (get class-gene-map (subs group 22 26))
        r2-bin (subs group 26 32)
        r2 (get-part-name r2-class part region r2-bin)]
    {:d d
     :r1 r1
     :r2 r2}))

(defn get-traits
  [genes]
  (let [[basic appearance eyes mouth ears horn back tail] (-> genes genes->binary slice-genes)
        class (group->class basic)
        region (group->region basic)]
    {:class class
     :region region
     :eyes (group->parts "eyes" eyes region)
     :mouth (group->parts "mouth" mouth region)
     :ears (group->parts "ears" ears region)
     :horn (group->parts "horn" horn region)
     :back (group->parts "back" back region)
     :tail (group->parts "tail" tail region)}))

(def trait-probs {:d 37.5 :r1 9.375 :r2 3.125})

(defn add-trait-probabilities
  [probs traits]
  (reduce-kv
    (fn [p k v]
      (update p (get traits k) (fnil + 0) (get trait-probs k)))
    probs
    traits))

(defn sum-probs
  [sire-traits matron-traits]
  (->> (-> {}
           (add-trait-probabilities sire-traits)
           (add-trait-probabilities matron-traits))
       (map (fn [[part prob]]
              {:part part :prob prob}))
       (sort-by :prob >)))

(defn calc-breed
  [sire-genes matron-genes]
  (let [sire-traits (get-traits sire-genes)
        matron-traits (get-traits matron-genes)]
    (let [breed (reduce
                  (fn [m part]
                    (assoc m part (sum-probs (get sire-traits part) (get matron-traits part))))
                  {}
                  [:eyes :mouth :ears :horn :back :tail])]
      (assoc breed :class [{:class (:class sire-traits) :prob 50}
                           {:class (:class matron-traits) :prob 50}]))))

(defn combine-probs
  [breed]
  (->> [:eyes :mouth :ears :horn :back :tail :class]
       (map breed)
       (map :prob)
       (map #(/ % 100.))
       (reduce *)
       (* 100)))

(defn combine-breed
  [{:keys [eyes mouth ears horn back tail class]}]
  (for [a eyes
        b mouth
        c ears
        d horn
        e back
        f tail
        g class]
    (let [breed {:eyes a
                 :mouth b
                 :ears c
                 :horn d
                 :back e
                 :tail f
                 :class g}]
      (-> breed
          (assoc :total-prob (combine-probs breed))))))

(defn can-breed?
  [sire matron]
  (not
    (or
      ;; can't breed with yourself
      (= (:id sire) (:id matron))
      ;; can't breed with a parent
      (= (:id sire) (:matron-id matron))
      (= (:id sire) (:sire-id matron))
      (= (:id matron) (:matron-id sire))
      (= (:id matron) (:sire-id sire))
      ;; can breed if either is tagged
      (= 0 (:matron-id sire))
      (= 0 (:matron-id matron))
      ;; can't be siblings
      (= (:matron-id sire) (:matron-id matron))
      (= (:matron-id sire) (:sire-id matron))
      (= (:sire-id sire) (:matron-id matron))
      (= (:sire-id sire) (:sire-id matron)))))

(defn replace-part-names
  [name->body-part breed]
  (assoc
    breed :parts
    (->> [:eyes :ears :mouth :horn :back :tail]
         (reduce
           (fn [parts part-key]
             (conj parts (get-in name->body-part [part-key (get-in breed [part-key :part])])))
           []))))

(defn predict-breed
  [name->body-part sire matron]
  (when (and (not (empty? name->body-part)) (some? sire) (some? matron))
    (->> (calc-breed (:genes sire) (:genes matron))
         combine-breed
         (take 500)
         (map-indexed (fn [i b] (assoc b :id i)))
         (map (partial replace-part-names name->body-part))
         (map (fn [b]
                (assoc b
                       :cls (:class b)
                       :class (:class (:class b)))))
         (map (fn [b]
                (assoc b :stats
                       (merge-with +
                                   (base-stats (:class b))
                                   (parts->stats (:parts b))))))
         (map attach-tank-body)
         (map attach-dps-body)
         (map attach-support-body))))

(defn calc-weighted-avgs
  [prob-key ks all-vals]
  (let [prob-mult (->> all-vals (map prob-key) (reduce + 0) (/ 1.0))]
    (->> ks
         (map (fn [k]
                [k (->> all-vals
                        (mapcat (juxt k prob-key))
                        (apply weighted-average)
                        (* prob-mult))]))
         (into {}))))

(defn predict-score
  [prediction]
  (calc-weighted-avgs
    :total-prob
    [:tank-body :dps-body :support-body]
    prediction))
