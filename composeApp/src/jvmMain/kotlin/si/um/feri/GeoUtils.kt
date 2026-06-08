package si.um.feri

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun hasValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
    return latitude != null && longitude != null &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
}

fun haversineDistanceMeters(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double
): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(toLatitude - fromLatitude)
    val dLon = Math.toRadians(toLongitude - fromLongitude)
    val lat1 = Math.toRadians(fromLatitude)
    val lat2 = Math.toRadians(toLatitude)

    val a = sin(dLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}

fun isParkingInsideRadius(
    parking: ParkingDto,
    centerLatitude: Double,
    centerLongitude: Double,
    radiusMeters: Double
): Boolean {
    if (!hasValidCoordinates(parking.latitude, parking.longitude)) return false

    val distance = haversineDistanceMeters(
        centerLatitude,
        centerLongitude,
        parking.latitude!!,
        parking.longitude!!
    )

    return distance <= radiusMeters
}