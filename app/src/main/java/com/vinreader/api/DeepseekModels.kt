package com.vinreader.api

/** DeepSeek Chat Completion 请求体 */
data class DeepseekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepseekMessage>,
    val temperature: Double = 0.01,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class DeepseekMessage(
    val role: String,  // "system" | "user" | "assistant"
    val content: String
)

/** DeepSeek Chat Completion 响应体 */
data class DeepseekResponse(
    val id: String?,
    val choices: List<DeepseekChoice>?,
    val usage: DeepseekUsage?,
    val error: DeepseekApiError?      // API 层错误
)

data class DeepseekChoice(
    val index: Int,
    val message: DeepseekMessage,
    val finish_reason: String?
)

data class DeepseekUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class DeepseekApiError(
    val message: String,
    val type: String?
)
