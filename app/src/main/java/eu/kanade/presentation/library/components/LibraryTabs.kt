package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)

    ScrollableTabRow(
        selectedTabIndex = currentPageIndex,
        edgePadding = 12.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {},
        modifier = Modifier
            .zIndex(2f)
            .padding(vertical = 8.dp)
    ) {
        categories.forEachIndexed { index, category ->
            val isSelected = currentPageIndex == index

            Tab(
                selected = isSelected,
                onClick = { onTabItemClick(index) },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    ),
                text = {
                    val contentColor = if (isSelected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant

                    // Inyectamos el color para que TabText lo use
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                            TabText(
                                text = category.visualName,
                                badgeCount = getItemCountForCategory(category),
                            )
                        }
                    }
                }
            )
        }
    }
}
