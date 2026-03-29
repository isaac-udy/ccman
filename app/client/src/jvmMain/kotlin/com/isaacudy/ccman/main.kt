package com.isaacudy.ccman

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.application
import dev.enro.NavigationKey
import dev.enro.context.activeLeaf
import dev.enro.context.getNavigationHandle
import dev.enro.platform.desktop.GenericRootWindow
import dev.enro.platform.desktop.RootWindow
import dev.enro.platform.desktop.openWindow
import dev.enro.requestClose
import dev.enro.ui.EnroApplicationContent
import dev.enro.ui.LocalNavigationContext

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
            val context = LocalNavigationContext.current
            MenuBar {
                Menu("Window") {
                    Item(
                        "Back",
                        shortcut = KeyShortcut(
                            key = Key.LeftBracket,
                            meta = true
                        )
                    ) {
                        context.activeLeaf().getNavigationHandle<NavigationKey>().requestClose()
                    }
                    Item(
                        "Close",
                        shortcut = KeyShortcut(
                            key = Key.W,
                            meta = true
                        )
                    ) {
                        context.activeLeaf().getNavigationHandle<NavigationKey>().requestClose()
                    }
                }
            }
            ApplicationContent()
        }
    )
    application {
        EnroApplicationContent(controller)
    }
}
