package com.example.taskmanager.domain.port.`in`

import com.example.taskmanager.domain.model.Task

interface CreateTaskUseCase {
    fun createTask(command: CreateTaskCommand): Task
}

data class CreateTaskCommand(
    val title: String,
    val description: String?
)
