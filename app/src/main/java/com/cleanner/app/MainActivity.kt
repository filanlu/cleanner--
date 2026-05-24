package com.cleanner.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import java.io.File
import java.io.FileOutputStream

fun getStorageInfo(): Pair<Long, Long> {
    val stat = android.os.StatFs(Environment.getExternalStorageDirectory().path)
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    return Pair(availableBytes, totalBytes)
}

fun formatSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return "%.2f GB".format(gb)
}

fun createTestImage(context: android.content.Context): String? {
    return try {
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            isAntiAlias = true
        }

        canvas.drawText("这是测试截图", 50f, 100f, paint)
        canvas.drawText("密码: MySecret123!", 50f, 180f, paint)
        canvas.drawText("银行卡: 6222 **** **** 1234", 50f, 260f, paint)
        canvas.drawText("删除此图片后尝试恢复", 50f, 340f, paint)

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, "test_secret_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // 让媒体库扫描到这张图片
        MediaStore.Images.Media.insertImage(
            context.contentResolver,
            file.absolutePath,
            file.name,
            "Test image for Cleanner demo"
        )

        bitmap.recycle()
        file.absolutePath
    } catch (e: Exception) {
        Log.e("MainActivity", "创建测试图片失败: ${e.message}", e)
        null
    }
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
            onDemo = { currentPage = 2 },
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
        2 -> DemoPage(onBack = { currentPage = 0 })
    }
}

@Composable
fun IntroPage(onNext: () -> Unit, onDemo: () -> Unit, hasPermission: Boolean, onRequestPermission: () -> Unit) {
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
                           "使用专业恢复软件（如 DiskDigger）可以轻松恢复这些「已删除」的文件，包括照片、视频、聊天记录等隐私信息。",
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
            Text("开始擦除")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDemo,
            enabled = hasPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("演示验证")
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
fun DemoPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var testImagePath by remember { mutableStateOf("") }
    var isWiping by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var wipeStatus by remember { mutableStateOf("") }
    var currentFilePath by remember { mutableStateOf("") }
    var writtenMB by remember { mutableStateOf(0L) }
    var totalMB by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "演示验证",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "通过实际操作证明：普通删除不安全，擦除后无法恢复",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 步骤 1：创建测试图片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStep >= 1) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "第 1 步：创建测试图片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "创建一张包含「密码」和「银行卡号」的测试图片",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val path = createTestImage(context)
                        if (path != null) {
                            testImagePath = path
                            currentStep = 1
                        }
                    },
                    enabled = currentStep == 0
                ) {
                    Text(if (currentStep >= 1) "已创建" else "创建测试图片")
                }
                if (testImagePath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "保存位置: $testImagePath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 步骤 2：用户删除图片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStep >= 2) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "第 2 步：手动删除图片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "打开相册或文件管理器，找到并删除刚才创建的测试图片\n\n" +
                           "删除后点击下方按钮继续",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { currentStep = 2 },
                    enabled = currentStep == 1
                ) {
                    Text(if (currentStep >= 2) "已删除" else "我已删除图片")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 步骤 3：用 DiskDigger 恢复
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStep >= 3) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "第 3 步：尝试恢复（证明删除不安全）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "安装 DiskDigger（应用商店搜索），扫描设备\n\n" +
                           "你会发现刚才删除的图片可以被恢复！\n\n" +
                           "这证明：普通删除并不能保护你的隐私数据",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(
                        onClick = {
                            // 打开应用商店搜索 DiskDigger
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("market://details?id=com.defianttech.diskdigger")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.defianttech.diskdigger")
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下载 DiskDigger")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { currentStep = 3 },
                        enabled = currentStep == 2,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentStep >= 3) "已确认" else "我已看到恢复结果")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 步骤 4：用 Cleanner 擦除
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStep >= 4) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "第 4 步：使用 Cleanner 擦除",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "擦除 1GB 可用空间，覆盖已删除图片的数据",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isWiping) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = wipeStatus,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (currentFilePath.isNotEmpty()) {
                        Text(
                            text = "写入: $currentFilePath",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (totalMB > 0) {
                        Text(
                            text = "${writtenMB} MB / ${totalMB} MB",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            isWiping = true
                            wipeStatus = "擦除中..."
                            scope.launch {
                                try {
                                    DataWiper.wipe(1, object : DataWiper.ProgressCallback {
                                        override fun onProgress(p: Float, filePath: String, written: Long, total: Long) {
                                            progress = p
                                            currentFilePath = filePath
                                            writtenMB = written
                                            totalMB = total
                                            wipeStatus = "擦除中..."
                                        }
                                        override fun onComplete() {
                                            isWiping = false
                                            wipeStatus = "擦除完成！"
                                            progress = 1f
                                            currentStep = 4
                                        }
                                        override fun onError(e: Exception) {
                                            isWiping = false
                                            wipeStatus = "错误: ${e.message}"
                                        }
                                    })
                                } catch (e: Exception) {
                                    isWiping = false
                                    wipeStatus = "错误: ${e.message}"
                                }
                            }
                        },
                        enabled = currentStep == 3
                    ) {
                        Text("开始擦除 1GB")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 步骤 5：再次恢复验证
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStep >= 5) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "第 5 步：再次尝试恢复（证明擦除有效）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "再次使用 DiskDigger 扫描\n\n" +
                           "这次你会发现：刚才的图片无法恢复了！\n\n" +
                           "这证明：Cleanner 擦除后，数据已彻底消失",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { currentStep = 5 },
                    enabled = currentStep == 4
                ) {
                    Text(if (currentStep >= 5) "验证完成" else "我已确认无法恢复")
                }
            }
        }

        if (currentStep >= 5) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "验证完成！",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cleanner 已成功擦除存储空间\n已删除的文件无法被恢复",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
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
                    Log.d("MainActivity", "启动擦除协程...")
                    scope.launch {
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
                                }
                                override fun onError(e: Exception) {
                                    Log.e("MainActivity", "擦除错误回调: ${e.message}", e)
                                    isWiping = false
                                    status = "错误: ${e.message}"
                                    currentFilePath = ""
                                }
                            })
                        } catch (e: Exception) {
                            Log.e("MainActivity", "擦除异常: ${e.message}", e)
                            isWiping = false
                            status = "错误: ${e.message}"
                        }
                    }
                }
            },
            enabled = !isWiping && sizeGB.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isWiping) "擦除中..." else "开始擦除")
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
