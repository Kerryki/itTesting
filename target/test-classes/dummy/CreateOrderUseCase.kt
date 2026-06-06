package com.example.order.usecase

import com.example.order.dto.CreateOrderRequest
import com.example.order.dto.CreateOrderResponse

class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val productService: ProductService
) {
    fun execute(request: CreateOrderRequest): CreateOrderResponse {
        val orderId = generateOrderId()
        val totalAmount = calculateTotal(request.items)

        val order = Order(
            id = orderId,
            userId = request.userId,
            items = request.items,
            shippingAddress = request.shippingAddress,
            totalAmount = totalAmount,
            status = "PENDING"
        )

        orderRepository.save(order)

        return CreateOrderResponse(
            orderId = orderId,
            status = "PENDING",
            totalAmount = totalAmount,
            createdAt = java.time.LocalDateTime.now(),
            estimatedDelivery = "2024-01-15"
        )
    }

    private fun generateOrderId(): String = "ORD-${System.currentTimeMillis()}"

    private fun calculateTotal(items: List<OrderItem>): BigDecimal {
        return items.map { it.quantity * it.price }
            .fold(BigDecimal.ZERO) { acc, value -> acc + value }
    }
}

interface OrderRepository {
    fun save(order: Order)
    fun findById(id: String): Order?
}

interface ProductService {
    fun getProduct(id: String): Product?
}

data class Order(
    val id: String,
    val userId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val totalAmount: BigDecimal,
    val status: String
)

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: BigDecimal
)

data class Product(
    val id: String,
    val name: String,
    val price: BigDecimal
)
