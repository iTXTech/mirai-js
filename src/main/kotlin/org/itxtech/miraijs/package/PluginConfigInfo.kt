package org.itxtech.miraijs.`package`

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PluginConfigInfo(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String = "<No name>",
    @SerialName("author")
    val author: String = "<No author>",
    @SerialName("description")
    val description: String = "<No description>",
    @SerialName("order")
    val order: List<String> = listOf("...", "test")
)