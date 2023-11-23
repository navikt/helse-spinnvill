package no.nav.helse

interface KriterieObserver {
    fun avvikVurdert(harAkseptabeltAvvik: Boolean, avviksprosent: Double) {}
}