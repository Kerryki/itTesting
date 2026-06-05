package com.example.testgen.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigLoaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `load() returns defaults when no testgen yml exists`() {
        val config = ConfigLoader.load(tempDir)

        assertThat(config.sourceDir).isEqualTo("src/main/kotlin")
        assertThat(config.outputDir).isEqualTo("src/test/kotlin")
        assertThat(config.basePackage).isEqualTo("")
        assertThat(config.includes).isEmpty()
        assertThat(config.excludes).isEmpty()
        assertThat(config.templatePath).isNull()
    }

    @Test
    fun `load() parses all fields from a valid YAML file`() {
        val yaml = """
            sourceDir: src/main/java
            outputDir: src/test/java
            basePackage: com.example.myapp
            includes:
              - "**/*UseCase.kt"
              - "**/*Service.kt"
            excludes:
              - "**/legacy/**"
            templatePath: /custom/template.mustache
        """.trimIndent()
        File(tempDir, ".testgen.yml").writeText(yaml)

        val config = ConfigLoader.load(tempDir)

        assertThat(config.sourceDir).isEqualTo("src/main/java")
        assertThat(config.outputDir).isEqualTo("src/test/java")
        assertThat(config.basePackage).isEqualTo("com.example.myapp")
        assertThat(config.includes).containsExactly("**/*UseCase.kt", "**/*Service.kt")
        assertThat(config.excludes).containsExactly("**/legacy/**")
        assertThat(config.templatePath).isEqualTo("/custom/template.mustache")
    }

    @Test
    fun `load() returns non-null includes and excludes when those keys are absent`() {
        // Verifies the NullIsSameAsDefault fix: absent list keys fall back to emptyList(), not null
        val yaml = """
            sourceDir: src/main/kotlin
            basePackage: com.example
        """.trimIndent()
        File(tempDir, ".testgen.yml").writeText(yaml)

        val config = ConfigLoader.load(tempDir)

        assertThat(config.includes).isNotNull.isEmpty()
        assertThat(config.excludes).isNotNull.isEmpty()
    }

    @Test
    fun `load() throws IllegalArgumentException when YAML is malformed`() {
        val malformedYaml = "key: : : :"
        File(tempDir, ".testgen.yml").writeText(malformedYaml)

        assertThatThrownBy { ConfigLoader.load(tempDir) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(".testgen.yml")
    }

    @Test
    fun `load() throws IllegalArgumentException when projectBaseDir does not exist`() {
        val nonExistentDir = File(tempDir, "does-not-exist")

        assertThatThrownBy { ConfigLoader.load(nonExistentDir) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("does not exist")
    }

    @Test
    fun `load() throws IllegalArgumentException when projectBaseDir is a file not a directory`() {
        val file = File(tempDir, "not-a-directory.txt").also { it.writeText("content") }

        assertThatThrownBy { ConfigLoader.load(file) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not a directory")
    }
}
