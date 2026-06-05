package com.example.order.dto

data class CreateOrderRequest(
    val userId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val paymentMethod: String,
    val notes: String? = null
)

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: BigDecimal
)
