package com.example.testgen.domain

data class DataClassInfo(
    val name: String,
    val packageName: String,
    val fields: List<DtoField>
)
