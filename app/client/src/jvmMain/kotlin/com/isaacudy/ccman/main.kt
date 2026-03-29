package com.isaacudy.ccman

import androidx.compose.ui.window.application
import dev.enro.platform.desktop.GenericRootWindow
import dev.enro.platform.desktop.RootWindow
import dev.enro.platform.desktop.openWindow
import dev.enro.ui.EnroApplicationContent

fun main() {
    val controller = ApplicationNavigation.installNavigationController(Unit)
    controller.openWindow(
        GenericRootWindow(
            windowConfiguration = {
                RootWindow.WindowConfiguration(
                    title = "CCMan"
                )
            }
        ) {
            ApplicationContent()
        }
    )
    application {
        EnroApplicationContent(controller)
    }
}
