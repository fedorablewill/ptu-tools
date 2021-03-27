package com.willstep.ptutools.dataaccess.dto

data class Move(
    val name: String,
    val type: String? = null,
    val frequency: String? = null,
    val accuracyCheck: Int? = null,
    val damageBase: Int? = null,
    val damageClass: String? = null,
    val range: String? = null,
    val effects: String? = null,
    val contestType: String? = null,
    val contestEffect: String? = null
)
