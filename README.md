# 🚗 车架号查询 App — VINReader

免费 Android 应用，通过车架号(VIN)拍照识别或手动输入，查询车辆详细信息。

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 📸 **拍照识别** | 拍摄车架号铭牌照片，自动 OCR 识别 17 位 VIN |
| ⌨️ **手动输入** | 直接输入车架号查询 |
| 🔍 **车辆信息查询** | 品牌、型号、年款、发动机、排量、产地等 50+ 字段 |
| 🆓 **完全免费** | 无需注册、无需 API Key、无广告 |

## 📡 数据来源

- **OCR 识别**: Google ML Kit（本地处理，照片不上传服务器）
- **车辆数据**: [NHTSA VPIC](https://vpic.nhtsa.dot.gov/api/)（美国交通部官方 API，完全免费）

---

# 🔧 如何获取 APK（选一个即可）

## 🥇 方案 1：GitHub Actions 在线编译（推荐，只需浏览器）

> **不需要安装任何东西，全程在线完成，约 5 分钟**

1. **注册 GitHub 账号**
   - 打开 https://github.com/signup ，注册一个免费账号

2. **创建新仓库**
   - 登录后点右上角 `+` → **New repository**
   - Repository name: 随便填，比如 `VINReader`
   - 选 **Private**（隐私）或 **Public** 都行
   - 点 **Create repository**

3. **上传项目文件**
   - 打开 `C:\Users\胡向飞\Desktop\VINReader\` 文件夹
   - **全选所有文件** → 拖拽上传到 GitHub 页面
   - 点 **Commit changes**

4. **自动编译**
   - GitHub 会自动检测到 `.github/workflows/build-apk.yml`
   - 点仓库顶部的 **Actions** 标签
   - 你会看到一个正在运行的 **Build Android APK** 工作流
   - 等它完成（约 5 分钟，黄灯 → 绿灯 ✅）

5. **下载 APK**
   - 点那个已完成的工作流
   - 往下翻到 **Artifacts** 区域
   - 点 **VINReader-APK** 下载 ZIP
   - 解压得到 `app-debug.apk`
   - **传到手机安装即可使用**

---

## 🥈 方案 2：双击 `build.ps1` 自动安装+编译

> **适合有基础的用户，脚本会自动检测/安装 JDK 和 Android SDK**

1. **右键** `VINReader\build.ps1` → 选择 **使用 PowerShell 运行**
2. 如果提示执行策略，选 **Y (Yes)**
3. 脚本会自动检测环境，缺少什么就装什么
4. APK 最终会复制到 **桌面**

> 注意：首次运行需要联网下载依赖，耗时 5~15 分钟

---

## 🥉 方案 3：Android Studio 手动编译（最标准）

1. 下载安装 [Android Studio](https://developer.android.com/studio)
2. 打开软件 → **File → Open** → 选择 `VINReader` 文件夹
3. 等待 Gradle 同步完成（右下角进度条跑完）
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. 去 `app/build/outputs/apk/debug/app-debug.apk` 拿 APK

---

## 📦 直接获取（找人帮忙）

如果你不想折腾，也可以：
- 把这个文件夹发给有 Android Studio 的朋友
- 或者在群里问一下，让别人帮你编译好发你
- APK 很小，几 MB 而已

---

## 📱 安装说明

1. 把 APK 传到手机（微信/QQ/数据线都行）
2. 手机上点 APK 文件安装
3. 如果提示"未知来源应用"，去设置里允许安装
4. 打开 App，点"拍照识别"或手动输入 VIN 即可

---

## 🔒 隐私说明

- 拍照识别在本地完成，图片**不会**上传
- 仅车架号发送到 NHTSA 官方 API（美国政府机构）
- 无需注册、无追踪、无广告

---

*有啥问题直接问我 👋*
