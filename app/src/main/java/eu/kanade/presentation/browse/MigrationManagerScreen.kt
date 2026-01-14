package eu.kanade.tachiyomi.ui.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.MigrateSourceScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
// Si esta línea da error rojo, bórrala y deja que Android Studio la sugiera (Alt+Enter):
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen

class MigrationManagerScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MigrateSourceScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Migración") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            }
        ) { contentPadding ->
            MigrateSourceScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    navigator.push(MigrateMangaScreen(source.id))
                },
                onToggleSortingDirection = { screenModel.toggleSortingDirection() },
                onToggleSortingMode = { screenModel.toggleSortingMode() }
            )
        }
    }
}
