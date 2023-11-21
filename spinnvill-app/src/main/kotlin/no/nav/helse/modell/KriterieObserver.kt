package no.nav.helse.modell

interface KriterieObserver {
    fun avvikVurdert(harAkseptabeltAvvik: Boolean, avviksprosent: Double) {}
}