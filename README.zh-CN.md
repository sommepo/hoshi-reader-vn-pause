<div align="center">

# Hoshi Reader Android — Push to Continue

![Platform](https://img.shields.io/badge/platform-Android-lightgrey)
![License](https://img.shields.io/badge/license-GPLv3-blue)
![Fork](https://img.shields.io/badge/fork-unofficial-orange)

[English](README.md) | **简体中文**

</div>

> **这是非官方的公开 fork，不是官方 Hoshi Reader。**
>
> 上游仓库：[HuangAntimony/Hoshi-Reader-Android](https://github.com/HuangAntimony/Hoshi-Reader-Android)
>
> 请不要把 **Push to Continue** 的问题或功能请求发给上游。原作者没有制作这个 fork，也不对此负责。

## 这个 fork 是什么

Hoshi Reader Android 的非官方副本。保留上游的日语 EPUB 阅读器（Yomitan 查词、Anki 制卡、Sasayaki 有声书跟读、VN 模式、墨水屏），并增加一项阅读功能：**Push to Continue**（点击继续）。

Debug APK 会作为 **Hoshi Debug**（`moe.antimony.hoshi.debug`）安装在官方 Hoshi **旁边**，不会覆盖官方 App，也不共用书库。

## Push to Continue 做什么

在 **视觉小说（VN）** 模式下，配合 Sasayaki 有声书（音频 + SRT 匹配）：

1. 音频会播完 **当前 VN 屏幕** 上的所有句子。
2. 该屏最后一句有声书结束后，播放 **暂停**。
3. 屏幕保持显示。点击 **空白处** 或按播放，进入下一屏。

这个 fork 的目的就是：听完一屏、停下、再按一下继续。

默认开启。若要连续跟读，可在 Sasayaki 设置里关闭。点文字仍会查词。只在 VN 模式生效；分页和滚动阅读不变。

<div align="center">

<table>
  <tr>
    <td><img src="docs/images/readme/bookshelf.jpg" alt="书架" width="100%"></td>
    <td><img src="docs/images/readme/reader-lookup-popup.jpg" alt="阅读器查词弹窗" width="100%"></td>
    <td><img src="docs/images/readme/reader-dark-theme.jpg" alt="深色阅读器" width="100%"></td>
    <td><img src="docs/images/readme/reader-eink-mode.jpg" alt="墨水屏阅读器" width="100%"></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/sasayaki-audiobook.jpg" alt="Sasayaki 有声书" width="100%"></td>
    <td><img src="docs/images/readme/reader-statistics.jpg" alt="阅读统计" width="100%"></td>
    <td><img src="docs/images/readme/reader-highlights.jpg" alt="阅读器高亮" width="100%"></td>
    <td><img src="docs/images/readme/dictionary-recursive-lookup.jpg" alt="辞典递归查词" width="100%"></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/reader-appearance-settings.jpg" alt="阅读器外观设置" width="100%"></td>
    <td><img src="docs/images/readme/dictionary-management.jpg" alt="辞典管理" width="100%"></td>
    <td><img src="docs/images/readme/anki-card-settings.jpg" alt="Anki 制卡设置" width="100%"></td>
    <td><img src="docs/images/readme/sync-settings.jpg" alt="同步设置" width="100%"></td>
  </tr>
</table>

</div>

## 功能

### 书架

- 单本、批量或按文件夹递归导入 EPUB，并在书架查看阅读进度。
- 使用自定义书架整理书籍。
- 导出 EPUB，或把远端同步的书籍拉回本地书库。

### 阅读

- 以竖排或横排阅读日文书籍，并在分页和连续滚动之间切换。
- 自定义主题、字体、段落间距和阅读器控件，并支持配置自定义阅读主题。
- 支持沉浸式专注模式、音量键翻页和墨水屏显示选项。
- 全屏查看阅读器图片，支持缩放、复制、保存和分享。

### 查词

- 导入、下载、更新和管理 Yomitan 辞典。
- 在阅读器中点击文本查词，在辞典页搜索，或从其他 Android App 选中文本查词。
- 点击释义中的生词进行递归查询。
- 注入自定义 CSS 样式。
- 使用在线或本地单词音频。

### 标注与统计

- 阅读时添加五色高亮标注，并随时进行跳转。
- 阅读数据统计，包括阅读的字符数、时长、阅读速度，可在阅读时实时显示。

### Anki 制卡

- 通过 AnkiDroid 或 AnkiConnect 制卡。
- 使用 [Lapis](https://github.com/donkuri/lapis) 兼容字段，支持重复检查和媒体导出。

### 有声书跟读

- 将有声书字幕文件匹配到书籍以高亮当前句子。
- 跟随高亮自动翻页。
- 控制播放速度、跳转动作和 Android 系统媒体控制。
- **Push to Continue（本 fork）：** VN 模式下，当前屏幕音频结束后暂停，点击后再继续。

### 数据同步与迁移

- 通过 Google Drive 同步阅读进度、统计和书籍，兼容 ッツ Reader。
- 导入或导出 ッツ bookdata，并使用 `.hoshi` 归档备份或恢复书籍和辞典，兼容 Hoshi Reader iOS。

## 为什么从 Yomitan + ッツ Reader 迁移到 Hoshi

- **一个 App 替代多段拼接流程：** EPUB 阅读、Yomitan 查词、Anki 制卡、本地音频和有声书跟读都在 Hoshi 内完成。
- **更快的导入速度和查词速度：** 底层使用 C++ hoshidicts，辞典导入速度提升约百倍，并支持批量导入 Yomitan 辞典；查词路径也做了大量性能优化，在低性能设备和墨水屏设备上尤为明显。
- **更好的 Android 阅读体验：** 自定义阅读主题、墨水屏显示优化、低性能设备表现和音量键翻页都作为核心阅读体验处理。
- **标注与书架管理：** 句子高亮和书签可以保存之后想回看的段落；自定义书架和文件夹递归导入让本地书籍整理更灵活。
- **更顺滑的弹窗与本地音频流程：** 查词弹窗可滑动关闭，本地单词音频可以直接用于阅读和制卡，不需要额外配置 AnkiConnect Android。
- **直接对接 AnkiDroid 的制卡流程：** Hoshi 可以不经过 AnkiConnect Android，直接与 AnkiDroid 交互完成制卡，让常见 Android 制卡路径更简单。
- **更顺畅的有声书工作流：** Hoshi 简化有声书配置，并提供更好的有声书音频进度和播放控制；制卡时还能自动把命中的音频片段扩展到完整句子，不需要手动拼接字幕行。
- **同步与迁移路径：** Google Drive 可同步阅读进度、统计和书籍，并兼容 ッツ；还支持 `.hoshi` iOS 备份恢复和 ッツ bookdata 导入/导出。

## 为什么在 EPUB 阅读场景选择 Hoshi 而不是 jidoujisho

- **阅读场景优先：** Hoshi 把日语 EPUB 阅读、查词和制卡收进一条专注流程；jidoujisho 的视频、漫画、网页和跨媒体工具对只想阅读的用户偏重。
- **可靠支持核心 Yomitan 辞典：** Hoshi 支持 term、frequency、pitch 三类辞典；jidoujisho 对结构化释义内容和辞典媒体渲染的支持并不可靠。
- **专门的 C++ 辞典引擎：** 原生 hoshidicts 让导入更快、查词响应更快、辞典占用更紧凑，并为低内存设备提供 Low Memory Usage Mode。
- **释义内递归查词：** 直接点击辞典释义里的生词即可打开嵌套查词，不需要复制文本、重新搜索或离开当前上下文。
- **EPUB 阅读器体验：** Hoshi 内置可自定义的 EPUB 阅读器，并把标注和阅读统计接入主阅读流程；jidoujisho 内嵌的 ッツ Reader 版本偏旧，实际会遇到 EPUB 导入失败或打开报错。
- **墨水屏设备支持：** UI 高对比度、辞典弹窗专用 CSS，以及查词和有声书高亮使用下划线式显示，提升墨水屏可读性，同时兼顾低性能设备。
- **EPUB 有声书跟读：** Hoshi 会把有声书字幕对齐到书籍文本，用于句子高亮、自动翻页、播放控制和句子音频制卡；jidoujisho 没有面向 EPUB 阅读的等价书籍对齐有声书工作流。
- **多设备同步与迁移：** Google Drive 同步兼容 ッツ Reader，并支持跨设备导入、导出和备份恢复；jidoujisho 没有对应的同步或备份迁移路径。

## 下载

从本仓库最近一次成功的 [GitHub Actions](https://github.com/sommepo/hoshi-reader-vn-pause/actions) 运行下载 debug APK（产物名 `hoshi-debug-apk`）。它会作为 **Hoshi Debug** 安装在官方 Hoshi 旁边。

若不想要 Push to Continue，请使用[上游 Releases](https://github.com/HuangAntimony/Hoshi-Reader-Android/releases/latest) 中的官方 App。

需要 Android 8.0 或更高版本。

## 使用指南

App 交互指南可以参考这篇 Hoshi Reader iOS 使用文档：[Hoshi Reader 使用文档](https://my.feishu.cn/wiki/SXzUw9F6AiPw99kdzwac5Cv8n0f)。

日语学习方法可以参考这篇基于二语习得理论的指南：[基于二语习得理论的日语学习指南](https://my.feishu.cn/wiki/YeOSwsG7giLuQxkcDFscUXVZn2f)。

## 开发状态

本 fork 跟随上游 Hoshi Reader Android，并增加 Push to Continue。见 [docs/CHANGELOG.md](docs/CHANGELOG.md)。

## 功能请求

请不要把 Push to Continue 相关问题发到上游 Hoshi。官方 App 请走 [上游仓库](https://github.com/HuangAntimony/Hoshi-Reader-Android) 和 [Hoshi Reader iOS](https://github.com/Manhhao/Hoshi-Reader)。

## 隐私与数据

Hoshi Reader Android 会把导入的书籍、辞典、字体、有声书数据、阅读进度、高亮、统计和设置保存在 App 本地存储中。

Google Drive 同步使用由用户配置的 Google Cloud OAuth 设备代码流程。Anki 制卡会与 AnkiDroid 或已配置的 AnkiConnect 地址通信。更新检查会读取 GitHub Release 元数据。

## 鸣谢

Hoshi Reader Android 基于以下生态：

- [Hoshi Reader iOS](https://github.com/Manhhao/Hoshi-Reader)：参考实现。
- [hoshidicts](https://github.com/Manhhao/hoshidicts) 及 [hoshidicts-kotlin-bridge](https://github.com/Manhhao/hoshidicts-kotlin-bridge)：Yomitan 辞典支持。
- [Yomitan](https://github.com/yomidevs/yomitan)：辞典格式和查词体验参考。
- [AnkiDroid](https://github.com/ankidroid/Anki-Android)：Android 制卡集成。
- [Ankiconnect Android](https://github.com/KamWithK/AnkiconnectAndroid)：本地音频行为和 AnkiDroid 重复范围 / checksum 查询参考。
- [ッツ Ebook Reader](https://github.com/ttu-ttu/ebook-reader)：阅读器、统计和同步兼容性参考。

## 许可证

本项目基于 GNU General Public License v3.0 发布。详情见 [LICENSE](LICENSE)。

如果官方 Hoshi Reader 对你有帮助，请给 [HuangAntimony/Hoshi-Reader-Android](https://github.com/HuangAntimony/Hoshi-Reader-Android) 点 Star。
