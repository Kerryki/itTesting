package com.example.testgen.e2e

import com.example.testgen.parser.KotlinCodeParser
import com.example.testgen.generator.TestFileGenerator
import com.example.testgen.domain.ScanResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files
import java.nio.file.Paths

class UploadCompileE2ETest {
    @Test
    fun `should generate compilable test for SimpleUseCase`() {
        val dummyFile = Paths.get("src/test/resources/dummy/SimpleUseCase.kt")
        val content = Files.readString(dummyFile)

        val parser = KotlinCodeParser()
        val useCase = parser.parseUseCase(content)
        assertThat(useCase).isNotNull

        val requestDto = parser.parseDataClass(content, useCase!!.requestTypeName)
        assertThat(requestDto).isNotNull

        val responseDto = parser.parseDataClass(content, useCase.responseTypeName)
        assertThat(responseDto).isNotNull

        val useCaseWithFields = useCase.copy(
            requestFields = requestDto!!.fields,
            responseFields = responseDto!!.fields
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(ScanResult(useCaseWithFields, emptyList()))

        assertThat(generatedFile).isNotNull
        assertThat(generatedFile.filename).endsWith("Test.kt")
        assertThat(generatedFile.content).isNotBlank

        // Verify the generated code is valid Kotlin
        val isValidKotlin = isValidKotlinCode(generatedFile.content)
        assertThat(isValidKotlin).isTrue
    }

    private fun isValidKotlinCode(code: String): Boolean {
        return try {
            val config = KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable(),
                org.jetbrains.kotlin.config.CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val ktPsiFactory = org.jetbrains.kotlin.psi.KtPsiFactory(config.project)
            val ktFile = ktPsiFactory.createFile(code) as? KtFile

            // If we got here without exception, it's valid
            ktFile != null
        } catch (e: Exception) {
            false
        }
    }
}
