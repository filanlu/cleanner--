package com.cleanner.app

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import kotlin.coroutines.coroutineContext

private const val TAG = "DataWiper"

object DataWiper {
    internal const val BUFFER_SIZE = 1024 * 1024 // 1MB

    interface ProgressCallback {
        fun onProgress(progress: Float, filePath: String, writtenMB: Long, totalMB: Long)
        fun onComplete()
        fun onError(e: Exception)
    }

    suspend fun wipe(sizeGB: Long, callback: ProgressCallback) {
        val targetDir = Environment.getExternalStorageDirectory()
        wipe(targetDir, sizeGB, callback)
    }

    suspend fun wipe(targetDir: File, sizeGB: Long, callback: ProgressCallback) {
        val totalBytes = sizeGB * 1024 * 1024 * 1024L
        val file = File(targetDir, "wipe_${System.currentTimeMillis()}.tmp")
        var outputStream: FileOutputStream? = null

        Log.d(TAG, "开始擦除: sizeGB=$sizeGB, totalBytes=$totalBytes, targetDir=$targetDir")
        Log.d(TAG, "目标文件: ${file.absolutePath}")

        try {
            withContext(Dispatchers.IO) {
                val random = SecureRandom()
                Log.d(TAG, "创建输出流...")
                outputStream = file.outputStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var written = 0L

                Log.d(TAG, "开始写入数据...")
                while (written < totalBytes) {
                    coroutineContext.ensureActive()
                    random.nextBytes(buffer)
                    val toWrite = minOf(BUFFER_SIZE.toLong(), totalBytes - written).toInt()
                    outputStream!!.write(buffer, 0, toWrite)
                    written += toWrite

                    if (written % (100 * 1024 * 1024) == 0L || written == totalBytes) {
                        Log.d(TAG, "写入进度: ${written / (1024 * 1024)}MB / ${totalBytes / (1024 * 1024)}MB")
                    }

                    callback.onProgress(
                        written.toFloat() / totalBytes,
                        file.absolutePath,
                        written / (1024 * 1024),
                        totalBytes / (1024 * 1024)
                    )
                }
                Log.d(TAG, "写入完成，刷新流...")
                outputStream!!.flush()
                outputStream!!.close()
                outputStream = null
            }

            // 正常完成：删除临时文件
            Log.d(TAG, "删除临时文件: ${file.absolutePath}")
            file.delete()
            Log.d(TAG, "擦除完成")
            callback.onComplete()

        } catch (e: Exception) {
            Log.e(TAG, "擦除出错: ${e.message}", e)
            // 关闭流
            try { outputStream?.flush() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}

            if (e is CancellationException) {
                // 中断：保留已写入的文件
                Log.d(TAG, "擦除已中断，保留文件: ${file.absolutePath}")
            } else {
                // 其他错误：删除文件
                file.delete()
            }
            callback.onError(e)
        }
    }
}
