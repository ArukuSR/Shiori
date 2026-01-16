package eu.kanade.tachiyomi.ui.history

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import androidx.compose.ui.unit.sp

data object HistoryTab : Tab {

    private val snackbarHostState = SnackbarHostState()
    private val resumeLastChapterReadEvent = Channel<Unit>()

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val homeIcon = rememberVectorPainter(Icons.Outlined.Home)

            return remember(homeIcon) {
                TabOptions(
                    index = 0u,
                    title = "Inicio",
                    icon = homeIcon,
                )
            }
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastChapterReadEvent.send(Unit)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { HistoryScreenModel() }
        val state by screenModel.state.collectAsState()

        var showProfileDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Shiori",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(Icons.Outlined.Person, contentDescription = "Perfil")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val historyItems = state.list?.filterIsInstance<HistoryUiModel.Item>() ?: emptyList()
                val recentManga = state.recentlyAdded ?: emptyList()

                // --- HERO BANNER (NUEVO) ---
                if (historyItems.isNotEmpty()) {
                    val lastRead = historyItems.first().item
                    HeroResumeBanner(
                        history = lastRead,
                        onClick = { navigator.push(MangaScreen(lastRead.mangaId)) },
                        onResume = { screenModel.getNextChapterForManga(lastRead.mangaId, lastRead.chapterId) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Continuar leyendo",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )

                if (historyItems.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItems.drop(1)) { item -> // Saltamos el primero porque ya sale en el Banner
                            HistoryCarouselCard(
                                history = item.item,
                                onClick = { navigator.push(MangaScreen(item.item.mangaId)) },
                                onResume = { screenModel.getNextChapterForManga(item.item.mangaId, item.item.chapterId) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recién añadidos",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )

                if (recentManga.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentManga) { manga ->
                            MangaCarouselCard(
                                manga = manga,
                                onClick = { navigator.push(MangaScreen(manga.id)) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Biblioteca vacía...",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showProfileDialog) {
            UserProfileDialog(
                onDismiss = { showProfileDialog = false }
            )
        }

        LaunchedEffect(state.list) {
            if (state.list != null) (context as? MainActivity)?.ready = true
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { e ->
                when (e) {
                    is HistoryScreenModel.Event.OpenChapter -> openChapter(context, e.chapter)
                    else -> {}
                }
            }
        }

        LaunchedEffect(Unit) {
            resumeLastChapterReadEvent.receiveAsFlow().collectLatest {
                openChapter(context, screenModel.getNextChapter())
            }
        }
    }

    // --- NUEVO COMPONENTE: BANNER PRINCIPAL ---
    @Composable
    fun HeroResumeBanner(
        history: HistoryWithRelations,
        onClick: () -> Unit,
        onResume: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Altura controlada, no ocupa toda la pantalla
                .clickable { onClick() }
        ) {
            // Fondo Ambiental (Imagen borrosa y oscura)
            AsyncImage(
                model = history.coverData.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.3f, // Oscurecido
                modifier = Modifier.fillMaxSize()
            )

            // Gradiente para que el texto se lea bien
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                        )
                    )
            )

            // Contenido (Portada + Textos)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Portada Vertical "Flotante"
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                ) {
                    AsyncImage(
                        model = history.coverData.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info y Botón
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "REANUDAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Capítulo ${history.chapterNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onResume,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leer")
                    }
                }
            }
        }
    }

    @Composable
    fun UserProfileDialog(onDismiss: () -> Unit) {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("shiori_prefs", Context.MODE_PRIVATE) }

        var isDiscordConnected by remember {
            mutableStateOf(prefs.getBoolean("is_discord_connected", false))
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (isDiscordConnected) "Perfil de Discord" else "Conectar Discord",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isDiscordConnected) {
                        Text(
                            "Inicia sesión para activar Rich Presence y mostrar tu actividad en tiempo real.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val discordColor = Color(0xFF5865F2)
                        Button(
                            onClick = {
                                isDiscordConnected = true
                                prefs.edit().putBoolean("is_discord_connected", true).apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = discordColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Conectar con Discord")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))

                        AsyncImage(
                            model = "https://cdn.discordapp.com/embed/avatars/0.png",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ShioriUser",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF43B581),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rich Presence Activo",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF43B581)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isDiscordConnected = false
                                prefs.edit().putBoolean("is_discord_connected", false).apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar Sesión")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
    }

    @Composable
    fun HistoryCarouselCard(
        history: HistoryWithRelations,
        onClick: () -> Unit,
        onResume: () -> Unit
    ) {
        Column(modifier = Modifier.width(140.dp)) {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clickable { onResume() }
            ) {
                AsyncImage(
                    model = history.coverData.url,
                    contentDescription = history.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(history.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Cap. ${history.chapterNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    fun MangaCarouselCard(
        manga: Manga,
        onClick: () -> Unit
    ) {
        Column(modifier = Modifier.width(140.dp)) {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(manga.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    private suspend fun openChapter(context: Context, chapter: Chapter?) {
        if (chapter != null) {
            val intent = ReaderActivity.newIntent(context, chapter.mangaId, chapter.id)
            context.startActivity(intent)
        } else {
            snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
        }
    }
}
