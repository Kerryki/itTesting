package com.example.testgen.domain

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class ApiResponseTest {
    @Test
    fun `should create success response with data`() {
        val data = GeneratedFile(filename = "Test.kt", content = "class Test {}")

        val response = ApiResponse.success(data)

        assertThat(response.success).isTrue
        assertThat(response.data).isEqualTo(data)
        assertThat(response.error).isNull()
    }

    @Test
    fun `should create error response with message`() {
        val errorMessage = "Failed to parse file"

        val response = ApiResponse.error<GeneratedFile>(errorMessage)

        assertThat(response.success).isFalse
        assertThat(response.data).isNull()
        assertThat(response.error).isEqualTo(errorMessage)
    }

    @Test
    fun `should work with list data`() {
        val files = listOf(
            GeneratedFile("Test1.kt", "class Test1 {}"),
            GeneratedFile("Test2.kt", "class Test2 {}")
        )

        val response = ApiResponse.success(files)

        assertThat(response.success).isTrue
        assertThat(response.data).hasSize(2)
    }
}
