package com.example.testgen.domain

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data, error = null)

        fun <T> error(errorMessage: String): ApiResponse<T> =
            ApiResponse(success = false, data = null, error = errorMessage)
    }
}
