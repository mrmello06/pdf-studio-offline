# 📱 PDF Studio Offline

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="PDF Studio Logo" width="100" height="100" />
</p>

<p align="center">
  <strong>Complete iLovePDF-style toolkit running 100% offline on your Android device.</strong>
</p>

<p align="center">
  <a href="https://github.com/mrmello06/pdf-studio-offline/releases/download/v1.0.0/PDFStudio-Offline.apk">
    <img src="https://img.shields.io/badge/📥%20Download-Direct%20APK%20(v1.0.0)-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Mode-100%25%20Offline-10B981?style=for-the-badge&logo=aer-lingus&logoColor=white" alt="Offline" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

## 📖 Overview

Most online PDF utilities (such as iLovePDF, Smallpdf, etc.) require you to upload your sensitive contracts, financial statements, and personal IDs to third-party cloud servers over the internet.

**PDF Studio Offline** replicates all those everyday PDF manipulation workflows entirely **on-device**:
- 🔒 **Zero Internet Access**: No telemetry, no cloud APIs, no background tracking. Your documents never leave your phone.
- ⚡ **Instant Processing**: No upload or download queues — operations happen locally at native speeds.
- 🛡️ **Minimal Permissions**: No invasive "All Files Access" (`MANAGE_EXTERNAL_STORAGE`) and zero Camera permission required. Operates safely through Android's native **Storage Access Framework (SAF)**.

---

## ✨ Features & Tool Catalog

### 📄 1. PDF to Other Formats

| Tool | Input | Output | Description |
|---|---|---|---|
| **PDF to Word** | `.pdf` | `.docx` | Extracts document paragraphs and compiles standard Microsoft Word OpenXML packages (`[Content_Types].xml`, `styles.xml`, `document.xml.rels`) without corruption. |
| **PDF to PowerPoint** | `.pdf` | `.pptx` | Extracts content per page and formats into dynamic `.pptx` slides with titled layouts. |
| **PDF to Excel** | `.pdf` | `.csv` | Scans text streams for tabular patterns and formats into clean, RFC 4180-compliant spreadsheets compatible with Excel and Google Sheets. |
| **PDF to JPG** | `.pdf` | `.jpg` / `.zip` | Hardware-accelerated rendering. Lets you choose between converting **All Pages** (bundled into an instant ZIP archive + gallery) or selecting a **Single Page**. |

---

### 🔄 2. Other Formats to PDF

| Tool | Input | Output | Description |
|---|---|---|---|
| **Word to PDF** | `.docx` | `.pdf` | Unpacks Word OpenXML markup, parses `<w:p>` and `<w:t>` nodes, and renders them onto vector A4 PDF pages with word-wrapping. |
| **PowerPoint to PDF** | `.pptx` | `.pdf` | Extracts slide text runs and renders each presentation slide onto landscape PDF pages with header bars. |
| **Excel to PDF** | `.xlsx` | `.pdf` | Parses spreadsheet sheets, shared strings, and row cells, compiling a paginated PDF with grid lines and styled headers. |
| **JPG to PDF** | Images | `.pdf` | **Interactive Visual Preview**: Scroll through selected photos, rotate individual images by 90°, swap/reorder page positions, and delete unneeded photos before generating. |
| **HTML to PDF** | `.html` / `.htm` | `.pdf` | Formats offline HTML markup into clean, printable A4 PDF documents with automatic multi-page pagination. |

---

### 🛠️ 3. Basic Editing & File Management

