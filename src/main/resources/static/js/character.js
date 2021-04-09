/**
 * Javascript for Character Sheet pages (i.e. Pokemon)
 */

// Initialize
$(function () {
    initializeWidgets()
    initialize()
})

function initialize() {
    $('[data-tagcomplete="type"]').tagComplete({
        autocomplete: {
            data: TYPES
        },
        freeInput: true,
        freeEdit: true
    })

    $('[data-autocomplete="type"]').autocomplete({
        source: TYPES
    })

    $(".form-move [data-autocomplete=\"type\"]").each(function() {changeMoveTypeColor(this)})
}

//
// UI Builders & Templates
//


function buildTypeEffectivity() {
    let types = $("#char-types").val()

    if (!validateTypes(types.split(","), "Cannot calculate effectivity for type: ")) {
        return false
    }

    $.ajax("/typeEffectivity", {
        method: "GET",
        contentType: "application/json",
        data: { "types": types }
    }).done(function(response) {
        let modalBody = $("#typeEffectModalBody").empty()
        var current = -1
        var currentGroup = null

        for (const [type, effect] of Object.entries(response)) {
            if (effect !== current) {
                current = effect
                currentGroup = $('<div class="mb-4"></div>')
                currentGroup.append(`<h4>Damage x${effect}</h4><hr/>`)
                modalBody.append(currentGroup)
            }
            currentGroup.append(`<span class="badge bg-t-${type.toLowerCase()}">${type}</span>&nbsp;`)
        }
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting Type Effectivity: " + textStatus + " : " + errorThrown)
    })

    return true
}

function buildDBTooltip(inputElem) {
    inputElem = $(inputElem)
    let moveId = inputElem.closest(".form-move").attr("id")
    let db = $('#' + moveId + "-db").val()
    let atk = $('#' + moveId + "-class").val() === "Special" ? $('#char-stat-satk-total').val() : $('#char-stat-atk-total').val()
    var text = ""

    if (!db) {
        text = "No DB Entered"
    } else if (!DB.hasOwnProperty(db)) {
        text = "Invalid DB"
    } else {
        text = `${DB[db].diceCount}<img src=/img/icons/dice-d${DB[db].dice}-outline-white.png alt=&quot;d${DB[db].dice}&quot; /> + ${DB[db].modifier} (+ ${atk})`
    }

    $('#' + moveId + "-db-tooltip").attr("data-original-title", text).tooltip()
}

function changeMoveTypeColor(elem) {
    elem = $(elem)
    let type = elem.val()

    if (TYPES.includes(type)) {
        elem.closest(".form-move").attr("class", "form-table form-move col-sm-12 mt-3 border-t-" + type.toLowerCase())
    } else {
        elem.closest(".form-move").attr("class", "form-table form-move col-sm-12 mt-3")
    }
}

//
// Actions & Handlers
//

// Change label of "Show More" and "Show Less" on toggle
$('.move-collapse').on('hide.bs.collapse', function () {
    $("button[aria-controls='" + $(this).attr("id") + "']").html("Show More")
}).on('show.bs.collapse', function () {
    $("button[aria-controls='" + $(this).attr("id") + "']").html("Show Less")
})

// Submit handler for Quick Damage Modal
function onSubmitDamage() {
    let types = $("#char-types").val()
    let dmgClass = $('#quickDamage-class').val()
    let def = dmgClass === "Special" ? parseInt($('#char-stat-sdef-total').val()) : parseInt($('#char-stat-def-total').val())
    let dmgType = $('#quickDamage-type').val()
    let dmgAmt = parseInt($('#quickDamage-damage').val())

    if (dmgClass === "True") {
        takeDamage(dmgAmt)
        return
    } else if (isNaN(def)) {
        alert("Pokemon's defense value is not a number. Please enter defense under Stats and try again.")
        return false
    }

    if (dmgType === "Typeless") {
        takeDamage(dmgAmt - def)
    }

    if (!validateTypes(types.split(","), "Cannot calculate damage for Pokemon's type: ")) {
        return false
    }

    $.ajax("/calculateDamage", {
        method: "GET",
        contentType: "application/json",
        data: {
            targetTypes: types,
            targetDefense: def,
            attackType: dmgType,
            attackAmount: dmgAmt
        }
    }).done(function(response) {
        takeDamage(response)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error calculating damage: " + textStatus + " : " + errorThrown)
    })
}

// Subtract from Temporary Hit Points & Current Health using Damage Reduction
function takeDamage(amount) {
    let dr = parseInt($('#char-dr').val())
    let thpElem = $('#char-hp-temp');

    // Damage Reduction
    if (!isNaN(dr)) {
        amount -= dr

        if (amount < 1) {
            amount = 1
        }
    }

    // Temporary Hit Points
    let tempHp = parseInt(thpElem.val())

    if (!isNaN(tempHp) && tempHp > 0) {
        if (tempHp - amount > 0) {
            thpElem.val(tempHp - amount)
            return
        } else {
            amount -= tempHp
            thpElem.val("")
        }
    }

    // Reduce Current Health
    let healthElem = $('#char-hp-current')
    let health = parseInt(healthElem.val())

    if (isNaN(health)) {
        health = 0
    }

    healthElem.val(health - amount)
}

function onChangeMoveContest(elem) {
    elem = $(elem)
    let contestVals = elem.val().split(/ ?\/ ?/)

    elem.parent().find("input[name$='contestType']").val(contestVals[0])
    elem.parent().find("input[name$='contestEffect']").val(contestVals[1])
}

function onClickAddMove() {
    let rowsElem = $("#moves-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-move").last().attr("id").replace("move-", "")) + 1

    $.ajax("/pokemon/move", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index
        }
    }).done(function(response) {
        let newElem = $(`<div class="form-table form-move col-sm-12 mt-3" id="move-${index}"></div>`).html(response)
        rowsElem.append(newElem)

        newElem.find('[data-autocomplete="type"]').autocomplete({
            source: TYPES
        })
        newElem.find('.move-collapse').on('hide.bs.collapse', function () {
            $("button[aria-controls='" + $(this).attr("id") + "']").html("Show More")
        }).on('show.bs.collapse', function () {
            $("button[aria-controls='" + $(this).attr("id") + "']").html("Show Less")
        })
        $("button[aria-controls='move-" + index + "-collapse']").html("Show Less")
        newElem.find('[data-toggle="tooltip"]').tooltip()
        newElem.find('[data-subscribe]').each(loadSubscriber)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting move template: " + textStatus + " : " + errorThrown)
    })
}

function onClickDeleteMove(elem) {
    if (confirm("Are you sure you want to delete this move?")) {
        $(elem).closest(".form-move").remove()

        if ($("#moves-list").children().length === 0) {
            onClickAddMove()
        }
    }
}

function onClickAddAbility() {
    let rowsElem = $("#abilities-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-ability").last().attr("id").replace("ability-", "")) + 1

    $.ajax("/pokemon/ability", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index
        }
    }).done(function(response) {
        let newElem = $(`<div class="form-table form-ability col-sm-12 mt-3" id="ability-${index}"></div>`).html(response)
        rowsElem.append(newElem)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting ability template: " + textStatus + " : " + errorThrown)
    })
}

function onClickDeleteAbility(elem) {
    if (confirm("Are you sure you want to delete this ability?")) {
        $(elem).closest(".form-ability").remove()

        if ($("#abilities-list").children().length === 0) {
            onClickAddAbility()
        }
    }
}