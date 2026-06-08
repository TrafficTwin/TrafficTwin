package traffictwin.dsl

class GeoJsonExportException(message: String) : Exception(message)

data class GeoJsonValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    fun requireValid() {
        if (!isValid) {
            throw GeoJsonExportException(errors.joinToString(separator = "\n"))
        }
    }
}

class GeoJsonExporter(private val circleSegments: Int = 64) {

    init {
        require(circleSegments >= 4) { "circleSegments mora biti vsaj 4" }
    }

    fun export(program: ProgramNode, validate: Boolean = true): String {
        val featureCollection = buildFeatureCollection(program)

        if (validate) {
            validate(featureCollection).requireValid()
        }

        return featureCollection.toJson()
    }

    fun validate(program: ProgramNode): GeoJsonValidationResult {
        return validate(buildFeatureCollection(program))
    }

    private fun buildFeatureCollection(program: ProgramNode): GeoJsonFeatureCollection {
        val features = mutableListOf<GeoJsonFeature>()
        val rootScope = Scope()

        for (statement in program.statements) {
            when (statement) {
                is LetNode -> rootScope.set(statement.name, statement.expression)
                is CityNode -> features += cityFeatures(statement, rootScope.child())
                is NilNode -> Unit
            }
        }

        return GeoJsonFeatureCollection(features)
    }

    private fun cityFeatures(city: CityNode, scope: Scope): List<GeoJsonFeature> {
        val features = mutableListOf<GeoJsonFeature>()

        for (item in city.items) {
            when (item) {
                is LetNode -> scope.set(item.name, item.expression)
                is NilNode -> Unit
                is RoadNode -> features += roadFeatures(city.name, item, scope.child())
                is BuildingNode -> features += areaFeatures(city.name, item.name, "building", item.statements, scope.child())
                is ParkNode -> features += areaFeatures(city.name, item.name, "park", item.statements, scope.child())
                is ZoneNode -> features += areaFeatures(city.name, item.name, "zone", item.statements, scope.child())
                is ParkingNode -> parkingFeature(city.name, item, scope.child())?.let { features += it }
                is JunctionNode -> features += pointFeature(city.name, item.name, "junction", item.point, scope)
                is MarkerNode -> features += pointFeature(city.name, item.name, "marker", item.point, scope)
                is SensorNode -> features += sensorFeature(city.name, item, scope.child())
                is QueryNode -> Unit
                is MetadataNode -> Unit
            }
        }

        return features
    }

    private fun roadFeatures(cityName: String, road: RoadNode, scope: Scope): List<GeoJsonFeature> {
        val geometries = mutableListOf<GeometryNode>()
        val properties = linkedMapOf<String, Any?>(
            "dslType" to "road",
            "city" to cityName,
            "name" to road.name
        )

        for (statement in road.statements) {
            when (statement) {
                is GeometryStatement -> geometries += statement.geometry
                is RoadTypeStatement -> properties["roadType"] = statement.type
                is RoadRelationStatement -> properties["relation"] = statement.relation
                is RoadStateStatement -> properties["state"] = statement.state.name.lowercase()
                is SpeedLimitStatement -> properties["speedLimit"] = evalNumber(statement.expression, scope)
                is LanesStatement -> properties["lanes"] = evalNumber(statement.expression, scope)
                is OnewayStatement -> properties["oneway"] = statement.value
                is MetadataNode -> properties[statement.key] = evalValue(statement.value, scope)
                is LetNode -> scope.set(statement.name, statement.expression)
                is NilNode -> Unit
            }
        }

        return geometries.map { geometry ->
            val featureProperties = linkedMapOf<String, Any?>().also { it.putAll(properties) }
            featureProperties["geometrySource"] = geometry::class.simpleName ?: "Geometry"
            if (geometry is BendGeometry) {
                featureProperties["bendAmount"] = evalNumber(geometry.amount, scope)
            }

            GeoJsonFeature(
                geometry = geometryToGeoJson(geometry, scope),
                properties = featureProperties
            )
        }
    }

