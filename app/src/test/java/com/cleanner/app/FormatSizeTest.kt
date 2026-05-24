package com.cleanner.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSizeTest {

    @Test
    fun `formatSize zero bytes`() {
        assertEquals("0.00 GB", formatSize(0L))
    }

    @Test
    fun `formatSize 1 GB`() {
        val oneGB = 1024L * 1024L * 1024L
        assertEquals("1.00 GB", formatSize(oneGB))
    }

    @Test
    fun `formatSize 500 MB`() {
        val fiveHundredMB = 500L * 1024L * 1024L
        assertEquals("0.49 GB", formatSize(fiveHundredMB))
    }

    @Test
    fun `formatSize 10 GB`() {
        val tenGB = 10L * 1024L * 1024L * 1024L
        assertEquals("10.00 GB", formatSize(tenGB))
    }

    @Test
    fun `formatSize 1 byte`() {
        assertEquals("0.00 GB", formatSize(1L))
    }

    @Test
    fun `formatSize 1 KB`() {
        assertEquals("0.00 GB", formatSize(1024L))
    }

    @Test
    fun `formatSize 1 MB`() {
        assertEquals("0.00 GB", formatSize(1024L * 1024L))
    }

    @Test
    fun `formatSize 2_5 GB`() {
        val twoAndHalfGB = (2.5 * 1024 * 1024 * 1024).toLong()
        assertEquals("2.50 GB", formatSize(twoAndHalfGB))
    }

    @Test
    fun `formatSize large value 100 GB`() {
        val hundredGB = 100L * 1024L * 1024L * 1024L
        assertEquals("100.00 GB", formatSize(hundredGB))
    }

    @Test
    fun `formatSize negative value`() {
        val negGB = -1L * 1024L * 1024L * 1024L
        assertEquals("-1.00 GB", formatSize(negGB))
    }
}
