package si.um.feri

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import traffictwin.composeapp.generated.resources.Res
import traffictwin.composeapp.generated.resources.car

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Digital Twin: Traffic & Parking",
        icon = painterResource(Res.drawable.car)
    ) {
        App()
    }
}