(ns axiescope.moves)

(defn tank-move-score
  [move-name]
  (case move-name
    "Hot Butt" 4
    "Tiny Dino" 4
    "Pumpkin" 4
    "Red Ear" 4
    "Rose Bud" 4
    "Beech" 4
    "Zigzag" 4
    "Herbivore" 4
    "Carrot" 3
    "Cattail" 3
    "Thorny Caterpillar" 3
    "Hermit" 3
    "Sponge" 3
    "1ND14N-5T4R" 3
    "Snail Shell" 3
    "Shiitake" 3
    "Cerastes" 3
    "Cactus" 3
    "Leaf Bug" 3
    "Serious" 3
    "Silence Whisper" 3
    "Hatsune" 2
    "Bidens" 2
    "Bamboo Shoot" 2
    "Razor Bite" 2
    "Ant" 2
    "Mint" 2
    "Merry" 2
    "Toothless Bite" 2
    "Snake Jar" 2
    "Timber" 2
    "Incisor" 2
    "Tiny Turtle" 2
    "Fish Snack" 2
    "Watermelon" 2
    "Feather Fan" 2
    "Potato Leaf" 2
    "Wall Gecko" 2
    "Gila" 2
    "Grass Snake" 2
    "Koi" 1
    "Bone Sail" 1
    "Lagging" 1
    "Lam" 1
    "Pupae" 1
    "Turnip" 1
    "Scaly Spear" 1
    "Pincer" 1
    "Shrimp" 1
    "Anemone" 1
    "Teal Shell" 1
    "Piranha" 1
    "Croc" 1
    "Babylonia" 1
    "Risky Fish" 1
    "Navaga" 1
    "Green Thorns" 1
    "Pliers" 1
    "Nimo" 1
    "Watering Can" 1
    "Scaly Spoon" 1
    0))

(defn dps-move-score
  [move-name]
  (case move-name
    "The Last One" 4
    "Tri Feather" 4
    "Eggshell" 4
    "Little Owl" 4
    "Post Flight" 4
    "Kingfisher" 4
    "Cactus" 4
    "Nut Cracker" 4
    "Hare" 4
    "Ronin" 4
    "Feather Spear" 4
    "Doubletalk" 4
    "Little Branch" 4
    "Imp" 4
    "Swallow" 3
    "Raven" 3
    "Trump" 3
    "Peace Maker" 3
    "Shiba" 3
    "Cupid" 3
    "Wing Horn" 3
    "Hungry Bird" 3
    "Gerbil" 3
    "Scarab" 3
    "Cuckoo" 3
    "Axie Kiss" 3
    "Furball" 3
    "Shoal Star" 3
    "Cute Bunny" 3
    "Cloud" 3
    "Risky Beast" 3
    "Scaly Spear" 3
    "Tiny Turtle" 3
    "Navaga" 3
    "Jaguar" 3
    "Cerastes" 3
    "Razor Bite" 3
    "Iguana" 3
    "Hero" 3
    "Arco" 3
    "Toothless Bite" 3
    "Shrimp" 3
    "Granma's Fan" 2
    "Pigeon Post" 2
    "Pocky" 2
    "Goda" 2
    "Tadpole" 2
    "Bone Sail" 2
    "Pliers" 2
    "Kotaro" 2
    "Cottontail" 2
    "Perch" 2
    "Dual Blade" 2
    "Piranha" 2
    "Nimo" 2
    "Balloon" 2
    "Lagging" 2
    "Risky Fish" 2
    "Ranchu" 2
    "Blue Moon" 2
    "Parasite" 2
    "Rice" 2
    "Goldfish" 2
    "Kestrel" 2
    "Grass Snake" 2
    "Tri Spikes" 2
    "Bumpy" 2
    "Spiky Wing" 2
    "Babylonia" 2
    "Beech" 2
    "Snake Jar" 1
    "Anemone" 1
    "Teal Shell" 1
    "Catfish" 1
    "Koi" 1
    "Croc" 1
    "Merry" 1
    "Lam" 1
    "Hot Butt" 1
    "Garish Worm" 1
    "Clamshell" 1
    "Herbivore" 1
    "Green Thorns" 1
    "Serious" 1
    "Scaly Spoon" 1
    "Square Teeth" 1
    "Incisor" 1
    "Oranda" 1
    "Unko" 1
    "Watermelon" 1
    "Antenna" 1
    "Caterpillars" 1
    0))
