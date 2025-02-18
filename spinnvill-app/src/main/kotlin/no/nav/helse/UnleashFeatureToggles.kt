package no.nav.helse

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig


class UnleashFeatureToggles(env: Map<String, String>) : FeatureToggles {
    private val apiKey = requireNotNull(env["UNLEASH_SERVER_API_TOKEN"])
    private val apiUrl = requireNotNull(env["UNLEASH_SERVER_API_URL"]) + "/api"
    private val apiEnv = requireNotNull(env["UNLEASH_SERVER_API_ENV"])
    private val config: UnleashConfig =
        UnleashConfig.builder()
            .appName("spinnvill")
            .instanceId("spinnvill")
            .unleashAPI(apiUrl)
            .apiKey(apiKey)
            .environment(apiEnv)
            .build()

    private val unleash: Unleash = DefaultUnleash(config)

}
