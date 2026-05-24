# Cleanner 项目进度

## 当前状态

| 项目 | 详情 |
|------|------|
| 最新版本 | v1.8 (versionCode=9) |
| 测试覆盖 | 13 个单元测试，全部通过 |
| 真机测试 | 设备 2211133C - 16 |
| Git | 已初始化，首次提交完成，待推送到 GitHub |

## 版本历史

| 版本 | Code | 主要变更 |
|------|------|----------|
| 1.0 | 1 | 初始版本 |
| 1.1 | 2 | 添加单元测试 |
| 1.2 | 3 | 修复存储权限问题 |
| 1.3 | 4 | 权限 Intent 兼容小米设备 |
| 1.4 | 5 | 修复擦除崩溃（IO 移至后台线程） |
| 1.5 | 6 | 添加日志输出 |
| 1.6 | 7 | UI 交互重构（引导页 + 进度详情） |
| 1.7 | 8 | 添加演示验证功能（创建测试图片 → 删除 → 恢复验证 → 擦除 → 再验证） |
| 1.8 | 9 | 移除演示功能（DiskDigger 国内难下载），保留引导页 + 擦除页 |

## 详细记录

### 2026-05-24

#### 1. 单元测试实现

**状态：** 完成

**变更内容：**
- `app/build.gradle.kts` - 添加测试依赖（JUnit, MockK, kotlinx-coroutines-test）
- `app/src/main/java/com/cleanner/app/DataWiper.kt` - 重构：添加 `wipe(targetDir, sizeGB, callback)` 重载方法支持依赖注入
- `app/src/test/java/com/cleanner/app/FormatSizeTest.kt` - 新建：10 个测试覆盖 formatSize 函数
- `app/src/test/java/com/cleanner/app/DataWiperTest.kt` - 新建：13 个测试覆盖 DataWiper 逻辑

**测试结果：** 23/23 通过，0 失败

**验证：** 通过 verification agent 审查

---

#### 2. 真机部署

**状态：** 完成

**操作：**
- 构建 debug APK
- 安装到设备 `2211133C - 16`

---

#### 3. 版本管理规则

**状态：** 完成

**操作：**
- 版本升级至 v1.1 (versionCode=2)
- 保存规则：每次编译必须递增版本号

---

#### 4. 修复存储权限问题

**状态：** 完成

**问题：** Android 11+ 写入 `/storage/emulated/0/` 报 EPERM 错误

**修复：**
- `MainActivity.kt` - 添加 `MANAGE_EXTERNAL_STORAGE` 运行时权限检查
- 未授权时显示权限引导界面，点击「去授权」跳转系统设置
- 从设置返回后自动刷新权限状态

**版本：** v1.2 (versionCode=3)

---

#### 5. 权限 Intent 兼容性修复

**状态：** 完成

**问题：** 小米设备点击「去授权」后卡住/崩溃

**修复：**
- 添加 try-catch 兼容两种权限 Intent
- 使用 Activity.onResume 回调替代协程轮询刷新权限状态

**版本：** v1.3 (versionCode=4)

---

#### 6. 修复擦除崩溃问题

**状态：** 完成

**问题：** 输入 100GB 开始擦除后崩溃/ANR

**修复：**
- `DataWiper.kt` - 使用 `withContext(Dispatchers.IO)` 将文件 IO 移至后台线程
- 添加 `coroutineContext.ensureActive()` 支持协程取消
- 使用 `try-finally` 确保流正确关闭
- 每次创建新的 `SecureRandom` 实例避免线程安全问题

**版本：** v1.4 (versionCode=5)

**测试结果：** 21/21 通过

---

#### 7. 添加日志输出

**状态：** 完成

**操作：**
- `DataWiper.kt` - 添加 Log.d 日志，每 100MB 输出一次进度
- `MainActivity.kt` - 添加按钮点击和回调日志

**版本：** v1.5 (versionCode=6)

---

#### 8. UI 交互重构

**状态：** 完成

**改动：**
- 添加引导页面：解释为什么需要此工具（普通删除/格式化无法防止数据恢复）
- 添加工作原理说明（用随机数据填满可用空间覆盖残留数据）
- 擦除页面显示写入文件路径和实时进度（已写入 MB / 总 MB）
- ProgressCallback 接口扩展：onProgress 新增 filePath, writtenMB, totalMB 参数
- 配置 testOptions.unitTests.isReturnDefaultValues = true 解决 Log mock 问题

**版本：** v1.6 (versionCode=7)

**测试结果：** 13/13 通过

---

#### 9. README 更新与 Git 初始化

**状态：** 完成

**操作：**
- 更新 README.md：添加工具原理说明、使用流程、项目结构
- 创建 .gitignore
- Git 初始化并提交初始版本
- 推送到 GitHub: https://github.com/filanlu/cleanner--
