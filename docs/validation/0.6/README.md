# 0.6.0 验证

- 0.5.1签名安装包 → 0.6.0覆盖安装：体重、餐食、照片引用、1200千卡计划、五个挑战保留。`upgrade-data.txt`。
- `features.txt`：100格计数、同格重复编辑、时间、撤销、归档/重开、持久化；搜索大小写/归档过滤；照片路径约束；更新清单、摘要、旧版本拒绝全部通过。
- `ui-checks.json`：真实原生UI点击和滑动检查。UiFixture仅用于隔离模拟器，生产APK不包含测试运行器，也不预填数据。
- `food-regression.txt`：三张照片和数据库回归。宫保鸡丁与麻婆豆腐匹配成功，红烧肉仍可能误识别，需要用户确认；不把此已知模型局限算作识别准确率全部通过。
- `runtime-errors.txt`：AndroidRuntime错误检查，无异常输出。
- 视觉证据与大字体/空状态：`../../design/0.6/`；详细对照见仓库根目录 `design-qa.md`。
- `pager-frames.txt` 是最初SwiftShader软件渲染模拟器的性能记录，不能代表手机GPU，也不能据此声称稳定60fps。

APK签名延续旧版，私钥未上传。更新清单包含实际文件大小及SHA256。尚无真实手机上的性能和厂商安装器验证。

硬件加速复测：Android Emulator OpenGL ES Translator（Apple M1 Pro / Metal），相同API37、1080×2400px、动画倍率1。预热字形后连续左右滑动12次，469帧，Janky frames 8（1.71%），P50/P90/P95/P99为17/18/19/19ms，Slow UI thread 1。见 `pager-frames-hardware.txt`。这是宿主GPU加速模拟器结果，不等同于所有Android真机的帧率保证。
