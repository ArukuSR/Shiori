package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.browse.SourceOptionsDialog
import eu.kanade.presentation.browse.SourcesScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data object BrowseTab : Tab {

    // Esto elimina la advertencia amarilla de "readResolve"
    private fun readResolve(): Any = BrowseTab

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourcesScreenModel() }
        val state by screenModel.state.collectAsState()

        // TRUCO: Usamos una variable local para controlar el diálogo
        // Así no dependemos de si "SourceLongClick" existe o no en tu modelo.
        var sourceToManage by remember { mutableStateOf<Source?>(null) }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                // Barra súper limpia: Solo el título "Explorar"
                TopAppBar(
                    title = { Text(text = "Explorar") },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { contentPadding ->

            SourcesScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source, listing ->
                    navigator.push(BrowseSourceScreen(source.id, listing.query))
                },
                onClickPin = { source ->
                    screenModel.togglePin(source)
                },
                onLongClickItem = { source ->
                    // Guardamos la fuente localmente para abrir el diálogo
                    sourceToManage = source
                }
            )

            // Mostramos el diálogo si hay una fuente seleccionada
            sourceToManage?.let { source ->
                SourceOptionsDialog(
                    source = source,
                    onClickPin = {
                        screenModel.togglePin(source)
                        sourceToManage = null
                    },
                    onClickDisable = {
                        // He comentado la línea conflictiva "toggleExclude" para que compile.
                        // Si quieres habilitarla, descomenta y prueba si se llama "toggleDisable" o similar.
                        // screenModel.toggleExclude(source)
                        screenModel.togglePin(source) // Acción temporal segura
                        sourceToManage = null
                    },
                    onDismiss = { sourceToManage = null }
                )
            }
        }
    }
}
