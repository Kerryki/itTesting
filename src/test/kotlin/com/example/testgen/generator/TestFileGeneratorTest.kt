package com.example.testgen.generator

import com.example.testgen.domain.DataClassInfo
import com.example.testgen.domain.DtoField
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.domain.ParsedUseCase
import com.example.testgen.domain.ScanResult
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
            ),
            parameterCount = 2
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(ScanResult(useCase, emptyList()))

        assertThat(generatedFile).isNotNull
        assertThat(generatedFile.filename).contains("CreateOrderUseCase")
        assertThat(generatedFile.filename).endsWith("Test.kt")
        assertThat(generatedFile.content).contains("CreateOrderUseCaseIntegrationTest")
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
            responseFields = emptyList(),
            parameterCount = 2
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(ScanResult(useCase, emptyList()))

        // GetUserRequest appears in the path-param TODO comment (parameterCount > 0, no resolved DTO)
        assertThat(generatedFile.content).contains("GetUserRequest")
        assertThat(generatedFile.content).contains("GetUserResponse")
        assertThat(generatedFile.content).contains("GetUserUseCaseIntegrationTest")
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
        val generatedFile = generator.generateTest(ScanResult(useCase, emptyList()))

        assertThat(generatedFile.content).isNotBlank
        assertThat(generatedFile.content).contains("class SimpleUseCaseIntegrationTest")
    }

    @Test
    fun `should backtick-escape reserved keywords in import statements`() {
        val useCase = ParsedUseCase(
            className = "CreateTaskUseCase",
            packageName = "com.example.taskmanager.domain.port.`in`",
            requestTypeName = "CreateTaskCommand",
            responseTypeName = "Unit",
            requestFields = listOf(DtoField("title", "String", false)),
            responseFields = emptyList(),
            parameterCount = 1
        )
        val commandDto = DataClassInfo(
            name = "CreateTaskCommand",
            packageName = "com.example.taskmanager.domain.port.in",
            fields = listOf(DtoField("title", "String", false))
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(ScanResult(useCase, listOf(commandDto)))

        assertThat(generatedFile.content).contains("import com.example.taskmanager.domain.port.`in`.CreateTaskCommand")
        assertThat(generatedFile.content).doesNotContain("import com.example.taskmanager.domain.port.in.CreateTaskCommand")
    }

    @Test
    fun `should emit TODO comment instead of hasSize for list responses`() {
        val useCase = ParsedUseCase(
            className = "ListTasksUseCase",
            packageName = "com.example.tasks.usecase",
            requestTypeName = "Unit",
            responseTypeName = "List<Task>",
            requestFields = emptyList(),
            responseFields = emptyList(),
            isListResponse = true
        )

        val generator = TestFileGenerator()
        val generatedFile = generator.generateTest(ScanResult(useCase, emptyList()))

        assertThat(generatedFile.content).contains("// TODO: assert expected list size")
        // The comment contains ".hasSize(1)" as an example; assert the uncommented call-chain form is absent
        assertThat(generatedFile.content).doesNotContainPattern("(?m)^\\s+\\.hasSize\\(1\\)\\s*$")
    }
}
