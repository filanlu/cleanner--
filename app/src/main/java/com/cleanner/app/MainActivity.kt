package com.cleanner.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
    private var onResumeCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp(onResume = { callback -> onResumeCallback = callback })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onResumeCallback?.invoke()
    }
}

@Composable
fun MainApp(onResume: ((() -> Unit) -> Unit)? = null) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf(Environment.isExternalStorageManager()) }

    LaunchedEffect(Unit) {
        onResume?.invoke {
            hasPermission = Environment.isExternalStorageManager()
        }
    }

    when (currentPage) {
        0 -> IntroPage(
            onNext = { currentPage = 1 },
            hasPermission = hasPermission,
            onRequestPermission = {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        )
        1 -> WipePage(onBack = { currentPage = 0 })
    }
}

@Composable
fun IntroPage(onNext: () -> Unit, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "数据防恢复擦除工具",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "为什么需要这个工具？",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "普通删除或格式化并不会真正擦除数据。\n\n" +
                           "操作系统只是标记这些空间为「可用」，实际数据仍然残留在存储芯片上。\n\n" +
                           "使用专业恢复软件可以轻松恢复这些「已删除」的文件，包括照片、视频、聊天记录等隐私信息。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "工作原理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "本工具通过在存储空间中创建大体积临时文件，用随机数据填满所有可用空间，彻底覆盖已删除文件的残留数据。\n\n" +
                           "完成后自动删除临时文件，不影响正常使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "需要授权存储权限",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请先授予「所有文件访问权限」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestPermission) {
                        Text("去授权")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onNext,
            enabled = hasPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始使用")
        }

        if (!hasPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先完成授权",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WipePage(onBack: () -> Unit) {
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
            text = "数据擦除",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

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
                        isWiping = true
                        status = "准备中..."
                        currentFilePath = ""
                        writtenMB = 0
                        totalMB = 0
                        progress = 0f
                        Log.d("MainActivity", "启动擦除协程...")
                        wipeJob = scope.launch {
                            try {
                                Log.d("MainActivity", "调用 DataWiper.wipe...")
                                DataWiper.wipe(size, object : DataWiper.ProgressCallback {
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
                                        status = "擦除完成！"
                                        progress = 1f
                                        currentFilePath = ""
                                        wipeJob = null
                                    }
                                    override fun onError(e: Exception) {
                                        Log.e("MainActivity", "擦除错误回调: ${e.message}", e)
                                        isWiping = false
                                        if (e is kotlinx.coroutines.CancellationException) {
                                            status = "已中断（文件已保留）"
                                            // 保留 currentFilePath 显示
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
                                if (e is kotlinx.coroutines.CancellationException) {
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
                        color = if (status == "擦除完成！") MaterialTheme.colorScheme.primary
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
                                    // 用文件管理器打开所在目录
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val fileUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:${currentFilePath.removePrefix("/storage/emulated/0/").substringBeforeLast("/")}")
                                    intent.setDataAndType(fileUri, "resource/folder")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // 备用方案：打开存储根目录
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = Uri.parse("content://com.android.externalstorage.documents/document/primary:")
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        Log.e("MainActivity", "无法打开文件管理器: ${e2.message}")
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

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
        }
    }
}
