package com.example.testgen.domain

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class DtoFieldTest {
    @Test
    fun `should create DtoField with all properties`() {
        val field = DtoField(
            name = "userId",
            type = "String",
            nullable = false
        )

        assertThat(field.name).isEqualTo("userId")
        assertThat(field.type).isEqualTo("String")
        assertThat(field.nullable).isFalse
    }

    @Test
    fun `should create nullable DtoField`() {
        val field = DtoField(
            name = "middleName",
            type = "String",
            nullable = true
        )

        assertThat(field.name).isEqualTo("middleName")
        assertThat(field.type).isEqualTo("String")
        assertThat(field.nullable).isTrue
    }

    @Test
    fun `should default nullable to false`() {
        val field = DtoField(
            name = "orderId",
            type = "String"
        )

        assertThat(field.nullable).isFalse
    }

    @Test
    fun `should support complex types`() {
        val field = DtoField(
            name = "amount",
            type = "BigDecimal",
            nullable = false
        )

        assertThat(field.type).isEqualTo("BigDecimal")
    }
}
