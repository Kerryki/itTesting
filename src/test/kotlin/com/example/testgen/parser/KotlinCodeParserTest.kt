package com.example.testgen.parser

import com.example.testgen.domain.ParsedUseCase
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files
import java.nio.file.Paths

class KotlinCodeParserTest {
    @Test
    fun `should parse CreateOrderUseCase and extract use case details`() {
        val dummyFile = Paths.get("src/test/resources/dummy/CreateOrderUseCase.kt")
        val content = Files.readString(dummyFile)

        val parser = KotlinCodeParser()
        val useCase = parser.parseUseCase(content)

        assertThat(useCase).isNotNull
        assertThat(useCase!!.className).isEqualTo("CreateOrderUseCase")
        assertThat(useCase.packageName).isEqualTo("com.example.order.usecase")
        assertThat(useCase.requestTypeName).isEqualTo("CreateOrderRequest")
        assertThat(useCase.responseTypeName).isEqualTo("CreateOrderResponse")
    }

    @Test
    fun `should extract request fields from DTO`() {
        val dummyFile = Paths.get("src/test/resources/dummy/CreateOrderRequest.kt")
        val content = Files.readString(dummyFile)

        val parser = KotlinCodeParser()
        val dto = parser.parseDataClass(content, "CreateOrderRequest")

        assertThat(dto).isNotNull
        assertThat(dto!!.name).isEqualTo("CreateOrderRequest")
        assertThat(dto.fields).isNotEmpty
        assertThat(dto.fields).hasSize(5)
        assertThat(dto.fields.map { it.name }).contains("userId", "items", "shippingAddress", "paymentMethod", "notes")
    }

    @Test
    fun `should identify nullable fields`() {
        val dummyFile = Paths.get("src/test/resources/dummy/CreateOrderRequest.kt")
        val content = Files.readString(dummyFile)

        val parser = KotlinCodeParser()
        val dto = parser.parseDataClass(content, "CreateOrderRequest")

        assertThat(dto).isNotNull
        val notesField = dto!!.fields.find { it.name == "notes" }
        assertThat(notesField).isNotNull
        assertThat(notesField!!.nullable).isTrue

        val userIdField = dto.fields.find { it.name == "userId" }
        assertThat(userIdField!!.nullable).isFalse
    }

    @Test
    fun `should handle non-existent use case gracefully`() {
        val invalidContent = "class SomeOtherClass {}"

        val parser = KotlinCodeParser()
        val useCase = parser.parseUseCase(invalidContent)

        assertThat(useCase).isNull()
    }

    @Test
    fun `should parse interface-based UseCase (hexagonal pattern)`() {
        val content = """
            package com.example.taskmanager.domain.port.`in`
            interface CreateTaskUseCase {
                fun createTask(command: CreateTaskCommand): Task
            }
            data class CreateTaskCommand(val title: String, val description: String?)
        """.trimIndent()

        val parser = KotlinCodeParser()
        val useCase = parser.parseUseCase(content)

        assertThat(useCase).isNotNull
        val nonNullUseCase = requireNotNull(useCase) { "useCase must not be null" }
        assertThat(nonNullUseCase.className).isEqualTo("CreateTaskUseCase")
        assertThat(nonNullUseCase.requestTypeName).isEqualTo("CreateTaskCommand")
        assertThat(nonNullUseCase.responseTypeName).isEqualTo("Task")
    }

    @Test
    fun `should return null for interface with more than one method`() {
        val content = """
            interface TwoMethodUseCase {
                fun doA(cmd: CmdA): ResultA
                fun doB(cmd: CmdB): ResultB
            }
        """.trimIndent()

        val parser = KotlinCodeParser()
        val useCase = parser.parseUseCase(content)

        assertThat(useCase).isNull()
    }
}