    private fun areaFeatures(
        cityName: String,
        areaName: String,
        areaType: String,
        statements: List<AreaStatement>,
        scope: Scope
    ): List<GeoJsonFeature> {
        val geometries = mutableListOf<GeometryNode>()
        val properties = linkedMapOf<String, Any?>(
            "dslType" to areaType,
            "city" to cityName,
            "name" to areaName
        )

        for (statement in statements) {
            when (statement) {
                is GeometryStatement -> geometries += statement.geometry
                is MetadataNode -> properties[statement.key] = evalValue(statement.value, scope)
                is LetNode -> scope.set(statement.name, statement.expression)
                is NilNode -> Unit
            }
        }

        return geometries.map { geometry ->
            val featureProperties = linkedMapOf<String, Any?>().also { it.putAll(properties) }
            featureProperties["geometrySource"] = geometry::class.simpleName ?: "Geometry"

            GeoJsonFeature(
                geometry = geometryToGeoJson(geometry, scope),
                properties = featureProperties
            )
        }
    }

    private fun parkingFeature(cityName: String, parking: ParkingNode, scope: Scope): GeoJsonFeature? {
        val properties = linkedMapOf<String, Any?>(
            "dslType" to "parking",
            "city" to cityName,
            "name" to parking.name
        )
        var point: PointNode? = null

        for (statement in parking.statements) {
            when (statement) {
                is ParkingIdStatement -> properties["id"] = statement.id
                is ParkingPointStatement -> point = statement.point
                is CapacityStatement -> properties["capacity"] = evalNumber(statement.expression, scope)
                is OccupiedStatement -> properties["occupied"] = evalNumber(statement.expression, scope)
                is PaymentStatement -> properties["payment"] = statement.paymentType.name.lowercase()
                is ParkingStatusStatement -> properties["status"] = statement.status.name.lowercase()
                is MetadataNode -> properties[statement.key] = evalValue(statement.value, scope)
                is LetNode -> scope.set(statement.name, statement.expression)
                is NilNode -> Unit
            }
        }

        return point?.let {
            GeoJsonFeature(
                geometry = GeoJsonPoint(evalPoint(it, scope)),
                properties = properties
            )
        }
    }

    private fun sensorFeature(cityName: String, sensor: SensorNode, scope: Scope): GeoJsonFeature {
        val properties = linkedMapOf<String, Any?>(
            "dslType" to "sensor",
            "city" to cityName,
            "name" to sensor.name
        )

        for (metadata in sensor.metadata) {
            properties[metadata.key] = evalValue(metadata.value, scope)
        }

        return GeoJsonFeature(
            geometry = GeoJsonPoint(evalPoint(sensor.point, scope)),
            properties = properties
        )
    }

    private fun pointFeature(
        cityName: String,
        name: String?,
        dslType: String,
        point: PointNode,
        scope: Scope
    ): GeoJsonFeature {
        val properties = linkedMapOf<String, Any?>(
            "dslType" to dslType,
            "city" to cityName
        )

        if (name != null) {
            properties["name"] = name
        }

        return GeoJsonFeature(
            geometry = GeoJsonPoint(evalPoint(point, scope)),
            properties = properties
        )
    }

    private fun geometryToGeoJson(geometry: GeometryNode, scope: Scope): GeoJsonGeometry = when (geometry) {
        is LineGeometry -> GeoJsonLineString(
            listOf(
                evalPoint(geometry.from, scope),
                evalPoint(geometry.to, scope)
            )
        )

        is BendGeometry -> GeoJsonLineString(
            listOf(
                evalPoint(geometry.from, scope),
                evalPoint(geometry.to, scope)
            )
        )

        is PolylineGeometry -> GeoJsonLineString(
            geometry.points.map { evalPoint(it, scope) }
        )

        is PolygonGeometry -> GeoJsonPolygon(
            listOf(closeRing(geometry.points.map { evalPoint(it, scope) }))
        )

        is BoxGeometry -> GeoJsonPolygon(
            listOf(
                boxRing(
                    evalPoint(geometry.first, scope),
                    evalPoint(geometry.second, scope)
                )
            )
        )

        is CircleGeometry -> GeoJsonPolygon(
            listOf(
                circleRing(
                    evalPoint(geometry.center, scope),
                    evalNumber(geometry.radius, scope)
                )
            )
        )
    }

