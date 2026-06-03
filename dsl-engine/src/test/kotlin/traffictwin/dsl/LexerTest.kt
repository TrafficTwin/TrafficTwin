package traffictwin.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LexerTest {

    @Test
    fun `tokenizes minimal city`() {
        val source = """
            city "Mini" {
                road "Glavna" {
                    line((0, 0), (10, 0));
                };
            }
        """.trimIndent()

        val tokens = Lexer(source).tokenize()
        val types = tokens.map { it.type }

        assertTrue(TokenType.CITY in types)
        assertTrue(TokenType.ROAD in types)
        assertTrue(TokenType.LINE in types)
        assertEquals(TokenType.EOF, tokens.last().type)
    }

    @Test
    fun `skips line comments`() {
        val source = """
            // komentar
            city "Mini" {}
        """.trimIndent()

        val tokens = Lexer(source).tokenize()

        assertEquals(TokenType.CITY, tokens[0].type)
        assertEquals(TokenType.STRING, tokens[1].type)
        assertEquals("Mini", tokens[1].lexeme)
    }

    @Test
    fun `skips nested block comments`() {
        val source = """
            city "Mini" {
                /* prvi /* drugi */ konec prvega */
                road "Glavna" {};
            }
        """.trimIndent()

        val tokens = Lexer(source).tokenize()
        val types = tokens.map { it.type }

        assertTrue(TokenType.CITY in types)
        assertTrue(TokenType.ROAD in types)
        assertEquals(TokenType.EOF, tokens.last().type)
    }

    @Test
    fun `tokenizes escaped string`() {
        val source = """city "Mini \"Center\"" {}"""

        val tokens = Lexer(source).tokenize()

        assertEquals(TokenType.STRING, tokens[1].type)
        assertEquals("Mini \"Center\"", tokens[1].lexeme)
    }

    @Test
    fun `keeps fst snd freeSpaces and distance as identifiers`() {
        val source = """
            let q = fst(p) + snd(p);
            where freeSpaces > distance;
        """.trimIndent()

        val tokens = Lexer(source).tokenize()
        val identifiers = tokens
            .filter { it.type == TokenType.IDENTIFIER }
            .map { it.lexeme }

        assertTrue("fst" in identifiers)
        assertTrue("snd" in identifiers)
        assertTrue("freeSpaces" in identifiers)
        assertTrue("distance" in identifiers)
    }

    @Test
    fun `minus is separate token from number`() {
        val tokens = Lexer("let x = -12.5;").tokenize()
        val types = tokens.map { it.type }

        assertEquals(
            listOf(
                TokenType.LET,
                TokenType.IDENTIFIER,
                TokenType.EQUAL,
                TokenType.MINUS,
                TokenType.NUMBER,
                TokenType.SEMICOLON,
                TokenType.EOF,
            ),
            types,
        )
    }

    @Test
    fun `throws on invalid character`() {
        val ex = assertFailsWith<LexerException> {
            Lexer("""city "Mini" { @ }""").tokenize()
        }

        assertTrue(ex.message!!.contains("@"))
    }

    @Test
    fun `throws on bare exclamation mark`() {
        val ex = assertFailsWith<LexerException> {
            Lexer("where occupied ! capacity;").tokenize()
        }

        assertTrue(ex.message!!.contains("!="))
    }

    @Test
    fun `throws on unterminated string`() {
        assertFailsWith<LexerException> {
            Lexer("""city "Mini""").tokenize()
        }
    }

    @Test
    fun `throws on unterminated block comment`() {
        assertFailsWith<LexerException> {
            Lexer("""city "Mini" { /* komentar """).tokenize()
        }
    }
}