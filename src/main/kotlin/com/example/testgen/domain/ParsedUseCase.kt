package com.example.testgen.domain

data class ParsedUseCase(
    val className: String,
    val packageName: String,
    val requestTypeName: String,
    val responseTypeName: String,
    val requestFields: List<DtoField>,
    val responseFields: List<DtoField>,
    val parameterCount: Int = 0,
    val isListResponse: Boolean = false
)
