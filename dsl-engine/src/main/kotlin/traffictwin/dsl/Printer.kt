package traffictwin.dsl

/**
 * Pretty-printer: pretvori AST nazaj v berljivo DSL izvorno kodo.
 */
class Printer(private val indentSize: Int = 4) {

    private val sb = StringBuilder()
    private var depth = 0
    private val indent get() = " ".repeat(depth * indentSize)

    fun print(program: ProgramNode): String {
        sb.clear(); depth = 0
        program.statements.forEachIndexed { i, stmt ->
            printTopStatement(stmt)
            if (i < program.statements.lastIndex) sb.append("\n")
        }
        return sb.toString()
    }

    private fun printTopStatement(stmt: TopStatement) = when (stmt) {
        is CityNode -> printCity(stmt)
        is LetNode  -> printLet(stmt)
        is NilNode  -> line("nil")
    }

    private fun printCity(node: CityNode) {
        line("city ${node.name.q()} {")
        indented { node.items.forEach { printCityItem(it) } }
        line("}")
    }

    private fun printCityItem(item: CityItem) = when (item) {
        is RoadNode     -> { line("road ${item.name.q()} {"); indented { item.statements.forEach { printRoadStatement(it) } }; line("}") }
        is BuildingNode -> { line("building ${item.name.q()} {"); indented { item.statements.forEach { printAreaStatement(it) } }; line("}") }
        is ParkingNode  -> { line("parking ${item.name.q()} {"); indented { item.statements.forEach { printParkingStatement(it) } }; line("}") }
        is ParkNode     -> { line("park ${item.name.q()} {"); indented { item.statements.forEach { printAreaStatement(it) } }; line("}") }
        is ZoneNode     -> { line("zone ${item.name.q()} {"); indented { item.statements.forEach { printAreaStatement(it) } }; line("}") }
        is JunctionNode -> line("junction${item.name?.let { " ${it.q()}" } ?: ""} ${pp(item.point)};")
        is MarkerNode   -> line("marker${item.name?.let { " ${it.q()}" } ?: ""} ${pp(item.point)};")
        is SensorNode   -> if (item.metadata.isEmpty()) line("sensor ${item.name.q()} ${pp(item.point)};")
        else { line("sensor ${item.name.q()} ${pp(item.point)} {"); indented { item.metadata.forEach { printMetadata(it) } }; line("}") }
        is QueryNode    -> { line("query ${item.name.q()} {"); indented { item.statements.forEach { printQueryStatement(it) } }; line("}") }
        is LetNode      -> printLet(item)
        is MetadataNode -> printMetadata(item)
        is NilNode      -> line("nil")
    }

    private fun printRoadStatement(stmt: RoadStatement) = when (stmt) {
        is GeometryStatement     -> line("${pg(stmt.geometry)};")
        is RoadTypeStatement     -> line("type = ${stmt.type.q()};")
        is RoadRelationStatement -> line("relation = ${stmt.relation.q()};")
        is RoadStateStatement    -> line("state = ${stmt.state.name.lowercase()};")
        is SpeedLimitStatement   -> line("speed_limit = ${pe(stmt.expression)};")
        is LanesStatement        -> line("lanes = ${pe(stmt.expression)};")
        is OnewayStatement       -> line("oneway = ${stmt.value};")
        is MetadataNode          -> line("set ${stmt.key.q()} = ${pe(stmt.value)};")
        is LetNode               -> printLet(stmt)
        is NilNode               -> line("nil")
    }

    private fun printAreaStatement(stmt: AreaStatement) = when (stmt) {
        is GeometryStatement -> line("${pg(stmt.geometry)};")
        is MetadataNode      -> line("set ${stmt.key.q()} = ${pe(stmt.value)};")
        is LetNode           -> printLet(stmt)
        is NilNode           -> line("nil")
    }

    private fun printParkingStatement(stmt: ParkingStatement) = when (stmt) {
        is ParkingIdStatement     -> line("id = ${stmt.id};")
        is ParkingPointStatement  -> line("point = ${pp(stmt.point)};")
        is CapacityStatement      -> line("capacity = ${pe(stmt.expression)};")
        is OccupiedStatement      -> line("occupied = ${pe(stmt.expression)};")
        is PaymentStatement       -> line("payment = ${stmt.paymentType.name.lowercase()};")
        is ParkingStatusStatement -> line("status = ${stmt.status.name.lowercase()};")
        is MetadataNode           -> line("set ${stmt.key.q()} = ${pe(stmt.value)};")
        is LetNode                -> printLet(stmt)
        is NilNode                -> line("nil")
    }

    private fun printQueryStatement(stmt: QueryStatement) = when (stmt) {
        is NearbyQueryStatement    -> line("nearby ${pe(stmt.point)}, ${pe(stmt.radius)}, ${stmt.target.name.lowercase()};")
        is WhereQueryStatement     -> line("where ${pc(stmt.condition)};")
        is SortByQueryStatement    -> line("sort_by ${stmt.identifier};")
        is HighlightQueryStatement -> line("highlight ${stmt.identifier};")
    }

    private fun printLet(node: LetNode) = line("let ${node.name} = ${pe(node.expression)};")

    private fun printMetadata(node: MetadataNode) = line("${node.key.q()} = ${pe(node.value)};")

    private fun pg(geo: GeometryNode): String = when (geo) {
        is LineGeometry     -> "line(${pp(geo.from)}, ${pp(geo.to)})"
        is BendGeometry     -> "bend(${pp(geo.from)}, ${pp(geo.to)}, ${pe(geo.amount)})"
        is PolylineGeometry -> "polyline(${geo.points.joinToString(", ") { pp(it) }})"
        is PolygonGeometry  -> "polygon(${geo.points.joinToString(", ") { pp(it) }})"
        is BoxGeometry      -> "box(${pp(geo.first)}, ${pp(geo.second)})"
        is CircleGeometry   -> "circ(${pp(geo.center)}, ${pe(geo.radius)})"
    }

    private fun pp(pt: PointNode) = "(${pe(pt.x)}, ${pe(pt.y)})"

    private fun pe(expr: ExpressionNode): String = when (expr) {
        is NumberExpression     -> { val d = expr.value; if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString() }
        is StringExpression     -> "\"${expr.value}\""
        is BoolExpression       -> expr.value.toString()
        is IdentifierExpression -> expr.name
        is PointExpression      -> pp(expr.point)
        is BinaryExpression     -> {
            val l = if (expr.left  is BinaryExpression && prec((expr.left  as BinaryExpression).operator) < prec(expr.operator)) "(${pe(expr.left)})"  else pe(expr.left)
            val r = if (expr.right is BinaryExpression && prec((expr.right as BinaryExpression).operator) < prec(expr.operator)) "(${pe(expr.right)})" else pe(expr.right)
            "$l ${expr.operator} $r"
        }
        is FunctionCallExpression -> "${expr.functionName}(${pe(expr.argument)})"
    }

    private fun pc(cond: ConditionNode) = "${pe(cond.left)} ${cond.operator} ${pe(cond.right)}"

    private fun prec(op: String) = when (op) { "+", "-" -> 1; "*", "/" -> 2; else -> 0 }

    private fun String.q() = if (contains(' ') || contains('-') || isEmpty()) "\"$this\"" else this

    private fun line(text: String) { sb.append(indent).append(text).append("\n") }
    private fun indented(block: () -> Unit) { depth++; block(); depth-- }
}