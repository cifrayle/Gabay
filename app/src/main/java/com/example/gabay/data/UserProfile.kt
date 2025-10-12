package com.example.gabay.data

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String? = null,
    val total_chapters_completed: Int = 0,
    val total_quizzes_passed: Int = 0,
    val created_at: String? = null
)