    private fun evalPoint(point: PointNode, scope: Scope): GeoJsonPosition {
        val x = point.x
        val y = point.y

        if (x is IdentifierExpression && y is IdentifierExpression && x.name == y.name) {
            return evalPointExpression(scope.get(x.name), scope, x.name)
        }

        return GeoJsonPosition(
            longitude = evalNumber(x, scope),
            latitude = evalNumber(y, scope)
        )
    }

    private fun evalPointExpression(
        expression: ExpressionNode,
        scope: Scope,
        nameForError: String? = null
    ): GeoJsonPosition {
        return when (expression) {
            is PointExpression -> evalPoint(expression.point, scope)
            is IdentifierExpression -> evalPointExpression(scope.get(expression.name), scope, expression.name)

            else -> throw GeoJsonExportException(
                if (nameForError == null) {
                    "Izraz ni točka in ga ni mogoče izvoziti kot GeoJSON koordinato."
                } else {
                    "Spremenljivka '$nameForError' ni točka in je ni mogoče izvoziti kot GeoJSON koordinato."
                }
            )
        }
    }

    private fun evalNumber(expression: ExpressionNode, scope: Scope): Double = when (expression) {
        is NumberExpression -> expression.value
        is IdentifierExpression -> evalNumber(scope.get(expression.name), scope)

        is BinaryExpression -> {
            val left = evalNumber(expression.left, scope)
            val right = evalNumber(expression.right, scope)

            when (expression.operator) {
                "+" -> left + right
                "-" -> left - right
                "*" -> left * right
                "/" -> {
                    if (right == 0.0) {
                        throw GeoJsonExportException("Deljenje z nič v izrazu za GeoJSON izvoz.")
                    }
                    left / right
                }

                else -> throw GeoJsonExportException(
                    "Nepodprt operator '${expression.operator}' v numeričnem izrazu."
                )
            }
        }

        is FunctionCallExpression -> {
            val argument = evalPointExpression(expression.argument, scope)

            when (expression.functionName) {
                "fst" -> argument.longitude
                "snd" -> argument.latitude

                else -> throw GeoJsonExportException(
                    "Nepodprta funkcija '${expression.functionName}' v GeoJSON izvozu."
                )
            }
        }

        else -> throw GeoJsonExportException("Pričakovan numerični izraz za GeoJSON izvoz.")
    }

    private fun evalValue(expression: ExpressionNode, scope: Scope): Any? = when (expression) {
        is NumberExpression -> expression.value
        is StringExpression -> expression.value
        is BoolExpression -> expression.value
        is IdentifierExpression -> evalValue(scope.get(expression.name), scope)

        is PointExpression -> {
            val point = evalPoint(expression.point, scope)
            listOf(point.longitude, point.latitude)
        }

        is BinaryExpression -> evalNumber(expression, scope)
        is FunctionCallExpression -> evalNumber(expression, scope)
    }

    private fun boxRing(first: GeoJsonPosition, second: GeoJsonPosition): List<GeoJsonPosition> {
        val minX = minOf(first.longitude, second.longitude)
        val maxX = maxOf(first.longitude, second.longitude)
        val minY = minOf(first.latitude, second.latitude)
        val maxY = maxOf(first.latitude, second.latitude)

        return listOf(
            GeoJsonPosition(minX, minY),
            GeoJsonPosition(maxX, minY),
            GeoJsonPosition(maxX, maxY),
            GeoJsonPosition(minX, maxY),
            GeoJsonPosition(minX, minY)
        )
    }

