package com.documaker.app.data

import android.content.Context
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocumentImporter {
    data class Imported(val title: String, val html: String)

    fun read(context: Context, uri: Uri, displayName: String?): Imported {
        val rawName = displayName?.substringAfterLast('/') ?: "Imported document"
        val name = rawName.substringBeforeLast('.', rawName).ifBlank { "Imported document" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= 25 * 1024 * 1024) { "The selected file is larger than 25 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Could not open the selected document")
        val extension = (displayName ?: "").substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val html = when {
            extension in listOf("html", "htm") || mime.contains("html") ->
                sanitizeHtml(bytes.toString(StandardCharsets.UTF_8))
            extension == "docx" || mime.contains("wordprocessingml") ->
                docxToHtml(bytes)
            extension == "odt" || mime.contains("opendocument.text") ->
                odtToHtml(bytes)
            extension == "epub" ->
                epubToHtml(bytes)
            extension == "rtf" || mime.contains("rtf") ->
                rtfToHtml(bytes.toString(StandardCharsets.UTF_8))
            extension in listOf("txt", "md", "csv") || mime.startsWith("text/") ->
                plainTextToHtml(bytes.toString(StandardCharsets.UTF_8))
            extension == "pdf" || mime == "application/pdf" ->
                error("PDF files can be printed or exported, but editable text extraction is not available yet. Open the PDF in a PDF reader or import a DOCX/TXT copy.")
            else -> error("Unsupported file type. Import DOCX, ODT, EPUB, RTF, HTML, TXT, MD or CSV.")
        }
        return Imported(name, html.ifBlank { "<p></p>" })
    }

    fun sanitizeHtml(source: String): String {
        var html = source
        html = html.replace(Regex("(?is)<(script|iframe|object|embed|form|input|button|textarea|select|meta|link)\\b[^>]*>.*?</\\1\\s*>"), "")
        html = html.replace(Regex("(?is)<(script|iframe|object|embed|form|input|button|textarea|select|meta|link)\\b[^>]*/?>"), "")
        html = html.replace(Regex("(?is)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)"), "")
        html = html.replace(Regex("""(?i)(href|src)\s*=\s*(['"])\s*javascript:[^'"]*\2"""), "$1=\"#\"")
        html = html.replace(Regex("(?is)<html[^>]*>|</html>|<head[^>]*>.*?</head>|<body[^>]*>|</body>"), "")
        return html.trim()
    }

    private fun plainTextToHtml(text: String): String =
        text.replace("\r\n", "\n").replace("\r", "\n")
            .split('\n').joinToString("") { "<p>${escape(it)}</p>" }

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
        val output = StringBuilder()
        val paragraphRegex = Regex("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        paragraphRegex.findAll(xml).forEach { paragraph ->
            val inner = paragraph.groupValues[1]
            val paragraphStyle = Regex("<w:pStyle[^>]*w:val=\"([^\"]+)\"").find(inner)?.groupValues?.get(1).orEmpty()
            val tag = when {
                paragraphStyle.contains("Title", true) -> "h1"
                paragraphStyle.contains("Heading1", true) -> "h1"
                paragraphStyle.contains("Heading2", true) -> "h2"
                paragraphStyle.contains("Heading3", true) -> "h3"
                else -> "p"
            }
            val runs = StringBuilder()
            val runRegex = Regex("<w:r(?:\\s[^>]*)?>(.*?)</w:r>", RegexOption.DOT_MATCHES_ALL)
            runRegex.findAll(inner).forEach { run ->
                val content = run.groupValues[1]
                var value = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(content).joinToString("") { unescapeXml(it.groupValues[1]) }
                if (content.contains("<w:tab")) value += "\t"
                if (content.contains("<w:br")) value += "<br>"
                if (value.isBlank()) return@forEach
                var styled = escape(value)
                if (Regex("<w:b(?:\\s[^>]*)?/>|<w:b>").containsMatchIn(content)) styled = "<b>$styled</b>"
                if (Regex("<w:i(?:\\s[^>]*)?/>|<w:i>").containsMatchIn(content)) styled = "<i>$styled</i>"
                if (Regex("<w:u(?:\\s[^>]*)?").containsMatchIn(content)) styled = "<u>$styled</u>"
                if (Regex("<w:strike(?:\\s[^>]*)?/>|<w:strike>").containsMatchIn(content)) styled = "<s>$styled</s>"
                runs.append(styled)
            }
            output.append("<").append(tag).append(">").append(runs).append("</").append(tag).append(">")
        }
        if (output.isEmpty()) {
            val text = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml).joinToString(" ") { unescapeXml(it.groupValues[1]) }
            output.append(plainTextToHtml(text))
        }
        return sanitizeHtml(output.toString())
    }

    private fun odtToHtml(bytes: ByteArray): String {
        val xml = ZipInputStream(bytes.inputStream()).use { zip ->
            var found: String? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "content.xml") {
                    found = zip.readBytes().toString(StandardCharsets.UTF_8)
                    break
                }
            }
            found ?: error("This ODT file does not contain content.xml")
        }
        val paragraphs = Regex("<text:p(?:\\s[^>]*)?>(.*?)</text:p>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { match ->
                val text = match.groupValues[1]
                    .replace(Regex("<text:line-break\\s*/>"), "<br>")
                    .replace(Regex("<text:tab\\s*/>"), "&#9;")
                "<p>${text.replace(Regex("<[^>]+>"), "").let(::unescapeXml).let(::escape)}</p>"
            }.joinToString("")
        return sanitizeHtml(paragraphs)
    }

    private fun epubToHtml(bytes: ByteArray): String {
        val documents = mutableListOf<Pair<String, String>>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true) || entry.name.endsWith(".htm", true)) {
                    documents += entry.name to zip.readBytes().toString(StandardCharsets.UTF_8)
                }
            }
        }
        if (documents.isEmpty()) error("No readable chapter was found in this EPUB file.")
        return documents.sortedBy { it.first }.joinToString("<hr>") { sanitizeHtml(it.second) }
    }

    private fun rtfToHtml(source: String): String {
        var plain = source
            .replace(Regex("""\\par[d]? ?"""), "\n")
            .replace(Regex("""\\line ?"""), "\n")
            .replace(Regex("""\\tab ?"""), "\t")
            .replace(Regex("""\\'[0-9a-fA-F]{2}"""), " ")
            .replace(Regex("""\\u(-?\d+)\??""")) { match ->
                val code = match.groupValues[1].toIntOrNull() ?: 32
                String(Character.toChars(if (code < 0) code + 65536 else code))
            }
            .replace(Regex("""\\[a-zA-Z]+-?\d* ?"""), "")
            .replace(Regex("""\\[*{}]"""), "")
            .replace("[{}]".toRegex(), "")
        plain = plain.replace("\r\n", "\n")
        return plainTextToHtml(plain)
    }

    private fun unescapeXml(s: String) = s.replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
}

