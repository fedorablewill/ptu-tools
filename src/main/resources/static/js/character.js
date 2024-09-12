/**
 * Javascript for Character Sheet pages (i.e. Pokemon)
 */

// Initialize
$(function () {
    if ($("#form").length > 0) {
        initialize()
        initializeWidgets()
    }
})

function initialize() {
    let afflictions = AFFLICTIONS_VOLATILE.concat(AFFLICTIONS_PERSISTENT).concat(AFFLICTIONS_OTHER)
    afflictions.sort()

    var elem = $("#char-afflict")
    var elemVal = elem.val()
    elem.attr("value", null).magicSuggest({
        data: afflictions,
        value: elemVal ? elemVal.split(/, ?/) : null,
        placeholder: null
    })

    elem = $('#char-types')
    elemVal = elem.val() || ""
    elem.attr("value", null).magicSuggest({
        data: TYPES,
        value: elemVal ? elemVal.split(/, ?/) : null,
        expandOnFocus: true
    })

    $("#char-species").autocomplete({
        source: "/speciesOptions",
        minLength: 2,
        appendTo: "#char-species-container",
        select: function( event, ui ) {
            $("#char-pokedexId").val(ui.item.value);
            if (ui.item.value) {
                $.ajax("/pokedex/" + ui.item.value, {
                    method: "GET",
                    contentType: "application/json"
                }).done(function (pokedexEntries) {
                    if (confirm("Would you like to evolve into " + ui.item.label + "?")) {
                        let pokedexEntry = pokedexEntries[0]

                        applyNewAbilities(pokedexEntry["abilityLearnset"])

                        $("[name*='pokedexEntry.']").each(function () {
                            var propName = $(this).attr("name").substring(13)
                            var val = null
                            if (propName.includes("['")) {
                                var childProps = propName.match(/\['(.*?)'\]/);
                                propName = propName.substring(0, propName.indexOf("['"))
                                val = pokedexEntry[propName][childProps[1]]
                            } else {
                                val = pokedexEntry[propName]
                            }

                            if ($(this).is("[type='checkbox']")) {
                                $(this).prop("checked", val)
                            } else {
                                $(this).val(val)
                            }
                        })

                        moveLearnset = pokedexEntry["moveLearnset"]
                        buildMoveLearnset()
                        buildAbilityLearnset(pokedexEntry["abilityLearnset"])
                        checkEvolutionMoves()

                        // Check if Image exists
                        $.get('/img/pokemon/' + pokedexEntry["imageFileUrl"])
                            .done(function () {
                                $("#char-img").attr("src", '/img/pokemon/' + pokedexEntry["imageFileUrl"])
                            }).fail(function () {
                                $("#char-img").attr("src", '/img/exodus-ptu-icon.png')
                            })

                        // Update Types
                        let typesMs = $("#char-types").magicSuggest()
                        typesMs.clear()
                        typesMs.setValue(pokedexEntry.types)
                        typesMs.collapse()

                        // Base Stats
                        $("#char-stat-hp-base").val(pokedexEntry.baseStats.hp)
                        $("#char-stat-atk-base").val(pokedexEntry.baseStats.atk)
                        $("#char-stat-def-base").val(pokedexEntry.baseStats.def)
                        $("#char-stat-spatk-base").val(pokedexEntry.baseStats.spatk)
                        $("#char-stat-spdef-base").val(pokedexEntry.baseStats.spdef)
                        $("#char-stat-spd-base").val(pokedexEntry.baseStats.spd)
                        onChangeNature($("#char-nature").attr('data-prev', null))
                    }
                }).fail(function (jqxhr, textStatus, errorThrown) {
                    alert("Error getting Pokedex Entry: " + textStatus + " : " + errorThrown)
                })
            }
        }
    });

    $('[data-autocomplete="type"]').autocomplete({
        source: TYPES
    })

    $(".form-move [data-autocomplete=\"type\"]").each(function() {changeMoveTypeColor(this)})

    $(".navbar-nav a").click(function () {
        $("#navbarNav").collapse('hide')
    })

    $("#char-level").change(onChangeLevel)

    $("#navbarNav").collapse('hide')

    $('#form').data('saved-state', $('#form').serialize()).submit(onFormSubmit);

    $(window).bind('beforeunload', function(e){
        let form = $('#form');
        // If page is not dirty, don't give warning
        if(form.length === 0 || form.serialize() === form.data('saved-state')){
            return undefined;
        }

        var confirmationMessage = 'You have unsaved changes. Are you sure you want to leave and discard them?';

        (e || window.event).returnValue = confirmationMessage; //Gecko + IE
        return confirmationMessage; //Gecko + Webkit, Safari, Chrome etc.
    });

    window.quills = []
    $(".quill-editor").each(function (i) {
        buildNotesQuillEditor(this, i);
    });

    $("#abilityLookupModal-learnable").find('.collapse').collapse('hide')

    buildPageTitle();
    buildMoveLearnset();
}

//
// UI Builders & Templates
//


