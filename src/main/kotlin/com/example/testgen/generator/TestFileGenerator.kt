package com.example.testgen.generator

import com.example.testgen.domain.GeneratedFile
import com.example.testgen.domain.ParsedUseCase
import com.samskivert.mustache.Mustache
import java.io.InputStreamReader

class TestFileGenerator {
    fun generateTest(useCase: ParsedUseCase): GeneratedFile {
        val template = loadTemplate("templates/integration_test.mustache")
        val context = buildContext(useCase)
        val renderedContent = template.execute(context).toString()

        val filename = "${useCase.className}Test.kt"

        return GeneratedFile(
            filename = filename,
            content = renderedContent
        )
    }

    private fun loadTemplate(path: String): com.samskivert.mustache.Template {
        val resource = this::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Template not found: $path")

        val templateContent = InputStreamReader(resource).use { it.readText() }
        return Mustache.compiler().compile(templateContent)
    }

    private fun buildContext(useCase: ParsedUseCase): Map<String, Any> {
        return mapOf(
            "packageName" to useCase.packageName,
            "classNameTest" to "${useCase.className}Test",
            "className" to useCase.className,
            "requestTypeName" to useCase.requestTypeName,
            "responseTypeName" to useCase.responseTypeName,
            "defaultPath" to inferPath(useCase.className),
            "requestFields" to useCase.requestFields.map { field ->
                mapOf(
                    "name" to field.name,
                    "type" to field.type,
                    "nullable" to field.nullable,
                    "defaultValue" to inferDefaultValue(field.type)
                )
            }
        )
    }

    private fun inferPath(className: String): String {
        val withoutUseCase = className.removeSuffix("UseCase")
        val camelCase = withoutUseCase.lowercase()
        return "/api/${camelCase}"
    }

    private fun inferDefaultValue(type: String): String {
        return when {
            type.startsWith("String") -> ""
            type.startsWith("Int") -> "0"
            type.startsWith("Long") -> "0L"
            type.startsWith("Boolean") -> "false"
            type.startsWith("BigDecimal") -> "0.0"
            else -> "null"
        }
    }
}
