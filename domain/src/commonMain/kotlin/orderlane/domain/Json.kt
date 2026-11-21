package orderlane.domain

/** 시뮬레이션 결과를 내보내기 위한 최소 JSON 기록기. 외부 라이브러리 없이 도메인 모듈을 순수하게 둔다. */
internal object Json {
    fun str(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
        return sb.append('"').toString()
    }

    fun obj(vararg pairs: Pair<String, Any?>): String =
        pairs.joinToString(",", "{", "}") { (k, v) -> str(k) + ":" + value(v) }

    fun value(v: Any?): String = when (v) {
        null -> "null"
        is String -> str(v)
        is Boolean, is Int, is Long -> v.toString()
        is List<*> -> v.joinToString(",", "[", "]") { value(it) }
        is RawJson -> v.text
        else -> str(v.toString())
    }
}

internal class RawJson(val text: String)