function buildPageTitle() {
    let name = $('#char-name').val()
    if (name) {
        document.title = name + " - PokéSheets Character Sheet"
    } else {
        document.title = "New Pokemon - PokéSheets Character Sheet"
    }
}

function buildTypeEffectivity() {
    let types = $("#char-types").magicSuggest().getValue()

    if (!validateTypes(types, "Cannot calculate effectivity for type: ")) {
        return false
    }

    $.ajax("/typeEffectivity", {
        method: "GET",
        contentType: "application/json",
        data: { "types": types.join() }
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
    let atk = $('#' + moveId + "-class").val() === "Special" ? $('#char-stat-spatk-total').val() : $('#char-stat-atk-total').val()
    var text = ""
    var formula = ""

    if (!db) {
        text = "No DB Entered"
    } else if (!DB.hasOwnProperty(db)) {
        text = "Invalid DB"
    } else {
        text = `${DB[db].diceCount}<img src=/img/icons/dice-d${DB[db].dice}-outline-white.png alt=&quot;d${DB[db].dice}&quot; /> + ${DB[db].modifier} (+ ${atk})` +
            `<br/><small class="text-muted">CLICK TO COPY</small>`
        formula = `${DB[db].diceCount}d${DB[db].dice}+${DB[db].modifier}+${atk}`
    }

    $('#' + moveId + "-db-tooltip").attr("data-original-title", text).attr("data-db-formula", formula).tooltip()
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

function buildCaptureRate(elem) {
    let level = parseInt($('#char-level').val())
    let hpCurrent = parseInt($('#char-hp-current').val())
    let hpMax = parseInt($('#char-hp-max').val())
    let injuries = parseInt($('#char-injuries').val())
    let afflictions = $('#char-afflict').magicSuggest().getValue()
    let evStageRemain = parseInt($('#char-evolution-remaining').val())
    let isShiny = $('#char-shiny').is(':checked')
    let isLegendary = $('#char-legendary').is(':checked')

    var captureRate = 100

    if (level) {
        captureRate -= level * 2
    }

    if (hpCurrent && hpMax && hpMax > 0) {
        let hpPcnt = hpCurrent / hpMax
        if (hpCurrent === 1) {
            captureRate += 30
        } else if (hpPcnt > 0.75) {
            captureRate -= 30
        } else if (hpPcnt > 0.5) {
            captureRate -= 15
        } else if (hpPcnt < 0.25) {
            captureRate += 15
        }
    }

    if (evStageRemain != NaN) {
        if (evStageRemain >= 2) {
            captureRate += 10
        } else if (evStageRemain === 0) {
            captureRate -= 10
        }
    } else {
        captureRate -= 10
    }

    if (isShiny) {
        captureRate -= 10
    }

    if (isLegendary) {
        captureRate -= 30
    }

    if (afflictions) {
        for (var affliction of afflictions) {
            if ("Stuck" === affliction) {
                captureRate += 10
            } else if ("Slowed" === affliction) {
                captureRate += 5
            } else if (AFFLICTIONS_PERSISTENT.includes(affliction)) {
                captureRate += 10
            } else if (AFFLICTIONS_VOLATILE.includes(affliction)) {
                captureRate += 5
            }
        }
    }

    if (injuries) {
        captureRate += 5 * injuries
    }

    $(elem).val(captureRate)
}

function buildMaxHealth(elem) {
    if ($('#char-species').val() === "Shedinja") {
        $(elem).val(1)
    } else {
        buildValueFromSubscribedFields(elem)
    }
}

function buildNotesQuillEditor(elem, i) {
    let value = elem.innerHTML;
    elem.innerHTML = "";
    var quill = new Quill(elem, {
        theme: 'bubble',
        placeholder: 'Type something here... Select text to format...',
        modules: {
            toolbar: [
                [{ 'header': 1 }, { 'header': 2 }],               // custom button values
                ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
                ['blockquote', 'code-block'],
                [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                [{ 'script': 'sub'}, { 'script': 'super' }],      // superscript/subscript
                [{ 'indent': '-1'}, { 'indent': '+1' }],          // outdent/indent
                [{ 'direction': 'rtl' }],                         // text direction
                [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
                [{ 'align': [] }],
                ['clean']                                         // remove formatting button
            ]
        }
    });
    if (value) {
        quill.setContents(JSON.parse(value));
    }
    quill.on('text-change', function(delta, oldDelta, source) {
        $(`[name='notes[${i}].body']`).val(JSON.stringify(quill.getContents()));
    });
    window.quills.push(quill);
}

function buildMoveLearnset() {
    window.moveCache = {}

    if (moveLearnset) {
        $.ajax("/pokemon/moveset", {
            method: "GET",
            contentType: "application/json",
            data: {
                "moveLearnset": JSON.stringify(moveLearnset),
                "stabTypes": $("#char-types").magicSuggest().getValue().join()
            }
        }).done(function (response) {
            $("#moveLookupModal-learnable").html(response).find('.collapse').collapse('hide')
        }).fail(function (jqxhr, textStatus, errorThrown) {
            alert("Error loading move learnset: " + textStatus + " : " + errorThrown)
        })
    } else {
        $("#moveLookupModal-learnable").html("")
    }
}

function buildAbilityLearnset(abilityLearnset) {
    window.moveCache = {}

    if (abilityLearnset) {
        $.ajax("/pokemon/abilityLearnset", {
            method: "GET",
            contentType: "application/json",
            data: {
                "abilityLearnset": JSON.stringify(abilityLearnset)
            }
        }).done(function (response) {
            $("#abilityLookupModal-learnable").html(response).find('.collapse').collapse('hide')
        }).fail(function (jqxhr, textStatus, errorThrown) {
            alert("Error loading ability learnset: " + textStatus + " : " + errorThrown)
        })
    }
}

function checkEvolutionMoves() {
    for (let moveEntry of moveLearnset['levelUpMoves']) {
        if (moveEntry.learnedLevel === -1) {
            let name = $("#char-name").val() || "You"
            buildMoveLearnToast(name, moveEntry.moveName)
        }
    }
}

//
// Actions & Handlers
//

// Submit Form for saving!!!
function onFormSubmit() {
    let hiddenContainer = $("#generatedHiddenFields").empty()
    var i;

    let eggGroups = $("#char-egg-group").val().split(/ ?, ?/)
    for (i = 0; i < eggGroups.length; i++) {
        hiddenContainer.append(`<input type="hidden" name="pokedexEntry.eggGroups[${i}]" value="${eggGroups[i]}" />`)
    }

    hiddenContainer.append($(`<input type="hidden" name="pokedexEntry.moveLearnset" />`).val(JSON.stringify(moveLearnset)))

    let form = $("#form")

    form.data('saved-state', form.serialize());
}

function onClickSaveAsToGoogleDrive() {
    let fileNameElem = $('#gDrive-fileName')
    if (!fileNameElem.val()) {
        fileNameElem.val($('#char-name').val())
    }
    $('#gDrive-folder').val("My Drive")
    $("[name='googleDriveFolderId']").val("")
    $('#gDriveSaveModal').modal('show')
}

function onClickSaveToGoogleDrive() {
    onFormSubmit()
    if ($("[name='googleDriveFileId']").val()) {
        validateOAuthToken(function () {
            $.ajax("/savePokemonToGoogleDrive", {
                method: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: $("#form").serialize()
            }).done(function(response) {
                buildToast("Saved to Google Drive.")
                $('#saveToGDrive').removeClass("d-none")
                $('#saveToGDriveMobile').removeClass("d-none")
            }).fail(function(jqxhr, textStatus, errorThrown)  {
                alert("Error saving Pokemon to Google Drive: " + textStatus + " : " + errorThrown)
            })
        })
        return false
    } else {
        loadFolderPicker()
    }
}

// Save file to Google Drive
function onFolderPicked(folderId, folderName) {
    $("[name='googleDriveFolderId']").val(folderId)
    $("#gDrive-folder").val(folderName)
}

function onClickGDriveModalSave() {
    onFormSubmit()
    $('#fileName').val($('#gDrive-fileName').val())
    validateOAuthToken(function () {
        $.ajax("/uploadPokemonToGoogleDrive", {
            method: "POST",
            contentType: "application/x-www-form-urlencoded",
            data: $("#form").serialize()
        }).done(function (response) {
            $("[name='googleDriveFileId']").val(response);
            window.history.pushState(null, null, "/pokemon/drive/" + response);
            buildToast("Saved to Google Drive.")
            $('#saveToGDrive').removeClass("d-none")
            $('#saveToGDriveMobile').removeClass("d-none")
        }).fail(function (jqxhr, textStatus, errorThrown) {
            alert("Error saving Pokemon to Google Drive: " + textStatus + " : " + errorThrown)
        })
    })
}

// Change label of "Show More" and "Show Less" on toggle
$('.move-collapse').on('hide.bs.collapse', function () {
    $("button[aria-controls='" + $(this).attr("id") + "']").html("Show More")
}).on('show.bs.collapse', function () {
    $("button[aria-controls='" + $(this).attr("id") + "']").html("Show Less")
})

// Submit handler for Quick Damage Modal
function onSubmitDamage() {
    let types = $("#char-types").magicSuggest().getValue()
    let dmgClass = $('#quickDamage-class').val()
    let def = dmgClass === "Special" ? parseInt($('#char-stat-spdef-total').val()) : parseInt($('#char-stat-def-total').val())
    let dmgType = $('#quickDamage-type').val()
    let dmgAmt = parseInt($('#quickDamage-damage').val())

    if (dmgClass === "True") {
        takeDamage(dmgAmt)
        return true
    } else if (isNaN(def)) {
        alert("Pokemon's defense value is not a number. Please enter defense under Stats and try again.")
        return false
    }

    if (dmgType === "Typeless") {
        takeDamage(dmgAmt - def)
        return true
    }

    if (!validateTypes(types, "Cannot calculate damage for Pokemon's type: ")) {
        return false
    }

    $.ajax("/calculateDamage", {
        method: "GET",
        contentType: "application/json",
        data: {
            targetTypes: types.join(),
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
    let totalTaken = amount
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
            thpElem.val(tempHp - amount).change()
            buildToast(`Took ${totalTaken} damage. <a href="#" onclick="healDamage(0, ${amount});$(this).closest('.toast').toast('hide')">UNDO</a>`,
                false, "damage")
            return
        } else {
            amount -= tempHp
            thpElem.val(0).change()
        }
    }

    // Reduce Current Health
    let healthElem = $('#char-hp-current')
    let health = parseInt(healthElem.val())

    if (isNaN(health)) {
        health = 0
    }

    healthElem.val(health - amount).change()

    buildToast(`Took ${totalTaken} damage. <a href="#" onclick="healDamage(${amount}, ${tempHp});$(this).closest('.toast').toast('hide')">UNDO</a>`,
        false, "damage")
}

function healDamage(amountToHealth, amountToTemp) {
    if (amountToTemp) {
        let thpElem = $('#char-hp-temp');
        let tempHp = parseInt(thpElem.val())
        if (isNaN(tempHp)) {
            tempHp = 0
        }
        thpElem.val(tempHp + amountToTemp).change()
    }

    if (amountToHealth) {
        let healthElem = $('#char-hp-current')
        let health = parseInt(healthElem.val())
        if (isNaN(health)) {
            health = 0
        }
        healthElem.val(health + amountToHealth).change()
    }

    buildToast(`Healed ${amountToHealth + amountToTemp} damage.`, 10000)
}

function onChangeNature(elem) {
    elem = $(elem)
    let prevVal = elem.attr("data-prev")
    let val = elem.val()
    let dexId = $('#char-pokedexId').val()

    elem.attr("data-prev", val)

    if (prevVal === val) return

    // Since we can't tell if a lowered base stat was originally 1 or 2, look up the stats
    if (dexId && prevVal) {
        $.ajax("/pokedex/" + dexId, {
            method: "GET",
            contentType: "application/json"
        }).done(function (response) {
            if (response[0]) {
                // Reset old values
                $(`#char-stat-${NATURES[prevVal].up}-base`).val(response[0]['baseStats'][NATURES[prevVal].up]).change()
                $(`#char-stat-${NATURES[prevVal].down}-base`).val(response[0]['baseStats'][NATURES[prevVal].down]).change()

                // Set new values
                applyNewNature(val)
            } else {
                adjustNatureWithoutDex(prevVal, val)
            }
        }).fail(function (jqxhr, textStatus, errorThrown) {
            adjustNatureWithoutDex(prevVal, val)
        })
    } else {
        adjustNatureWithoutDex(prevVal, val)
    }

}

function adjustNatureWithoutDex(prevVal, val) {
    if (prevVal) {
        let statElem = $(`#char-stat-${NATURES[prevVal].up}-base`)
        let amt = NATURES[prevVal].up === "hp" ? 1 : 2
        if (statElem.val()) {
            statElem.val(Math.max(parseInt(statElem.val()) - amt, 1))
            statElem.change()
        }
        statElem = $(`#char-stat-${NATURES[prevVal].down}-base`)
        amt = NATURES[prevVal].down === "hp" ? 1 : 2
        if (statElem.val()) {
            statElem.val(parseInt(statElem.val()) + amt)
            statElem.change()
        }
    }
    applyNewNature(val)
}

function applyNewNature(val) {
    let statElem = $(`#char-stat-${NATURES[val].up}-base`)
    let amt = NATURES[val].up === "hp" ? 1 : 2
    if (statElem.val()) {
        statElem.val(Math.max(parseInt(statElem.val()) + amt, 1))
        statElem.change()
    } else {
        statElem.val(amt)
        statElem.change()
    }
    statElem = $(`#char-stat-${NATURES[val].down}-base`)
    amt = NATURES[val].down === "hp" ? 1 : 2
    if (statElem.val()) {
        statElem.val(parseInt(statElem.val()) - amt)
        if (parseInt(statElem.val()) < 1) {
            statElem.val(1)
        }
        statElem.change()
    } else {
        statElem.val(1)
        statElem.change()
    }
}

function applyNewAbilities(abilityLearnset) {
    var basicList = {}
    var advList = {}
    var highList = {}

    $("#accordionAbilities-basicAbilities input[id*='-name']").each(function () {
        if (this.value) {
            basicList[this.value.toLowerCase()] = parseInt($(this).closest(".move-item").attr("data-index"))
        }
    })
    $("#accordionAbilities-advancedAbilities input[id*='-name']").each(function () {
        if (this.value) {
            advList[this.value.toLowerCase()] = parseInt($(this).closest(".move-item").attr("data-index"))
        }
    })
    $("#accordionAbilities-highAbilities input[id*='-name']").each(function () {
        if (this.value) {
            highList[this.value.toLowerCase()] = parseInt($(this).closest(".move-item").attr("data-index"))
        }
    })

    $("#abilities-list input[id*='-name']").each(function () {
        let val = this.value.trim().toLowerCase()

        if (basicList[val] !== undefined) {
            swapAbility(this, abilityLearnset.basicAbilities[basicList[val]])
        } else if (advList[val] !== undefined) {
            swapAbility(this, abilityLearnset.advancedAbilities[advList[val]])
        } else if (highList[val] !== undefined) {
            swapAbility(this, abilityLearnset.highAbilities[highList[val]])
        }
    });
}

function swapAbility(elem, ability) {
    let parentElem = $(elem).closest(".form-ability")

    parentElem.find("input[id*='-name']").val(ability.name)
    parentElem.find("textarea[id*='-effect']").val(ability.effect)
    parentElem.find("input[id*='-trigger']").val(ability.trigger)
    parentElem.find("input[id*='-target']").val(ability.target)
    parentElem.find("input[id*='-freq']").val(ability.frequency)
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
        onMoveDone(response, index, rowsElem)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting move template: " + textStatus + " : " + errorThrown)
    })
}

function onClickLookupMove() {
    $("#moveLookupModal").on('shown.bs.modal', function () {
        $("#moveLookupModal-query").focus()
    }).modal('show')
}

function addMoveByName(moveName) {
    $.ajax("/move/" + moveName, {
        method: "GET",
        contentType: "application/json"
    }).done(function(moveJson) {
        if (!moveJson) {
            alert("Move not found: " + moveName)
            return
        }
        if ($("#char-types").magicSuggest().getValue().includes(moveJson["type"]) && moveJson["damageBase"]) {
            moveJson["damageBase"] += 2
            moveJson["stab"] = true
        }
        addMoveByMoveJson(moveJson)
    })

}

function addMoveByMoveJson(moveJson) {
    let rowsElem = $("#moves-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-move").last().attr("id").replace("move-", "")) + 1

    moveJson['name'] = moveJson['name'].replace(' (N)', '').replace('§ ', '')

    $.ajax("/pokemon/move", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index,
            "move": JSON.stringify(moveJson)
        }
    }).done(function(response) {
        onMoveDone(response, index, rowsElem)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting move template: " + textStatus + " : " + errorThrown)
    })
}

function onMoveDone(response, index, rowsElem) {
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
    changeMoveTypeColor(newElem.find('[data-autocomplete="type"]'))
}

function onMoveSearch() {
    let query = $("#moveLookupModal-query").val()

    if (query) {
        $.ajax("/pokemon/move/search", {
            method: "GET",
            contentType: "application/json",
            data: {
                "term": query,
                "stabTypes": $("#char-types").magicSuggest().getValue().join()
            }
        }).done(function (response) {
            $("#moveLookupModal-queryResults").show().html(response).find('.collapse').collapse('hide')
            $("#moveLookupModal-learnable").hide()
        }).fail(function (jqxhr, textStatus, errorThrown) {
            alert("Error searching moves: " + textStatus + " : " + errorThrown)
        })
    } else {
        $("#moveLookupModal-queryResults").hide()
        $("#moveLookupModal-learnable").show()
    }
}

function onClickDeleteMove(elem) {
    if (confirm("Are you sure you want to delete this move?")) {
        $(elem).closest(".form-move").remove()

        if ($("#moves-list").children().length === 0) {
            onClickAddMove()
        }
    }
}

function onAbilitySearch() {
    let query = $("#abilityLookupModal-query").val()

    if (query) {
        $.ajax("/pokemon/ability/search", {
            method: "GET",
            contentType: "application/json",
            data: {
                "term": query
            }
        }).done(function (response) {
            $("#abilityLookupModal-queryResults").show().html(response).find('.collapse').collapse('hide')
            $("#abilityLookupModal-learnable").hide()
        }).fail(function (jqxhr, textStatus, errorThrown) {
            alert("Error searching abilities: " + textStatus + " : " + errorThrown)
        })
    } else {
        $("#abilityLookupModal-queryResults").hide()
        $("#abilityLookupModal-learnable").show()
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

function onClickLookupAbility() {
    $("#abilityLookupModal").on('shown.bs.modal', function () {
        $("#abilityLookupModal-query").focus()
    }).modal('show')
}

function addAbilityByName(abilityName) {
    let rowsElem = $("#abilities-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-ability").last().attr("id").replace("ability-", "")) + 1

    $.ajax("/ability/" + abilityName, {
        method: "GET",
        contentType: "application/json"
    }).done(function(abilityJson) {
        if (!abilityJson) {
            alert("Ability not found: " + abilityName)
            return
        }
        $.ajax("/pokemon/ability", {
            method: "GET",
            contentType: "application/json",
            data: {
                "index": index,
                "ability": JSON.stringify(abilityJson)
            }
        }).done(function(response) {
            let newElem = $(`<div class="form-table form-ability col-sm-12 mt-3" id="ability-${index}"></div>`).html(response)
            rowsElem.append(newElem)
        }).fail(function(jqxhr, textStatus, errorThrown)  {
            alert("Error getting ability template: " + textStatus + " : " + errorThrown)
        })
    })

}

function addAbilityByAbilityJson(abilityJson) {
    let rowsElem = $("#abilities-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-ability").last().attr("id").replace("ability-", "")) + 1

    $.ajax("/pokemon/ability", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index,
            "ability": JSON.stringify(abilityJson)
        }
    }).done(function(response) {
        let newElem = $(`<div class="form-table form-ability col-sm-12 mt-3" id="ability-${index}"></div>`).html(response)
        rowsElem.append(newElem)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting move template: " + textStatus + " : " + errorThrown)
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

function onClickAddPokeEdge() {
    let rowsElem = $("#pokeedges-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-pokeedge").last().attr("id").replace("pokeedge-", "")) + 1

    $.ajax("/pokemon/pokeedge", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index
        }
    }).done(function(response) {
        let newElem = $(`<div class="form-table form-pokeedge col-sm-12 mt-3" id="pokeedge-${index}"></div>`).html(response)
        rowsElem.append(newElem)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting PokeEdge template: " + textStatus + " : " + errorThrown)
    })
}

