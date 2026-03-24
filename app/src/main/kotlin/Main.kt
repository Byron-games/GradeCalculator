import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import ui.components.LocalAwtWindow   // ← your own local

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title          = "Grade Calculator",
        state          = WindowState(size = DpSize(1100.dp, 720.dp))
    ) {
        // 'window' here is ComposeWindow, available directly in FrameWindowScope
        CompositionLocalProvider(LocalAwtWindow provides window) {
            App()
        }
    }
}