package si.um.feri

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Parking(
    val id: Int,
    val location: String,
    val typeOfPayment: String,
    val capacity: Int, //kako veliko je parkirisce oz. koliko mest. Morda pride prav za parkirne hise!!!
    val lengthOfParking: List<Pair<Double, Double>>
)

//brezplačna, časovno omejena
val urlKratkotrajna = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&" +
        "request=GetFeature" +
        "&typeName=public:mom_parkirisca_kratkotrajna_l" +
        "&outputFormat=application/json" +
        "&srsName=EPSG:4326"

//plačljiva, parkomati
val urlPlacljiva = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=public:mom_parkirisca_cone_placilo_l" +
        "&outputFormat=application/json" +
        "&srsName=EPSG:4326"

val urlOgrajena = "https://prostor.maribor.si/ows/public/wfs?" +
        "service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=public:mom_parkirisca_ograjena_p" +
        "&outputFormat=application/json" +
        "&srsName=EPSG:4326"
fun downloadData(webUrl: String): String {
    val url = URL(webUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)

    return reader.use { it.readText() }
}

fun parseParkingData(jsonString: String, isPaid: Boolean): List<Parking> {
    val parkingList = mutableListOf<Parking>()
    val allData = JSONObject(jsonString)
    val parkingArray = allData.getJSONArray("features")

    for (i in 0 until parkingArray.length()) {
        val item = parkingArray.getJSONObject(i)
        val info = item.getJSONObject("properties")
        val shape = item.getJSONObject("geometry")

        val polyline = mutableListOf<Pair<Double, Double>>()
        val coordLevels = shape.getJSONArray("coordinates")

        for (j in 0 until coordLevels.length()) {
            val line = coordLevels.getJSONArray(j)
            for (k in 0 until line.length()) {
                val point = line.getJSONArray(k)
                polyline.add(Pair(point.getDouble(1), point.getDouble(0)))
            }
        }

        //lokacija in ulica ne spremnijaj!
        val streetName = info.optString("lokacija", info.optString("ulica", "Neznana lokacija"))

        val cap = info.optString("stevilo_pm", "0").toIntOrNull() ?: 0

        val spot = Parking(
            id = item.getString("id").hashCode(),
            location = streetName,
            typeOfPayment = if (isPaid) "PAYABLE (ZONE)" else "FREE",
            capacity = cap,
            lengthOfParking = polyline
        )
        parkingList.add(spot)
    }
    return parkingList
}

fun parseParkingObjects(jsonString: String): List<Parking> {
    val parkingList = mutableListOf<Parking>()
    val allData = JSONObject(jsonString)
    val features = allData.getJSONArray("features")

    for (i in 0 until features.length()) {
        val item = features.getJSONObject(i)
        val info = item.getJSONObject("properties")
        val geometry = item.getJSONObject("geometry")

        val points = mutableListOf<Pair<Double, Double>>()

        val coordinates = geometry.getJSONArray("coordinates")
        val ring = coordinates.getJSONArray(0)
        for (k in 0 until ring.length()) {
            val coord = ring.getJSONArray(k)
            points.add(Pair(coord.getDouble(1), coord.getDouble(0)))
        }

        parkingList.add(Parking(
            id = item.getString("id").hashCode(),
            location = info.optString("lokacija", "PARKING HOUSE/OBJECT"),
            typeOfPayment = info.optString("PAYABLE", "UNKNOWN"),
            capacity = info.optString("st_park_m", "0").toIntOrNull() ?: 0,
            lengthOfParking = points
        ))
    }
    return parkingList
}

fun parserParking() {
    System.setOut(java.io.PrintStream(System.`out`, true, "UTF-8"))

    try {
        val rawShort = downloadData(urlKratkotrajna)
        val shortTerm = parseParkingData(rawShort,false)

        val rawPaid = downloadData(urlPlacljiva)
        val paidParking = parseParkingData(rawPaid, true)

        val rawOgrajena = downloadData(urlOgrajena)
        val ograjena = parseParkingObjects(rawOgrajena)

        val allTogether = shortTerm + paidParking + ograjena

        println("\n--- MARIBOR PARKING ---")

        allTogether.forEachIndexed { index, spot ->
            val capacityText = if (spot.capacity > 0) spot.capacity.toString() else "Unknown"

            println("${index + 1}. LOCATION: ${spot.location}")
            println("   CAPACITY: $capacityText spaces")
            println("   PAYMENT:  ${spot.typeOfPayment}")
            println("   GEOMETRY: ${spot.lengthOfParking.size} GPS points")
            println("------------------------------")
        }
        println("\nSuccess! Total found: ${allTogether.size}")

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}