function onClickAddPokeEdgeByName() {
    let name = window.prompt("Enter Poke Edge name")

    if (name) {
        addPokeEdgeByName(name)
    }
}

function addPokeEdgeByName(pokeEdgeName) {
    let rowsElem = $("#pokeedges-list")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".form-pokeedge").last().attr("id").replace("pokeedge-", "")) + 1

    $.ajax("/pokeedge/" + pokeEdgeName, {
        method: "GET",
        contentType: "application/json"
    }).done(function(pokeEdgeJson) {
        if (!pokeEdgeJson) {
            alert("Poke Edge not found: " + pokeEdgeName)
            return
        }
        $.ajax("/pokemon/pokeedge", {
            method: "GET",
            contentType: "application/json",
            data: {
                "index": index,
                "pokeEdge": JSON.stringify(pokeEdgeJson)
            }
        }).done(function(response) {
            let newElem = $(`<div class="form-table form-pokeedge col-sm-12 mt-3" id="pokeedge-${index}"></div>`).html(response)
            rowsElem.append(newElem)
        }).fail(function(jqxhr, textStatus, errorThrown)  {
            alert("Error getting Poke Edge template: " + textStatus + " : " + errorThrown)
        })
    })

}

function onClickDeletePokeEdge(elem) {
    if (confirm("Are you sure you want to delete this Poke Edge?")) {
        $(elem).closest(".form-pokeedge").remove()

        if ($("#pokeedges-list").children().length === 0) {
            onClickAddPokeEdge()
        }
    }
}

