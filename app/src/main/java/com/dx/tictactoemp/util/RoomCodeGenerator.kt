package com.dx.tictactoemp.util

import kotlin.random.Random

object RoomCodeGenerator {
    /**
     * Generate a random 4-digit numeric code
     * Format: "0000" to "9999"
     */
    fun generate(): String {
        val code = Random.nextInt(0, 10000)
        return String.format("%04d", code)
    }
}

