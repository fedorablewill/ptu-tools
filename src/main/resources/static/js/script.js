const TYPES = [
    "Normal",
    "Fire",
    "Water",
    "Grass",
    "Electric",
    "Ice",
    "Fighting",
    "Poison",
    "Ground",
    "Flying",
    "Psychic",
    "Bug",
    "Rock",
    "Ghost",
    "Dark",
    "Dragon",
    "Steel",
    "Fairy"
]

const HABITATS = [
    "Arctic",
    "Beach",
    "Cave",
    "Desert",
    "Forest",
    "Freshwater",
    "Grassland",
    "Marsh",
    "Mountain",
    "Ocean",
    "Rainforest",
    "Taiga",
    "Tundra",
    "Urban"
]

const REGIONS = [
    "Kanto",
    "Johto",
    "Hoenn",
    "Sinnoh",
    "Unova",
    "Kalos",
    "Alola",
    "Galar"
]

const DB = {
    1: {
        diceCount: 1,
        dice: 6,
        modifier: 1
    },
    2: {
        diceCount: 1,
        dice: 6,
        modifier: 3
    },
    3: {
        diceCount: 1,
        dice: 6,
        modifier: 5
    },
    4: {
        diceCount: 1,
        dice: 8,
        modifier: 6
    },
    5: {
        diceCount: 1,
        dice: 8,
        modifier: 8
    },
    6: {
        diceCount: 2,
        dice: 6,
        modifier: 8
    },
    7: {
        diceCount: 2,
        dice: 6,
        modifier: 10
    },
    8: {
        diceCount: 2,
        dice: 8,
        modifier: 10
    },
    9: {
        diceCount: 2,
        dice: 10,
        modifier: 10
    },
    10: {
        diceCount: 3,
        dice: 8,
        modifier: 10
    },
    11: {
        diceCount: 3,
        dice: 10,
        modifier: 10
    },
    12: {
        diceCount: 3,
        dice: 12,
        modifier: 10
    },
    13: {
        diceCount: 4,
        dice: 10,
        modifier: 10
    },
    14: {
        diceCount: 4,
        dice: 10,
        modifier: 15
    },
    15: {
        diceCount: 4,
        dice: 10,
        modifier: 20
    },
    16: {
        diceCount: 5,
        dice: 10,
        modifier: 20
    },
    17: {
        diceCount: 5,
        dice: 12,
        modifier: 25
    },
    18: {
        diceCount: 6,
        dice: 12,
        modifier: 25
    },
    19: {
        diceCount: 6,
        dice: 12,
        modifier: 30
    },
    20: {
        diceCount: 6,
        dice: 12,
        modifier: 35
    },
    21: {
        diceCount: 6,
        dice: 12,
        modifier: 40
    },
    22: {
        diceCount: 6,
        dice: 12,
        modifier: 45
    },
    23: {
        diceCount: 6,
        dice: 12,
        modifier: 50
    },
    24: {
        diceCount: 6,
        dice: 12,
        modifier: 55
    },
    25: {
        diceCount: 6,
        dice: 12,
        modifier: 60
    },
    26: {
        diceCount: 7,
        dice: 12,
        modifier: 65
    },
    27: {
        diceCount: 8,
        dice: 12,
        modifier: 70
    },
    28: {
        diceCount: 8,
        dice: 12,
        modifier: 80
    },
}


//
// INITIALIZE WIDGETS
//

$(function () {
    $('.collapse').collapse()
    $('[data-toggle="tooltip"]').tooltip()
})



//
// UPDATE SUBSCRIBED FIELDS
//

$(function () {
    $('[data-subscribe]').each(function () {
        let elem = $(this)
        subscribe(this, elem.attr("data-subscribe").split(','), window[elem.attr("data-subscribe-callback")])
    })
})

function subscribe(elem, targetIds, callback) {
    targetIds.forEach(targetId => $('#' + targetId).change(function () {callback(elem); $(this).change();}))
    callback(elem)
}