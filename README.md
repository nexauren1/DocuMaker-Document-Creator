# DocuMaker: Document Creator

An offline-first Android document editor inspired by familiar mobile document workflows: recent files, grid/list browsing, page editing, formatting drawers, insert tools, version history, overflow actions and local export.

## Included in this foundation
- Compose-based recent-documents library with search, sort, grid/list browsing, document actions and templates.
- Page-oriented rich-text editing surface with formatting, paragraph spacing, insert tools, outline, find/replace and word count.
- Local SQLite persistence with autosave, persistent starred/offline flags and throttled document version snapshots.
- Import for TXT, MD, CSV, HTML, DOCX, ODT, EPUB and RTF; images can be inserted as document content. Imported HTML and pasted HTML are sanitized before editing.
- Export to DOCX (basic inline text styling), ODT, RTF, TXT, HTML and EPUB; PDF output uses Android's print pipeline.
- Page setup, document details, duplicate and share flows.

## Build
Open this folder in Android Studio (JDK 17, Android SDK 35) and allow Gradle sync. The project uses Gradle 8.9 / Android Gradle Plugin 8.7.3.

## Important scope notes
This project is a new implementation inspired by common document-editor workflows. It does not use Google Docs source code, branding or assets. The first version is local-first; real-time multi-user collaboration, cloud sync, advanced PDF text editing/OCR and track-changes review require additional services and dedicated implementation.

## UI reference
The screen recording supplied for this project is used as a workflow reference for the document library, editor toolbar, formatting panel, insert panel and overflow menu. DocuMaker is implemented independently with its own code and branding; it does not use Google Docs source code, logos or proprietary assets.

- Search matches document titles and saved document content.
- Version history stores the initial version and periodic snapshots (up to 40 versions per document).
- External document links open outside the editor so they do not replace the current editing session.

## Known limitations
- PDF files cannot yet be imported as editable text; PDF export/printing is supported.
- ODT and EPUB exports preserve document text and structure at a basic level; advanced page layout, embedded media and every Word formatting feature are not fully round-tripped.
- Shared editing, comments as collaborative threads, cloud sync, OCR and automatic live page numbering need additional implementation.
