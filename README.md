<div align="center">

# Hoshi Reader Android — Push to Continue

![Platform](https://img.shields.io/badge/platform-Android-lightgrey)
![License](https://img.shields.io/badge/license-GPLv3-blue)
![Fork](https://img.shields.io/badge/fork-unofficial-orange)

**English** | [简体中文](README.zh-CN.md)

</div>

> **This is an unofficial public fork. It is not official Hoshi Reader.**
>
> Upstream: [HuangAntimony/Hoshi-Reader-Android](https://github.com/HuangAntimony/Hoshi-Reader-Android)
>
> Do not send bug reports or feature requests for **Push to Continue** to the upstream project. The original author did not make this fork and is not responsible for it.

## What this fork is

An unofficial fork of Hoshi Reader Android. It keeps the upstream Japanese EPUB reader (Yomitan lookup, Anki mining, Sasayaki audiobook read-along, VN mode, e-ink) and adds one extra reading mode: **Push to Continue**.

The debug APK installs **next to** official Hoshi as **Hoshi Debug** (`moe.antimony.hoshi.debug`). It does not replace the official app or share its library.

## What Push to Continue does

In **Visual Novel** mode with a Sasayaki audiobook (audio + SRT match):

1. Audio plays through every sentence on the **current VN screen**.
2. When that screen’s last audiobook cue ends playback **pauses**.
3. The screen stays up. Tap a **blank area** or press play to start the next screen.
4. If you have dictionary tapped to look up a word, you can safely tap a blank area to exit the lookup without pausing or playing audio.

It is **on by default**. Turn it off in Sasayaki settings if you want normal uninterrupted read-along. Dictionary taps still look up words. It only applies in VN mode; paginated and continuous reading are unchanged.

## Audiobook mode and the Audiobooks tab

This fork also has a line-by-line listening view for books with a Sasayaki audiobook (audio + SRT match):

- **Audiobook mode** (Reader Appearance → Mode → Listen) shows the chapter as a column of lines. The line being spoken is highlighted in the middle of the screen, the other lines are greyed out, and the view glides to each new line.
- Large buttons replay the line, go to the previous or next line, play or pause, and open the chapter list. Tapping a word looks it up and pauses the audio.
- The **Audiobooks** tab lists every book that has audio and searches book titles, chapter titles, ID numbers and the spoken text. Tapping a chapter opens it in Audiobook mode and starts playing.

Books that work best with it:

- One EPUB per collection, with one XHTML file and one table-of-contents entry per dialogue. Put the ID in the entry title, for example `N3-042 〜てしまう`; searching `42`, `n3 42` or `てしまう` then finds it.
- One paragraph per spoken line.
- One audio file (MP3, M4B or Opus) and one SRT for the whole book, added from the Sasayaki menu. Each SRT cue should hold the text of one line, in book order.

<div align="center">

<table>
  <tr>
    <td><img src="docs/images/readme/bookshelf.jpg" alt="Bookshelf" width="100%"></td>
    <td><img src="docs/images/readme/reader-lookup-popup.jpg" alt="Reader lookup popup" width="100%"></td>
    <td><img src="docs/images/readme/reader-dark-theme.jpg" alt="Dark reader" width="100%"></td>
    <td><img src="docs/images/readme/reader-eink-mode.jpg" alt="E-ink reader" width="100%"></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/sasayaki-audiobook.jpg" alt="Sasayaki audiobook" width="100%"></td>
    <td><img src="docs/images/readme/reader-statistics.jpg" alt="Reader statistics" width="100%"></td>
    <td><img src="docs/images/readme/reader-highlights.jpg" alt="Reader highlights" width="100%"></td>
    <td><img src="docs/images/readme/dictionary-recursive-lookup.jpg" alt="Dictionary recursive lookup" width="100%"></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/reader-appearance-settings.jpg" alt="Reader appearance settings" width="100%"></td>
    <td><img src="docs/images/readme/dictionary-management.jpg" alt="Dictionary management" width="100%"></td>
    <td><img src="docs/images/readme/anki-card-settings.jpg" alt="Anki card settings" width="100%"></td>
    <td><img src="docs/images/readme/sync-settings.jpg" alt="Sync settings" width="100%"></td>
  </tr>
</table>

</div>

## Features

### Bookshelf

- Import EPUBs individually, in batches, or recursively from folders, and keep reading progress visible from the bookshelf.
- Organize books with custom shelves.
- Export EPUBs or pull remote synced books back into the local library.

### Reading

- Read Japanese books in vertical or horizontal text, with paginated or continuous scrolling.
- Customize themes, fonts, paragraph spacing, and reader controls, including custom reader themes.
- Use immersive focus mode, volume-key page turning, and e-ink display options.
- Open reader images in fullscreen with zoom, copy, save, and share actions.

### Lookup

- Import, download, update, and manage Yomitan dictionaries.
- Tap text in the reader, search from the Dictionary tab, or look up selected text from other Android apps.
- Tap unknown words inside definitions for recursive lookup.
- Inject custom CSS styles.
- Use online or local word audio.

### Highlights And Statistics

- Add five-color highlights while reading and jump to them at any time.
- Track reading statistics, including characters read, time spent, and reading speed, with live display while reading.

### Anki Card Mining

- Create cards through AnkiDroid or AnkiConnect.
- Use [Lapis](https://github.com/donkuri/lapis)-compatible fields, duplicate checks, and media export.

### Audiobook Read-Along

- Match audiobook subtitle files to book text to highlight the current sentence.
- Follow highlights with automatic page turning.
- Control playback speed, skip actions, and Android media controls.
- **Push to Continue (this fork):** in VN mode, pause when the current screen’s audio ends and wait for a tap before continuing.
- **Audiobook mode (this fork):** a centred, line-by-line listening view with large controls, plus a searchable Audiobooks tab.

### Data Sync And Migration

- Sync reading progress, statistics, and books through Google Drive, compatible with ッツ Reader.
- Import or export ッツ bookdata, and back up or restore books and dictionaries with `.hoshi` archives compatible with Hoshi Reader iOS.

## Why Migrate From Yomitan + ッツ Reader

- **One app instead of a stitched setup:** EPUB reading, Yomitan lookup, Anki mining, local audio, and audiobook read-along all live inside Hoshi.
- **Faster import and lookup speed:** native C++ hoshidicts makes dictionary import roughly 100x faster, supports batch importing Yomitan dictionaries, and heavily optimizes lookup speed, especially on low-end and e-ink devices.
- **Better Android reading experience:** custom reader themes, e-ink display tuning, low-end-device performance, and volume-key paging are treated as first-class reader features.
- **Annotations and bookshelf control:** sentence highlights and bookmarks make it easy to save passages you want to revisit, while custom bookshelves and recursive folder import keep local libraries organized.
- **Smoother popups and local audio:** dictionary popups can be dismissed with a swipe, and local word audio can be used directly in reading and card creation without configuring AnkiConnect Android.
- **Direct AnkiDroid card mining:** Hoshi can create cards through AnkiDroid without AnkiConnect Android, keeping the common Android mining path simpler.
- **Better audiobook workflow:** Hoshi streamlines audiobook setup and provides better audiobook audio progress and playback controls; when creating cards, it can also expand captured audio to the full sentence instead of making you stitch subtitle lines manually.
- **Sync and migration path:** Google Drive sync covers progress, statistics, and books with ッツ compatibility, plus `.hoshi` iOS backup restore and ッツ bookdata import/export.

## Why Choose Hoshi Over jidoujisho For EPUB Reading

- **Reading-focused design:** Hoshi keeps Japanese EPUB reading, lookup, and card mining in one focused flow; jidoujisho's video, manga, web, and cross-media tooling can feel heavy for reading-only workflows.
- **Reliable support for core Yomitan dictionaries:** Hoshi supports term, frequency, and pitch dictionaries; jidoujisho is unreliable with structured glossary content and dictionary media rendering.
- **Dedicated C++ dictionary engine:** native hoshidicts keeps imports fast, lookups responsive, and dictionary storage compact, with Low Memory Usage Mode for low-memory devices.
- **Recursive lookup inside definitions:** tap unknown words directly inside dictionary definitions to open nested lookups without copying text, starting a new search, or leaving the current context.
- **Maintained EPUB reader:** Hoshi ships a customizable EPUB reader with annotations and reading statistics in the core flow; jidoujisho bundles an older ッツ Reader build that can fail to import or open real-world EPUBs.
- **E-ink device support:** high-contrast UI, dedicated popup definition CSS, and underline-style lookup and audiobook highlights improve e-ink readability, with solid support for low-end devices.
- **EPUB audiobook read-along:** Hoshi aligns audiobook subtitles with the book text for sentence highlighting, auto page turning, playback control, and sentence-audio mining; jidoujisho does not provide an equivalent book-aligned audiobook workflow for EPUB reading.
- **Multi-device sync and migration:** Google Drive sync is compatible with ッツ Reader, and Hoshi supports cross-device import, export, and backup restore; jidoujisho does not provide an equivalent sync or backup migration path.

## Download

Install the debug APK from [Releases](https://github.com/sommepo/hoshi-reader-vn-pause/releases/latest). It sideloads as **Hoshi Debug** next to official Hoshi.

If you do not want Push to Continue, use the official app from [upstream releases](https://github.com/HuangAntimony/Hoshi-Reader-Android/releases/latest).

Requires Android 8.0 or later.

## Development Status

This fork tracks upstream Hoshi Reader Android and adds Push to Continue. See [docs/CHANGELOG.md](docs/CHANGELOG.md).

## Feature Requests

Do not open Push to Continue issues on upstream Hoshi. For the official app, follow [upstream](https://github.com/HuangAntimony/Hoshi-Reader-Android) and [Hoshi Reader iOS](https://github.com/Manhhao/Hoshi-Reader).

## Privacy And Data

Hoshi Reader Android stores imported books, dictionaries, fonts, audiobook data, reading progress, highlights, statistics, and settings locally in app storage.

Google Drive sync uses a user-configured Google Cloud OAuth device-code flow. Anki card mining talks to AnkiDroid or the configured AnkiConnect endpoint. Update checks read GitHub release metadata.

## Attribution

Hoshi Reader Android builds on this ecosystem:

- [Hoshi Reader iOS](https://github.com/Manhhao/Hoshi-Reader) as the reference implementation.
- [hoshidicts](https://github.com/Manhhao/hoshidicts) and [hoshidicts-kotlin-bridge](https://github.com/Manhhao/hoshidicts-kotlin-bridge) for Yomitan dictionary support.
- [Yomitan](https://github.com/yomidevs/yomitan) for dictionary format and lookup inspiration.
- [AnkiDroid](https://github.com/ankidroid/Anki-Android) for Android card creation integration.
- [Ankiconnect Android](https://github.com/KamWithK/AnkiconnectAndroid) for local audio behavior and AnkiDroid duplicate scope/checksum query references.
- [ッツ Ebook Reader](https://github.com/ttu-ttu/ebook-reader) for reader, statistics, and sync compatibility references.

## License

Distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

If official Hoshi Reader helps your reading workflow, star [HuangAntimony/Hoshi-Reader-Android](https://github.com/HuangAntimony/Hoshi-Reader-Android).
