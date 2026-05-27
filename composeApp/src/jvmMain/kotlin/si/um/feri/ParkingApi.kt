package si.um.feri

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class ParkingDto(
    val id: Int,
    val location: String,
    val typeOfPayment: String,
    val capacity: Int,
    val occupied: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

object ParkingApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE: String get() = ApiConfig.baseUrl

    fun getAll(): List<ParkingDto> {
        val req = Request.Builder().url("$BASE/api/parking").get().build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val type = object : TypeToken<List<ParkingDto>>() {}.type
        return gson.fromJson(body, type)
    }

    fun add(p: ParkingDto): Boolean {
        val body = gson.toJson(p).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking").post(body).build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun update(id: Int, p: ParkingDto): Boolean {
        val body = gson.toJson(p).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking/$id").put(body).build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun delete(id: Int): Boolean {
        val req = Request.Builder().url("$BASE/api/parking/$id").delete().build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun sync(list: List<ParkingDto>): Boolean {
        val body = gson.toJson(list).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking/sync").post(body).build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun fromParking(p: Parking, occupied: Int = 0) = ParkingDto(
        id = p.id,
        location = p.location,
        typeOfPayment = p.typeOfPayment,
        capacity = p.capacity,
        occupied = occupied,
        latitude = p.latitude,
        longitude = p.longitude
    )
}