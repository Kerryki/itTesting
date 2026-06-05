package com.example.simple

data class SimpleRequest(
    val id: String,
    val name: String
)

data class SimpleResponse(
    val id: String,
    val message: String,
    val success: Boolean
)

class SimpleUseCase {
    fun execute(request: SimpleRequest): SimpleResponse {
        return SimpleResponse(
            id = request.id,
            message = "Processed ${request.name}",
            success = true
        )
    }
}