function onChangeShiny(elem) {
    if ($(elem).is(':checked')) {
        $('#char-picture-shiny').removeClass("d-none")
    } else {
        $('#char-picture-shiny').addClass("d-none")
    }
}

function onClickDbTooltip(elem) {
    clipboardText($(elem).attr("data-db-formula"))
    buildToast("Copied damage formula to clipboard.")
}

function onClickToggleStab(elem) {
    elem = $(elem)
    let dbElem = elem.closest(".form-move").find("[id$='-db']")
    let db = dbElem.val()

    if (!db) {
        return
    }

    if (elem.is(':checked')) {
        dbElem.val(parseInt(db) + 2).change()
    } else {
        dbElem.val(parseInt(db) - 2).change()
    }
}

function onChangeExp(elem) {
    let exp = parseInt($(elem).val())
    let level = parseInt($("#char-level").val()) || 0

    if (exp) {
        $.ajax("/levelUpPokemon", {
            method: "GET",
            contentType: "application/json",
            data: {
                "pokedexEntryDocumentId": $("[name='pokedexEntry.pokedexEntryDocumentId']").val(),
                "currentLevel": level,
                "exp": exp
            }
        }).done(function(response) {
            if (level !== response["level"]) {
                let name = $("#char-name").val() || "You"
                if (level < response["level"]) {
                    buildToast(name + " Leveled up!")
                }
                $("#char-level").val(response["level"]).change()

                if (response["moves"].length > 0) {
                    for (var move of response["moves"]) {
                        buildMoveLearnToast(name, move['name'])
                    }
                }
            }
        }).fail(function(jqxhr, textStatus, errorThrown)  {
            alert("Error checking for level up: " + textStatus + " : " + errorThrown)
        })
    }
}

