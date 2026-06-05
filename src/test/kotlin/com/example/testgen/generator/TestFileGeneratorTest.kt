package com.example.testgen.generator

import com.example.testgen.domain.DtoField
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.domain.ParsedUseCase
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class TestFileGeneratorTest {
    @Test
    fun `should generate test file for CreateOrderUseCase`() {
        val useCase = ParsedUseCase(
            className = "CreateOrderUseCase",
            packageName = "com.example.order.usecase",
            requestTypeName = "CreateOrderRequest",
            responseTypeName = "CreateOrderResponse",
            requestFields = listOf(
                DtoField("userId", "String", false),
                DtoField("items", "List<OrderItem>", false),
                DtoField("shippingAddress", "String", false)
            ),
            responseFields = listOf(
                DtoField("orderId", "String", false),
                DtoField("status", "String", false)
            )
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(useCase)

        assertThat(generatedFile).isNotNull
        assertThat(generatedFile.filename).contains("CreateOrderUseCase")
        assertThat(generatedFile.filename).endsWith("Test.kt")
        assertThat(generatedFile.content).contains("CreateOrderUseCaseTest")
        assertThat(generatedFile.content).contains("@SpringBootTest")
        assertThat(generatedFile.content).contains("WebTestClient")
        assertThat(generatedFile.content).contains("TODO")
    }

    @Test
    fun `should include request and response types in generated test`() {
        val useCase = ParsedUseCase(
            className = "GetUserUseCase",
            packageName = "com.example.user.usecase",
            requestTypeName = "GetUserRequest",
            responseTypeName = "GetUserResponse",
            requestFields = emptyList(),
            responseFields = emptyList()
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(useCase)

        assertThat(generatedFile.content).contains("GetUserRequest")
        assertThat(generatedFile.content).contains("GetUserResponse")
        assertThat(generatedFile.content).contains("GetUserUseCaseTest")
    }

    @Test
    fun `should generate compilable Kotlin code`() {
        val useCase = ParsedUseCase(
            className = "SimpleUseCase",
            packageName = "com.example.simple",
            requestTypeName = "SimpleRequest",
            responseTypeName = "SimpleResponse",
            requestFields = listOf(
                DtoField("id", "String", false)
            ),
            responseFields = listOf(
                DtoField("id", "String", false),
                DtoField("message", "String", false)
            )
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(useCase)

        assertThat(generatedFile.content).isNotBlank
        assertThat(generatedFile.content).contains("class SimpleUseCaseTest")
    }
}