| Tool | Feature Details |
|---|---|
| **🗂️ Organize PDF** | **Full-Screen Visual Reorder Studio**: Displays all document pages as high-definition, scrollable preview cards (360dp tall, 900px render scale). Easily move pages with **⏫ Move to Top**, **▲ Up**, **▼ Down**, **⏬ Move to Bottom**, enter a target page number to **Jump directly (e.g. Page 3 to 1)**, or use **⇄ Reverse All**. Tap any preview to inspect in full-screen zoom. |
| **🔄 Rotate PDF** | Rotates document pages (+90°, +180°, +270°) with immediate real-time thumbnail preview feedback before saving. |
| **🔀 Merge PDF** | Sequentially combines multiple PDF documents into one single document using PDFBox stream mergers. |
| **✂️ Split PDF** | Extracts custom page ranges (e.g. Page 1 to 5) into a new standalone PDF document. |
| **🗜️ Compress PDF** | Downsamples and recompresses PDF bitmap streams at 70% quality to significantly reduce file size without document corruption. |
| **💧 Watermark PDF** | Stamps diagonal semi-transparent text annotations (e.g. `CONFIDENTIAL`, `DRAFT`) with 35% opacity across every page. |
| **🔢 Page Numbers** | Automatically calculates total pages and centers custom `Page X of Y` headers/footers at the bottom of each page. |
| **📐 Crop PDF** | Trims document margins and eliminates scanning borders by adjusting the PDF `/CropBox` geometry. |
| **✏️ Edit PDF** | Stamps custom vector notes, approval annotations, and date stamps directly onto document pages. |

---

## 🏗️ Architecture & Project Structure

The project is built entirely in **Modern Kotlin** using **Jetpack Compose** (100% declarative UI, zero legacy XML layouts):

```
app/src/main/java/com/pdfmaster/app/
│
├── data/
│   ├── OpenXmlHelper.kt          # OpenXML ZIP packager for DOCX, PPTX, and XLSX
│   ├── PdfOperationsHelper.kt    # Core offline engine (PDFBox + Android PdfRenderer)
│   └── PdfToolData.kt            # Catalog of all tools, capabilities, and categories
│
├── ui/
│   ├── components/
│   │   ├── ActiveToolProcessingDialog.kt  # Unified modal for tool options & processing
│   │   ├── JpgToPdfPreviewDialog.kt       # Interactive photo carousel (swap/rotate)
│   │   ├── OrganizePdfDialog.kt           # Full-screen visual reorder feed with big previews
│   │   └── ToolDetailBottomSheet.kt       # Bottom sheet showing tool capabilities & launch
│   │
│   ├── screens/
│   │   └── HomeScreen.kt         # Smooth memoized tool directory with search & filters
│   │
│   └── theme/                    # Material 3 typography, colors, and dynamic theme
│
├── MainActivity.kt               # Single Activity host with SAF lifecycle hooks
└── PDFApplication.kt             # Application init for PDFBox-Android resources
```

---

## 🔒 Security & Privacy Guarantees

- **No Network Permissions**: The app does **not** declare `<uses-permission android:name="android.permission.INTERNET" />`. It is mathematically impossible for data to leak or be transmitted.
- **No Camera Permissions**: Document image conversions work via gallery pickers and Storage Access Framework.
- **No All-Files-Access**: Avoids Google Play-restricted `MANAGE_EXTERNAL_STORAGE`. Only requests standard Android media permissions for selected images.
- **Private Sandbox Storage**: Output PDFs are saved to your device's standard `Documents/PDFStudio/` folder using Android's secure `FileProvider`.

---

## 🚀 Building & Installing

### Prerequisites
- **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer)
- **JDK**: Java 17 or higher
- **Android SDK**: Compile SDK `34`, Min SDK `24` (Android 7.0+)

### Quick Build Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/<your-username>/pdf-studio-offline.git
   cd pdf-studio-offline
   ```
2. **Open in Android Studio**:
   - Open Android Studio and choose **Open**, then select the project folder.
   - Wait for Gradle to finish indexing and syncing dependencies.
3. **Build the APK**:
   - In the menu bar, navigate to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Once completed, click **locate** in the popup to find `app-debug.apk`.
4. **Install on device**:
   - Transfer the APK to your phone or run directly via USB debugging by clicking **Run 'app'** (`Shift + F10`).

---

## 📦 Core Dependencies

| Library | Version | Purpose |
|---|---|---|
| **PDFBox-Android** | `2.0.27.0` | Comprehensive offline PDF document manipulation and OpenXML streaming |
| **Jetpack Compose BOM** | `2024.02.00` | Modern reactive UI foundation |
| **Material 3** | `1.2.0` | Material You theme, components, and animations |
| **Coil Compose** | `2.5.0` | Lightweight image loading for previews and thumbnails |
| **Kotlin Coroutines** | `1.7.3` | Multi-threaded background processing (`Dispatchers.IO`) |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
Feel free to fork, customize, and build upon it!
