package com.example.lexiscan.data.model

/**
 * 单物品识别结果
 */
data class ObjectRecognitionResult(
    val itemName: ItemName,
    val boundingBox: BoundingBox,
    val confidence: Float,
    val category: String? = null,
    val description: String? = null
)

/**
 * 物品名称信息（含LLM标准化后的完整学习信息）
 */
data class ItemName(
    val english: String,
    val chinese: String,
    val phonetic: String? = null,
    val plural: String? = null,           // 复数形式，如 "apples"
    val exampleSentence: String? = null   // 少儿英语短句例句
) {
    companion object {
        /**
         * 从百度API标签创建物品名称（兜底）
         * 注意：百度API返回的是中文标签，需要转换为英文
         */
        fun fromBaiduLabel(label: String): ItemName {
            val englishName = when (label) {
                "果蔬生鲜" -> "Fruits and Vegetables"
                "家居家纺" -> "Home Textiles"
                "食品饮料" -> "Food and Beverage"
                "文化娱乐" -> "Culture and Entertainment"
                "家居家电" -> "Home Appliances"
                else -> label
            }

            return ItemName(
                english = englishName,
                chinese = label,
                phonetic = null
            )
        }
    }
}

/**
 * 边界框信息
 */
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)