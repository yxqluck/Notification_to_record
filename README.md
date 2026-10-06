# Record

一个基于 Android 通知监听服务的应用：监听指定app通知（此处内置微信、QQ、小雅智能助手），从通知文本中提取日程信息，写入系统日历。

## 功能特性

- **通知监听**：监听指定应用（微信、QQ、小雅智能助手）的通知，记录最近五条这些应用的通知内容，便于添加日程模板。（如有其他应用的需求可在`NotifyService`中添加对应包名，并在`NotifyService`的`isTargetApp`和`getAppName`函数中以及`MainActivity`的模块定义中补充）
- **日程转换**：配置提取模板，从通知文本中提取 `{时间}` 作为日程开始时间、`{内容}` 作为日程标题，写入系统日历。
- **多条模板**：每个应用可配置多条提取模板，全部未命中则跳过、不写入日程。
- **日程提醒**：各应用独立配置提醒方式（无提醒 / 通知提醒）与提前小时数，写入日程时自动附加系统提醒。
- **最近消息**：模块内展示每个应用最近 5 条通知消息（标题 / 内容 / 时间），保存于手机本地。
- **模块化界面**：主界面三个模块（微信 / QQ / 小雅）竖向排列，顶部为应用图标、名称、启用开关、设置入口；模块显示该应用最近通知内容。
- **启用开关**：可单独停用某应用的监听与日程写入。

## 环境要求

- Android SDK 37（compileSdk），minSdk 30（Android 11+）
- JDK 11
- Android Studio（Gradle 构建）

## 构建与运行

```bash
# 在项目根目录执行
./gradlew assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

## 首次使用步骤

1. 安装并打开 App，点击「获取权限」，跳转系统设置开启**通知使用权**（授权后返回自动进入模块页）。
2. 按提示授予**日历读写**权限与**通知**权限。
3. 在模块右上角打开设置：
   - 提醒方式选择「通知提醒」，并填写提前提醒的小时数。
   - 填写「提取模板」模板，示例：
     ```
     你好{内容}，{时间}
     ```
     表示从通知文本中提取 `{时间}` 为日程开始时间、`{内容}` 为日程标题；可添加多条规则。
4. 保持模块「启用」开关开启，收到对应应用通知后即自动写入日程并（按设置）提醒。

### 提取规则说明

- 占位符：`{时间}`（开始时间）、`{内容}`（标题）。
- 支持的时间格式示例：`2026-10-06 23:59`、`2026/10/06 23:59`、`2026年10月6日 23:59`、`2026.10.06` 等；单独 `HH:mm` 自动补当天日期。
- 例：通知文本 `你好lucky，2026/10/06 23:59` + 规则 `你好{内容}，{时间}` → 提取内容 `lucky`、时间 `2026/10/06 23:59`。

## 项目结构

```
app/src/main/java/com/example/record/
├── MainActivity.java      # 主界面：权限切换、三模块卡片、设置弹窗
├── NotifyService.java     # 通知监听服务，受开关控制，存消息 + 写日程
├── NotifyHelper.java      # 通知事件分发（单例）
├── NotifyListener.java    # 通知回调接口
├── NotifyMessage.java     # 通知消息数据模型（JSON 序列化）
├── AppStore.java          # 本地存储：开关 / 规则模板 / 提醒设置 / 最近5条消息
├── CalendarHelper.java    # 系统日历写入 + 提醒（CalendarProvider）
└── TemplateParser.java    # 提取规则解析：模板 → 正则 → 时间 / 内容
```

## 权限说明

| 权限 | 用途 |
|---|---|
| `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | 通知使用权（系统授权） |
| `READ/WRITE_CALENDAR` | 写入系统日历与提醒 |
| `POST_NOTIFICATIONS` | Android 13+ 通知权限 |

## 后台运行说明

- 需配置自启动权限以及关联启动的权限，用以在后台能够启动通知监听服务
- 如若不能在后台实时监听，需设置手机管家的省电策略

## 项目说明

本项目基于[NotifyListenerDemo](https://github.com/lilongweidev/NotifyListenerDemo)开发，博客原文在[Android 通知监听服务、NotificationListenerService使用方式（详细步骤+源码）](https://blog.csdn.net/qq_38436214/article/details/119424903)，感谢大佬的开源项目。