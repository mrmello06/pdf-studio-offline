# PDF Studio (100% Offline Android App)

A modern, offline Android application for PDF conversion, manipulation, and organization — bringing the capabilities of online tools like iLovePDF entirely on-device with zero internet data usage and zero file tracking.

## 🌟 Key Features

### 📄 Convert PDFs
- **PDF to Word**: Converts PDF pages into editable `.docx` (Microsoft Word OpenXML standard format).
- **PDF to PowerPoint**: Transforms pages into `.pptx` presentation slides.
- **PDF to Excel**: Extracts tabular structures into RFC 4180-compliant `.csv` spreadsheets.
- **PDF to JPG**: Hardware-accelerated rendering into high-resolution JPG images with "All Pages" ZIP package or single-page extraction.
- **Word to PDF**: Converts `.docx` documents into vector PDF pages with typography formatting.
- **PowerPoint to PDF**: Formats `.pptx` presentation slides onto landscape PDF pages.
- **Excel to PDF**: Auto-fits spreadsheet tables with styled headers and borders onto printable pages.
- **JPG to PDF**: Interactive preview allowing you to rotate individual photos, swap/reorder pages, and delete photos before compiling.
- **HTML to PDF**: Multi-page offline article/markup converter.

### 🛠️ Edit & Manage PDFs
- **Organize PDF**: Visual scrollable high-definition page preview feed with instant ⏫ Top / ▲ Up / ▼ Down / ⏬ Bottom controls, direct position jumping (e.g. move page 3 to 1), and one-tap reverse.
- **Rotate PDF**: Rotates document pages (90°, 180°, 270°) with immediate live preview.
- **Merge PDF**: Fast offline combination of multiple PDFs into one unified document.
- **Split PDF**: Extracts custom page ranges (From Page X to Page Y).
- **Compress PDF**: Downsamples and recompresses PDF stream to significantly reduce file size without corruption.
- **Watermark PDF**: Stamps diagonal semi-transparent custom watermark text across pages.
- **Page Numbers**: Adds centered "Page X of Y" numbering at the bottom of each page.
- **Crop PDF**: Precision crop margins to trim white borders and scanning margins.
- **Edit / Annotate PDF**: Stamps custom text annotations and approval stamps onto documents.

## 🔒 Privacy & Permissions
- **100% Offline Engine**: All document processing is performed locally on your device via PDFBox-Android and Android OS graphics engines.
- **Minimal Permissions**: No Camera permission required, no "All Files Access" (`MANAGE_EXTERNAL_STORAGE`) required. Works cleanly via Android Storage Access Framework (SAF).

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3)
- **PDF Engine**: PDFBox-Android (`com.tom-roush:pdfbox-android:2.0.27.0`) + Android native `PdfRenderer` & `PdfDocument`
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)

## 🚀 Building the Project
1. Open this folder in **Android Studio**.
2. Let Gradle sync project dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated `.apk` onto your Android device.
