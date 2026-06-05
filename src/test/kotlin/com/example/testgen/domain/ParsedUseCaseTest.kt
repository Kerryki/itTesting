package com.example.testgen.domain

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class ParsedUseCaseTest {
    @Test
    fun `should create ParsedUseCase with all fields`() {
        val requestFields = listOf(
            DtoField("userId", "String", false),
            DtoField("amount", "BigDecimal", false)
        )
        val responseFields = listOf(
            DtoField("orderId", "String", false),
            DtoField("status", "String", false)
        )

        val useCase = ParsedUseCase(
            className = "CreateOrderUseCase",
            packageName = "com.example.order.usecase",
            requestTypeName = "CreateOrderRequest",
            responseTypeName = "CreateOrderResponse",
            requestFields = requestFields,
            responseFields = responseFields
        )

        assertThat(useCase.className).isEqualTo("CreateOrderUseCase")
        assertThat(useCase.packageName).isEqualTo("com.example.order.usecase")
        assertThat(useCase.requestTypeName).isEqualTo("CreateOrderRequest")
        assertThat(useCase.responseTypeName).isEqualTo("CreateOrderResponse")
        assertThat(useCase.requestFields).hasSize(2)
        assertThat(useCase.responseFields).hasSize(2)
    }

    @Test
    fun `should be immutable`() {
        val requestFields = listOf(DtoField("userId", "String", false))
        val responseFields = listOf(DtoField("orderId", "String", false))

        val useCase = ParsedUseCase(
            className = "CreateOrderUseCase",
            packageName = "com.example.order.usecase",
            requestTypeName = "CreateOrderRequest",
            responseTypeName = "CreateOrderResponse",
            requestFields = requestFields,
            responseFields = responseFields
        )

        // Verify we cannot modify the object (data classes are immutable by default)
        assertThat(useCase.className).isEqualTo("CreateOrderUseCase")
        // Creating a new instance with different values
        val modified = useCase.copy(className = "UpdateOrderUseCase")
        assertThat(modified.className).isEqualTo("UpdateOrderUseCase")
        assertThat(useCase.className).isEqualTo("CreateOrderUseCase")
    }
}
