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

(defn attack-score
  [attack]
  (condp in-range attack
    [28 31] 5
    [21 27] 4
    [17 20] 3
    [10 16] 2
    [0 9] 1
    0))

(defn accuracy-score
  [accuracy]
  (condp in-range accuracy
    [90 100] 5
    [82 89] 4
    [78 81] 3
    [70 77] 2
    [60 69] 1
    0))
