package it.scadenziario.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

data class OffInfo(
    val name: String, val brand: String, val size: String, val category: Category,
    val kcal: String, val fat: String, val sat: String, val carb: String,
    val sugar: String, val protein: String, val salt: String
)

/** Cerca il prodotto nel database pubblico Open Food Facts. */
object Off {
    suspend fun lookup(code: String): OffInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/" +
                    URLEncoder.encode(code, "UTF-8") +
                    ".json?fields=product_name,product_name_it,brands,quantity,nutriments,categories_tags"
            )
            val c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Scadenziario-Android/1.0")
            }
            try {
                if (c.responseCode != 200) return@withContext null
                val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                if (j.optInt("status") != 1) return@withContext null
                val p = j.optJSONObject("product") ?: return@withContext null
                val name = p.optString("product_name_it").ifBlank { p.optString("product_name") }
                if (name.isBlank()) return@withContext null
                val n = p.optJSONObject("nutriments")
                fun nv(key: String): String {
                    val d = n?.optDouble(key, Double.NaN) ?: Double.NaN
                    return if (d.isNaN()) "" else (Math.round(d * 10) / 10.0).toString().removeSuffix(".0")
                }
                val tags = p.optJSONArray("categories_tags")?.let { arr ->
                    (0 until arr.length()).joinToString(" ") { arr.optString(it) }
                } ?: ""
                OffInfo(
                    name = name,
                    brand = p.optString("brands").split(",").first().trim(),
                    size = p.optString("quantity"),
                    category = guessCategory(tags),
                    kcal = nv("energy-kcal_100g"), fat = nv("fat_100g"), sat = nv("saturated-fat_100g"),
                    carb = nv("carbohydrates_100g"), sugar = nv("sugars_100g"),
                    protein = nv("proteins_100g"), salt = nv("salt_100g")
                )
            } finally {
                c.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun guessCategory(t: String): Category = when {
        Regex("alcoholic-beverages|wines|beers|spirits").containsMatchIn(t) -> Category.ALCOLICI
        Regex("frozen").containsMatchIn(t) -> Category.CONGELATI
        Regex("beverages|waters|sodas|juices").containsMatchIn(t) -> Category.BEVANDE
        Regex("flours|pasta|rice|cereals|semolina").containsMatchIn(t) -> Category.FARINA
        Regex("dairies|milks|cheeses|eggs|butters|yogurts").containsMatchIn(t) -> Category.LATTICINI
        Regex("snacks|sweet|chocolates|biscuits|candies|confectioneries").containsMatchIn(t) -> Category.DOLCI
        else -> Category.CIBO
    }
}

data class Scanned(val code: String, val expiryDay: Long?)

/** Interpreta il testo del codice. Se è un GS1 con scadenza (AI 17) la estrae, altrimenti usa il numero così com'è. */
fun parseScan(raw: String): Scanned {
    val s = raw.trim()
    var gtin = Regex("\\(01\\)(\\d{14})").find(s)?.groupValues?.get(1)
    var exp = Regex("\\(17\\)(\\d{6})").find(s)?.groupValues?.get(1)
    if (gtin == null && s.length >= 16 && s.startsWith("01") && s.substring(2, 16).all { it.isDigit() }) {
        gtin = s.substring(2, 16)
        if (s.length >= 24 && s.substring(16, 18) == "17" && s.substring(18, 24).all { it.isDigit() }) {
            exp = s.substring(18, 24)
        }
    }
    val code = when {
        gtin == null -> s
        gtin.startsWith("0") -> gtin.substring(1)
        else -> gtin
    }
    val day = exp?.let {
        try {
            val yy = it.substring(0, 2).toInt()
            val mm = it.substring(2, 4).toInt()
            var dd = it.substring(4, 6).toInt()
            if (dd == 0) dd = LocalDate.of(2000 + yy, mm, 1).lengthOfMonth()
            LocalDate.of(2000 + yy, mm, dd).toEpochDay()
        } catch (e: Exception) {
            null
        }
    }
    return Scanned(code, day)
}
