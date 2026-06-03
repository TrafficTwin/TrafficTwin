package traffictwin.dsl

enum class RoadState {
    OPEN,
    CONGESTED,
    WORKS,
    CLOSED,
    UNKNOWN
}

enum class PaymentType {
    FREE,
    PAID,
    MIXED,
    UNKNOWN
}

enum class ParkingStatus {
    OPEN,
    FULL,
    CLOSED,
    UNKNOWN
}

enum class QueryTarget {
    PARKING,
    ROAD,
    BUILDING,
    PARK,
    ZONE,
    SENSOR
}

data class CoordinatePoint(
    val longitude: Double,
    val latitude: Double
)