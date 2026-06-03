package traffictwin.dsl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoJsonExporterTest {

    private fun parse(source: String): ProgramNode {
        return Parser(Lexer(source.trimIndent()).tokenize()).parse()
    }

    @Test
    fun `exports road line as GeoJSON LineString`() {
        val program = parse(
            """
            city "Geo" {
                road "Glavna" {
                    line((16.1, 46.6), (16.2, 46.7));
                    state OPEN;
                    speedLimit 50;
                };
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"type\": \"FeatureCollection\""))
        assertTrue(geoJson.contains("\"type\": \"LineString\""))
        assertTrue(geoJson.contains("\"dslType\": \"road\""))
        assertTrue(geoJson.contains("\"name\": \"Glavna\""))
        assertTrue(geoJson.contains("\"state\": \"open\""))
        assertTrue(geoJson.contains("\"speedLimit\": 50"))
        assertTrue(geoJson.contains("[16.1, 46.6]"))
        assertTrue(geoJson.contains("[16.2, 46.7]"))
    }

    @Test
    fun `exports building polygon as GeoJSON Polygon`() {
        val program = parse(
            """
            city "Geo" {
                building "Obcina" {
                    polygon((16.1, 46.6), (16.2, 46.6), (16.2, 46.7), (16.1, 46.7));
                };
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"type\": \"Polygon\""))
        assertTrue(geoJson.contains("\"dslType\": \"building\""))
        assertTrue(geoJson.contains("\"name\": \"Obcina\""))
        assertTrue(geoJson.contains("[16.1, 46.6]"))
    }

    @Test
    fun `exports box as GeoJSON Polygon`() {
        val program = parse(
            """
            city "Geo" {
                park "Mestni park" {
                    box((16.1, 46.6), (16.2, 46.7));
                    set("surface", "grass");
                };
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"type\": \"Polygon\""))
        assertTrue(geoJson.contains("\"dslType\": \"park\""))
        assertTrue(geoJson.contains("\"surface\": \"grass\""))
        assertTrue(geoJson.contains("[16.1, 46.6]"))
        assertTrue(geoJson.contains("[16.2, 46.7]"))
    }

    @Test
    fun `exports parking as GeoJSON Point`() {
        val program = parse(
            """
            city "Geo" {
                parking "P1" {
                    id 1;
                    point (16.15, 46.65);
                    capacity 100;
                    occupied 25;
                    payment FREE;
                    status OPEN;
                };
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"type\": \"Point\""))
        assertTrue(geoJson.contains("\"dslType\": \"parking\""))
        assertTrue(geoJson.contains("\"name\": \"P1\""))
        assertTrue(geoJson.contains("\"id\": 1"))
        assertTrue(geoJson.contains("\"capacity\": 100"))
        assertTrue(geoJson.contains("\"occupied\": 25"))
        assertTrue(geoJson.contains("\"payment\": \"free\""))
        assertTrue(geoJson.contains("\"status\": \"open\""))
        assertTrue(geoJson.contains("[16.15, 46.65]"))
    }

    @Test
    fun `exports junction marker and sensor as GeoJSON Point`() {
        val program = parse(
            """
            city "Geo" {
                junction "J1" (16.1, 46.6);
                marker "M1" (16.2, 46.7);
                sensor "S1" (16.3, 46.8) {
                    "type" = "traffic";
                }
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"dslType\": \"junction\""))
        assertTrue(geoJson.contains("\"dslType\": \"marker\""))
        assertTrue(geoJson.contains("\"dslType\": \"sensor\""))
        assertTrue(geoJson.contains("\"type\": \"traffic\""))
        assertTrue(geoJson.contains("[16.1, 46.6]"))
        assertTrue(geoJson.contains("[16.2, 46.7]"))
        assertTrue(geoJson.contains("[16.3, 46.8]"))
    }

    @Test
    fun `resolves let point references and fst snd expressions`() {
        val program = parse(
            """
            city "Geo" {
                let p = (16.1, 46.6);
                let q = (fst(p) + 0.01, snd(p) + 0.005);

                road "Izracunana" {
                    line(p, q);
                };
            }
            """
        )

        val geoJson = GeoJsonExporter().export(program)

        assertTrue(geoJson.contains("\"type\": \"LineString\""))
        assertTrue(geoJson.contains("[16.1, 46.6]"))

        assertTrue(
            geoJson.contains("16.11") ||
                    geoJson.contains("16.110000000000003")
        )

        assertTrue(
            geoJson.contains("46.605") ||
                    geoJson.contains("46.605000000000004")
        )
    }

    @Test
    fun `validation accepts valid GeoJSON export`() {
        val program = parse(
            """
            city "Geo" {
                road "Glavna" {
                    polyline((16.1, 46.6), (16.2, 46.7), (16.3, 46.8));
                };
            }
            """
        )

        val result = GeoJsonExporter().validate(program)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validation rejects invalid latitude`() {
        val program = parse(
            """
            city "Geo" {
                parking "Napacna tocka" {
                    id 1;
                    point (16.1, 120.0);
                    capacity 10;
                    occupied 1;
                    payment FREE;
                };
            }
            """
        )

        val result = GeoJsonExporter().validate(program)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("latitude") })
    }

    @Test
    fun `export throws on invalid GeoJSON when validation is enabled`() {
        val program = parse(
            """
            city "Geo" {
                parking "Napacna tocka" {
                    point (16.1, 120.0);
                };
            }
            """
        )

        try {
            GeoJsonExporter().export(program)
            assertTrue(false, "Pričakovan GeoJsonExportException")
        } catch (ex: GeoJsonExportException) {
            assertTrue(ex.message!!.contains("latitude"))
        }
    }
}