package no.nav.helse

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    val enabled get() = _enabled

    object LesemodusOnly : Toggle("LESEMODUS", default = false)
}