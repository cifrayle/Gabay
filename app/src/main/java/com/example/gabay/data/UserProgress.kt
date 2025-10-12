package com.example.gabay.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserProgress(
    val id: String? = null,
    val user_id: String,
    val chapter_number: Int,
    val level_number: Int,
    val completed: Boolean = true,
    val completed_at: String? = null
) {
    constructor(userId: String, chapter: Int, level: Int) : this(
        user_id = userId,
        chapter_number = chapter,
        level_number = level,
        completed = true,
    )
}