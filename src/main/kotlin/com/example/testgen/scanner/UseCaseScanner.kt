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
        val includes = config.includes.map { fs.getPathMatcher("glob:$it") }
        val excludes = config.excludes.map { fs.getPathMatcher("glob:$it") }
        return sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val rel = sourceDir.toPath().relativize(file.toPath())
                val included = includes.isEmpty() || includes.any { it.matches(rel) }
                val excluded = excludes.isNotEmpty() && excludes.any { it.matches(rel) }
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
