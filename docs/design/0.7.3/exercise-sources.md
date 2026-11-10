# 运动参考

2024 Adult Compendium，查阅于2026-11-10。22项选择的活动代码与MET保存在 ExerciseCatalog.java，中文名称概括活动及强度。参考主要适用于19–59岁成人，个体消耗可能不同。

- https://pacompendium.com/walking/
- https://pacompendium.com/running/
- https://pacompendium.com/bicycling/
- https://pacompendium.com/conditioning-exercise/
- https://pacompendium.com/sports/

沿用原有额外消耗口径：(MET−1) × 3.5 × kg ÷ 200 × minutes，扣除同时间静息消耗；不与日常活动重复记录。用户可调整MET或切换手动额外消耗。修改既有记录默认保留已保存热量，只有主动切回估算才重算。保存仍使用原 burn 字段，兼容日汇总与趋势。