function buildMoveLearnToast(name, moveName) {
    buildToast(`${name} wants to learn <strong>${moveName}</strong>!<br/>` +
        `<button type="button" class="btn btn-sm btn-outline-white" onclick="learnToastMove('${moveName}', this)">Learn</button> ` +
        `<button type="button" class="btn btn-sm btn-outline-white" data-dismiss="toast" aria-label="Close">Don't Learn</button>`, false)
}

function onChangeLevel() {
    //Add Tutor Points
    var tpRemainingElem = $("#char-tp-remaining")
    var tpMaxElem = $("#char-tp-max")
    var tpMaxCurrent = parseInt(tpMaxElem.text().substring(2))
    var tpMaxNew = Math.floor($("#char-level").val() / 5) + 1

    if (!isNaN(tpMaxCurrent) && tpMaxNew !== tpMaxCurrent) {
        var tpDiff = tpMaxNew - tpMaxCurrent
        var tpRemain = parseInt(tpRemainingElem.val()) + tpDiff
        tpRemainingElem.val(tpRemain)
    }
}

function learnToastMove(moveName, elem) {
    let parent = $(elem).parent()
    addMoveByName(moveName)
    parent.html("1, 2 and...")
    setTimeout(function () {
        parent.html("1, 2 and... Poof!")
        setTimeout(function () {
            let name = $("#char-name").val() || "You"
            parent.html(name + " learned " + moveName + "!")
            setTimeout(function () {
                parent.closest(".toast").toast('hide')
            }, 2000)
        }, 1000)
    }, 1000)
}

