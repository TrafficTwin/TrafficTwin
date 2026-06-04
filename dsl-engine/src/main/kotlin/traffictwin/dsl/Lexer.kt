package traffictwin.dsl

class LexerException(message: String) : Exception(message)

class Lexer(private val source: String) {

    private var start = 0
    private var current = 0
    private var line = 1
    private var lineStart = 0

    private val tokens = mutableListOf<Token>()

    private val keywords = mapOf(
        // Bloki
        "city" to TokenType.CITY,
        "road" to TokenType.ROAD,
        "building" to TokenType.BUILDING,
        "parking" to TokenType.PARKING,
        "park" to TokenType.PARK,
        "zone" to TokenType.ZONE,
        "junction" to TokenType.JUNCTION,
        "marker" to TokenType.MARKER,
        "sensor" to TokenType.SENSOR,
        "query" to TokenType.QUERY,
        "let" to TokenType.LET,
        "nil" to TokenType.NIL,

        // Geometrija
        "line" to TokenType.LINE,
        "bend" to TokenType.BEND,
        "polyline" to TokenType.POLYLINE,
        "polygon" to TokenType.POLYGON,
        "box" to TokenType.BOX,
        "circ" to TokenType.CIRC,

        // Poizvedbe
        "nearby" to TokenType.NEARBY,
        "where" to TokenType.WHERE,
        "sort_by" to TokenType.SORT_BY,
        "sortBy" to TokenType.SORT_BY,
        "highlight" to TokenType.HIGHLIGHT,
        "set" to TokenType.SET,

        // Atributi
        "id" to TokenType.ID,
        "point" to TokenType.POINT,
        "capacity" to TokenType.CAPACITY,
        "occupied" to TokenType.OCCUPIED,
        "payment" to TokenType.PAYMENT,
        "status" to TokenType.STATUS,
        "type" to TokenType.TYPE,
        "relation" to TokenType.RELATION,
        "state" to TokenType.STATE,
        "speed_limit" to TokenType.SPEED_LIMIT,
        "speedLimit" to TokenType.SPEED_LIMIT,
        "lanes" to TokenType.LANES,
        "oneway" to TokenType.ONEWAY,

        // Stanja ceste
        "open" to TokenType.OPEN,
        "OPEN" to TokenType.OPEN,
        "congested" to TokenType.CONGESTED,
        "CONGESTED" to TokenType.CONGESTED,
        "works" to TokenType.WORKS,
        "WORKS" to TokenType.WORKS,
        "closed" to TokenType.CLOSED,
        "CLOSED" to TokenType.CLOSED,
        "unknown" to TokenType.UNKNOWN,
        "UNKNOWN" to TokenType.UNKNOWN,

        // Plačilo
        "free" to TokenType.FREE,
        "FREE" to TokenType.FREE,
        "paid" to TokenType.PAID,
        "PAID" to TokenType.PAID,
        "mixed" to TokenType.MIXED,
        "MIXED" to TokenType.MIXED,

        // Status parkirišča
        "full" to TokenType.FULL,
        "FULL" to TokenType.FULL,

        // Boolean
        "true" to TokenType.TRUE,
        "false" to TokenType.FALSE,

        // Funkciji za točke ostaneta IDENTIFIER, ker ju parser obravnava kot klic funkcije.
        "fst" to TokenType.IDENTIFIER,
        "snd" to TokenType.IDENTIFIER,
    )

    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", line, current - lineStart + 1))
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

            '/' -> when {
                match('/') -> skipLineComment()
                match('*') -> skipBlockComment()
                else -> addToken(TokenType.SLASH)
            }

            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)

            '!' -> {
                if (match('=')) {
                    addToken(TokenType.BANG_EQUAL)
                } else {
                    throw LexerException("Nepričakovani znak '!' v vrstici $line, stolpcu ${start - lineStart + 1}. Ali si mislil '!='?")
                }
            }

            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            '"' -> string()

            ' ', '\r', '\t' -> Unit

            '\n' -> newLine()

            else -> when {
                c.isDigit() -> number()
                c.isLetter() || c == '_' -> identifier()
                else -> throw LexerException("Nepričakovani znak '$c' v vrstici $line, stolpcu ${start - lineStart + 1}")
            }
        }
    }

    private fun skipLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }
    }

    private fun skipBlockComment() {
        var depth = 1
        val commentStartLine = line
        val commentStartColumn = start - lineStart + 1

        while (!isAtEnd() && depth > 0) {
            when {
                peek() == '/' && peekNext() == '*' -> {
                    advance()
                    advance()
                    depth++
                }

                peek() == '*' && peekNext() == '/' -> {
                    advance()
                    advance()
                    depth--
                }

                else -> {
                    val c = advance()
                    if (c == '\n') {
                        newLine()
                    }
                }
            }
        }

        if (depth > 0) {
            throw LexerException("Nezaključen blokovni komentar, začet v vrstici $commentStartLine, stolpcu $commentStartColumn")
        }
    }

    private fun string() {
        val value = StringBuilder()
        val stringStartLine = line
        val stringStartColumn = start - lineStart + 1

        while (!isAtEnd()) {
            val c = advance()

            when (c) {
                '"' -> {
                    tokens.add(Token(TokenType.STRING, value.toString(), stringStartLine, stringStartColumn))
                    return
                }

                '\\' -> {
                    if (isAtEnd()) {
                        throw LexerException("Nezaključen escape v nizu, začet v vrstici $stringStartLine, stolpcu $stringStartColumn")
                    }

                    val escaped = advance()
                    value.append(
                        when (escaped) {
                            '"' -> '"'
                            '\\' -> '\\'
                            'n' -> '\n'
                            't' -> '\t'
                            'r' -> '\r'
                            else -> escaped
                        }
                    )
                }

                '\n' -> {
                    newLine()
                    value.append(c)
                }

                else -> value.append(c)
            }
        }

        throw LexerException("Nezaključen niz, začet v vrstici $stringStartLine, stolpcu $stringStartColumn")
    }

    private fun number() {
        while (!isAtEnd() && peek().isDigit()) {
            advance()
        }

        val isFloat = peek() == '.' && peekNext().isDigit()
        if (isFloat) {
            advance()
            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }
        }

        addToken(if (isFloat) TokenType.NUMBER else TokenType.INTEGER)
    }

    private fun identifier() {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }

        val text = source.substring(start, current)
        addToken(keywords[text] ?: TokenType.IDENTIFIER, text)
    }

    private fun newLine() {
        line++
        lineStart = current
    }

    private fun advance(): Char = source[current++]

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]

    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun isAtEnd(): Boolean = current >= source.length

    private fun addToken(type: TokenType, lexeme: String = source.substring(start, current)) {
        tokens.add(Token(type, lexeme, line, start - lineStart + 1))
    }
}