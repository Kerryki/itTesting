package com.example.order.dto

import java.time.LocalDateTime

data class CreateOrderResponse(
    val orderId: String,
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: LocalDateTime,
    val estimatedDelivery: String
)
