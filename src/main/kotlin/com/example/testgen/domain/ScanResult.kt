package com.example.testgen.domain

data class ScanResult(
    val useCase: ParsedUseCase,
    val resolvedDtos: List<DataClassInfo>
)
