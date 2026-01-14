package eu.kanade.tachiyomi.ui.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.openInBrowser

class ExtensionManagerScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { ExtensionsScreenModel() }
        val state by screenModel.state.collectAsState()

        var isSearchActive by remember { mutableStateOf(false) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (isSearchActive) {
                    // CORRECCIÓN 2: Ajustado SearchToolbar para tu versión
                    SearchToolbar(
                        searchQuery = state.searchQuery ?: "",
                        onChangeSearchQuery = screenModel::search,
                        placeholderText = "Buscar extensiones...",
                        scrollBehavior = scrollBehavior,
                        onClickCloseSearch = {
                            isSearchActive = false
                            screenModel.search("")
                        },
                        navigateUp = {
                            isSearchActive = false
                            screenModel.search("")
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = "Extensiones") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { contentPadding ->
            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension -> screenModel.uninstallExtension(extension) },
                onClickItemCancel = { extension -> screenModel.cancelInstallUpdateExtension(extension) },
                onOpenWebView = { extension ->
                    extension.sources.firstOrNull()?.let {
                        context.openInBrowser(it.baseUrl)
                    }
                },
                onInstallExtension = { extension -> screenModel.installExtension(extension) },
                onUninstallExtension = { extension -> screenModel.uninstallExtension(extension) },
                onUpdateExtension = { extension -> screenModel.updateExtension(extension) },

                // CORRECCIÓN 3: Usar trustExtension en lugar de trustSignature
                onTrustExtension = { extension -> screenModel.trustExtension(extension) },

                // CORRECCIÓN 4: Código nativo para abrir detalles de la app
                onOpenExtension = { extension ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },

                onClickUpdateAll = { screenModel.updateAllExtensions() },
                onRefresh = { screenModel.findAvailableExtensions() },
            )
        }
    }
}
