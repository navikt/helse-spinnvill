package no.nav.helse.helpers

import java.time.LocalDate
import java.time.YearMonth

fun Int.januar(year: Int = 2018): LocalDate = LocalDate.of(year, 1, this)
fun Int.februar(year: Int = 2018): LocalDate = LocalDate.of(year, 2, this)
fun Int.mars(year: Int = 2018): LocalDate = LocalDate.of(year, 3, this)
fun Int.april(year: Int = 2018): LocalDate = LocalDate.of(year, 4, this)
fun Int.mai(year: Int = 2018): LocalDate = LocalDate.of(year, 5, this)
fun Int.juni(year: Int = 2018): LocalDate = LocalDate.of(year, 6, this)
fun Int.juli(year: Int = 2018): LocalDate = LocalDate.of(year, 7, this)
fun Int.august(year: Int = 2018): LocalDate = LocalDate.of(year, 8, this)
fun Int.september(year: Int = 2018): LocalDate = LocalDate.of(year, 9, this)
fun Int.oktober(year: Int = 2018): LocalDate = LocalDate.of(year, 10, this)
fun Int.november(year: Int = 2018): LocalDate = LocalDate.of(year, 11, this)
fun Int.desember(year: Int = 2018): LocalDate = LocalDate.of(year, 12, this)
val Int.januar get() = this.januar()
val Int.februar get() = this.februar()
val Int.mars get() = this.mars()
val Int.april get() = this.april()

fun januar(year: Int = 2018): YearMonth = YearMonth.from(1.januar(year))
fun desember(year: Int = 2018): YearMonth = YearMonth.from(1.desember(year))
