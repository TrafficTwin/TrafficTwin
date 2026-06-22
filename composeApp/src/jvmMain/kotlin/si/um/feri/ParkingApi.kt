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
    val longitude: Double? = null,
    val distanceMeters: Double? = null
)

object ParkingApi {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE: String get() = ApiConfig.baseUrl

    private fun Request.Builder.withAuth() =
        addHeader("Authorization", "Bearer ${ApiConfig.jwtToken}")

    fun login(email: String, password: String): Boolean {
        val body = gson.toJson(mapOf("email" to email, "password" to password))
            .toRequestBody(JSON)
        val req = Request.Builder()
            .url("$BASE/api/auth/login")
            .post(body)
            .build()
        return client.newCall(req).execute().use { response ->
            val json = gson.fromJson(response.body!!.string(), Map::class.java)
            val token = json["token"] as? String ?: return false
            ApiConfig.jwtToken = token
            response.isSuccessful
        }
    }
    fun getAll(): List<ParkingDto> {
        val req = Request.Builder().url("$BASE/api/parking").get().withAuth().build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val type = object : TypeToken<List<ParkingDto>>() {}.type
        return gson.fromJson(body, type)
    }

    fun add(p: ParkingDto): Boolean {
        val body = gson.toJson(p).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking").post(body).withAuth().build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun update(id: Int, p: ParkingDto): Boolean {
        val body = gson.toJson(p).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking/$id").put(body).withAuth().build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun delete(id: Int): Boolean {
        val req = Request.Builder().url("$BASE/api/parking/$id").delete().withAuth().build()
        return client.newCall(req).execute().use { it.isSuccessful }
    }

    fun sync(list: List<ParkingDto>): Boolean {
        val body = gson.toJson(list).toRequestBody(JSON)
        val req = Request.Builder().url("$BASE/api/parking/sync").post(body).withAuth().build()
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

    fun runParserLocal(): List<ParkingDto> {
        val rawShortTerm = downloadData(shortTermParkingUrl)
        val rawPaid = downloadData(paidParkingUrl)
        val rawFenced = downloadData(fencedParkingUrl)
        val all = parseStreetParking(rawShortTerm, false) +
                parseStreetParking(rawPaid, true) +
                parseFencedParking(rawFenced)
        return all.map { p ->
            ParkingDto(p.id, p.location, p.typeOfPayment, p.capacity, p.occupied, p.latitude, p.longitude)
        }
    }
    fun getNearby(latitude: Double, longitude: Double, radiusMeters: Int): List<ParkingDto> {
        val req = Request.Builder()
            .url("$BASE/api/parking/nearby?lat=$latitude&lon=$longitude&radius=$radiusMeters")
            .get()
            .withAuth()
            .build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val type = object : TypeToken<List<ParkingDto>>() {}.type
        return gson.fromJson(body, type)
    }
}