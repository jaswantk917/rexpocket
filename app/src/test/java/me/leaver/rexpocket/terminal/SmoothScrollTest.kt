package me.leaver.rexpocket.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

/** The history moves by the pixel and rests flush at its ends. Rows are 20 px here. */
class SmoothScrollTest {
    private fun step(phase: Float, pixels: Float, newer: Int = 100, older: Int = 100) = stepHistory(phase, pixels, 20f, newer, older)

    @Test
    fun `less than a row is drawn as an offset, not a row`() = assertEquals(HistoryStep(0, 7f, false), step(0f, 7f, newer = 5))

    @Test
    fun `whole rows go to the viewport and the rest stays an offset`() = assertEquals(HistoryStep(2, 5f, false), step(10f, 35f))

    @Test
    fun `scrolling back past the top row takes a row up`() = assertEquals(HistoryStep(-1, 15f, false), step(5f, -10f))

    @Test
    fun `at the oldest row the history rests flush and a fling stops`() = assertEquals(HistoryStep(0, 0f, true), step(5f, -30f, older = 0))

    @Test
    fun `reaching the bottom leaves no offset, as there is no row below`() = assertEquals(HistoryStep(1, 0f, false), step(15f, 10f, newer = 1))

    @Test
    fun `past the bottom the history rests flush and a fling stops`() = assertEquals(HistoryStep(2, 0f, true), step(0f, 90f, newer = 2))

    @Test
    fun `a long fling back moves many rows at once`() = assertEquals(HistoryStep(-10, 0f, false), step(0f, -200f))
}
