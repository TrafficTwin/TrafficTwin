package si.um.feri

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson

val shortTermParkingUrl = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=public:mom_parkirisca_kratkotrajna_l" +
        "&outputFormat=application/json&srsName=EPSG:4326"

val paidParkingUrl = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=public:mom_parkirisca_cone_placilo_l" +
        "&outputFormat=application/json&srsName=EPSG:4326"

val fencedParkingUrl = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=public:mom_parkirisca_ograjena_p" +
        "&outputFormat=application/json&srsName=EPSG:4326"

data class Parking(
    val id: Int,
    val location: String,
    val typeOfPayment: String,
    val capacity: Int,
    val occupied: Int,
    val lengthOfParking: List<Pair<Double, Double>> = emptyList()
)

fun sendToApi(data: List<Parking>) {
    val client = OkHttpClient()
    val gson = Gson()
    val dtoList = data.map { p ->
        ParkingDto(
            id = p.id,
            location = p.location,
            typeOfPayment = p.typeOfPayment,
            capacity = p.capacity,
            occupied = p.occupied
        )
    }
    val json = gson.toJson(dtoList)
    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3000/api/parking/sync")
        .post(body)
        .build()
    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) println("Data successfully sent to API!")
            else println("Sync error: ${response.code}")
        }
    } catch (e: Exception) {
        println("Connection error: ${e.message}")
    }
}

fun downloadData(webUrl: String): String {
    val url = URL(webUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

fun parseStreetParking(jsonString: String, isPaid: Boolean): List<Parking> {
    val parkingList = mutableListOf<Parking>()
    val allData = JSONObject(jsonString)
    val features = allData.getJSONArray("features")
    for (i in 0 until features.length()) {
        val item = features.getJSONObject(i)
        val properties = item.getJSONObject("properties")
        val geometry = item.getJSONObject("geometry")
        val polyline = mutableListOf<Pair<Double, Double>>()
        if (geometry.has("coordinates")) {
            val coordLevels = geometry.getJSONArray("coordinates")
            for (j in 0 until coordLevels.length()) {
                val line = coordLevels.getJSONArray(j)
                for (k in 0 until line.length()) {
                    val point = line.getJSONArray(k)
                    polyline.add(Pair(point.getDouble(1), point.getDouble(0)))
                }
            }
        }
        var cap = properties.optString("stevilo_pm", "0").toIntOrNull() ?: 0
        if (cap <= 0) cap = (10..40).random()
        val occ = (0..cap).random()
        val streetName = properties.optString("lokacija", properties.optString("ulica", "Unknown Location"))
        parkingList.add(
            Parking(
                id = item.getString("id").hashCode(),
                location = streetName,
                typeOfPayment = if (isPaid) "PAYABLE (ZONE)" else "FREE",
                capacity = cap,
                occupied = occ,
                lengthOfParking = polyline
            )
        )
    }
    return parkingList
}

fun parseFencedParking(jsonString: String): List<Parking> {
    val parkingList = mutableListOf<Parking>()
    val allData = JSONObject(jsonString)
    val features = allData.getJSONArray("features")
    for (i in 0 until features.length()) {
        val item = features.getJSONObject(i)
        val properties = item.getJSONObject("properties")
        val geometry = item.getJSONObject("geometry")
        val points = mutableListOf<Pair<Double, Double>>()
        val coordinates = geometry.getJSONArray("coordinates")
        val ring = coordinates.getJSONArray(0)
        for (k in 0 until ring.length()) {
            val coord = ring.getJSONArray(k)
            points.add(Pair(coord.getDouble(1), coord.getDouble(0)))
        }
        var cap = properties.optString("st_park_m", "0").toIntOrNull() ?: 0
        if (cap <= 0) cap = (50..150).random()
        val occ = (0..(cap / 2)).random()
        parkingList.add(
            Parking(
                id = item.getString("id").hashCode(),
                location = properties.optString("lokacija", "PARKING HOUSE/OBJECT"),
                typeOfPayment = "PAYABLE",
                capacity = cap,
                occupied = occ,
                lengthOfParking = points
            )
        )
    }
    return parkingList
}

fun runParser() {
    System.setOut(java.io.PrintStream(System.`out`, true, "UTF-8"))
    try {
        println("Downloading parking data for Maribor")
        val rawShortTerm = downloadData(shortTermParkingUrl)
        val shortTermList = parseStreetParking(rawShortTerm, false)
        val rawPaid = downloadData(paidParkingUrl)
        val paidList = parseStreetParking(rawPaid, true)
        val rawFenced = downloadData(fencedParkingUrl)
        val fencedList = parseFencedParking(rawFenced)
        val allTogether = shortTermList + paidList + fencedList
        println("Total locations found: ${allTogether.size}")
        if (allTogether.isNotEmpty()) {
            sendToApi(allTogether)
        }
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
        e.printStackTrace()
    }
}