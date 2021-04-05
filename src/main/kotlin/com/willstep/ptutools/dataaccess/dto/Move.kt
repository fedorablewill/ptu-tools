package com.willstep.ptutools.dataaccess.dto

data class Move(
    var name: String? = null,
    var type: String? = null,
    var stab: Boolean = false,
    var frequency: String? = null,
    var accuracyCheck: Int? = null,
    var damageBase: Int? = null,
    var damageClass: String? = null,
    var range: String? = null,
    var effects: String? = null,
    var contestType: String? = null,
    var contestEffect: String? = null,
    var critsOn: String? = null
)
