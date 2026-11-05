# DocuMaker: Document Creator

An offline-first Android document editor inspired by the familiar mobile document workflow: recent files, grid/list browsing, page editing, formatting drawers, insert tools, overflow actions and local export.

## Included in this foundation
- Compose-based recent-documents library with search, sort, grid/list browsing, document actions and templates.
- Page-oriented rich-text editing surface with formatting, paragraph spacing, insert tools, outline, find/replace and word count.
- Local SQLite persistence with autosave.
- Import for TXT, HTML and basic DOCX text extraction.
- Export to DOCX, ODT, RTF, TXT, HTML and EPUB; PDF output uses Android's print pipeline.
- Page setup, document details, duplicate and share flows.

## Build
Open this folder in Android Studio (JDK 17, Android SDK 35) and allow Gradle sync. The project uses Gradle 8.9 / Android Gradle Plugin 8.7.3.

## Important scope notes
This project is a new implementation inspired by common document-editor workflows. It does not use Google Docs source code, branding or assets. The first version is local-first; real-time multi-user collaboration, cloud sync, advanced PDF text editing/OCR and track-changes review require additional services and dedicated implementation.

## UI reference
The screen recording supplied for this project is used as a workflow reference for the document library, editor toolbar, formatting panel, insert panel and overflow menu. DocuMaker is implemented independently with its own code and branding; it does not use Google Docs source code, logos or proprietary assets.
