package com.vinreader.api

/**
 * VIN 解码入口
 *
 * 当前使用 DeepSeek AI 大模型进行车架号解码，
 * 替代了原来的 NHTSA VPIC API（不包含中国车辆数据）。
 *
 * 如需切换回 NHTSA，请使用 VinDeepseekDecoder 并修改 BuildConfig.
 *
 * @see VinDeepseekDecoder
 */
object VinApi {

    /**
     * 创建 DeepSeek 解码器实例
     */
    fun createDeepseekDecoder(apiKey: String): VinDeepseekDecoder {
        return VinDeepseekDecoder(apiKey)
    }
}
