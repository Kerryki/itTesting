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
import org.springframework.stereotype.Component

@Component
class KotlinCodeParser {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var environment: KotlinCoreEnvironment? = null

    private fun getEnvironment(): KotlinCoreEnvironment {
        if (environment == null) {
            synchronized(this) {
                if (environment == null) {
                    val parentDisposable = org.jetbrains.kotlin.com.intellij.openapi.util.Disposer.newDisposable()
                    environment = KotlinCoreEnvironment.createForProduction(
                        parentDisposable,
                        org.jetbrains.kotlin.config.CompilerConfiguration(),
                        EnvironmentConfigFiles.JVM_CONFIG_FILES
                    )
                    logger.debug("KotlinCodeParser environment initialized")
                }
            }
        }
        return environment!!
    }

    fun parseUseCase(content: String): ParsedUseCase? {
        val ktFile = parseKotlinCode(content) ?: return null

        val packageName = ktFile.packageFqName.asString()
        val useCaseClass = findUseCaseClass(ktFile) ?: return null
        val className = useCaseClass.name ?: return null

        val executeMethod = useCaseClass.declarations.filterIsInstance<KtFunction>()
            .find { it.name == "execute" } ?: return null
        val paramList = executeMethod.valueParameters.firstOrNull() ?: return null
        val requestTypeName = paramList.typeReference?.text?.trim() ?: return null

        val returnType = executeMethod.typeReference?.text?.trim() ?: return null
        val responseTypeName = returnType

        return ParsedUseCase(
            className = className,
            packageName = packageName,
            requestTypeName = requestTypeName,
            responseTypeName = responseTypeName,
            requestFields = emptyList(),
            responseFields = emptyList()
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
            .find { it.name?.endsWith("UseCase") == true &&
                    it.declarations.filterIsInstance<KtFunction>().any { f -> f.name == "execute" } }
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
