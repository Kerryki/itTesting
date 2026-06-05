package com.example.testgen.parser

import com.example.testgen.domain.DataClassInfo
import com.example.testgen.domain.DtoField
import com.example.testgen.domain.ParsedUseCase
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

class KotlinCodeParser {
    private val environment: KotlinCoreEnvironment

    init {
        val config = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            org.jetbrains.kotlin.config.CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        environment = config
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
            val ktPsiFactory = org.jetbrains.kotlin.psi.KtPsiFactory(environment.project)
            ktPsiFactory.createFile(content) as? KtFile
        } catch (e: Exception) {
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
        val fields = mutableListOf<DtoField>()

        val primaryConstructor = dataClass.primaryConstructor
        primaryConstructor?.valueParameters?.forEach { param ->
            val fieldName = param.name ?: return@forEach
            val fieldType = param.typeReference?.text?.trim() ?: "Any"
            val isNullable = param.typeReference?.text?.contains("?") == true

            fields.add(DtoField(fieldName, fieldType, isNullable))
        }

        return fields
    }
}