object DocumentExporter {
    fun write(context: Context, uri: Uri, format: String, title: String, html: String, plainText: String) {
        val safeHtml = DocumentImporter.sanitizeHtml(html)
        val bytes = when (format.lowercase(Locale.ROOT)) {
            "txt" -> plainText.toByteArray(StandardCharsets.UTF_8)
            "html" -> htmlDocument(title, safeHtml).toByteArray(StandardCharsets.UTF_8)
            "rtf" -> toRtf(Html.fromHtml(safeHtml, Html.FROM_HTML_MODE_COMPACT))
            "docx" -> toDocx(title, Html.fromHtml(safeHtml, Html.FROM_HTML_MODE_COMPACT))
            "odt" -> toOdt(title, Html.fromHtml(safeHtml, Html.FROM_HTML_MODE_COMPACT))
            "epub" -> toEpub(title, safeHtml)
            else -> htmlDocument(title, safeHtml).toByteArray(StandardCharsets.UTF_8)
        }
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
            ?: error("Could not create the export file")
    }

    fun htmlDocument(title: String, body: String) = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${xml(title)}</title><style>body{font-family:Arial,sans-serif;line-height:1.55;max-width:850px;margin:40px auto;padding:0 24px;color:#202124}table{border-collapse:collapse;width:100%}td,th{border:1px solid #aaa;padding:8px}img{max-width:100%}</style></head><body>$body</body></html>"""

    private fun toRtf(source: Spanned): ByteArray {
        val text = StringBuilder("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\f0\\fs22 ")
        var index = 0
        while (index < source.length) {
            val char = source[index]
            if (char == '\n') { text.append("\\par "); index++; continue }
            val spans = source.getSpans(index, index + 1, Any::class.java)
            val bold = spans.filterIsInstance<StyleSpan>().any { it.style and Typeface.BOLD != 0 }
            val italic = spans.filterIsInstance<StyleSpan>().any { it.style and Typeface.ITALIC != 0 }
            text.append(if (bold) "\\b " else "\\b0 ")
            text.append(if (italic) "\\i " else "\\i0 ")
            if (spans.any { it is UnderlineSpan }) text.append("\\ul ") else text.append("\\ul0 ")
            if (spans.any { it is StrikethroughSpan }) text.append("\\strike ") else text.append("\\strike0 ")
            when {
                char == '\\' -> text.append("\\\\")
                char == '{' -> text.append("\\{")
                char == '}' -> text.append("\\}")
                char.code < 32 -> Unit
                else -> text.append(char)
            }
            index++
        }
        text.append("}")
        return text.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun toDocx(title: String, source: Spanned): ByteArray {
        val body = StringBuilder()
        val fullText = source.toString()
        val lines = fullText.split('\n')
        var globalOffset = 0
        lines.forEach { line ->
            body.append("<w:p>")
            if (line.isEmpty()) {
                body.append("<w:r><w:t></w:t></w:r>")
            } else {
                var local = 0
                while (local < line.length) {
                    val start = globalOffset + local
                    val spans = source.getSpans(start, start + 1, Any::class.java)
                    var next = local + 1
                    while (next < line.length) {
                        val nextSpans = source.getSpans(globalOffset + next, globalOffset + next + 1, Any::class.java)
                        if (!sameRunStyle(spans, nextSpans)) break
                        next++
                    }
                    val runText = line.substring(local, next)
                    body.append("<w:r>")
                    val properties = buildString {
                        val bold = spans.filterIsInstance<StyleSpan>().any { it.style and Typeface.BOLD != 0 }
                        val italic = spans.filterIsInstance<StyleSpan>().any { it.style and Typeface.ITALIC != 0 }
                        if (bold) append("<w:b/>")
                        if (italic) append("<w:i/>")
                        if (spans.any { it is UnderlineSpan }) append("<w:u w:val=\"single\"/>")
                        if (spans.any { it is StrikethroughSpan }) append("<w:strike/>")
                        if (spans.any { it is SuperscriptSpan }) append("<w:vertAlign w:val=\"superscript\"/>")
                        if (spans.any { it is SubscriptSpan }) append("<w:vertAlign w:val=\"subscript\"/>")
                        spans.filterIsInstance<ForegroundColorSpan>().firstOrNull()?.let {
                            append("<w:color w:val=\"${"%06X".format(it.foregroundColor and 0xFFFFFF)}\"/>")
                        }
                        spans.filterIsInstance<BackgroundColorSpan>().firstOrNull()?.let {
                            append("<w:highlight w:val=\"yellow\"/>")
                        }
                        spans.filterIsInstance<RelativeSizeSpan>().firstOrNull()?.let {
                            append("<w:sz w:val=\"${(22 * it.sizeChange).toInt().coerceIn(12, 96)}\"/>")
                        }
                    }
                    if (properties.isNotEmpty()) body.append("<w:rPr>$properties</w:rPr>")
                    body.append("<w:t xml:space=\"preserve\">${xml(runText)}</w:t></w:r>")
                    local = next
                }
            }
            body.append("</w:p>")
            globalOffset += line.length + 1
        }
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>"""
        return zipBytes {
            add("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            add("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            add("word/document.xml", document)
            add("docProps/core.xml", """<?xml version="1.0" encoding="UTF-8"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>${xml(title)}</dc:title><dc:creator>DocuMaker</dc:creator></cp:coreProperties>""")
        }
    }

    private fun sameRunStyle(a: Array<Any>, b: Array<Any>): Boolean {
        fun key(spans: Array<Any>) = spans.mapNotNull {
            when (it) {
                is StyleSpan -> "s${it.style}"
                is UnderlineSpan -> "u"
                is StrikethroughSpan -> "x"
                is SuperscriptSpan -> "sup"
                is SubscriptSpan -> "sub"
                is ForegroundColorSpan -> "c${it.foregroundColor}"
                is BackgroundColorSpan -> "b${it.backgroundColor}"
                is RelativeSizeSpan -> "z${it.sizeChange}"
                is URLSpan -> "l${it.url}"
                else -> null
            }
        }.sorted()
        return key(a) == key(b)
    }

    private fun toOdt(title: String, source: Spanned): ByteArray {
        val paragraphs = source.toString().split('\n').joinToString("") { line ->
            "<text:p>${xml(line)}</text:p>"
        }
        return zipBytes(mimetype = "application/vnd.oasis.opendocument.text") {
            add("content.xml", """<?xml version="1.0" encoding="UTF-8"?><office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2"><office:body><office:text>$paragraphs</office:text></office:body></office:document-content>""")
            add("META-INF/manifest.xml", """<?xml version="1.0" encoding="UTF-8"?><manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2"><manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/><manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/><manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/></manifest:manifest>""")
            add("meta.xml", """<?xml version="1.0" encoding="UTF-8"?><office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2"><office:meta><dc:title>${xml(title)}</dc:title></office:meta></office:document-meta>""")
        }
    }

    private fun toEpub(title: String, body: String): ByteArray = zipBytes(mimetype = "application/epub+zip") {
        add("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
        add("OEBPS/content.xhtml", """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(title)}</title></head><body><h1>${xml(title)}</h1>$body</body></html>""")
        add("OEBPS/content.opf", """<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="uid">documaker-${title.hashCode()}</dc:identifier><dc:title>${xml(title)}</dc:title><dc:language>en</dc:language></metadata><manifest><item id="content" href="content.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="content"/></spine></package>""")
    }

    private class ZipBuilder(private val out: ZipOutputStream) {
        fun add(name: String, content: String) {
            out.putNextEntry(ZipEntry(name))
            out.write(content.toByteArray(StandardCharsets.UTF_8))
            out.closeEntry()
        }
    }

    private fun zipBytes(mimetype: String? = null, build: ZipBuilder.() -> Unit): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            if (mimetype != null) {
                val bytes = mimetype.toByteArray(StandardCharsets.UTF_8)
                val entry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = bytes.size.toLong()
                    crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            ZipBuilder(zip).apply(build)
        }
        return output.toByteArray()
    }

    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
