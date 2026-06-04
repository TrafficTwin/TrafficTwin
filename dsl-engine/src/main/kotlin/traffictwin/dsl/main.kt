package traffictwin.dsl

// ═══════════════════════════════════════════════════════════════════════════════
//  TrafficTwin DSL — Testni primeri (Main)
//
//  Zaganjanje:  kotlinc Lexer.kt Token.kt Types.kt Ast.kt Parser.kt
//                       SemanticValidator.kt Printer.kt Main.kt -include-runtime
//                       -d traffictwin.jar && java -jar traffictwin.jar
// ═══════════════════════════════════════════════════════════════════════════════

import java.io.File

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        null -> {
            val runner = TestRunner()
            runner.runAll()
            exportDefaultDemo()
        }

        "test" -> {
            val runner = TestRunner()
            runner.runAll()
        }

        "geojson" -> {
            val inputPath = args.getOrNull(1)
                ?: error("Manjka vhodna DSL datoteka. Primer: geojson src/main/resources/demo_mesto.dsl output.geojson")
            val outputPath = args.getOrNull(2) ?: "output.geojson"
            exportGeoJson(inputPath, outputPath)
        }

        "help", "--help", "-h" -> printUsage()

        else -> {
            println("Neznan ukaz: ${args.first()}")
            printUsage()
        }
    }
}

private fun parseDsl(source: String): ProgramNode {
    val tokens = Lexer(source).tokenize()
    return Parser(tokens).parse()
}

private fun exportGeoJson(inputPath: String, outputPath: String) {
    val inputFile = File(inputPath)
    require(inputFile.exists()) { "Vhodna datoteka ne obstaja: ${inputFile.path}" }

    val source = inputFile.readText()
    val ast = parseDsl(source)

    val validation = SemanticValidator.validate(ast)
    if (!validation.isValid) {
        println("Semantična validacija ni uspela:")
        validation.errors.forEach { println("  NAPAKA: $it") }
        return
    }
    validation.warnings.forEach { println("  OPOZORILO: $it") }

    val geoJson = GeoJsonExporter().export(ast)
    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(geoJson)

    println("GeoJSON izvoz je pripravljen: ${outputFile.path}")
    println("Datoteko lahko odpreš ali kopiraš v https://geojson.io")
}

private fun exportDefaultDemo() {
    val candidates = listOf(
        File("dsl-engine/src/main/resources/demo_mesto.dsl"),
        File("src/main/resources/demo_mesto.dsl")
    )

    val demo = candidates.firstOrNull { it.exists() }
    if (demo == null) {
        println("Demo datoteke demo_mesto.dsl nisem našel, zato GeoJSON ni bil izvožen.")
        return
    }

    val output = File("output.geojson")
    exportGeoJson(demo.path, output.path)
}

private fun printUsage() {
    println(
        """
        Uporaba:
          ./gradlew :dsl-engine:run
              Zažene vse teste in izvozi demo_mesto.dsl v output.geojson.

          ./gradlew :dsl-engine:run --args="test"
              Zažene samo testno zbirko.

          ./gradlew :dsl-engine:run --args="geojson <vhod.dsl> <izhod.geojson>"
              Prebere DSL datoteko, zgradi AST, izvede semantično validacijo in izvozi GeoJSON.

        Primer:
          ./gradlew :dsl-engine:run --args="geojson dsl-engine/src/main/resources/demo_mesto.dsl output.geojson"
        """.trimIndent()
    )
}

// ── Pomožni razred za poganjanje testov ──────────────────────────────────────

class TestRunner {

    private val printer = Printer()
    private var passed = 0
    private var failed = 0

