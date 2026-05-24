package com.cleanner.app

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

fun getStorageInfo(): Pair<Long, Long> {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    return Pair(availableBytes, totalBytes)
}

fun formatSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return "%.2f GB".format(gb)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WipeScreen()
                }
            }
        }
    }
}

@Composable
fun WipeScreen() {
    val context = LocalContext.current
    var sizeGB by remember { mutableStateOf("") }
    var isWiping by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("就绪") }
    var currentFilePath by remember { mutableStateOf("") }
    var writtenMB by remember { mutableStateOf(0L) }
    var totalMB by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    var wipeJob by remember { mutableStateOf<Job?>(null) }

    val storageInfo = remember { getStorageInfo() }
    val availableGB = storageInfo.first / (1024.0 * 1024.0 * 1024.0)
    val maxWipeGB = (availableGB * 0.95).toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cleanner",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "数据防恢复擦除工具",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "普通删除不安全",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "删除文件后，数据仍残留在存储中，可被恢复工具还原。本工具用随机数据填满可用空间，彻底覆盖残留数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "存储空间",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可用: ${formatSize(storageInfo.first)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "总计: ${formatSize(storageInfo.second)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "最大可擦除: ${maxWipeGB} GB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = sizeGB,
            onValueChange = { newValue ->
                val filtered = newValue.filter { c -> c.isDigit() }
                val num = filtered.toLongOrNull() ?: 0
                if (num <= maxWipeGB) {
                    sizeGB = filtered
                }
            },
            label = { Text("输入擦除大小 (GB)") },
            supportingText = { Text("最大: ${maxWipeGB} GB") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isWiping
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val size = sizeGB.toLongOrNull()
                    Log.d("MainActivity", "点击开始擦除: size=$size GB")
                    if (size != null && size > 0) {
                        val cacheDir = context.externalCacheDir
                        if (cacheDir == null) {
                            status = "错误: 无法访问缓存目录"
                            return@Button
                        }
                        isWiping = true
                        status = "准备中..."
                        currentFilePath = ""
                        writtenMB = 0
                        totalMB = 0
                        progress = 0f
                        Log.d("MainActivity", "启动擦除协程，目标目录: $cacheDir")
                        wipeJob = scope.launch {
                            try {
                                DataWiper.wipe(cacheDir, size, object : DataWiper.ProgressCallback {
                                    override fun onProgress(p: Float, filePath: String, written: Long, total: Long) {
                                        progress = p
                                        currentFilePath = filePath
                                        writtenMB = written
                                        totalMB = total
                                        status = "擦除中..."
                                    }
                                    override fun onComplete() {
                                        Log.d("MainActivity", "擦除完成回调")
                                        isWiping = false
                                        status = "擦除完成！（文件保留在缓存中）"
                                        progress = 1f
                                        wipeJob = null
                                    }
                                    override fun onError(e: Exception) {
                                        Log.e("MainActivity", "擦除错误回调: ${e.message}", e)
                                        isWiping = false
                                        if (e is CancellationException) {
                                            status = "已中断（文件已保留）"
                                        } else {
                                            status = "错误: ${e.message}"
                                            currentFilePath = ""
                                        }
                                        wipeJob = null
                                    }
                                })
                            } catch (e: Exception) {
                                Log.e("MainActivity", "擦除异常: ${e.message}", e)
                                isWiping = false
                                if (e is CancellationException) {
                                    status = "已中断（文件已保留）"
                                } else {
                                    status = "错误: ${e.message}"
                                    currentFilePath = ""
                                }
                                wipeJob = null
                            }
                        }
                    }
                },
                enabled = !isWiping && sizeGB.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("开始擦除")
            }

            if (isWiping) {
                Button(
                    onClick = {
                        Log.d("MainActivity", "用户中断擦除")
                        wipeJob?.cancel()
                        status = "正在中断..."
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("中断")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isWiping || progress > 0f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.contains("完成")) MaterialTheme.colorScheme.primary
                               else if (status.contains("中断")) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentFilePath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "写入文件:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentFilePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val file = java.io.File(currentFilePath)
                                    val parentDir = file.parentFile
                                    if (parentDir != null && parentDir.exists()) {
                                        // 使用 content URI 打开文件管理器
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            parentDir
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                        intent.setDataAndType(uri, "resource/folder")
                                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "无法打开文件管理器: ${e.message}")
                                    // 备用方案：打开存储设置
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        Log.e("MainActivity", "备用方案也失败: ${e2.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("查看文件位置")
                        }
                    }

                    if (totalMB > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已写入: ${writtenMB} MB / ${totalMB} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "文件保留在应用缓存中，可在系统设置里清除",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
