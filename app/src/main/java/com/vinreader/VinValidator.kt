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

    /** VIN 首位 WMI（国家/地区代码）常见值 */
    private val COUNTRY_CODES = mapOf(
        'L' to "中国",
        'J' to "日本",
        'K' to "韩国",
        '1' to "美国",
        '2' to "加拿大",
        '3' to "墨西哥",
        '4' to "美国",
        '5' to "美国",
        '6' to "澳大利亚",
        '9' to "巴西",
        'S' to "英国",
        'V' to "法国/欧洲",
        'W' to "德国",
        'Z' to "意大利",
        'Y' to "瑞典",
        'X' to "荷兰/欧洲",
        'N' to "土耳其",
        'M' to "印度/东南亚"
    )

    /** OCR 常见误读映射（双向） */
    private val OCR_CONFUSIONS = mapOf(
        'O' to setOf('0'),
        '0' to setOf('O', 'Q', 'D'),
        'I' to setOf('1', 'l'),
        '1' to setOf('I', 'l', 'L', '7'),
        'L' to setOf('1', '7', 'I'),
        '7' to setOf('1', 'L', 'J', 'T'),
        'Q' to setOf('0', 'O', '9'),
        '9' to setOf('Q', '8', 'g'),
        '8' to setOf('B', '6', '9'),
        'B' to setOf('8', '6', 'R'),
        '6' to setOf('G', '8', 'C'),
        'G' to setOf('6', 'C', 'O'),
        'S' to setOf('5', '8'),
        '5' to setOf('S'),
        'J' to setOf('7', '1', 'U'),
        'T' to setOf('1', '7', 'J'),
        'Z' to setOf('2', '7'),
        '2' to setOf('Z', '7'),
        'D' to setOf('0', 'O')
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
     * 判断 VIN 首位是否是常见的国家/地区代码
     */
    fun hasKnownCountryCode(vin: String): Boolean {
        if (vin.isEmpty()) return false
        return vin[0] in COUNTRY_CODES
    }

    /**
     * 获取 VIN 对应国家描述
     */
    fun getCountryDescription(vin: String): String? {
        if (vin.isEmpty()) return null
        return COUNTRY_CODES[vin[0]]
    }

    /**
     * 清洗 OCR 可能误识别的字符（基础版）
     * 应用最可靠的替换规则
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
     * 智能纠正 OCR 识别结果
     * - 对 WMI（前3位）应用常见误读修正
     * - 优先保留符合校验位的候选
     *
     * @param raw OCR 原始识别文本
     * @return 排序后的候选 VIN 列表（最优在前）
     */
    fun smartCorrect(raw: String): List<CandidateVin> {
        val cleaned = raw.uppercase().filter { it in VALID_CHARS || it in setOf('O', 'I', 'Q', 'l') }
            .replace('O', '0')
            .replace('I', '1')
            .replace('Q', '9')
            .replace('l', '1')
            .filter { it in VALID_CHARS }

        // 提取所有 17 位候选
        if (cleaned.length < 17) return emptyList()

        val candidates = mutableListOf<CandidateVin>()

        // 用滑动窗口提取
        for (start in 0..cleaned.length - 17) {
            val segment = cleaned.substring(start, start + 17)
            // 对首位可能误读的字符生成多种解释
            val firstCharAlternatives = generateAlternatives(segment[0])
            for (fc in firstCharAlternatives) {
                val modified = fc + segment.substring(1)
                val checksumOk = isValidChecksum(modified)
                val knownCountry = hasKnownCountryCode(modified)
                val score = when {
                    checksumOk && knownCountry -> 100
                    checksumOk -> 80
                    knownCountry -> 60
                    else -> 40
                }
                candidates.add(CandidateVin(modified.uppercase(), score, checksumOk, knownCountry))
            }
        }

        // 去重并按分数排序
        return candidates
            .distinctBy { it.vin }
            .sortedByDescending { it.score }
            .take(5)
    }

    /**
     * 为指定字符生成可能的替换候选（针对 OCR 误读）
     */
    private fun generateAlternatives(c: Char): Set<Char> {
        val alternatives = mutableSetOf(c)
        OCR_CONFUSIONS[c]?.let { alternatives.addAll(it) }
        // 对 7 特别处理：中国车最常见是 L
        if (c == '7') alternatives.add('L')
        if (c == '1') alternatives.add('L')
        if (c == 'L') alternatives.add('7')
        return alternatives
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

    /**
     * 获取常见误读修正建议
     */
    fun getCorrectionSuggestions(vin: String): List<String> {
        val suggestions = mutableListOf<String>()
        for (i in vin.indices) {
            val c = vin[i]
            OCR_CONFUSIONS[c]?.let { alternatives ->
                for (alt in alternatives) {
                    val candidate = vin.substring(0, i) + alt + vin.substring(i + 1)
                    if (!suggestions.contains(candidate) && isValidFormat(candidate)) {
                        suggestions.add(candidate)
                    }
                }
            }
        }
        return suggestions.distinct()
    }

    /** 候选 VIN 结果 */
    data class CandidateVin(
        val vin: String,
        val score: Int,
        val checksumValid: Boolean,
        val countryCodeKnown: Boolean
    )
}
