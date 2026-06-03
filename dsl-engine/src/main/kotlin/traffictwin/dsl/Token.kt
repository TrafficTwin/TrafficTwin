package traffictwin.dsl

enum class TokenType {
    CITY,
    ROAD,
    BUILDING,
    PARKING,
    PARK,
    ZONE,
    JUNCTION,
    MARKER,
    SENSOR,
    QUERY,
    LET,
    NIL,

    LINE,
    BEND,
    POLYLINE,
    POLYGON,
    BOX,
    CIRC,

    NEARBY,
    WHERE,
    SORT_BY,
    HIGHLIGHT,
    SET,

    ID,
    POINT,
    CAPACITY,
    OCCUPIED,
    PAYMENT,
    STATUS,
    TYPE,
    RELATION,
    STATE,
    SPEED_LIMIT,
    LANES,
    ONEWAY,

    OPEN,
    CONGESTED,
    WORKS,
    CLOSED,
    UNKNOWN,
    FREE,
    PAID,
    MIXED,
    FULL,

    TRUE,
    FALSE,

    IDENTIFIER,
    STRING,
    NUMBER,
    INTEGER,

    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_PAREN,
    RIGHT_PAREN,
    COMMA,
    SEMICOLON,
    EQUAL,
    PLUS,
    MINUS,
    STAR,
    SLASH,

    EQUAL_EQUAL,
    BANG_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,

    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int
)