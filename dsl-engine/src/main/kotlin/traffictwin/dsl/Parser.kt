package traffictwin.dsl

class ParseException(message: String) : Exception(message)

class Parser(private val tokens: List<Token>) {

    private var current = 0

    // ── Vstopna točka ─────────────────────────────────────────────────────────

    fun parse(): ProgramNode {
        val statements = mutableListOf<TopStatement>()
        while (!isAtEnd()) {
            statements.add(topStatement())
        }
        return ProgramNode(statements)
    }

    // ── Vrhnja raven ──────────────────────────────────────────────────────────

    private fun topStatement(): TopStatement = when {
        check(TokenType.CITY) -> city()
        check(TokenType.LET)  -> letStatement()
        check(TokenType.NIL)  -> { advance(); NilNode }
        else -> throw error("Pričakovan city, let ali nil")
    }

    private fun city(): CityNode {
        consume(TokenType.CITY, "Pričakovan 'city'")
        val name = consumeName("ime mesta")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{' za imenom mesta")
        val items = mutableListOf<CityItem>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) items.add(cityItem())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}' za telesom mesta")
        return CityNode(name, items)
    }

    private fun cityItem(): CityItem {
        while (check(TokenType.SEMICOLON)) advance()
        return when {
            check(TokenType.ROAD)     -> road()
            check(TokenType.BUILDING) -> building()
            check(TokenType.PARKING)  -> parking()
            check(TokenType.PARK)     -> park()
            check(TokenType.ZONE)     -> zone()
            check(TokenType.JUNCTION) -> junction()
            check(TokenType.MARKER)   -> marker()
            check(TokenType.SENSOR)   -> sensor()
            check(TokenType.QUERY)    -> query()
            check(TokenType.LET)      -> letStatement()
            check(TokenType.NIL)      -> { advance(); NilNode }
            else -> throw error("Pričakovan element mesta (road, building, parking, …), dobil '${peek().lexeme}'")
        }
    }

    // ── Elementi mesta ────────────────────────────────────────────────────────

    private fun road(): RoadNode {
        consume(TokenType.ROAD, "Pričakovan 'road'")
        val name = consumeName("ime ceste")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<RoadStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.SEMICOLON)) { advance(); continue }
            stmts.add(roadStatement())
        }
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return RoadNode(name, stmts)
    }

    private fun building(): BuildingNode {
        consume(TokenType.BUILDING, "Pričakovan 'building'")
        val name = consumeName("ime stavbe")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<AreaStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) stmts.add(areaStatement())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return BuildingNode(name, stmts)
    }

    private fun parking(): ParkingNode {
        consume(TokenType.PARKING, "Pričakovan 'parking'")
        val name = consumeName("ime parkirišča")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<ParkingStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) stmts.add(parkingStatement())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return ParkingNode(name, stmts)
    }

    private fun park(): ParkNode {
        consume(TokenType.PARK, "Pričakovan 'park'")
        val name = consumeName("ime parka")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<AreaStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) stmts.add(areaStatement())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return ParkNode(name, stmts)
    }

    private fun zone(): ZoneNode {
        consume(TokenType.ZONE, "Pričakovan 'zone'")
        val name = consumeName("ime cone")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<AreaStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) stmts.add(areaStatement())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return ZoneNode(name, stmts)
    }

    private fun junction(): JunctionNode {
        consume(TokenType.JUNCTION, "Pričakovan 'junction'")
        val name = if (checkAny(TokenType.IDENTIFIER, TokenType.STRING)) consumeName("ime križišča") else null
        val pt = point()
        consumeOptionalSemicolon()
        return JunctionNode(name, pt)
    }

    private fun marker(): MarkerNode {
        consume(TokenType.MARKER, "Pričakovan 'marker'")
        val name = if (checkAny(TokenType.IDENTIFIER, TokenType.STRING)) consumeName("ime markerja") else null
        val pt = point()
        consumeOptionalSemicolon()
        return MarkerNode(name, pt)
    }

    private fun sensor(): SensorNode {
        consume(TokenType.SENSOR, "Pričakovan 'sensor'")
        val name = consumeName("ime senzorja")
        val pt = point()
        val meta = mutableListOf<MetadataNode>()
        if (check(TokenType.LEFT_BRACE)) {
            advance()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) meta.add(metadata())
            consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        } else {
            consumeOptionalSemicolon()
        }
        return SensorNode(name, pt, meta)
    }

    private fun query(): QueryNode {
        consume(TokenType.QUERY, "Pričakovan 'query'")
        val name = consumeName("ime poizvedbe")
        consume(TokenType.LEFT_BRACE, "Pričakovan '{'")
        val stmts = mutableListOf<QueryStatement>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) stmts.add(queryStatement())
        consume(TokenType.RIGHT_BRACE, "Pričakovan '}'")
        consumeOptionalSemicolon()
        return QueryNode(name, stmts)
    }

    // ── Stavki ceste ──────────────────────────────────────────────────────────
    private fun roadStatement(): RoadStatement {
        if (check(TokenType.SEMICOLON)) { advance(); return NilNode }
        return when {
            isGeometry() -> GeometryStatement(geometry())
            check(TokenType.TYPE) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val t = consumeName("tip ceste")
                consumeOptionalSemicolon()
                RoadTypeStatement(t)
            }
            check(TokenType.RELATION) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val r = consumeName("relacija")
                consumeOptionalSemicolon()
                RoadRelationStatement(r)
            }
            check(TokenType.STATE) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val s = roadState()
                consumeOptionalSemicolon()
                RoadStateStatement(s)
            }
            check(TokenType.SPEED_LIMIT) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val e = expression()
                consumeOptionalSemicolon()
                SpeedLimitStatement(e)
            }
            check(TokenType.LANES) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val e = expression()
                consumeOptionalSemicolon()
                LanesStatement(e)
            }
            check(TokenType.ONEWAY) -> {
                advance(); matchOptional(TokenType.EQUAL)
                val b = boolean()
                consumeOptionalSemicolon()
                OnewayStatement(b)
            }
            check(TokenType.SET) -> parseSet()
            check(TokenType.LET) -> letStatement()
            check(TokenType.NIL) -> { advance(); NilNode }
            else -> throw error("Nepričakovan stavek v cesti: '${peek().lexeme}'")
        }
    }

    // ── Stavki območja ────────────────────────────────────────────────────────
    private fun areaStatement(): AreaStatement = when {
        isGeometry()           -> GeometryStatement(geometry())
        check(TokenType.SET)   -> parseSet()
        check(TokenType.LET)   -> letStatement()
        check(TokenType.NIL)   -> { advance(); NilNode }
        else -> throw error("Nepričakovan stavek v območju: '${peek().lexeme}'")
    }

    // ── Stavki parkirišča ─────────────────────────────────────────────────────
    private fun parkingStatement(): ParkingStatement = when {
        check(TokenType.ID) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val id = consume(TokenType.INTEGER, "Pričakovan id").lexeme.toInt()
            consumeOptionalSemicolon()
            ParkingIdStatement(id)
        }
        check(TokenType.POINT) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val pt = point()
            consumeOptionalSemicolon()
            ParkingPointStatement(pt)
        }
        check(TokenType.CAPACITY) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val e = expression()
            consumeOptionalSemicolon()
            CapacityStatement(e)
        }
        check(TokenType.OCCUPIED) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val e = expression()
            consumeOptionalSemicolon()
            OccupiedStatement(e)
        }
        check(TokenType.PAYMENT) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val pt = paymentType()
            consumeOptionalSemicolon()
            PaymentStatement(pt)
        }
        check(TokenType.STATUS) -> {
            advance(); matchOptional(TokenType.EQUAL)
            val s = parkingStatus()
            consumeOptionalSemicolon()
            ParkingStatusStatement(s)
        }
        check(TokenType.SET) -> parseSet()
        check(TokenType.LET) -> letStatement()
        check(TokenType.NIL) -> { advance(); NilNode }
        else -> throw error("Nepričakovan stavek v parkirišču: '${peek().lexeme}'")
    }

    // ── Stavki poizvedbe ──────────────────────────────────────────────────────
    private fun queryStatement(): QueryStatement = when {
        check(TokenType.NEARBY) -> {
            advance()
            val paren = matchOptional(TokenType.LEFT_PAREN)
            val pt = expression()
            consume(TokenType.COMMA, "Pričakovan ',' po točki")
            val radius = expression()
            consume(TokenType.COMMA, "Pričakovan ',' po radiju")
            val target = queryTarget()
            if (paren) consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            NearbyQueryStatement(pt, radius, target)
        }
        check(TokenType.WHERE) -> {
            advance()
            val cond = condition()
            consumeOptionalSemicolon()
            WhereQueryStatement(cond)
        }
        check(TokenType.SORT_BY) -> {
            advance()
            val id = consume(TokenType.IDENTIFIER, "Pričakovan identifikator po 'sort_by'").lexeme
            consumeOptionalSemicolon()
            SortByQueryStatement(id)
        }
        check(TokenType.HIGHLIGHT) -> {
            advance()
            val id = consume(TokenType.IDENTIFIER, "Pričakovan identifikator po 'highlight'").lexeme
            consumeOptionalSemicolon()
            HighlightQueryStatement(id)
        }
        else -> throw error("Nepričakovan stavek v poizvedbi: '${peek().lexeme}'")
    }

    // ── Skupne metode ─────────────────────────────────────────────────────────
    private fun letStatement(): LetNode {
        consume(TokenType.LET, "Pričakovan 'let'")
        val name = consume(TokenType.IDENTIFIER, "Pričakovan identifikator").lexeme
        consume(TokenType.EQUAL, "Pričakovan '='")
        val expr = expression()
        consumeOptionalSemicolon()
        return LetNode(name, expr)
    }

    private fun metadata(): MetadataNode {
        val key = consumeName("ključ metapodatkov")
        matchOptional(TokenType.EQUAL)
        val value = expression()
        consumeOptionalSemicolon()
        return MetadataNode(key, value)
    }

    private fun parseSet(): MetadataNode {
        consume(TokenType.SET, "Pričakovan 'set'")
        return if (check(TokenType.LEFT_PAREN)) {
            advance()
            val key = consumeName("ključ")
            consume(TokenType.COMMA, "Pričakovan ','")
            val value = expression()
            consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            MetadataNode(key, value)
        } else {
            metadata()
        }
    }

    // ── Geometrija ────────────────────────────────────────────────────────────
    private fun isGeometry() = checkAny(
        TokenType.LINE, TokenType.BEND, TokenType.POLYLINE,
        TokenType.POLYGON, TokenType.BOX, TokenType.CIRC
    )

    private fun geometry(): GeometryNode = when {
        check(TokenType.LINE) -> {
            advance()
            // Sintaksa: line(fromPt, toPt) ali line fromPt, toPt
            // fromPt/toPt je bodisi (x,y) bodisi spremenljivka
            val hasParen = matchOptional(TokenType.LEFT_PAREN)
            val from = pointArg()
            consume(TokenType.COMMA, "Pričakovan ',' med točkama")
            val to = pointArg()
            if (hasParen) consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            LineGeometry(from, to)
        }
        check(TokenType.BEND) -> {
            advance()
            val hasParen = matchOptional(TokenType.LEFT_PAREN)
            val from = pointArg()
            consume(TokenType.COMMA, "Pričakovan ','")
            val to = pointArg()
            consume(TokenType.COMMA, "Pričakovan ','")
            val amount = expression()
            if (hasParen) consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            BendGeometry(from, to, amount)
        }
        check(TokenType.POLYLINE) -> {
            advance()
            consume(TokenType.LEFT_PAREN, "Pričakovan '('")
            val pts = pointList()
            consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            PolylineGeometry(pts)
        }
        check(TokenType.POLYGON) -> {
            advance()
            consume(TokenType.LEFT_PAREN, "Pričakovan '('")
            val pts = pointList()
            consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            PolygonGeometry(pts)
        }
        check(TokenType.BOX) -> {
            advance()
            val hasParen = matchOptional(TokenType.LEFT_PAREN)
            val first = pointArg()
            consume(TokenType.COMMA, "Pričakovan ','")
            val second = pointArg()
            if (hasParen) consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            BoxGeometry(first, second)
        }
        check(TokenType.CIRC) -> {
            advance()
            val hasParen = matchOptional(TokenType.LEFT_PAREN)
            val center = pointArg()
            consume(TokenType.COMMA, "Pričakovan ','")
            val radius = expression()
            if (hasParen) consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
            consumeOptionalSemicolon()
            CircleGeometry(center, radius)
        }
        else -> throw error("Pričakovana geometrija")
    }

    /**
     * Razčleni točko. Podpira:
     *   (x, y)   — par koordinat z oklepaji (primary() vrne PointExpression)
     *   x, y     — par koordinat brez oklepajev
     * Opomba: identifier/spremenljivka tipa točka se razčleni kot PointExpression
     * v primary() ko je obdan z oklepaji: (ident_x, ident_y).
     * Za surovo referenco (line(p, q)) uporabi pointOrRef().
     */
    private fun point(): PointNode {
        val e = expression()
        return when (e) {
            is PointExpression -> e.point
            else -> {
                consume(TokenType.COMMA, "Pričakovan ',' med koordinatama točke")
                val y = expression()
                PointNode(e, y)
            }
        }
    }

    /**
     * Razčleni en argument geometrije ki predstavlja točko:
     *   (x, y)    — koordinatni par z oklepaji
     *   ident     — spremenljivka tipa točka (brez oklepajev)
     *
     * Ključna razlika od point(): tukaj '(' je del točke same, ne zunanji oklepaj.
     * Ko vidimo '(', preverimo spekulativno ali je to (x, y) ali (ident) grupiranje.
     */
    private fun pointArg(): PointNode {
        return if (check(TokenType.LEFT_PAREN)) {
            advance() // poje (
            val x = additive()   // ← NE expression(), da primary() ne naredi točke
            consume(TokenType.COMMA, "Pričakovan ',' v točki")
            val y = additive()   // ← enako
            consume(TokenType.RIGHT_PAREN, "Pričakovan ')' v točki")
            PointNode(x, y)
        } else {
            // za identifikatorje (p, q) — expression() je OK ker IDENTIFIER ne sproži točkovne veje
            val e = expression()
            PointNode(e, e) // referenca
        }
    }

    /** Bere golo referenco (identifier ali izraz) kot točko brez oklepajev */
    private fun pointArgFallback(): PointNode {
        val e = expression()
        return when (e) {
            is PointExpression -> e.point
            else -> PointNode(e, e)
        }
    }

    private fun pointList(): List<PointNode> {
        val pts = mutableListOf(point())
        while (matchOptional(TokenType.COMMA)) pts.add(point())
        return pts
    }

    // ── Izrazi ────────────────────────────────────────────────────────────────
    private fun expression(): ExpressionNode = additive()

    private fun additive(): ExpressionNode {
        var left = multiplicative()
        while (checkAny(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance().lexeme
            left = BinaryExpression(left, op, multiplicative())
        }
        return left
    }

    private fun multiplicative(): ExpressionNode {
        var left = unary()
        while (checkAny(TokenType.STAR, TokenType.SLASH)) {
            val op = advance().lexeme
            left = BinaryExpression(left, op, unary())
        }
        return left
    }

    private fun unary(): ExpressionNode {
        if (check(TokenType.MINUS)) {
            advance()
            return BinaryExpression(NumberExpression(0.0), "-", primary())
        }
        return primary()
    }

    private fun primary(): ExpressionNode = when {
        check(TokenType.NUMBER)  -> NumberExpression(advance().lexeme.toDouble())
        check(TokenType.INTEGER) -> NumberExpression(advance().lexeme.toDouble())
        check(TokenType.STRING)  -> StringExpression(advance().lexeme)
        check(TokenType.TRUE)    -> { advance(); BoolExpression(true) }
        check(TokenType.FALSE)   -> { advance(); BoolExpression(false) }

        check(TokenType.LEFT_PAREN) -> {
            advance()
            val e = expression()
            // Če po izrazu sledi vejica → je to točka (x, y)
            if (check(TokenType.COMMA)) {
                advance()
                val y = expression()
                consume(TokenType.RIGHT_PAREN, "Pričakovan ')' za točko")
                PointExpression(PointNode(e, y))
            } else {
                consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
                e
            }
        }

        check(TokenType.IDENTIFIER) -> {
            val name = advance().lexeme
            if (check(TokenType.LEFT_PAREN)) {
                advance()
                val arg = expression()
                consume(TokenType.RIGHT_PAREN, "Pričakovan ')'")
                FunctionCallExpression(name, arg)
            } else {
                IdentifierExpression(name)
            }
        }
        else -> throw error("Pričakovan izraz, dobil '${peek().lexeme}'")
    }

    private fun condition(): ConditionNode {
        val left = expression()
        val op = if (checkAny(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL, TokenType.LESS,
                TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            advance().lexeme
        } else throw error("Pričakovan operator")
        return ConditionNode(left, op, expression())
    }

    // ── Enumi ─────────────────────────────────────────────────────────────────
    private fun roadState(): RoadState = when {
        check(TokenType.OPEN)      -> { advance(); RoadState.OPEN }
        check(TokenType.CONGESTED) -> { advance(); RoadState.CONGESTED }
        check(TokenType.WORKS)     -> { advance(); RoadState.WORKS }
        check(TokenType.CLOSED)    -> { advance(); RoadState.CLOSED }
        else -> throw error("Stanje ceste")
    }

    private fun paymentType(): PaymentType = when {
        check(TokenType.FREE)  -> { advance(); PaymentType.FREE }
        check(TokenType.PAID)  -> { advance(); PaymentType.PAID }
        else -> throw error("Tip plačila")
    }

    private fun parkingStatus(): ParkingStatus = when {
        check(TokenType.OPEN)   -> { advance(); ParkingStatus.OPEN }
        check(TokenType.FULL)   -> { advance(); ParkingStatus.FULL }
        else -> throw error("Status parkirišča")
    }

    private fun queryTarget(): QueryTarget = when {
        check(TokenType.PARKING) -> { advance(); QueryTarget.PARKING }
        check(TokenType.ROAD)    -> { advance(); QueryTarget.ROAD }
        else -> throw error("Cilj poizvedbe")
    }

    private fun boolean(): Boolean = when {
        check(TokenType.TRUE)  -> { advance(); true }
        check(TokenType.FALSE) -> { advance(); false }
        else -> throw error("Boolean")
    }

    // ── Pomožne metode ────────────────────────────────────────────────────────
    private fun matchOptional(type: TokenType): Boolean {
        if (!check(type)) return false
        advance(); return true
    }
    private fun consumeOptionalSemicolon() { matchOptional(TokenType.SEMICOLON) }
    private fun consumeName(what: String): String = if (checkAny(TokenType.IDENTIFIER, TokenType.STRING)) advance().lexeme
    else throw error("Pričakovano $what")
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(message)
    }
    private fun check(type: TokenType) = !isAtEnd() && peek().type == type
    private fun checkAny(vararg types: TokenType) = types.any { check(it) }
    private fun peek() = tokens[current]
    private fun peekNext() = if (current + 1 < tokens.size) tokens[current + 1] else tokens[current]
    private fun isAtEnd() = peek().type == TokenType.EOF
    private fun advance(): Token = tokens[current++]
    private fun error(message: String): ParseException = ParseException("Vrstica ${peek().line}: $message")
}