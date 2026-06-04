package traffictwin.dsl

/**
 * Semantični analizator za TrafficTwin DSL.
 *
 * Izvaja preverjanja, ki jih parser ne more — logična in prostorska pravila:
 *  1. Zasedenost parkirišča ne sme presegati kapacitete  (napaka)
 *  2. Geografske koordinate morajo biti v veljavnih mejah (napaka)
 *  3. Ceste ne smejo sekati zgradb                        (opozorilo)
 *  4. Poligoni območij morajo biti zaključeni: prva točka mora biti enaka zadnji (napaka)
 */
class SemanticException(message: String) : Exception(message)

object SemanticValidator {

    data class ValidationResult(
        val errors:   List<String>,
        val warnings: List<String>
    ) {
        val isValid get() = errors.isEmpty()
        fun hasWarnings() = warnings.isNotEmpty()
    }

    fun validateOrThrow(program: ProgramNode) {
        val result = validate(program)
        if (result.errors.isNotEmpty()) {
            throw SemanticException(result.errors.joinToString("\n"))
        }
    }

    fun validate(program: ProgramNode): ValidationResult {
        val errors   = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (stmt in program.statements) {
            if (stmt is CityNode) validateCity(stmt, errors, warnings)
        }

        return ValidationResult(errors, warnings)
    }

    private fun validateCity(city: CityNode, errors: MutableList<String>, warnings: MutableList<String>) {
        val buildingBoxes     = mutableListOf<Pair<String, BoxGeometry>>()
        val buildingPolygons  = mutableListOf<Pair<String, PolygonGeometry>>()

        // 1. Preveri parkirišča in stavbe, zberi geometrije stavb
        for (item in city.items) {
            when (item) {
                is ParkingNode  -> validateParking(item, city.name, errors)
                is BuildingNode -> validateBuilding(item, city.name, errors, buildingBoxes, buildingPolygons)
                is ParkNode     -> validateAreaPolygons(item.name, item.statements, city.name, "park", errors)
                is ZoneNode     -> validateAreaPolygons(item.name, item.statements, city.name, "cona", errors)
                else -> {}
            }
        }

        // 2. Preveri preseke cest s stavbami (T10)
        for (item in city.items) {
            if (item is RoadNode) validateRoad(item, city.name, warnings, buildingBoxes, buildingPolygons)
        }
    }

    // ── Parkirišče ────────────────────────────────────────────────────────────

    private fun validateParking(node: ParkingNode, cityName: String, errors: MutableList<String>) {
        var capacity: Double? = null
        var occupied: Double? = null
        var pointX:   Double? = null
        var pointY:   Double? = null

        for (stmt in node.statements) {
            when (stmt) {
                is CapacityStatement     -> capacity = evalNumber(stmt.expression)
                is OccupiedStatement     -> occupied = evalNumber(stmt.expression)
                is ParkingPointStatement -> {
                    pointX = evalNumber(stmt.point.x)
                    pointY = evalNumber(stmt.point.y)
                }
                else -> {}
            }
        }

        // Pravilo 1: occupied <= capacity
        if (capacity != null && occupied != null && occupied > capacity) {
            errors.add(
                "[${cityName}/${node.name}] Zasedenost ($occupied) presega kapaciteto ($capacity). " +
                        "Vrednost 'occupied' ne sme biti večja od 'capacity'."
            )
        }

        // Pravilo 2: veljavne koordinate
        if (pointX != null && pointY != null) {
            validateCoordinates(pointX, pointY, "${cityName}/${node.name}", errors)
        }
    }

    // ── Stavba ───────────────────────────────────────────────────────────────

    private fun validateBuilding(
        node: BuildingNode,
        cityName: String,
        errors: MutableList<String>,
        buildingBoxes: MutableList<Pair<String, BoxGeometry>>,
        buildingPolygons: MutableList<Pair<String, PolygonGeometry>>
    ) {
        for (stmt in node.statements) {
            when (val geo = (stmt as? GeometryStatement)?.geometry) {
                is BoxGeometry -> {
                    // Pravilo 2: koordinate robov
                    validateCoordinates(
                        evalNumber(geo.first.x), evalNumber(geo.first.y),
                        "${cityName}/${node.name}", errors
                    )
                    validateCoordinates(
                        evalNumber(geo.second.x), evalNumber(geo.second.y),
                        "${cityName}/${node.name}", errors
                    )
                    buildingBoxes.add(node.name to geo)
                }
                is PolygonGeometry -> {
                    validatePolygon(geo, cityName, node.name, "stavba", errors)
                    buildingPolygons.add(node.name to geo)
                }
                else -> {}
            }
        }
    }

    private fun validateAreaPolygons(
        areaName: String,
        statements: List<AreaStatement>,
        cityName: String,
        areaType: String,
        errors: MutableList<String>
    ) {
        for (stmt in statements) {
            val geo = (stmt as? GeometryStatement)?.geometry
            if (geo is PolygonGeometry) {
                validatePolygon(geo, cityName, areaName, areaType, errors)
            }
        }
    }

