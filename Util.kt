package it.scadenziario.app

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object Dates {
    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    fun today(): Long = LocalDate.now().toEpochDay()
    fun format(day: Long): String = LocalDate.ofEpochDay(day).format(fmt)
}

enum class Level { OK, WARN, BAD }

data class Status(val level: Level, val text: String)

fun statusOf(day: Long, warnDays: Int): Status {
    val n = (day - Dates.today()).toInt()
    return when {
        n < 0 -> Status(Level.BAD, if (n == -1) "scaduto ieri" else "scaduto da ${-n} g")
        n == 0 -> Status(Level.BAD, "scade oggi")
        n <= warnDays -> Status(Level.WARN, if (n == 1) "domani" else "tra $n g")
        else -> Status(Level.OK, "tra $n g")
    }
}

/* ---------- Ricerca intelligente: ignora accenti e maiuscole, tollera un errore di battitura ---------- */

private val accents = Regex("\\p{InCombiningDiacriticalMarks}+")
private val spaces = Regex("\\s+")

fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(accents, "")

/** Vero se le due parole differiscono al massimo di una lettera. */
fun near(a: String, b: String): Boolean {
    if (a == b) return true
    if (abs(a.length - b.length) > 1) return false
    var i = 0
    var j = 0
    var e = 0
    while (i < a.length && j < b.length) {
        if (a[i] == b[j]) {
            i++; j++; continue
        }
        e++
        if (e > 1) return false
        if (a.length > b.length) i++ else if (a.length < b.length) j++ else {
            i++; j++
        }
    }
    return e + (a.length - i) + (b.length - j) <= 1
}

private fun score(p: Product, q: String): Int {
    val hay = norm("${p.name} ${p.brand} ${p.barcode}")
    val words = hay.split(spaces)
    var total = 0
    for (t in q.split(spaces).filter { it.isNotBlank() }) {
        val s = when {
            words.any { it.startsWith(t) } -> 3
            hay.contains(t) -> 2
            t.length >= 4 && words.any { near(t, it) || near(t, it.take(t.length)) } -> 1
            else -> 0
        }
        if (s == 0) return 0
        total += s
    }
    return total
}

fun search(list: List<Product>, query: String): List<Product> {
    val q = norm(query.trim())
    if (q.isEmpty()) return emptyList()
    return list.map { it to score(it, q) }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenBy { it.first.expiryDay })
        .map { it.first }
}
