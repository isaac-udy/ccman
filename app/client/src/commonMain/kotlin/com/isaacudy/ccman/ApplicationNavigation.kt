package com.isaacudy.ccman

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.enro.annotations.NavigationComponent
import dev.enro.controller.NavigationComponentConfiguration
import dev.enro.controller.createNavigationModule
import dev.enro.navigationHandle
import dev.enro.ui.decorators.NavigationDestinationDecorator
import dev.enro.viewmodel.withNavigationHandle
import org.koin.compose.currentKoinScope
import kotlin.reflect.KClass

@NavigationComponent
object ApplicationNavigation : NavigationComponentConfiguration(
    module = createNavigationModule {
        decorator {
            NavigationDestinationDecorator(
                onRemove = {},
                decorator = { destination ->
                    val localViewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current)
                    val scope = currentKoinScope()
                    val navigationHandle = navigationHandle()
                    val owner = remember {
                        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
                            override val viewModelStore: ViewModelStore
                                get() = localViewModelStoreOwner.viewModelStore

                            override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                                get() = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(
                                        modelClass: KClass<T>,
                                        extras: CreationExtras
                                    ): T {
                                        return scope.get(modelClass)
                                    }
                                }.withNavigationHandle(navigationHandle)
                        }
                    }
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides owner
                    ) {
                        destination.content()
                    }
                }
            )
        }
    }
)