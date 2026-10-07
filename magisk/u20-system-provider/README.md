# U20 Telephony Provider Bridge

新版 U20 固件删除了 AOSP `ContactsProvider`，但保留的系统拨号器仍会访问
`com.android.voicemail`、`com.android.contacts` 和 `call_log`。

本模块把同签名的 UFI-TOOLS APK 作为只读 Magisk system overlay 加载，使 APK
内的空数据兼容 Provider 能被系统拨号器发现。它不修改原厂 system 分区，也不
伪造联系人、语音信箱或通话记录。

v1.4 使用标准 Magisk 模块 ZIP 安装，并携带 u20.24 同签名 APK。
不要在模块正在挂载时直接覆盖 `/system/app` 或模块源文件。

设备端模块目录：`/data/adb/modules/u20-system-provider`。

打包：

```sh
./build-module.sh /path/to/UFI-TOOLS.apk /path/to/U20_System_Provider_Magisk.zip
```

回退：在模块目录创建 `disable` 后重启，或删除该模块目录后重启。