function onClickAddNote() {
    let rowsElem = $("#accordionNotes")
    let index = rowsElem.children().length === 0 ? 0 : parseInt(rowsElem.find(".note-item").last().attr("data-index")) + 1

    $.ajax("/pokemon/note", {
        method: "GET",
        contentType: "application/json",
        data: {
            "index": index
        }
    }).done(function(response) {
        let newElem = $(`<div class="card note-item" id="note-${index}" data-index="${index}"></div>`).html(response)
        rowsElem.append(newElem)
        buildNotesQuillEditor(newElem.find(".quill-editor").get(0), index)
    }).fail(function(jqxhr, textStatus, errorThrown)  {
        alert("Error getting note template: " + textStatus + " : " + errorThrown)
    })
}

function onClickDeleteNote(elem) {
    if (confirm("Are you sure you want to delete this note?")) {
        $(elem).closest(".note-item").remove()
    }
}

function onNoteEditTitle(elem) {
    let newTitle = prompt("Enter new title:")
    if (!newTitle) {
        return
    }

    let container = $(elem).closest(".note-item")
    let index = container.attr("data-index")

    container.find(`input[name='notes[${index}].title']`).val(newTitle)
    container.find(".note-title").text(newTitle)
}

function onClickEditPhoto() {
    let urlInput = prompt("Enter the URL to a photo")

    if (urlInput) {
        if (!isValidHttpsUrl(urlInput)) {
            alert("URL is not a valid https URL.")
        } else {
            $("input[name='pokedexEntry.imageFileUrl']").val(urlInput)
            $("#char-img").attr("src", urlInput)
        }
    }
}


