package si.um.feri

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    println("Izberi možnost:")
    println("1 - parser parkirišč")
    println("2 - parser stanja na cestah")
    print("Vnos: ")

    when (readln()) {
        "1" -> {
            println("Zagnal se bo prvi parser")
            parserParking()
        }

        "2" -> {
            println("Zagnal se bo parser stanja")
            parserStanje()
        }

        else -> {
            println("Napačna izbira. Izberi 1 ali 2.")
        }
    }
}