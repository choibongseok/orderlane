package orderlane.app.web

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import orderlane.app.OrderlaneProperties
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

data class ApiError(val code: String, val message: String)

class ApiException(val status: Int, val code: String, message: String) : RuntimeException(message)

fun ApiError.json(mapper: ObjectMapper): String = mapper.writeValueAsString(this)

@RestControllerAdvice
class ErrorHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun api(e: ApiException) = ResponseEntity.status(e.status).contentType(MediaType.APPLICATION_JSON).body(ApiError(e.code, e.message ?: ""))

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun header(e: MissingRequestHeaderException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("MISSING_HEADER", "${e.headerName} 헤더가 필요하다"))

    @ExceptionHandler(HttpMessageNotReadableException::class, IllegalArgumentException::class)
    fun bad(e: Exception) = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("BAD_REQUEST", e.message?.take(200) ?: "요청 형식 오류"))

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException::class)
    fun notFound(e: Exception) = ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError("NOT_FOUND", "없는 경로다"))

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException::class)
    fun method(e: Exception) = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiError("METHOD_NOT_ALLOWED", "허용되지 않는 메서드다"))

    @ExceptionHandler(Exception::class)
    fun other(e: Exception): ResponseEntity<ApiError> {
        log.error("처리하지 못한 오류", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError("INTERNAL", "서버 오류"))
    }
}

/** 요청 추적 ID 를 로그(MDC)와 응답 헤더에 싣는다. 들어온 X-Request-Id 가 있으면 이어 쓴다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val id = req.getHeader("X-Request-Id")?.takeIf { it.isNotBlank() && it.length <= 64 } ?: UUID.randomUUID().toString()
        MDC.put("traceId", id)
        res.setHeader("X-Request-Id", id)
        try { chain.doFilter(req, res) } finally { MDC.remove("traceId") }
    }
}

/** 고정 토큰 인증. 헬스 체크·지표는 열어 두고, 게이트웨이 콜백은 별도 비밀 헤더를 쓴다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class TokenFilter(private val props: OrderlaneProperties, private val mapper: ObjectMapper) : OncePerRequestFilter() {
    override fun shouldNotFilter(req: HttpServletRequest): Boolean {
        val p = req.requestURI
        return p.startsWith("/actuator") || (req.method == "GET" && p.matches(Regex("/stores/\\d+/(menu|kitchen)")))
    }

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val ok = if (req.requestURI.startsWith("/callbacks/")) req.getHeader("X-Callback-Secret") == props.auth.callbackSecret
        else req.getHeader("Authorization") == "Bearer ${props.auth.token}"
        if (!ok) {
            res.status = 401
            res.contentType = MediaType.APPLICATION_JSON_VALUE
            res.writer.write(mapper.writeValueAsString(ApiError("UNAUTHORIZED", "인증 정보가 없거나 틀렸다")))
            return
        }
        chain.doFilter(req, res)
    }
}
