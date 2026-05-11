package com.turbodownloader.data.model

data class DownloadRequest(
    val url: String,
    val fileName: String = "",
    val headers: Map<String, String> = emptyMap(),
    val threadCount: Int = 0
)
