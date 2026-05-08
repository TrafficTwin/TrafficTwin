package si.um.feri

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    println("Izberi možnost:")
    println("1 - prvi parser")
    println("2 - parser gostota")
    print("Vnos: ")

    when (readln()) {
        "1" -> {
            println("Zagnal se bo prvi parser")
            parserParking()
        }

        "2" -> {
            println("Zagnal se bo parser gostota")
            parserGostota()
        }

        else -> {
            println("Napačna izbira. Izberi 1 ali 2.")
        }
    }
}