package it.scadenziario.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Tipi di prodotto con IVA indicativa italiana. Verifica le aliquote con il tuo commercialista. */
enum class Category(val label: String, val iva: Int, val emoji: String) {
    BEVANDE("Bevande analcoliche", 10, "🥤"),
    ALCOLICI("Bevande alcoliche", 22, "🍷"),
    CIBO("Cibo confezionato", 10, "🥫"),
    CONGELATI("Congelati", 10, "🧊"),
    FARINA("Farina, pasta e riso", 4, "🌾"),
    LATTICINI("Latticini e uova", 4, "🥛"),
    DOLCI("Dolci e snack", 10, "🍫"),
    ALTRO("Altro", 22, "📦");

    companion object {
        fun from(name: String): Category = entries.firstOrNull { it.name == name } ?: ALTRO
    }
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val quantity: Int = 1,
    val size: String = "",
    val category: String = Category.CIBO.name,
    /** Scadenza come numero di giorni dal 1/1/1970 (0 = non impostata). */
    val expiryDay: Long = 0L,
    val kcal: String = "",
    val fat: String = "",
    val sat: String = "",
    val carb: String = "",
    val sugar: String = "",
    val protein: String = "",
    val salt: String = "",
    val note: String = ""
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY expiryDay ASC")
    fun all(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :code AND barcode != '' ORDER BY expiryDay ASC")
    suspend fun byBarcode(code: String): List<Product>

    @Query("SELECT * FROM products WHERE expiryDay <= :limit ORDER BY expiryDay ASC")
    suspend fun expiringUntil(limit: Long): List<Product>

    @Insert
    suspend fun insert(p: Product): Long

    @Update
    suspend fun update(p: Product)

    @Delete
    suspend fun delete(p: Product)
}

@Database(entities = [Product::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ProductDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "scadenziario.db"
            ).build().also { instance = it }
        }
    }
}