function onClickCopyR20ToClipboard() {
    try {
        navigator.clipboard.writeText(JSON.stringify(buildRoll20Json()))
        buildToast("Roll20 JSON copied to clipboard!")
    } catch (error) {
        buildToast("Oops! Something went wrong with JSON construction.")
        console.log(error)
    }
}

// Conversions from roll20 JSON
var skillLookup = {
    "Athletics": "athletics",
    "Acrobatics": "acrobatics",
    "Charm": "charm",
    "Combat": "combat",
    "Command": "command",
    "GeneralEducation": "generalEdu",
    "MedicineEducation": "medicineEdu",
    "OccultEducation": "occultEdu",
    "PokeEducation": "pokemonEdu",
    "TechnologyEducation": "techEdu",
    "Focus": "focus",
    "Guile": "guile",
    "Intimidate": "intimidate",
    "Intuition": "intuition",
    "Perception": "perception",
    "Stealth": "stealth",
    "Survival": "survival"
}

function buildRoll20Json() {

    var json = {
        "CharType": 0,
        "nickname": $("#char-name").val(),
        "species": $("#char-species").val(),
        "type1": "None",
        "type2": "None",
        "Level": $("#char-level").val(),
        "HeldItem": "None",
        "Gender": $("#char-gender").val(),
        "Nature": $("#char-nature").val(),
        "height": $("#char-size").val().split("(").pop().split(")")[0],
        "weight":$("#char-weight").val(),
        "weightClass": $("#char-weight").val().split("(").pop().split(")")[0],

        "hitPoints":$("#char-hp-current").val(),
        "injuries":$("#char-injuries").val(),
        "tempHitPoints":$("#char-hp-temp").val(),

        "base_HP":$("#char-stat-hp-base").val(),
        "base_ATK":$("#char-stat-atk-base").val(),
        "base_DEF":$("#char-stat-def-base").val(),
        "base_SPATK":$("#char-stat-spatk-base").val(),
        "base_SPDEF":$("#char-stat-spdef-base").val(),
        "base_SPEED":$("#char-stat-spd-base").val(),
        "HP": $("#char-stat-hp-lvlup").val(),
        "ATK": $("#char-stat-atk-lvlup").val(),
        "DEF": $("#char-stat-def-lvlup").val(),
        "SPATK": $("#char-stat-spatk-lvlup").val(),
        "SPDEF": $("#char-stat-spdef-lvlup").val(),
        "SPEED": $("#char-stat-spd-lvlup").val(),

        "Capabilities": {},
        "Athletics": 2,
        "Acrobatics": 2,
        "Charm": 2,
        "Combat": 2,
        "Command": 2,
        "GeneralEducation": 1,
        "MedicineEducation": 1,
        "OccultEducation": 1,
        "PokeEducation": 1,
        "TechnologyEducation": 1,
        "Focus": 2,
        "Guile": 2,
        "Intimidate": 2,
        "Intuition": 2,
        "Perception": 2,
        "Stealth": 2,
        "Survival": 2,

        "Athletics_bonus": 0,
        "Acrobatics_bonus": 0,
        "Charm_bonus": 0,
        "Combat_bonus": 0,
        "Command_bonus": 0,
        "GeneralEducation_bonus": 0,
        "MedicineEducation_bonus": 0,
        "OccultEducation_bonus": 0,
        "PokeEducation_bonus": 0,
        "TechnologyEducation_bonus": 0,
        "Focus_bonus": 0,
        "Guile_bonus": 0,
        "Intimidate_bonus": 0,
        "Intuition_bonus": 0,
        "Perception_bonus": 0,
        "Stealth_bonus": 0,
        "Survival_bonus": 0
    }

    var pokemonForm = $("#char-form").val()
    if (pokemonForm != "") {
        json["species"] = ($("#char-species").val() + " (" + pokemonForm + ")")
    }

    json["Capabilities"] = {
        "Overland": Number($("#char-capble-overland").val()),
        "Swim": Number($("#char-capble-swim").val()),
        "LJ": Number($("#char-capble-high").val()),
        "HJ": Number($("#char-capble-long").val()),
        "Power": Number($("#char-capble-power").val())
    }

    var others = $("#char-capble-other").val()
    if (others.includes("Naturewalk")) {
        try {
            splits = others.split("Naturewalk (")[1].split(")")[0].split(",")
            for (var key in splits) {
                json["Capabilities"]["Naturewalk (" + splits[key] + ")"] = true
            }

            splitsA = others.split("Naturewalk (")[0]
            splitsB = others.split("Naturewalk (")[1].split(")")[1]
            if (splitsB == undefined) {splitsB = ""}

            moreSplits = (splitsA + splitsB).split(",")

            for (var key in moreSplits) {
                if (moreSplits[key] != "") {
                    json["Capabilities"][moreSplits[key]] = true
                }
            }
        } catch (meh) {

            splits = others.split(",")
            for (var key in splits) {
                if (splits[key] != "") {
                    json["Capabilities"][splits[key]] = true
                }
            }

        }
    } else {

        splits = others.split(",")
        for (var key in splits) {
            if (splits[key] != "") {
                json["Capabilities"][splits[key]] = true
            }
        }
    }


    var test
    if ($("#char-types").val().length == 0) {
        test = $("#char-types").magicSuggest().getValue()
    } else {
        test = $("#char-types").val().split(",")
    }
    json["type1"] = test[0]
    json["type2"] = (test.length > 1 ? test[1] : "None")

    for (var key in skillLookup) {
        test = $("#char-"+skillLookup[key]).val()
        if (test.length > 0) {
            json[key] = Number(test.split("d6")[0]);
            json[key+"_bonus"] = parseInt(test.substring(test.length-2))
        }
    }

    test = $("#char-capble-burrow").val()
    if (test>0) {json["Capabilities"]["Burrow"]=Number(test)}
    test = $("#char-capble-sky").val()
    if (test>0) {json["Capabilities"]["Sky"]=Number(test)}
    test = $("#char-capble-levitate").val()
    if (test>0) {json["Capabilities"]["Levitate"]=Number(test)}

    $(".form-move").each(function(JQMove) {

        var x = $(".form-move")[JQMove].id.split("-")[1]
        var mname = "#move-"+x+"-"

        json["Move"+x] = {
            "Name": $(mname+"name").val(),
            "Type": $(mname+"type").val(),
            "Freq": $(mname+"freq").val(),
            "AC": $(mname+"ac").val(),
            "DB": $(mname+"db").val(),
            "DType": $(mname+"class").val(),
            "Range": $(mname+"range").val(),
            "Effects": $(mname+"effect").val()
        }
        var cont = $(mname+"contest").val().split(" / ")
        json["Move"+x]["Contest Type"] = cont[0]
        json["Move"+x]["Contest Effect"] = cont[1]

    })


    $(".form-ability").each(function(JQAbility) {
        var x = $(".form-ability")[JQAbility].id.split("-")[1]

        var aname = "#ability-"+x+"-"
        json["Ability-"+x] = {
            "Name": $(aname+"name").val(),
            "Freq": $(aname+"freq").val(),
            "Target": $(aname+"target").val(),
            "Trigger": $(aname+"trigger").val(),
            "Info": $(aname+"effect").val()
        }
    })

    $(".form-pokeedge").each(function(JQEdge) {
        var x = $(".form-pokeedge")[JQEdge].id.split("-")[1]

        var ename = "#pokeedge-"+x+"-"
        json["PokeEdge-"+x] = {
            "Name": $(ename+"name").val(),
            "Cost": $(ename+"cost").val(),
            "Prereq": $(ename+"prereq").val(),
            "Info": $(ename+"effect").val()
        }
    })


    return json
}

