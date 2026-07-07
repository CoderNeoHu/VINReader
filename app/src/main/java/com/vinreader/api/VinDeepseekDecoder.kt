package com.vinreader.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 基于 DeepSeek 大模型的 VIN 解码器
 *
 * 调用 DeepSeek Chat Completion API，让 AI 根据训练数据中的车辆知识
 * 解码车架号并返回结构化的车辆信息。
 */
class VinDeepseekDecoder(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://api.deepseek.com/chat/completions"
        private const val MODEL = "deepseek-chat"

        /** 请求 DeepSeek 返回的字段列表 */
        val EXPECTED_FIELDS = listOf(
            "品牌", "制造商", "型号", "车系", "年款", "车辆类型", "车身形式",
            "发动机型号", "排量", "最大功率", "燃油类型", "驱动方式",
            "变速箱", "变速档位数", "座位数", "车门数",
            "生产工厂", "生产城市", "生产国家"
        )

        /** 系统提示词：指导 DeepSeek 如何解析并返回 VIN 数据 */
        private const val SYSTEM_PROMPT = """你是一个严格的VIN解码器。你的输出将被程序自动解析，因此必须严格遵守格式要求。

规则：
1. 只输出纯JSON对象，前后不要有任何其他文字、不要markdown代码块标记、不要注释
2. 输出必须是严格的JSON格式，可以被标准的JSON解析器直接解析
3. 不知道或不确定的字段值设为null，不要编造
4. 特别重视中国品牌车辆(VIN首位为L)的数据准确性

必须包含以下字段，尽可能填写完整：
{"品牌":"","制造商":"","型号":"","车系":"","年款":"","车辆类型":"","车身形式":"","发动机型号":"","排量":"","最大功率":"","燃油类型":"","驱动方式":"","变速箱":"","变速档位数":"","座位数":"","车门数":"","生产工厂":"","生产城市":"","生产国家":""}"""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 解码车架号
     *
     * @param vin 17位车架号
     * @return 车辆信息键值对，解析失败时包含 "原始结果" 或 "错误" 字段
     */
    suspend fun decodeVin(vin: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = DeepseekRequest(
                model = MODEL,
                messages = listOf(
                    DeepseekMessage("system", SYSTEM_PROMPT),
                    DeepseekMessage("user", "请解析车架号：$vin")
                ),
                temperature = 0.01,  // 低温度以获得准确稳定的结果
                max_tokens = 1024
            )

            val jsonBody = gson.toJson(requestBody)
            val body = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: throw Exception("服务器返回空响应")

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $responseBody")
            }

            val deepseekResp = gson.fromJson(responseBody, DeepseekResponse::class.java)

            // 检查 API 层错误
            if (deepseekResp.error != null) {
                throw Exception("API 错误: ${deepseekResp.error.message}")
            }

            val content = deepseekResp.choices?.firstOrNull()?.message?.content
                ?: throw Exception("模型未返回有效内容")

            // 解析 JSON
            parseVehicleJson(content)
        } catch (e: Exception) {
            mapOf("错误" to "查询失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * 从 DeepSeek 返回的文本中提取并解析 JSON
     */
    private fun parseVehicleJson(rawContent: String): Map<String, String> {
        try {
            // 清理可能的 markdown 代码块标记和多余空白
            val cleanJson = rawContent
                .trim()
                .replace(Regex("""^```(?:json)?\s*"""), "")
                .replace(Regex("""\s*```$"""), "")
                .trim()

            val type = object : TypeToken<Map<String, String>>() {}.type
            val parsed: Map<String, String> = gson.fromJson(cleanJson, type)

            // 过滤掉 "未知" 字段，排列成易读顺序
            val result = linkedMapOf<String, String>()
            // 先按预期字段顺序排列
            for (field in EXPECTED_FIELDS) {
                val value = parsed[field]
                if (!value.isNullOrBlank() && value != "未知" && value != "null") {
                    result[field] = value
                }
            }
            // 再补充其他额外字段
            for ((key, value) in parsed) {
                if (key !in EXPECTED_FIELDS && !value.isNullOrBlank() && value != "未知" && value != "null") {
                    result[key] = value
                }
            }

            if (result.isEmpty()) {
                return mapOf("原始结果" to rawContent)
            }
            return result
        } catch (e: Exception) {
            // JSON 解析失败，返回原始文本
            return mapOf("原始结果" to rawContent)
        }
    }
}
