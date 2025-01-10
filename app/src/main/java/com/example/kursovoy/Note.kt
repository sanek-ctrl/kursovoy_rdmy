package com.example.kursovoy

data class Note(
    var id: String? = null,
    val title: String? = null,
    val content: String? = null,
    var isFavorite: Boolean = false,
    var isInTrash: Boolean = false
)