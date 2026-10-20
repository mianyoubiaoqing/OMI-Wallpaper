# OMI-Wallpaper

原生 Android 互动动态壁纸引擎，使用 Java 和 Android Canvas，无需联网或运行时第三方库。默认示例是完全由代码绘制的原创几何机器人，演示代码与引擎采用 MIT 协议。

Native Android interactive live wallpaper engine with a procedural, MIT-licensed demo. Android 10+; targets Android 16 / API 36.

<img src="docs/demo.png" alt="程序绘制的原创互动演示角色" width="280">

演示图由项目自己的 Canvas 渲染器输出，随项目采用 MIT 协议。

## 功能

- 同一渲染器用于应用内预览、全屏互动预览和系统动态壁纸。
- 独立身体、头部与手部图层，连续关节摆动和限定区域的发丝／饰带网格动画。
- 连续眼睑遮挡、单眼与双眼动作，短点按张嘴后恢复微笑。
- 触摸涟漪与粒子、拖动跟随、倾斜视差；动作幅度、构图与帧率可调。
- 可见性与息屏暂停、系统省电帧率限制、硬件／兼容画布切换。
- 系统壁纸选择器、备用设置入口、本地诊断及用户主动导出日志。

这是参考实现和可运行的引擎示例。内置网格权重、关节坐标与表情区域针对 1240 × 1754 的参考布局，其他角色需要重新标定；并非任意图片的一键自动绑定工具。

## 构建与安装

需要 JDK 17+ 和 Android SDK 36。Gradle Wrapper 已包含；首次构建会下载 Gradle 与构建依赖。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 使用 `gradlew.bat`；也可运行 `powershell -File tools/build.ps1`。Android SDK 通过 `ANDROID_HOME` 或本地 `local.properties` 配置，个人路径无需提交。

打开 **OMI Wallpaper** →「全屏互动预览」体验 →「设为动态壁纸」在系统页面确认。锁屏支持和桌面触摸转发由设备系统决定。应用 ID 为 `io.github.omi.wallpaper`。

## 接入自己的素材

默认项目没有图片依赖，克隆后可直接运行。开发者可准备自有授权的完整分层素材，接入方式见 [素材与绑定文档](docs/ARTWORK.md)。素材目录默认被 Git 忽略，发布自己的主题时应另行明确其来源与许可证。

## 测试

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.github.omi.wallpaper.test/io.github.omi.wallpaper.EngineInstrumentation
```

单元测试覆盖眼睛时序、点按与拖动判定、关节位移、发束末端保形、局部连续性和画面边缘约束。Android 仪器检查覆盖无素材启动、真实 Canvas 动效、静态模式、共享图层生命周期和表情。GitHub Actions 执行构建、单元测试、Lint 和仪器测试 APK 编译；仪器运行需连接 Android 设备或模拟器。

## 代码结构

| 文件 | 职责 |
| --- | --- |
| SceneRenderer | 绘制、坐标变换、网格、互动与粒子 |
| CharacterMotion / HairMotion / SceneMath | 可独立测试的动作与几何 |
| ArtworkAssets / LayeredArtwork | 素材校验与共享图层生命周期 |
| DemoArtwork | 原创程序绘制演示 |
| FaceExpressions / TextureFaceExpressions | 程序绘制或贴图式连续表情 |
| OmiWallpaperService | 壁纸生命周期、触摸与传感器接入 |
| WallpaperApplyController / AppDiagnostics | 设置入口与本地诊断 |

## 隐私与开源范围

应用不申请网络、相机、麦克风或存储读取权限；不上传日志。用户主动导出的诊断会包含设备型号、系统版本、动效设置与本应用事件，分享前应自行检查。诊断可能含本应用异常堆栈。

公开仓库采用全新提交历史；不包含个人目录、设备实测日志、聊天附件、原项目图片、参考主题、已签名安装包或签名密钥。发布前可执行 `python tools/audit-public.py` 扫描暂存／已跟踪文件。不要把真实用户日志提交到 issues 或仓库中。

## 许可证

引擎与原创程序绘制演示： [MIT](LICENSE)。Gradle Wrapper 保留其 Apache-2.0 许可证及版权声明，见 [第三方声明](THIRD_PARTY_NOTICES.md)。之后接入的第三方素材不自动获得本项目的 MIT 授权。
