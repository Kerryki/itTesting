package com.example.testgen.scanner

import com.example.testgen.config.TestgenConfig
import com.example.testgen.domain.DataClassInfo
import com.example.testgen.domain.ParsedUseCase
import com.example.testgen.domain.ScanResult
import com.example.testgen.parser.KotlinCodeParser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems

class UseCaseScanner(private val parser: KotlinCodeParser) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun scan(sourceDir: File, config: TestgenConfig): List<ScanResult> {
        val contents = readFileContents(collectKtFiles(sourceDir, config))
        val useCases = parseUseCases(contents)
        val allDataClasses = parseAllDataClasses(contents)
        return useCases.map { ScanResult(it, resolveDataClasses(it, allDataClasses)) }
    }

    private fun collectKtFiles(sourceDir: File, config: TestgenConfig): List<File> {
        val fs = FileSystems.getDefault()
        // For each glob pattern we compile two matchers:
        //   1. The full pattern — matches nested paths like "a/b/Foo.kt" against "**/Foo.kt".
        //   2. A filename-only pattern (the last path component of the glob) — matches a
        //      single-element relative path ("Foo.kt") that the full "**/Foo.kt" pattern
        //      does NOT match on the JVM because "**" requires at least one directory segment.
        // Both matchers operate exclusively on relative paths, so no absolute-path exposure.
        fun matchers(patterns: List<String>) = patterns.map { pattern ->
            val full = fs.getPathMatcher("glob:$pattern")
            val fileNameGlob = pattern.substringAfterLast('/')
            val fileNameOnly = fs.getPathMatcher("glob:$fileNameGlob")
            Pair(full, fileNameOnly)
        }

        val includes = matchers(config.includes)
        val excludes = matchers(config.excludes)

        fun List<Pair<java.nio.file.PathMatcher, java.nio.file.PathMatcher>>.anyMatches(
            rel: java.nio.file.Path,
            fileName: java.nio.file.Path
        ) = any { (full, fileNameOnly) -> full.matches(rel) || fileNameOnly.matches(fileName) }

        return sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val rel = sourceDir.toPath().relativize(file.toPath())
                val fileName = rel.fileName
                val included = includes.isEmpty() || includes.anyMatches(rel, fileName)
                val excluded = excludes.isNotEmpty() && excludes.anyMatches(rel, fileName)
                included && !excluded
            }
            .toList()
    }

    private fun readFileContents(files: List<File>): Map<File, String> {
        return files.mapNotNull { file ->
            try {
                file to file.readText()
            } catch (e: IOException) {
                logger.warn("Skipping unreadable file ${file.path}: ${e.message}")
                null
            }
        }.toMap()
    }

    private fun parseUseCases(fileContents: Map<File, String>): List<ParsedUseCase> {
        return fileContents.values.mapNotNull { content ->
            try {
                parser.parseUseCase(content)
            } catch (e: Exception) {
                logger.warn("Failed to parse use case: ${e.message}")
                null
            }
        }
    }

    private fun parseAllDataClasses(fileContents: Map<File, String>): List<DataClassInfo> {
        return fileContents.values.flatMap { content ->
            extractDataClassNames(content).mapNotNull { name ->
                try {
                    parser.parseDataClass(content, name)
                } catch (e: Exception) {
                    logger.warn("Failed to parse data class '$name': ${e.message}")
                    null
                }
            }
        }
    }

    private fun extractDataClassNames(content: String): List<String> {
        return Regex("""data\s+class\s+(\w+)""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun extractSimpleTypeNames(typeName: String): Set<String> {
        return Regex("""\b([A-Z][a-zA-Z0-9_]*)\b""")
            .findAll(typeName)
            .map { it.value }
            .filter { it !in setOf("List", "Set", "Map", "Flow", "Result", "Pair", "Triple", "Unit", "Any", "Nothing") }
            .toSet()
    }

    private fun resolveDataClasses(useCase: ParsedUseCase, all: List<DataClassInfo>): List<DataClassInfo> {
        val targets = extractSimpleTypeNames(useCase.requestTypeName) + extractSimpleTypeNames(useCase.responseTypeName)
        return all.filter { it.name in targets }
    }
}
