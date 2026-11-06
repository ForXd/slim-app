# 0.6.0 原生界面视觉 QA

final result: passed

## Target and evidence

- Source visual truth: `docs/design/0.6/reference.png`，用户指定的暖纸色、朱红色日记参考。
- Implementation: `docs/design/0.6/home.png`、`challenges.png`、`trends.png`、`plan.png`、`large-font.png`、`empty-home.png`。
- Full comparison: `docs/design/0.6/comparison-final.png`；focused typography/card/timeline comparison: `comparison-detail.png`。两张图均包含参考与实际截图，并已打开并排检查。
- Native Android API 37 ARM64，1080×2400px，420dpi，约411dp宽；另测360dp宽、font_scale=1.3。
- Source 1024×1536px；Android原始截图1080×2400px。对照时移除系统状态栏和底栏，将两侧内容等比缩到480px宽，未纵向拉伸。原生设备内容更长，追加加餐、能量汇总和底部导航；不把系统栏算作视觉差异。没有CSS视口或浏览器页面，本次交付为原生APK。
- State: 午餐已有一条、两次体重数据。参考日期/菜名为11月4日/番茄炒蛋，模拟器测试为11月6日/宫保鸡丁，二者结构一致但数据不同。测试照片来自固定来源，不是正式安装的预填记录。

## Comparison history / fixes

1. 首轮 `comparison-first.png`：P1 首页顺序虽已重构，但与参考比纵向过松、照片偏窄；P2 摘要卡片边框过重、底栏标题未居中、时间线状态节点不足。修正：首页顶部压缩13dp，摘要/餐次间距收紧，真实照片改为126×96dp；边框改为1物理像素；底栏文字居中；餐次节点按有无记录显示实心/空心。
2. P2 百次卡片初始测量宽度与吸附步长不一致：卡片宽度在布局完成后统一，实测按钮与手势都能选中正确卡片；搜索保留选择，清空后恢复卡片位置。
3. P2 360dp/1.3倍字体下「查看全部」换行截断：控件宽度跟随字体比例，顶部日期区域同样随字体扩展。`large-font.png`复查无截断。
4. P2 整页透明淡入的首帧为空：改为立即显示内容，使用4dp/180ms位移和临时硬件层；关闭系统动画时直接显示。
5. 稳定态重新截图并比对 `comparison-final.png` 与 `comparison-detail.png`；无未解决P0/P1/P2视觉问题。

## Required fidelity surfaces

- Fonts/type: 内置Noto Serif CJK，日期/标题/按钮/日记使用宋体；输入、细小网格与图表数字用系统字体保持可读性。1.3倍字体验证通过。
- Spacing/layout: 两个并列主入口、双摘要卡、细线分隔的饮食时间线；主区域顺序与参考一致。常驻五项导航不会被滚动内容遮挡。新增挑战使用横向预览卡片与单个明细区。
- Colors/tokens: 暖米白纸纹、朱红主色、深棕文字、克制的橄榄绿体重变化；已去掉绿色大块导航和旧版厚重按钮堆叠。
- Imagery: 纸纹和透明水彩番茄为专门生成的实际位图；图标来自Material Design Icons；餐图显示所保存的真实照片，不用示例图代替未记录状态。原有树形桌面标识保留。
- Copy/content: 保留用户指定名称「歪脖树修炼笔记」。不伪造参考图中的餐点时间、体重曲线和食物照片；没有数据时显示空状态。

## Interactions verified

原生UI自动化已验证挑战按钮/手势切换、吸附、名称搜索（大小写）、无结果、归档范围、饮食入口、7/30天趋势、1200千卡计划；FeatureCheck验证100格边界、编辑/撤销/归档/持久化及更新安全校验。0.5.1覆盖安装后记录保留。运行日志无AndroidRuntime异常。

## Follow-up polish (P3)

- 参考图的摘要内微型趋势线未复制；真实趋势位于「趋势」页，避免为仅两次数据绘制参考中的虚构曲线。
- 标准图标与参考手绘线条存在细微差异；采用开放图标库，便于维护与清晰缩放。
- 原生长列表、输入弹窗与平台安装器保持Android交互，未将整张参考图静态铺成界面。

## Implementation checklist

- [x] Source and stable implementation opened together, including focused regions.
- [x] Earlier P1/P2 findings fixed and recaptured.
- [x] Default, empty, large-font and challenge states checked.
- [x] Core interaction and upgrade-data tests passed.
- [x] Performance evidence kept separately; visual approval is not a claim of stable device FPS.
