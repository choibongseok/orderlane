package orderlane.app.cache

import io.micrometer.core.instrument.MeterRegistry
import orderlane.app.OrderlaneProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

/** 메뉴(재고 포함) 읽기 캐시. 재고가 바뀌는 쓰기가 커밋되면 해당 매장 키를 지운다. */
@Component
class MenuCache(private val redis: StringRedisTemplate, private val props: OrderlaneProperties, private val meters: MeterRegistry) {
    private fun key(storeId: Long) = "menu:$storeId"

    fun get(storeId: Long, loader: () -> String): String {
        redis.opsForValue().get(key(storeId))?.let { meters.counter("orderlane.cache", "result", "hit").increment(); return it }
        meters.counter("orderlane.cache", "result", "miss").increment()
        val fresh = loader()
        redis.opsForValue().set(key(storeId), fresh, Duration.ofSeconds(props.cache.ttlSeconds))
        return fresh
    }

    /** 트랜잭션 안이면 커밋 뒤에, 밖이면 바로 지운다. 롤백되면 지우지 않는다. */
    fun invalidateAfterCommit(storeId: Long) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() { invalidate(storeId) }
            })
        } else invalidate(storeId)
    }

    fun invalidate(storeId: Long) {
        redis.delete(key(storeId))
        meters.counter("orderlane.cache", "result", "invalidated").increment()
    }
}