    fun runAll() {
        header("TrafficTwin DSL — Testna zbirka")
        runPositive()
        runNegative()
        runWarning()
        summary()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POZITIVNI TESTI (1–6)
    // ─────────────────────────────────────────────────────────────────────────

    private fun runPositive() {
        section("POZITIVNI TESTI — parser mora uspešno razčleniti")

        // ── Test 1 ────────────────────────────────────────────────────────────
        positiveTest(1, "Minimalno veljavno mesto — city, road, building") {
            """
            city "Mini" {
                road "Glavna" {
                    line((0, 0), (10, 0));
                };
                building "Obcina" {
                    box((2, 2), (4, 0));
                };
            }
            """.trimIndent()
        }

        // ── Test 2 ────────────────────────────────────────────────────────────
        positiveTest(2, "Cesta s stanjem in relacijo") {
            """
            city "Promet" {
                road "R2-441" {
                    type "regionalna";
                    relation "Murska Sobota - Rakičan";
                    state WORKS;
                    speedLimit 50;
                    polyline((16.1600, 46.6620), (16.1700, 46.6640), (16.1800, 46.6660));
                };
            }
            """.trimIndent()
        }

        // ── Test 3 ────────────────────────────────────────────────────────────
        positiveTest(3, "Parkirišče z zasedenostjo — occupied < capacity") {
            """
            city "Parkirisca" {
                parking "Parkirisce center" {
                    id 1;
                    point (16.1608, 46.6624);
                    capacity 120;
                    occupied 73;
                    payment PAID;
                    status OPEN;
                };
            }
            """.trimIndent()
        }

        // ── Test 4 ────────────────────────────────────────────────────────────
        positiveTest(4, "Poizvedba nearby — bližnja parkirišča") {
            """
            city "Iskanje" {
                let lokacija = (16.1608, 46.6624);
                parking "P1" { id 1; point (16.1610, 46.6620); capacity 50; occupied 10; payment FREE; };
                parking "P2" { id 2; point (16.1900, 46.6800); capacity 30; occupied 30; payment PAID; };

                query "Prosta v blizini" {
                    nearby(lokacija, 1000, parking);
                    where freeSpaces > 0;
                    sortBy distance;
                };
            }
            """.trimIndent()
        }

        // ── Test 5 ────────────────────════════════════════════════════════════
        positiveTest(5, "Spremenljivke in izrazi — let, fst, snd, aritmetika") {
            """
            city "Izrazi" {
                let p = (16.1000, 46.6000);
                let q = (fst(p) + 0.0100, snd(p) + 0.0050);

                road "Izracunana cesta" {
                    line(p, q);
                    state OPEN;
                };
            }
            """.trimIndent()
        }

        // ── Test 6 ────────────────────────────────────────────────────────────
        positiveTest(6, "Park in križišče — polygon in junction") {
            """
            city "Zeleno mesto" {
                park "Mestni park" {
                    polygon((0, 0), (4, 0), (4, 3), (0, 3), (0, 0));
                    set("surface", "grass");
                };
                junction "Krizisce pri parku" (4, 1.5);
            }
            """.trimIndent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NEGATIVNI TESTI (7–9)
    // ─────────────────────────────────────────────────────────────────────────

    private fun runNegative() {
        section("NEGATIVNI TESTI — semantični analizator mora vrniti napako")

        // ── Test 7 ────────────────────────────────────────────────────────────
        // Parser sprejme sintakso, semantični analizator pa zavrne degeneriran poligon.
        // Opomba: polygon z (0,0),(3,0),(3,2),(0,2) ima 4 unikatne točke → OK za validator.
        // Da demonstriramo napako test 7, uporabimo poligon z le 2 unikatnima točkama.
        negativeTest(7, "Nezaključen poligon zgradbe — first != last") {
            """
    city "Napaka poligon" {
        building "Nezakljucena" {
            polygon((0, 0), (3, 0), (3, 2), (0, 2));
        };
    }
    """.trimIndent()
        }

        // ── Test 8 ────────────────────────────────────────────────────────────
        negativeTest(8, "Zasedenost večja od kapacitete — occupied > capacity") {
            """
            city "Napaka parkirisce" {
                parking "Prepolno" {
                    id 5;
                    point (16.1608, 46.6624);
                    capacity 20;
                    occupied 25;
                    payment PAID;
                };
            }
            """.trimIndent()
        }

        // ── Test 9 ────────────────────────────────────────────────────────────
        negativeTest(9, "Neveljavne geografske koordinate — latitude 120 ni veljavna") {
            """
            city "Napaka koordinate" {
                parking "Napacna tocka" {
                    id 8;
                    point (16.1608, 120.0000);
                    capacity 10;
                    occupied 2;
                    payment FREE;
                };
            }
            """.trimIndent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPOZORILNI TEST (10)
    // ─────────────────────────────────────────────────────────────────────────

    private fun runWarning() {
        section("OPOZORILNI TEST — sintaksa veljavna, validator opozori")

        // ── Test 10 ───────────────────────────────────────────────────────────
        warningTest(10, "Cesta seka zgradbo — prostorski konflikt") {
            """
            city "Presek" {
                road "Cesta skozi stavbo" {
                    line((0, 1), (5, 1));
                    state OPEN;
                };
                building "Stavba" {
                    box((2, 0), (4, 2));
                };
            }
            """.trimIndent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Pomožne metode za poganjanje testov
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pozitiven test: parser IN semantični analizator morata uspeti.
     */
    private fun positiveTest(num: Int, desc: String, source: () -> String) {
        print("  [T$num] $desc\n        ")
        try {
            val src = source()
            val tokens = Lexer(src).tokenize()
            val ast    = Parser(tokens).parse()
            val result = SemanticValidator.validate(ast)

            if (!result.isValid) {
                println("✘  NEUSPEH (semantična napaka pri pozitivnem testu!)")
                result.errors.forEach { println("        ↳ NAPAKA: $it") }
                failed++
            } else {
                // Round-trip: natisni in ponovno razčleni
                val reprinted = printer.print(ast)
                Parser(Lexer(reprinted).tokenize()).parse()
                println("✔  USPEH")
                if (result.hasWarnings()) result.warnings.forEach { println("        ⚠ OPOZORILO: $it") }
                passed++
            }
        } catch (e: Exception) {
            println("✘  NEUSPEH (${e::class.simpleName}: ${e.message})")
            failed++
        }
    }

    /**
     * Negativen test: parser mora uspeti, semantični analizator pa mora vrniti napako.
     */
    private fun negativeTest(num: Int, desc: String, source: () -> String) {
        print("  [T$num] $desc\n        ")
        try {
            val src = source()
            val tokens = Lexer(src).tokenize()
            val ast    = Parser(tokens).parse()
            val result = SemanticValidator.validate(ast)

            if (!result.isValid) {
                println("✔  USPEH (semantična napaka pravilno zaznana)")
                result.errors.forEach { println("        ↳ NAPAKA: $it") }
                passed++
            } else {
                println("✘  NEUSPEH (pričakovana semantična napaka ni bila zaznana!)")
                failed++
            }
        } catch (e: ParseException) {
            // Parser ne bi smel zavrniti — napaka je pričakovana šele na semantični ravni
            println("✘  NEUSPEH (parser je zavrnil sintaktično veljavno kodo: ${e.message})")
            failed++
        } catch (e: Exception) {
            println("✘  NEUSPEH (${e::class.simpleName}: ${e.message})")
            failed++
        }
    }

    /**
     * Opozorilni test: vse mora uspeti, validator pa mora vrniti vsaj eno opozorilo.
     */
    private fun warningTest(num: Int, desc: String, source: () -> String) {
        print("  [T$num] $desc\n        ")
        try {
            val src = source()
            val tokens = Lexer(src).tokenize()
            val ast    = Parser(tokens).parse()
            val result = SemanticValidator.validate(ast)

            if (!result.isValid) {
                println("✘  NEUSPEH (nastala je napaka, pričakovano le opozorilo)")
                result.errors.forEach { println("        ↳ NAPAKA: $it") }
                failed++
            } else if (!result.hasWarnings()) {
                println("✘  NEUSPEH (ni opozoril — prostorski konflikt ni bil zaznan!)")
                failed++
            } else {
                println("✔  USPEH (opozorilo pravilno zaznano)")
                result.warnings.forEach { println("        ⚠ OPOZORILO: $it") }
                passed++
            }
        } catch (e: Exception) {
            println("✘  NEUSPEH (${e::class.simpleName}: ${e.message})")
            failed++
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun header(title: String) {
        println()
        println("╔══════════════════════════════════════════════════════════╗")
        println("║  $title")
        println("╚══════════════════════════════════════════════════════════╝")
        println()
    }

    private fun section(title: String) {
        println()
        println("  ── $title ──")
        println()
    }

    private fun summary() {
        val total = passed + failed
        println()
        println("  ──────────────────────────────────────────────────────────")
        println("  Rezultati: $passed/$total uspešnih, $failed/$total neuspešnih")
        println("  ──────────────────────────────────────────────────────────")
        println()
    }
}