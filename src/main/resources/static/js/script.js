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

// INITIALIZE WIDGETS

$(function () {
    $('.collapse').collapse()
    $('[data-toggle="tooltip"]').tooltip()

    $('[data-autocomplete="type"]').autocomplete({
        source: TYPES
    })
})