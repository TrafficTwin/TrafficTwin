package traffictwin.dsl

class LexerException(message: String) : Exception(message)

class Lexer(private val source: String) {

    private var start = 0
    private var current = 0
    private var line = 1
    private var lineStart = 0

    private val tokens = mutableListOf<Token>()

    // Podpira snake_case, camelCase in UPPERCASE variante za vse ključne besede
    private val keywords = mapOf(
        // --- bloki ---
        "city"        to TokenType.CITY,
        "road"        to TokenType.ROAD,
        "building"    to TokenType.BUILDING,
        "parking"     to TokenType.PARKING,
        "park"        to TokenType.PARK,
        "zone"        to TokenType.ZONE,
        "junction"    to TokenType.JUNCTION,
        "marker"      to TokenType.MARKER,
        "sensor"      to TokenType.SENSOR,
        "query"       to TokenType.QUERY,
        "let"         to TokenType.LET,
        "nil"         to TokenType.NIL,
        // --- geometrija ---
        "line"        to TokenType.LINE,
        "bend"        to TokenType.BEND,
        "polyline"    to TokenType.POLYLINE,
        "polygon"     to TokenType.POLYGON,
        "box"         to TokenType.BOX,
        "circ"        to TokenType.CIRC,
        // --- poizvedbe ---
        "nearby"      to TokenType.NEARBY,
        "where"       to TokenType.WHERE,
        "sort_by"     to TokenType.SORT_BY,
        "sortBy"      to TokenType.SORT_BY,       // camelCase alias
        "highlight"   to TokenType.HIGHLIGHT,
        "set"         to TokenType.SET,
        // --- atributi ---
        "id"          to TokenType.ID,
        "point"       to TokenType.POINT,
        "capacity"    to TokenType.CAPACITY,
        "occupied"    to TokenType.OCCUPIED,
        "payment"     to TokenType.PAYMENT,
        "status"      to TokenType.STATUS,
        "type"        to TokenType.TYPE,
        "relation"    to TokenType.RELATION,
        "state"       to TokenType.STATE,
        "speed_limit" to TokenType.SPEED_LIMIT,
        "speedLimit"  to TokenType.SPEED_LIMIT,   // camelCase alias
        "lanes"       to TokenType.LANES,
        "oneway"      to TokenType.ONEWAY,
        // --- stanja ceste (lowercase in UPPERCASE) ---
        "open"        to TokenType.OPEN,
        "OPEN"        to TokenType.OPEN,
        "congested"   to TokenType.CONGESTED,
        "CONGESTED"   to TokenType.CONGESTED,
        "works"       to TokenType.WORKS,
        "WORKS"       to TokenType.WORKS,
        "closed"      to TokenType.CLOSED,
        "CLOSED"      to TokenType.CLOSED,
        "unknown"     to TokenType.UNKNOWN,
        "UNKNOWN"     to TokenType.UNKNOWN,
        // --- plačilo (lowercase in UPPERCASE) ---
        "free"        to TokenType.FREE,
        "FREE"        to TokenType.FREE,
        "paid"        to TokenType.PAID,
        "PAID"        to TokenType.PAID,
        "mixed"       to TokenType.MIXED,
        "MIXED"       to TokenType.MIXED,
        // --- status parkirišča ---
        "full"        to TokenType.FULL,
        "FULL"        to TokenType.FULL,
        // --- bool ---
        "true"        to TokenType.TRUE,
        "false"       to TokenType.FALSE,
        // --- funkciji za točke ---
        "fst"         to TokenType.IDENTIFIER,    // ostane identifier; parser ga prepozna
        "snd"         to TokenType.IDENTIFIER,
    )

    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", line, column()))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            ',' -> addToken(TokenType.COMMA)
            ';' -> addToken(TokenType.SEMICOLON)
            '+' -> addToken(TokenType.PLUS)
            '-' -> addToken(TokenType.MINUS)
            '*' -> addToken(TokenType.STAR)
            '/' -> {
                when {
                    match('/') -> while (!isAtEnd() && peek() != '\n') advance()
                    match('*') -> blockComment()
                    else       -> addToken(TokenType.SLASH)
                }
            }
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '!' -> if (match('=')) addToken(TokenType.BANG_EQUAL)
            else throw LexerException("Nepričakovani znak '!' v vrstici $line")
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL    else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '"' -> string()
            ' ', '\r', '\t' -> { /* prezri bele znake */ }
            '\n' -> { line++; lineStart = current }
            else -> when {
                c.isDigit()              -> number()
                c.isLetter() || c == '_' -> identifier()
                else -> throw LexerException(
                    "Nepričakovani znak '$c' v vrstici $line, stolpcu ${column()}"
                )
            }
        }
    }

    private fun string() {
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') { line++; lineStart = current }
            advance()
        }
        if (isAtEnd()) throw LexerException("Nezaključen niz v vrstici $line")
        advance() // zaključni "
        addToken(TokenType.STRING, source.substring(start + 1, current - 1))
    }

    private fun number() {
        while (!isAtEnd() && peek().isDigit()) advance()
        val isFloat = !isAtEnd() && peek() == '.' && peekNext().isDigit()
        if (isFloat) { advance(); while (!isAtEnd() && peek().isDigit()) advance() }
        addToken(if (isFloat) TokenType.NUMBER else TokenType.INTEGER)
    }

    private fun identifier() {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
        val text = source.substring(start, current)
        addToken(keywords[text] ?: TokenType.IDENTIFIER, text)
    }

    private fun blockComment() {
        var depth = 1
        while (!isAtEnd() && depth > 0) {
            when {
                peek() == '/' && peekNext() == '*' -> { advance(); advance(); depth++ }
                peek() == '*' && peekNext() == '/' -> { advance(); advance(); depth-- }
                peek() == '\n' -> { line++; lineStart = current; advance() }
                else -> advance()
            }
        }
        if (depth > 0) throw LexerException("Nezaključen blokovni komentar")
    }

    // ── Pomožne metode ────────────────────────────────────────────────────────

    private fun advance(): Char = source[current++]

    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char     = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]
    private fun isAtEnd()        = current >= source.length
    private fun column()         = current - lineStart

    private fun addToken(type: TokenType, lexeme: String = source.substring(start, current)) {
        tokens.add(Token(type, lexeme, line, start - lineStart + 1))
    }
}