package com.vinreader

/**
 * VIN 基础校验工具
 * VIN 规则: 17位，不含 I、O、Q
 */
object VinValidator {

    /** 合法的 VIN 字符集（不含 I, O, Q） */
    private val VALID_CHARS = setOf(
        '0','1','2','3','4','5','6','7','8','9',
        'A','B','C','D','E','F','G','H','J','K',
        'L','M','N','P','R','S','T','U','V','W','X','Y','Z'
    )

    /** 权重 */
    private val WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    /** 字符映射数值 */
    private val CHAR_MAP = mapOf(
        'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7,
        'H' to 8, 'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7,
        'R' to 9, 'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7,
        'Y' to 8, 'Z' to 9
    )

    /**
     * 校验 VIN 基本格式（17位 + 合法字符）
     */
    fun isValidFormat(vin: String): Boolean {
        if (vin.length != 17) return false
        return vin.uppercase().all { it in VALID_CHARS }
    }

    /**
     * 校验 VIN 校验位（第9位）
     */
    fun isValidChecksum(vin: String): Boolean {
        val clean = vin.uppercase()
        if (!isValidFormat(clean)) return false

        var sum = 0
        for (i in 0 until 17) {
            val c = clean[i]
            val value = if (c.isDigit()) c - '0' else CHAR_MAP[c] ?: 0
            sum += value * WEIGHTS[i]
        }

        val remainder = sum % 11
        val expectedCheck = if (remainder == 10) 'X' else ('0' + remainder)
        return clean[8] == expectedCheck
    }

    /**
     * 清洗 OCR 可能误识别的字符
     * 例如: O→0, I→1, Q→9, l→1
     */
    fun cleanOcrResult(raw: String): String {
        return raw.uppercase()
            .replace('O', '0')
            .replace('I', '1')
            .replace('Q', '9')
            .replace('l', '1')
            .filter { it in VALID_CHARS }
            .take(17)
    }

    /**
     * 从一段文本中提取可能的 VIN 码（17位连续纯字母数字）
     */
    fun extractVinFromText(text: String): List<String> {
        val cleaned = text.uppercase()
            .replace('O', '0')
            .replace('I', '1')
            .replace('Q', '9')
        val pattern = Regex("[A-HJ-NPR-Z0-9]{17}")
        return pattern.findAll(cleaned).map { it.value }.toList()
    }
}