    private fun validatePolygon(
        polygon: PolygonGeometry,
        cityName: String,
        areaName: String,
        areaType: String,
        errors: MutableList<String>
    ) {
        val points = polygon.points.map { evalNumber(it.x) to evalNumber(it.y) }

        if (points.size < 4) {
            errors.add(
                "[${cityName}/${areaName}] Poligon ($areaType) mora imeti vsaj 4 zapisane točke, " +
                        "ker mora biti zadnja točka enaka prvi. Trenutno jih ima ${points.size}."
            )
            return
        }

        if (points.first() != points.last()) {
            errors.add(
                "[${cityName}/${areaName}] Poligon ($areaType) ni zaključen. " +
                        "Prva točka ${points.first()} mora biti enaka zadnji točki ${points.last()}."
            )
        }

        val uniquePointsWithoutClosingPoint = points.dropLast(1).toSet()
        if (uniquePointsWithoutClosingPoint.size < 3) {
            errors.add(
                "[${cityName}/${areaName}] Poligon ($areaType) mora imeti vsaj 3 različne točke. " +
                        "Trenutno jih ima ${uniquePointsWithoutClosingPoint.size}."
            )
        }

        points.forEachIndexed { index, (x, y) ->
            validateCoordinates(x, y, "${cityName}/${areaName}/polygon[$index]", errors)
        }
    }

    // ── Cesta ─────────────────────────────────────────────────────────────────

    private fun validateRoad(
        node: RoadNode,
        cityName: String,
        warnings: MutableList<String>,
        buildingBoxes: List<Pair<String, BoxGeometry>>,
        buildingPolygons: List<Pair<String, PolygonGeometry>>
    ) {
        for (stmt in node.statements) {
            val geo = (stmt as? GeometryStatement)?.geometry ?: continue

            // Pridobimo segmente ceste kot seznam parov točk
            val segments = roadSegments(geo)

            for ((roadP1, roadP2) in segments) {
                // Preveri presek z vsako stavbo (box geometrija)
                for ((bldName, box) in buildingBoxes) {
                    val minX = minOf(evalNumber(box.first.x),  evalNumber(box.second.x))
                    val maxX = maxOf(evalNumber(box.first.x),  evalNumber(box.second.x))
                    val minY = minOf(evalNumber(box.first.y),  evalNumber(box.second.y))
                    val maxY = maxOf(evalNumber(box.first.y),  evalNumber(box.second.y))

                    if (segmentIntersectsBox(roadP1, roadP2, minX, maxX, minY, maxY)) {
                        warnings.add(
                            "[${cityName}] Cesta '${node.name}' seka stavbo '$bldName'. " +
                                    "Preverite prostorsko umestitev."
                        )
                    }
                }
            }
        }
    }

    // ── Pomožne metode ────────────────────────────────────────────────────────

    private fun validateCoordinates(x: Double, y: Double, context: String, errors: MutableList<String>) {
        // Konvencija: x = longitude [-180, 180], y = latitude [-90, 90]
        // Ker DSL uporablja (lon, lat) ali pa lokalne koordinate, preverimo samo,
        // če so vrednosti izven absolutnih geografskih meja (kadar so v realnem obsegu)
        val looksLikeGeo = (Math.abs(x) <= 180 && Math.abs(y) <= 180)  // vsaj eden je v geo obsegu
        if (looksLikeGeo) {
            if (y < -90 || y > 90) {
                errors.add(
                    "[$context] Neveljavna geografska širina (latitude): $y. " +
                            "Vrednost mora biti med -90 in 90."
                )
            }
            if (x < -180 || x > 180) {
                errors.add(
                    "[$context] Neveljavna geografska dolžina (longitude): $x. " +
                            "Vrednost mora biti med -180 in 180."
                )
            }
        }
    }

    /** Vrne seznam segmentov (par točk) za dano geometrijo ceste. */
    private fun roadSegments(geo: GeometryNode): List<Pair<Pair<Double, Double>, Pair<Double, Double>>> {
        fun pt(p: PointNode) = evalNumber(p.x) to evalNumber(p.y)
        return when (geo) {
            is LineGeometry     -> listOf(pt(geo.from) to pt(geo.to))
            is BendGeometry     -> listOf(pt(geo.from) to pt(geo.to))
            is PolylineGeometry -> geo.points.zipWithNext { a, b -> pt(a) to pt(b) }
            is PolygonGeometry  -> geo.points.zipWithNext { a, b -> pt(a) to pt(b) }
            else                -> emptyList()
        }
    }

    /**
     * Preveri, ali segment (p1→p2) seka os-poravnan pravokotnik z Liang–Barsky algoritmom
     * (poenostavljena različica).
     */
    private fun segmentIntersectsBox(
        p1: Pair<Double, Double>, p2: Pair<Double, Double>,
        minX: Double, maxX: Double, minY: Double, maxY: Double
    ): Boolean {
        val dx = p2.first  - p1.first
        val dy = p2.second - p1.second
        var tMin = 0.0; var tMax = 1.0

        fun clip(p: Double, q: Double): Boolean {
            if (p == 0.0) return q >= 0
            val r = q / p
            if (p < 0) { if (r > tMax) return false; if (r > tMin) tMin = r }
            else       { if (r < tMin) return false; if (r < tMax) tMax = r }
            return true
        }

        if (!clip(-dx, p1.first  - minX)) return false
        if (!clip( dx, maxX - p1.first))  return false
        if (!clip(-dy, p1.second - minY)) return false
        if (!clip( dy, maxY - p1.second)) return false
        return true
    }

    /** Enostavni evaluator — vrne numerično vrednost izraza, 0.0 za ne-numerične. */
    private fun evalNumber(expr: ExpressionNode): Double = when (expr) {
        is NumberExpression -> expr.value
        is BinaryExpression -> {
            val l = evalNumber(expr.left); val r = evalNumber(expr.right)
            when (expr.operator) {
                "+" -> l + r; "-" -> l - r; "*" -> l * r
                "/" -> if (r != 0.0) l / r else 0.0
                else -> 0.0
            }
        }
        else -> 0.0
    }
}