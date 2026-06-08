package si.um.feri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposeAppDesktopTest {

    @Test
    fun centerOfReturnsAverageLatitudeAndLongitude() {
        val center = centerOf(
            listOf(
                46.55 to 15.64,
                46.57 to 15.66
            )
        )

        assertNotNull(center)
        assertEquals(46.56, center.first, 0.0001)
        assertEquals(15.65, center.second, 0.0001)
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        assertFalse(hasValidCoordinates(91.0, 15.0))
        assertFalse(hasValidCoordinates(46.0, 181.0))
        assertFalse(hasValidCoordinates(null, 15.0))
    }

    @Test
    fun nearbyQueryKeepsParkingInsideRadius() {
        val parking = ParkingDto(
            id = 1,
            location = "Glavni trg",
            typeOfPayment = "PAYABLE",
            capacity = 50,
            occupied = 10,
            latitude = 46.5547,
            longitude = 15.6459
        )

        assertTrue(
            isParkingInsideRadius(
                parking = parking,
                centerLatitude = 46.5547,
                centerLongitude = 15.6459,
                radiusMeters = 50.0
            )
        )
    }

    @Test
    fun nearbyQueryDropsParkingOutsideRadius() {
        val parking = ParkingDto(
            id = 2,
            location = "Oddaljeno parkirišče",
            typeOfPayment = "FREE",
            capacity = 20,
            occupied = 5,
            latitude = 46.70,
            longitude = 15.90
        )

        assertFalse(
            isParkingInsideRadius(
                parking = parking,
                centerLatitude = 46.5547,
                centerLongitude = 15.6459,
                radiusMeters = 1000.0
            )
        )
    }
}