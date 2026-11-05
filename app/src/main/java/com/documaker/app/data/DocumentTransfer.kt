package com.documaker.app.data

import android.content.Context
import android.net.Uri
import android.text.Html
import android.text.Spanned
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocumentImporter {
    data class Imported(val title: String, val html: String)

    fun read(context: Context, uri: Uri, displayName: String?): Imported {
        val name = displayName?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Imported document" }
            ?: "Imported document"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open the selected document")
        val extension = (displayName ?: "").substringAfterLast('.', "").lowercase()
        val text = when (extension) {
            "docx" -> docxToHtml(bytes)
            "rtf" -> rtfToHtml(bytes.toString(StandardCharsets.UTF_8))
            "html", "htm" -> return Imported(name, bytes.toString(StandardCharsets.UTF_8))
            "txt", "md", "csv" -> "<p>${escape(bytes.toString(StandardCharsets.UTF_8)).replace("\r?\n".toRegex(), "</p><p>")}</p>"
            "pdf" -> error("PDF import as editable text is not available yet. You can still open and print PDFs using your device's PDF viewer.")
            else -> {
                val guessed = context.contentResolver.getType(uri).orEmpty()
                if (guessed.contains("html")) return Imported(name, bytes.toString(StandardCharsets.UTF_8))
                "<p>${escape(bytes.toString(StandardCharsets.UTF_8)).replace("\r?\n".toRegex(), "</p><p>")}</p>"
            }
        }
        return Imported(name, text)
    }

    private fun docxToHtml(bytes: ByteArray): String {
        val xml = ZipInputStream(bytes.inputStream()).use { zip ->
            var found: String? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "word/document.xml") {
                    found = zip.readBytes().toString(StandardCharsets.UTF_8)
                    break
                }
            }
            found ?: error("This DOCX file does not contain a readable document body")
        }
        val paragraphs = Regex("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { match ->
                val runs = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(match.groupValues[1]).joinToString("") { unescapeXml(it.groupValues[1]) }
                "<p>${escape(runs)}</p>"
            }.toList()
        return paragraphs.joinToString("").ifBlank { "<p></p>" }
    }

    private fun rtfToHtml(source: String): String {
        val plain = source.replace(Regex("""\\par[d]?"""), "\n")
            .replace(Regex("""\\'[0-9a-fA-F]{2}"""), " ")
            .replace(Regex("""\\[a-zA-Z]+-?\d* ?"""), "")
            .replace("[{}]".toRegex(), "")
        return plain.lines().joinToString("") { "<p>${escape(it)}</p>" }
    }

    private fun unescapeXml(s: String) = s.replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
}

object DocumentExporter {
    fun write(context: Context, uri: Uri, format: String, title: String, html: String, plainText: String) {
        val bytes = when (format.lowercase()) {
            "txt" -> plainText.toByteArray(StandardCharsets.UTF_8)
            "html" -> htmlDocument(title, html).toByteArray(StandardCharsets.UTF_8)
            "rtf" -> toRtf(plainText).toByteArray(StandardCharsets.UTF_8)
            "docx" -> toDocx(title, plainText)
            "odt" -> toOdt(title, plainText)
            "epub" -> toEpub(title, plainText)
            else -> htmlDocument(title, html).toByteArray(StandardCharsets.UTF_8)
        }
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
            ?: error("Could not create the export file")
    }

    fun htmlDocument(title: String, body: String) = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${xml(title)}</title><style>body{font-family:Arial,sans-serif;line-height:1.55;max-width:850px;margin:40px auto;padding:0 24px;color:#202333}table{border-collapse:collapse;width:100%}td,th{border:1px solid #aaa;padding:8px}img{max-width:100%}</style></head><body>$body</body></html>"""

    private fun toRtf(text: String): String {
        val encoded = text.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
            .replace("\r\n", "\n").replace("\n", "\\par\n")
        return "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\f0\\fs22 $encoded}"
    }

    private fun toDocx(title: String, text: String): ByteArray {
        val paragraphs = text.split('\n').joinToString("") { line ->
            "<w:p><w:r><w:t xml:space=\"preserve\">${xml(line)}</w:t></w:r></w:p>"
        }
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paragraphs<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>"""
        return zipBytes {
            add("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            add("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            add("word/document.xml", document)
        }
    }

    private fun toOdt(title: String, text: String): ByteArray {
        val content = text.split('\n').joinToString("") { "<text:p>${xml(it)}</text:p>" }
        return zipBytes(mimetype = "application/vnd.oasis.opendocument.text") {
            add("content.xml", """<?xml version="1.0" encoding="UTF-8"?><office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2"><office:body><office:text>$content</office:text></office:body></office:document-content>""")
            add("META-INF/manifest.xml", """<?xml version="1.0" encoding="UTF-8"?><manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2"><manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/><manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/><manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/></manifest:manifest>""")
            add("meta.xml", """<?xml version="1.0" encoding="UTF-8"?><office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2"><office:meta><dc:title>${xml(title)}</dc:title></office:meta></office:document-meta>""")
        }
    }

    private fun toEpub(title: String, text: String): ByteArray {
        val paragraphs = text.split('\n').joinToString("") { "<p>${xml(it)}</p>" }
        return zipBytes(mimetype = "application/epub+zip") {
            add("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            add("OEBPS/content.xhtml", """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(title)}</title></head><body><h1>${xml(title)}</h1>$paragraphs</body></html>""")
            add("OEBPS/content.opf", """<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="uid">documaker-${title.hashCode()}</dc:identifier><dc:title>${xml(title)}</dc:title><dc:language>en</dc:language></metadata><manifest><item id="content" href="content.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="content"/></spine></package>""")
        }
    }

    private class ZipBuilder(private val out: ZipOutputStream) {
        fun add(name: String, content: String) {
            out.putNextEntry(ZipEntry(name)); out.write(content.toByteArray(StandardCharsets.UTF_8)); out.closeEntry()
        }
    }

    private fun zipBytes(mimetype: String? = null, build: ZipBuilder.() -> Unit): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            if (mimetype != null) {
                val bytes = mimetype.toByteArray(StandardCharsets.UTF_8)
                val entry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED; size = bytes.size.toLong(); compressedSize = bytes.size.toLong()
                    crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry()
            }
            ZipBuilder(zip).apply(build)
        }
        return output.toByteArray()
    }

    private fun xml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}