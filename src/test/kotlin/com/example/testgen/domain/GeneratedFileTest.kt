package com.example.testgen.domain

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class GeneratedFileTest {
    @Test
    fun `should create GeneratedFile with filename and content`() {
        val filename = "CreateOrderUseCaseTest.kt"
        val content = "class CreateOrderUseCaseTest { ... }"

        val file = GeneratedFile(
            filename = filename,
            content = content
        )

        assertThat(file.filename).isEqualTo(filename)
        assertThat(file.content).isEqualTo(content)
    }

    @Test
    fun `should be immutable`() {
        val file = GeneratedFile(
            filename = "CreateOrderUseCaseTest.kt",
            content = "class CreateOrderUseCaseTest { ... }"
        )

        val modified = file.copy(
            filename = "UpdateOrderUseCaseTest.kt"
        )

        assertThat(modified.filename).isEqualTo("UpdateOrderUseCaseTest.kt")
        assertThat(file.filename).isEqualTo("CreateOrderUseCaseTest.kt")
    }
}
