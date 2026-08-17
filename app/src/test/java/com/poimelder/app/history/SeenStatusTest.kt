package com.poimelder.app.history

import org.junit.Assert.assertEquals
import org.junit.Test

class SeenStatusTest {

    @Test
    fun unseen_whenNoDate() {
        assertEquals(SeenStatus.UNSEEN, SeenRepository.status(null, 100L))
    }

    @Test
    fun today_whenFirstSeenIsToday() {
        assertEquals(SeenStatus.TODAY, SeenRepository.status(100L, 100L))
    }

    @Test
    fun previous_whenFirstSeenEarlier() {
        assertEquals(SeenStatus.PREVIOUS, SeenRepository.status(99L, 100L))
        assertEquals(SeenStatus.PREVIOUS, SeenRepository.status(0L, 100L))
    }
}