    private fun circleRing(center: GeoJsonPosition, radius: Double): List<GeoJsonPosition> {
        if (radius <= 0.0) {
            throw GeoJsonExportException("Polmer kroga mora biti večji od 0 za GeoJSON izvoz.")
        }

        val points = mutableListOf<GeoJsonPosition>()

        for (i in 0 until circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments

            points += GeoJsonPosition(
                longitude = center.longitude + radius * kotlin.math.cos(angle),
                latitude = center.latitude + radius * kotlin.math.sin(angle)
            )
        }

        points += points.first()
        return points
    }

    private fun closeRing(points: List<GeoJsonPosition>): List<GeoJsonPosition> {
        if (points.isEmpty()) {
            return points
        }

        return if (points.first() == points.last()) {
            points
        } else {
            points + points.first()
        }
    }

    private fun validate(collection: GeoJsonFeatureCollection): GeoJsonValidationResult {
        val errors = mutableListOf<String>()

        collection.features.forEachIndexed { index, feature ->
            validateGeometry(feature.geometry, "features[$index].geometry", errors)
        }

        return GeoJsonValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun validateGeometry(
        geometry: GeoJsonGeometry,
        path: String,
        errors: MutableList<String>
    ) {
        when (geometry) {
            is GeoJsonPoint -> validatePosition(
                geometry.coordinates,
                "$path.coordinates",
                errors
            )

            is GeoJsonLineString -> {
                if (geometry.coordinates.size < 2) {
                    errors += "$path.coordinates: LineString mora imeti vsaj 2 točki."
                }

                geometry.coordinates.forEachIndexed { i, position ->
                    validatePosition(position, "$path.coordinates[$i]", errors)
                }
            }

            is GeoJsonPolygon -> {
                if (geometry.coordinates.isEmpty()) {
                    errors += "$path.coordinates: Polygon mora imeti vsaj en linearni obroč."
                }

                geometry.coordinates.forEachIndexed { ringIndex, ring ->
                    if (ring.size < 4) {
                        errors += "$path.coordinates[$ringIndex]: linearni obroč poligona mora imeti vsaj 4 točke."
                    }

                    if (ring.isNotEmpty() && ring.first() != ring.last()) {
                        errors += "$path.coordinates[$ringIndex]: linearni obroč poligona mora biti zaprt."
                    }

                    ring.forEachIndexed { pointIndex, position ->
                        validatePosition(
                            position,
                            "$path.coordinates[$ringIndex][$pointIndex]",
                            errors
                        )
                    }
                }
            }
        }
    }

    private fun validatePosition(
        position: GeoJsonPosition,
        path: String,
        errors: MutableList<String>
    ) {
        if (!position.longitude.isFinite()) {
            errors += "$path[0]: longitude mora biti končno število."
        }

        if (!position.latitude.isFinite()) {
            errors += "$path[1]: latitude mora biti končno število."
        }

        if (position.longitude < -180.0 || position.longitude > 180.0) {
            errors += "$path[0]: longitude mora biti med -180 in 180."
        }

        if (position.latitude < -90.0 || position.latitude > 90.0) {
            errors += "$path[1]: latitude mora biti med -90 in 90."
        }
    }

    private class Scope(private val parent: Scope? = null) {
        private val values = linkedMapOf<String, ExpressionNode>()

        fun child(): Scope = Scope(this)

        fun set(name: String, expression: ExpressionNode) {
            values[name] = expression
        }

        fun get(name: String): ExpressionNode {
            return values[name]
                ?: parent?.get(name)
                ?: throw GeoJsonExportException(
                    "Neznana spremenljivka '$name' pri GeoJSON izvozu."
                )
        }
    }
}

private data class GeoJsonFeatureCollection(
    val features: List<GeoJsonFeature>
) {
    fun toJson(): String {
        val sb = StringBuilder()

        sb.append("{\n")
        sb.append("  \"type\": \"FeatureCollection\",\n")
        sb.append("  \"features\": [")

        if (features.isNotEmpty()) {
            sb.append("\n")
        }

        features.forEachIndexed { index, feature ->
            sb.append(feature.toJson("    "))

            if (index < features.lastIndex) {
                sb.append(",")
            }

            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}")

        return sb.toString()
    }
}

private data class GeoJsonFeature(
    val geometry: GeoJsonGeometry,
    val properties: Map<String, Any?>
) {
    fun toJson(indent: String): String {
        val childIndent = "$indent  "

        return buildString {
            append(indent).append("{\n")
            append(childIndent).append("\"type\": \"Feature\",\n")
            append(childIndent).append("\"geometry\": ").append(geometry.toJson()).append(",\n")
            append(childIndent).append("\"properties\": ").append(properties.toJsonObject())
            append("\n").append(indent).append("}")
        }
    }
}

private sealed interface GeoJsonGeometry {
    fun toJson(): String
}

private data class GeoJsonPoint(
    val coordinates: GeoJsonPosition
) : GeoJsonGeometry {
    override fun toJson(): String {
        return "{\"type\": \"Point\", \"coordinates\": ${coordinates.toJson()}}"
    }
}

private data class GeoJsonLineString(
    val coordinates: List<GeoJsonPosition>
) : GeoJsonGeometry {
    override fun toJson(): String {
        return "{\"type\": \"LineString\", \"coordinates\": ${coordinates.toJsonArray()}}"
    }
}

private data class GeoJsonPolygon(
    val coordinates: List<List<GeoJsonPosition>>
) : GeoJsonGeometry {
    override fun toJson(): String {
        val rings = coordinates.joinToString(
            prefix = "[",
            postfix = "]"
        ) { ring ->
            ring.toJsonArray()
        }

        return "{\"type\": \"Polygon\", \"coordinates\": $rings}"
    }
}

private data class GeoJsonPosition(
    val longitude: Double,
    val latitude: Double
) {
    fun toJson(): String {
        return "[${longitude.toJsonNumber()}, ${latitude.toJsonNumber()}]"
    }
}

private fun List<GeoJsonPosition>.toJsonArray(): String {
    return joinToString(
        prefix = "[",
        postfix = "]"
    ) { position ->
        position.toJson()
    }
}

private fun Map<String, Any?>.toJsonObject(): String {
    return entries.joinToString(
        prefix = "{",
        postfix = "}"
    ) { (key, value) ->
        "\"${key.escapeJson()}\": ${value.toJsonValue()}"
    }
}

private fun Any?.toJsonValue(): String = when (this) {
    null -> "null"
    is String -> "\"${escapeJson()}\""
    is Boolean -> toString()
    is Int -> toString()
    is Long -> toString()
    is Float -> toDouble().toJsonNumber()
    is Double -> toJsonNumber()
    is Number -> toString()

    is List<*> -> joinToString(
        prefix = "[",
        postfix = "]"
    ) { item ->
        item.toJsonValue()
    }

    is Map<*, *> -> entries.joinToString(
        prefix = "{",
        postfix = "}"
    ) { (key, value) ->
        "\"${key.toString().escapeJson()}\": ${value.toJsonValue()}"
    }

    else -> "\"${toString().escapeJson()}\""
}

private fun Double.toJsonNumber(): String {
    if (!isFinite()) {
        throw GeoJsonExportException("GeoJSON ne podpira NaN ali neskončnih števil.")
    }

    return if (this == kotlin.math.floor(this)) {
        toLong().toString()
    } else {
        toString()
    }
}

private fun String.escapeJson(): String = buildString {
    for (char in this@escapeJson) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")

            else -> {
                if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
}