# DocuMaker: Document Creator

An offline-first Android document workspace with a rich-text editor, templates, local document library and multi-format export.

## Included in this foundation
- Compose-based document library with search, sort, star and template creation.
- WebView rich-text editing surface with formatting, insert tools, outline, find/replace and word count.
- Local SQLite persistence with autosave.
- Import for TXT, HTML and basic DOCX text extraction.
- Export to DOCX, ODT, RTF, TXT, HTML and EPUB; PDF output uses Android's print pipeline.
- Page setup, document details, duplicate and share flows.

## Build
Open this folder in Android Studio (JDK 17, Android SDK 35) and allow Gradle sync. The project uses Gradle 8.9 / Android Gradle Plugin 8.7.3.

## Important scope notes
This project is a new implementation inspired by common document-editor workflows. It does not use Google Docs source code, branding or assets. The first version is local-first; real-time multi-user collaboration, cloud sync, advanced PDF text editing/OCR and track-changes review require additional services and dedicated implementation.