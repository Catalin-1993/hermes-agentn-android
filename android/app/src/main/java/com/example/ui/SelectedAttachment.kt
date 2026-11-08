package com.example.ui

/** A file or picture picked for a message, as the library and project chat bars show it. */
data class SelectedAttachment(
    val type: String,
    val name: String,
    val path: String? = null
)
