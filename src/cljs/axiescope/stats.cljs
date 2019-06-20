(ns axiescope.stats)

(defn in-range
  [[bottom top] value]
  (<= bottom value top))

(defn hp-score
  [hp]
  (condp in-range hp
    [58 61] 5
    [50 57] 4
    [43 49] 3
    [35 42] 2
    [27 34] 1
    0))

(defn morale-score
  [morale]
  (condp in-range morale
    [58 61] 5
    [50 57] 4
    [43 49] 3
    [35 42] 2
    [27 34] 1
    0))

(defn speed-score
  [speed]
  (condp in-range speed
    [58 61] 5
    [51 57] 4
    [46 50] 3
    [39 45] 2
    [31 38] 1
    0))

(defn skill-score
  [skill]
  (case skill
    43 5
    39 4
    35 3
    31 2
    27 1
    0))

(defn defense-score
  [defense]
  (condp in-range defense
    [17 24] 5
    [13 16] 4
    [9 12] 3
    [1 8] 2
    [0 0] 1
    0))

;;define('ATK_RANGES',[
;;    '5' => range(28, 31),
;;    '4' => range(21, 27),
;;    '3' => range(17, 20),
;;    '2' => range(10, 16),
;;    '1' => range(0, 9)
;;]);
;;
;;define('DEF_RANGES',[
;;    '5' => range(17, 24),
;;    '4' => range(13, 16),
;;    '3' => range(9, 12),
;;    '2' => range(1, 8),
;;    '1' => range(0, 0)
;;]);
;;define('ACC_RANGES',[
;;    '5' => range(90, 100),
;;    '4' => range(82, 89),
;;    '3' => range(78, 81),
;;    '2' => range(70, 77),
;;    '1' => range(60, 69)
;;]);
