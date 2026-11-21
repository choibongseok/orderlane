@file:OptIn(ExperimentalJsExport::class)

package orderlane.domain

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** 브라우저에서 호출하는 진입점. 결과는 JSON 문자열. */
@JsExport
fun runScenario(scenario: String, variant: String, params: String): String {
    val map = params.split('&').filter { it.contains('=') }.associate { it.substringBefore('=') to it.substringAfter('=') }
    return Simulation.run(scenario, variant, map).toJson()
}

@JsExport
fun listScenarios(): String = Json.obj("scenarios" to Simulation.scenarios.map { RawJson(Json.obj("id" to it, "variants" to Simulation.variants(it))) })
