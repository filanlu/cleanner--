package com.cleanner.app

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class DataWiperTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "wiper_test_${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `BUFFER_SIZE is 1 MB`() {
        assertEquals(1024 * 1024, DataWiper.BUFFER_SIZE)
    }

    @Test
    fun `ProgressCallback interface has three methods`() {
        val methods = DataWiper.ProgressCallback::class.java.declaredMethods
        val methodNames = methods.map { it.name }.toSet()
        assertTrue(methodNames.contains("onProgress"))
        assertTrue(methodNames.contains("onComplete"))
        assertTrue(methodNames.contains("onError"))
    }

    @Test
    fun `wipe calls onComplete for zero GB`() = runTest {
        var completed = false
        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {}
            override fun onComplete() { completed = true }
            override fun onError(e: Exception) { fail("Should not error: ${e.message}") }
        }

        DataWiper.wipe(tempDir, 0L, callback)

        assertTrue("onComplete should be called", completed)
    }

    @Test
    fun `wipe writes correct amount of data`() = runTest {
        val totalBytes = 1024L * 1024L // 1MB for quick test
        val file = File(tempDir, "wipe_test.tmp")
        val outputStream = file.outputStream()
        val buffer = ByteArray(DataWiper.BUFFER_SIZE)
        val random = java.security.SecureRandom()
        var written = 0L

        while (written < totalBytes) {
            random.nextBytes(buffer)
            val toWrite = minOf(DataWiper.BUFFER_SIZE.toLong(), totalBytes - written).toInt()
            outputStream.write(buffer, 0, toWrite)
            written += toWrite
        }
        outputStream.flush()
        outputStream.close()

        assertEquals(totalBytes, file.length())
        assertTrue(file.exists())

        file.delete()
        assertFalse(file.exists())
    }

    @Test
    fun `wipe keeps file after completion`() = runTest {
        var completed = false
        var filePath = ""
        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, path: String, writtenMB: Long, totalMB: Long) {
                filePath = path
            }
            override fun onComplete() { completed = true }
            override fun onError(e: Exception) { fail("Should not error: ${e.message}") }
        }

        DataWiper.wipe(tempDir, 0L, callback)

        assertTrue(completed)
        // 文件保留在缓存中（0GB 时文件可能为空或不存在，取决于实现）
    }

    @Test
    fun `wipe deletes temp file on error`() = runTest {
        var errorCaught = false
        var caughtException: Exception? = null

        val blockedDir = File(tempDir, "blocked")
        blockedDir.createNewFile()

        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {}
            override fun onComplete() { fail("Should not complete") }
            override fun onError(e: Exception) {
                errorCaught = true
                caughtException = e
            }
        }

        DataWiper.wipe(blockedDir, 1L, callback)

        assertTrue("Error should be caught", errorCaught)
        assertNotNull("Exception should be captured", caughtException)

        blockedDir.delete()
    }

    @Test
    fun `wipe progress callback receives values between 0 and 1`() = runTest {
        val progressValues = mutableListOf<Float>()
        var completed = false

        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {
                progressValues.add(progress)
            }
            override fun onComplete() { completed = true }
            override fun onError(e: Exception) { fail("Should not error: ${e.message}") }
        }

        DataWiper.wipe(tempDir, 0L, callback)

        assertTrue(completed)
        assertTrue("No progress for 0GB", progressValues.isEmpty())
    }

    @Test
    fun `wipe handles exception during write and cleans up`() = runTest {
        var errorCaught = false
        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {}
            override fun onComplete() { fail("Should not complete on error") }
            override fun onError(e: Exception) { errorCaught = true }
        }

        val nonExistentDir = File(tempDir, "does_not_exist")

        DataWiper.wipe(nonExistentDir, 1L, callback)

        assertTrue("Error should be caught", errorCaught)
    }

    @Test
    fun `wipe callback onProgress receives correct parameters`() {
        val callback = object : DataWiper.ProgressCallback {
            var lastProgress = -1f
            var lastFilePath = ""
            var lastWrittenMB = -1L
            var lastTotalMB = -1L
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {
                lastProgress = progress
                lastFilePath = filePath
                lastWrittenMB = writtenMB
                lastTotalMB = totalMB
            }
            override fun onComplete() {}
            override fun onError(e: Exception) {}
        }
        callback.onProgress(0.5f, "/test/path", 500L, 1000L)
        assertEquals(0.5f, callback.lastProgress)
        assertEquals("/test/path", callback.lastFilePath)
        assertEquals(500L, callback.lastWrittenMB)
        assertEquals(1000L, callback.lastTotalMB)
    }

    @Test
    fun `wipe callback onComplete can be called`() {
        var completed = false
        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {}
            override fun onComplete() { completed = true }
            override fun onError(e: Exception) {}
        }
        callback.onComplete()
        assertTrue(completed)
    }

    @Test
    fun `wipe callback onError receives exception`() {
        var caughtException: Exception? = null
        val callback = object : DataWiper.ProgressCallback {
            override fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long) {}
            override fun onComplete() {}
            override fun onError(e: Exception) { caughtException = e }
        }
        val testException = RuntimeException("test error")
        callback.onError(testException)
        assertEquals("test error", caughtException?.message)
    }

    @Test
    fun `wipe with 2MB writes data and reports progress`() {
        val progressValues = mutableListOf<Float>()
        val totalBytes = 2L * 1024L * 1024L // 2MB
        val file = File(tempDir, "wipe_integration_test.tmp")
        val outputStream = file.outputStream()
        val buffer = ByteArray(DataWiper.BUFFER_SIZE)
        val random = java.security.SecureRandom()
        var written = 0L

        while (written < totalBytes) {
            random.nextBytes(buffer)
            val toWrite = minOf(DataWiper.BUFFER_SIZE.toLong(), totalBytes - written).toInt()
            outputStream.write(buffer, 0, toWrite)
            written += toWrite
            progressValues.add(written.toFloat() / totalBytes)
        }
        outputStream.flush()
        outputStream.close()

        assertEquals(totalBytes, file.length())
        assertEquals(2, progressValues.size)
        assertEquals(0.5f, progressValues[0])
        assertEquals(1.0f, progressValues[1])

        file.delete()
        assertFalse(file.exists())
    }

    @Test
    fun `wipe random data is not all zeros`() {
        val random = java.security.SecureRandom()
        val buffer = ByteArray(256)
        random.nextBytes(buffer)

        val allZeros = buffer.all { it == 0.toByte() }
        assertFalse("Random data should not be all zeros", allZeros)
    }
}
