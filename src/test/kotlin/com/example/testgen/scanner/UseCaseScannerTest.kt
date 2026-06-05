package com.example.testgen.scanner

import com.example.testgen.config.TestgenConfig
import com.example.testgen.parser.KotlinCodeParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UseCaseScannerTest {

    private val scanner = UseCaseScanner(KotlinCodeParser())

    private val dummyDir: File
        get() = File(
            UseCaseScannerTest::class.java
                .classLoader
                .getResource("dummy")!!
                .toURI()
        )

    // -------------------------------------------------------------------------
    // Empty / missing source directory
    // -------------------------------------------------------------------------

    @Test
    fun `scan() returns empty list when sourceDir has no Kotlin files`(@TempDir emptyDir: File) {
        val results = scanner.scan(emptyDir, TestgenConfig())

        assertThat(results).isEmpty()
    }

    @Test
    fun `scan() returns empty list when sourceDir does not exist`(@TempDir tempDir: File) {
        val nonExistent = File(tempDir, "no-such-dir")

        val results = scanner.scan(nonExistent, TestgenConfig())

        assertThat(results).isEmpty()
    }

    // -------------------------------------------------------------------------
    // Real fixture discovery
    // -------------------------------------------------------------------------

    @Test
    fun `scan() discovers use cases from the dummy test fixtures`() {
        val results = scanner.scan(dummyDir, TestgenConfig())

        // The dummy directory contains CreateOrderUseCase.kt and SimpleUseCase.kt,
        // both of which declare a class ending in UseCase with an execute() method.
        val classNames = results.map { it.useCase.className }
        assertThat(classNames).containsExactlyInAnyOrder("CreateOrderUseCase", "SimpleUseCase")
    }

    @Test
    fun `scan() respects excludes pattern — excluded file produces no results`() {
        // Exclude SimpleUseCase.kt; only CreateOrderUseCase should be returned.
        val config = TestgenConfig(excludes = listOf("**/SimpleUseCase.kt"))

        val results = scanner.scan(dummyDir, config)

        assertThat(results).hasSize(1)
        assertThat(results.map { it.useCase.className }).containsExactly("CreateOrderUseCase")
    }

    // -------------------------------------------------------------------------
    // resolvedDtos non-null guarantee
    // -------------------------------------------------------------------------

    @Test
    fun `scan() resolvedDtos are non-null lists — even if empty`() {
        val results = scanner.scan(dummyDir, TestgenConfig())

        assertThat(results).isNotEmpty
        results.forEach { result ->
            assertThat(result.resolvedDtos)
                .`as`("resolvedDtos for ${result.useCase.className} must not be null")
                .isNotNull
        }
    }
}
