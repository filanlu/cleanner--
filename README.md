# Cleanner - 数据防恢复擦除工具

一款 Android 应用，通过使用加密随机数据覆写设备外部存储的可用空间，防止已删除文件被数据恢复工具还原。

## 为什么需要这个工具？

普通删除或格式化**并不会真正擦除数据**。操作系统只是标记这些空间为「可用」，实际数据仍然残留在存储芯片上。

使用专业恢复软件（如 DiskDigger、Recuva 等）可以轻松恢复这些「已删除」的文件，包括照片、视频、聊天记录等隐私信息。

## 工作原理

本工具通过在存储空间中创建大体积临时文件，用随机数据填满所有可用空间，彻底覆盖已删除文件的残留数据。完成后自动删除临时文件，不影响正常使用。

## 功能特性

- 引导页面解释数据恢复风险和工具原理
- 用户指定擦除大小（GB），应用会生成等量的随机数据写入临时文件
- 使用 `SecureRandom` 生成加密级随机字节，以 1MB 为单位分块写入
- 实时显示写入文件路径、已写入 MB / 总 MB、进度百分比
- 写入完成后自动删除临时文件
- Android 11+ 权限引导，兼容小米等设备

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.0 |
| 构建系统 | Gradle (Kotlin DSL) | 8.2 |
| Android Gradle Plugin | com.android.application | 8.2.0 |
| UI 框架 | Jetpack Compose (Material 3) | BOM 2024.02.00 |
| 最低 Android 版本 | minSdk | 26 (Android 8.0) |
| 目标 Android 版本 | targetSdk | 34 (Android 14) |

## 构建与安装

### 前置要求

- JDK 8 或更高版本
- Android SDK（API Level 34）

### 构建

```bash
# 调试版
./gradlew assembleDebug

# 发布版
./gradlew assembleRelease
```

### 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 运行测试

```bash
./gradlew testDebugUnitTest
```

## 使用方法

1. 启动 Cleanner 应用
2. 阅读引导页面，了解数据恢复风险
3. 授予存储权限（Android 11+ 需手动授权「所有文件访问权限」）
4. 输入要擦除的存储空间大小（单位：GB）
5. 点击「开始擦除」按钮
6. 观察实时进度：写入文件路径、已写入量、进度百分比
7. 等待完成

## 权限说明

应用需要以下存储权限：

- `WRITE_EXTERNAL_STORAGE` - 写入外部存储
- `READ_EXTERNAL_STORAGE` - 读取外部存储
- `MANAGE_EXTERNAL_STORAGE` - 管理外部存储（Android 11+ 需在系统设置中手动授予"所有文件访问"权限）

## 项目结构

```
app/
  src/
    main/
      AndroidManifest.xml
      java/com/cleanner/app/
        MainActivity.kt      # Compose UI（引导页 + 擦除页）
        DataWiper.kt          # 核心擦除逻辑（协程 + IO 调度）
      res/values/
        strings.xml
        themes.xml
    test/
      java/com/cleanner/app/
        FormatSizeTest.kt    # formatSize 单元测试
        DataWiperTest.kt     # DataWiper 单元测试
  build.gradle.kts
build.gradle.kts
settings.gradle.kts
```

## 安全说明

- 本工具采用单次随机数据覆写策略，对于现代闪存存储设备通常已足够（由于磨损均衡机制，多次覆写模式在闪存上无效）
- 覆写操作针对外部存储的可用空间，不会影响已有文件
- 擦除完成后临时文件会被自动删除
- 所有 IO 操作在后台线程执行，不会阻塞 UI
