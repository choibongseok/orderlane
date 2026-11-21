package orderlane.domain

/** 금액은 정수 최소 단위(원)로만 다룬다. */
data class Money(val minor: Long) : Comparable<Money> {
    operator fun plus(o: Money) = Money(Math2.addExact(minor, o.minor))
    operator fun minus(o: Money) = Money(minor - o.minor)
    operator fun times(n: Int) = Money(minor * n)
    override fun compareTo(other: Money) = minor.compareTo(other.minor)
    override fun toString() = "$minor"
    companion object { val ZERO = Money(0) }
}

internal object Math2 {
    fun addExact(a: Long, b: Long): Long {
        val r = a + b
        require(((a xor r) and (b xor r)) >= 0) { "금액 오버플로" }
        return r
    }
}
