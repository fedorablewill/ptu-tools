package com.willstep.ptutools.dataaccess.dto

data class LevelUpChanges(
    var level: Int,
    var moves: List<Move> = ArrayList()
)
