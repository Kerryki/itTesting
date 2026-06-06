package com.example.testgen.generator

import com.example.testgen.domain.DataClassInfo
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.domain.ParsedUseCase
import com.example.testgen.domain.ScanResult
import com.samskivert.mustache.Mustache
import java.io.InputStreamReader

class TestFileGenerator {
    fun generateTest(scanResult: ScanResult): GeneratedFile {
        val template = loadTemplate("templates/integration_test.mustache")
        val context = buildContext(scanResult)
        val renderedContent = template.execute(context).toString()

        val useCase = scanResult.useCase
        val subDir = packageToPath(useCase.packageName)
        val filename = "$subDir/${useCase.className}IntegrationTest.kt"

        return GeneratedFile(filename = filename, content = renderedContent)
    }

    private fun loadTemplate(path: String): com.samskivert.mustache.Template {
        val resource = this::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Template not found: $path")
        val templateContent = InputStreamReader(resource).use { it.readText() }
        return Mustache.compiler().escapeHTML(false).compile(templateContent)
    }

    private fun buildContext(scanResult: ScanResult): Map<String, Any> {
        val useCase = scanResult.useCase
        val resolvedDtos = scanResult.resolvedDtos

        val httpMethod = inferHttpMethod(useCase.className)
        val expectedStatus = inferExpectedStatus(useCase.className)

        // A request body exists only when there is exactly one parameter that resolves to a known data class
        val commandTypeInfo: DataClassInfo? = if (useCase.parameterCount == 1) {
            resolvedDtos.firstOrNull { it.name == useCase.requestTypeName }
        } else null

        val hasRequestBody = commandTypeInfo != null
        val hasResponseBody = useCase.responseTypeName != "Unit" && useCase.responseTypeName.isNotEmpty()
        val isListResponse = useCase.isListResponse

        val responseInnerTypeName: String = if (isListResponse) {
            val nonNullable = useCase.responseTypeName.removeSuffix("?")
            // extract everything between the first < and last >
            val start = nonNullable.indexOf('<') + 1
            val end = nonNullable.lastIndexOf('>')
            if (start > 0 && end > start) nonNullable.substring(start, end).trim() else nonNullable
        } else {
            useCase.responseTypeName
        }

        val requestFields = (commandTypeInfo?.fields ?: emptyList()).mapIndexed { index, field ->
            mapOf(
                "name" to field.name,
                "type" to field.type,
                "nullable" to field.nullable,
                "defaultValue" to inferDefaultValue(field.type),
                "last" to (index == (commandTypeInfo?.fields?.size ?: 0) - 1)
            )
        }

        // Path/query param comment shown when there are params but no command object
        val hasPathParamComment = useCase.parameterCount > 0 && !hasRequestBody
        val pathParamComment = if (hasPathParamComment) useCase.requestTypeName else ""

        // Build imports from resolved DTOs
        val imports = mutableListOf<Map<String, String>>()
        if (hasRequestBody && commandTypeInfo != null) {
            imports.add(mapOf("fqn" to "${escapePackageName(commandTypeInfo.packageName)}.${commandTypeInfo.name}"))
        }
        // Response type import
        val responseDto = resolvedDtos.firstOrNull { it.name == responseInnerTypeName }
        if (responseDto != null && hasResponseBody) {
            imports.add(mapOf("fqn" to "${escapePackageName(responseDto.packageName)}.${responseDto.name}"))
        }

        return mapOf(
            "packageName" to escapePackageName(useCase.packageName),
            "classNameTest" to "${useCase.className}IntegrationTest",
            "className" to useCase.className,
            "requestTypeName" to (if (hasRequestBody) useCase.requestTypeName else ""),
            "responseTypeName" to useCase.responseTypeName,
            "responseInnerTypeName" to responseInnerTypeName,
            "httpMethodLower" to httpMethod,
            "uri" to inferUri(useCase.className),
            "expectedStatus" to expectedStatus,
            "testDescription" to inferTestDescription(useCase.className),
            "hasRequestBody" to hasRequestBody,
            "hasResponseBody" to (hasResponseBody && !isListResponse),
            "isListResponse" to isListResponse,
            "hasPathParamComment" to hasPathParamComment,
            "pathParamComment" to pathParamComment,
            "requestFields" to requestFields,
            "imports" to imports.distinctBy { it["fqn"] }
        )
    }

    private fun inferHttpMethod(className: String): String = when {
        className.startsWith("Create") -> "post"
        className.startsWith("List") || className.startsWith("Get") -> "get"
        className.startsWith("Update") -> "put"
        className.startsWith("Delete") -> "delete"
        else -> "post"
    }

    private fun inferUri(className: String): String {
        val withoutSuffix = className.removeSuffix("UseCase")
        val verb = listOf("Create", "List", "Get", "Update", "Delete")
            .firstOrNull { withoutSuffix.startsWith(it) } ?: ""
        val entityPart = withoutSuffix.removePrefix(verb)
        val entityWords = entityPart.split(Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])"))
            .filter { it.isNotEmpty() }
        val entity = entityWords.firstOrNull()?.lowercase() ?: className.lowercase()
        val plural = if (entity.endsWith("s")) entity else "${entity}s"
        return if (verb == "List" || verb == "Create") "/$plural" else "/$plural/{id}"
    }

    private fun inferExpectedStatus(className: String): String = when {
        className.startsWith("Create") -> "isCreated"
        className.startsWith("Delete") -> "isNoContent"
        else -> "isOk"
    }

    private fun inferTestDescription(className: String): String = when {
        className.startsWith("Create") -> "should create resource and return 201"
        className.startsWith("Get") -> "should return resource by id"
        className.startsWith("List") -> "should return list of resources"
        className.startsWith("Delete") -> "should delete resource and return 204"
        className.startsWith("Update") -> "should update resource and return 200"
        else -> "should execute successfully"
    }

    private fun escapePackageName(packageName: String): String {
        val reserved = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for",
            "fun", "if", "in", "interface", "is", "null", "object", "package",
            "return", "super", "this", "throw", "true", "try", "typealias",
            "val", "var", "when", "while", "by", "catch", "constructor",
            "delegate", "dynamic", "field", "file", "finally", "get", "import",
            "init", "out", "param", "property", "receiver", "set", "setparam",
            "where", "actual", "abstract", "annotation", "companion", "const",
            "crossinline", "data", "enum", "expect", "external", "final",
            "infix", "inline", "inner", "internal", "lateinit", "noinline",
            "open", "operator", "override", "private", "protected", "public",
            "reified", "sealed", "suspend", "tailrec", "vararg"
        )
        return packageName.split(".").joinToString(".") { segment ->
            if (segment in reserved) "`$segment`" else segment
        }
    }

    private fun packageToPath(packageName: String): String {
        require(packageName.isNotBlank()) { "packageName must not be blank" }
        return packageName.replace('.', '/')
    }

    private fun inferDefaultValue(type: String): String = when {
        type.startsWith("String") -> "\"TODO\""
        type.startsWith("Int") -> "0"
        type.startsWith("Long") -> "0L"
        type.startsWith("Boolean") -> "false"
        type.startsWith("BigDecimal") -> "java.math.BigDecimal.ZERO"
        type.startsWith("List") -> "emptyList()"
        type.startsWith("Map") -> "emptyMap()"
        type.startsWith("Set") -> "emptySet()"
        type.contains("UUID") -> "java.util.UUID.randomUUID()"
        else -> "TODO()"
    }
}
