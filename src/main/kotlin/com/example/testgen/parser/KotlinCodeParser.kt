package com.example.testgen.parser

import com.example.testgen.domain.DataClassInfo
import com.example.testgen.domain.DtoField
import com.example.testgen.domain.ParsedUseCase
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.slf4j.LoggerFactory

class KotlinCodeParser {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var environment: KotlinCoreEnvironment? = null

    private fun getEnvironment(): KotlinCoreEnvironment {
        environment?.let { return it }
        return synchronized(this) {
            environment ?: KotlinCoreEnvironment.createForProduction(
                org.jetbrains.kotlin.com.intellij.openapi.util.Disposer.newDisposable(),
                org.jetbrains.kotlin.config.CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            ).also {
                environment = it
            }
        }
    }

    fun parseUseCase(content: String): ParsedUseCase? {
        val ktFile = parseKotlinCode(content) ?: return null

        val packageName = ktFile.packageFqName.asString()
        if (packageName.isBlank()) {
            logger.warn("Skipping use case in file with no package declaration")
            return null
        }
        val useCaseClass = findUseCaseClass(ktFile) ?: return null
        val className = useCaseClass.name ?: return null

        val useCaseMethod = if (useCaseClass.isInterface()) {
            val methods = useCaseClass.declarations.filterIsInstance<KtFunction>()
            when {
                methods.isEmpty() -> {
                    logger.warn("Interface $className has no methods — skipping")
                    return null
                }
                methods.size > 1 -> {
                    logger.warn("Interface $className has ${methods.size} methods (expected exactly 1) — skipping")
                    return null
                }
                else -> methods.first()
            }
        } else {
            useCaseClass.declarations.filterIsInstance<KtFunction>()
                .find { it.name == "execute" } ?: return null
        }

        val params = useCaseMethod.valueParameters
        val parameterCount = params.size

        val requestTypeName = when {
            parameterCount == 0 -> ""
            parameterCount == 1 -> params[0].typeReference?.text?.trim() ?: return null
            else -> params.joinToString(", ") { p ->
                "${p.name ?: "_"}: ${p.typeReference?.text?.trim() ?: "Any"}"
            }
        }

        val rawResponseType = useCaseMethod.typeReference?.text?.trim() ?: "Unit"
        val isListResponse = rawResponseType.trimEnd('?').startsWith("List<")

        logger.debug("Parsed UseCase: $className paramCount=$parameterCount request=$requestTypeName response=$rawResponseType")

        return ParsedUseCase(
            className = className,
            packageName = packageName,
            requestTypeName = requestTypeName,
            responseTypeName = rawResponseType,
            requestFields = emptyList(),
            responseFields = emptyList(),
            parameterCount = parameterCount,
            isListResponse = isListResponse
        )
    }

    fun parseDataClass(content: String, className: String): DataClassInfo? {
        val ktFile = parseKotlinCode(content) ?: return null
        val packageName = ktFile.packageFqName.asString()

        val dataClass = findDataClass(ktFile, className) ?: return null

        val fields = extractDataClassFields(dataClass)

        return DataClassInfo(
            name = className,
            packageName = packageName,
            fields = fields
        )
    }

    private fun parseKotlinCode(content: String): KtFile? {
        return try {
            val ktPsiFactory = org.jetbrains.kotlin.psi.KtPsiFactory(getEnvironment().project)
            ktPsiFactory.createFile(content) as? KtFile
        } catch (e: Exception) {
            logger.warn("Failed to parse Kotlin code: ${e::class.simpleName}")
            null
        }
    }

    private fun findUseCaseClass(ktFile: KtFile): KtClass? {
        return ktFile.declarations.filterIsInstance<KtClass>()
            .find { ktClass ->
                ktClass.name?.endsWith("UseCase") == true && (
                    // Class-based: must have an execute() method
                    (!ktClass.isInterface() && ktClass.declarations.filterIsInstance<KtFunction>()
                        .any { f -> f.name == "execute" }) ||
                    // Interface-based: any interface ending in UseCase with at least one method
                    (ktClass.isInterface() && ktClass.declarations.filterIsInstance<KtFunction>().isNotEmpty())
                )
            }
    }

    private fun findDataClass(ktFile: KtFile, className: String): KtClass? {
        return ktFile.declarations.filterIsInstance<KtClass>()
            .find { it.name == className }
    }

    private fun extractDataClassFields(dataClass: KtClass): List<DtoField> {
        return dataClass.primaryConstructor
            ?.valueParameters
            ?.mapNotNull { param ->
                val fieldName = param.name ?: return@mapNotNull null
                val fieldTypeText = param.typeReference?.text?.trim() ?: "Any"
                val isNullable = fieldTypeText.contains("?")
                DtoField(fieldName, fieldTypeText, isNullable)
            }
            ?: emptyList()
    }
}
