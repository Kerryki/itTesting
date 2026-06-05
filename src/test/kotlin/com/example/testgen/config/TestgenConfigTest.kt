package com.example.testgen.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestgenConfigTest {

    @Test
    fun `default config has expected sourceDir and outputDir`() {
        val config = TestgenConfig()

        assertThat(config.sourceDir).isEqualTo("src/main/kotlin")
        assertThat(config.outputDir).isEqualTo("src/test/kotlin")
        assertThat(config.basePackage).isEqualTo("")
        assertThat(config.includes).isEmpty()
        assertThat(config.excludes).isEmpty()
        assertThat(config.templatePath).isNull()
    }

    @Test
    fun `copy() produces independent instance — immutability check`() {
        val original = TestgenConfig(
            sourceDir = "src/main/kotlin",
            outputDir = "src/test/kotlin",
            basePackage = "com.example",
            includes = listOf("**/*.kt"),
            excludes = listOf("**/test/**"),
            templatePath = "/some/template.mustache"
        )

        val copy = original.copy(sourceDir = "src/main/java", basePackage = "com.other")

        // Original is unchanged
        assertThat(original.sourceDir).isEqualTo("src/main/kotlin")
        assertThat(original.basePackage).isEqualTo("com.example")

        // Copy reflects the new values
        assertThat(copy.sourceDir).isEqualTo("src/main/java")
        assertThat(copy.basePackage).isEqualTo("com.other")

        // Unmodified fields are carried over from original
        assertThat(copy.outputDir).isEqualTo(original.outputDir)
        assertThat(copy.includes).isEqualTo(original.includes)
        assertThat(copy.excludes).isEqualTo(original.excludes)
        assertThat(copy.templatePath).isEqualTo(original.templatePath)

        // They are different object references
        assertThat(copy).isNotSameAs(original)
    }

    @Test
    fun `data class equality works`() {
        val a = TestgenConfig(
            sourceDir = "src/main/kotlin",
            outputDir = "src/test/kotlin",
            basePackage = "com.example",
            includes = listOf("**/*UseCase.kt"),
            excludes = emptyList(),
            templatePath = null
        )
        val b = TestgenConfig(
            sourceDir = "src/main/kotlin",
            outputDir = "src/test/kotlin",
            basePackage = "com.example",
            includes = listOf("**/*UseCase.kt"),
            excludes = emptyList(),
            templatePath = null
        )
        val different = a.copy(basePackage = "com.other")

        assertThat(a).isEqualTo(b)
        assertThat(a).hasSameHashCodeAs(b)
        assertThat(a).isNotEqualTo(different)
    }
}
