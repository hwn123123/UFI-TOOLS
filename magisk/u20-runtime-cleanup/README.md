# U20 Runtime Cleanup

适用于新版 FM_U20/MN901。该模块不改原厂分区，只处理已经实机确认与 U20
硬件不匹配的服务：

- 把原厂 `whale` 主音频 HAL 切换为 Android `default` 空音频 HAL。U20 没有
  ALSA 声卡，原配置会每 100 ms 重试不存在的扬声器、听筒和麦克风。
- 停止 `vendor.cplog_svc-default`、`cplogserver`、`slogmodem` 三组原厂 CP
  调试日志服务。它们不是 RIL，实机停止后 5G 数据仍保持注册。
- 停止缺少 VINTF 声明、不断崩溃重启的 ArmNN GPU 神经网络服务。

模块只消除无效重试和日志刷屏，不会凭空产生 U20 本来不存在的物理麦克风、
扬声器或蜂窝语音通道。

设备端路径：`/data/adb/modules/u20-runtime-cleanup`。

回退：在模块目录创建 `disable` 后重启，或删除该模块目录后重启。重启后原厂
音频和调试服务会自动恢复。
