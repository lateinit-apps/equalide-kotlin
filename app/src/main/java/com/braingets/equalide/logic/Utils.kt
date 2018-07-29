package com.braingets.equalide.logic

// Puzzle related
const val DEFAULT_DIRECTORY_INDEX = 0
const val DEFAULT_PACK_INDEX = 0
const val UNSET_DIRECTORY_ID = -1
const val NO_LEVEL_OPENED = -1

// Activity related
const val READ_PERMISSION_REQUEST = 1
const val WRITE_PERMISSION_REQUEST = 2
const val SELECT_LEVEL_REQUEST = 3
const val EXPORT_PACK_REQUEST = 4

// Edit edges buttons direction
enum class Direction {
    UP, DOWN, LEFT, RIGHT, NO_DIRECTION
}

// Edit edges buttons modes
const val INCREASE = 0
const val DECREASE = 1
