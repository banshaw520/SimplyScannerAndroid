package com.ccs.simplyscannerandroid.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccs.simplyscannerandroid.R
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scanItems: List<ScanItem> = emptyList(),
    isLoading: Boolean = false,
    isImporting: Boolean = false,
    onScanButtonClick: () -> Unit = {},
    onItemClick: (ScanItem) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onNewFolderClick: () -> Unit = {},
    onSortClick: () -> Unit = {},
    onViewModeClick: () -> Unit = {},
    onSelectClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.main_screen_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp
                    ),
                    color = darkSlateBlueThree
                )
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.menu_button_description),
                        tint = darkSlateBlueThree
                    )
                }
            },
            actions = {
                if (isSelectionMode) {
                    TextButton(
                        onClick = {
                            isSelectionMode = false
                            selectedItems = setOf()
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = darkSlateBlueThree
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        // Search Bar (conditionally visible)
        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search documents...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = slateGrey
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { 
                        isSearchVisible = false
                        searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = slateGrey
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
        }
        
        // Function Bar (hidden when search is visible or in selection mode)
        if (!isSearchVisible && !isSelectionMode) {
            FunctionBar(
                onSearchClick = { 
                    isSearchVisible = true
                    onSearchClick()
                },
                onImportClick = onImportClick,
                onNewFolderClick = onNewFolderClick,
                onSortClick = onSortClick,
                onViewModeClick = onViewModeClick,
                onSelectClick = { 
                    isSelectionMode = true
                    onSelectClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
            )
        }
        
        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(gainsboro)
        ) {
            if (scanItems.isEmpty()) {
                // Empty State
                EmptyStateContent(
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Animated Arrow and Scan Button (hidden in selection mode)
                if (!isSelectionMode) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        AnimatedArrowAndScanButton(
                            onScanButtonClick = onScanButtonClick,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                // Document List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scanItems) { item ->
                        ScanItemCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
                
                // Floating Action Button when documents exist (hidden in selection mode)
                if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = onScanButtonClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(32.dp),
                        containerColor = babyBlue
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.scan_button_description),
                            tint = blueberry
                        )
                    }
                }
            }
        }
        
        // Ad Banner Placeholder
        AdBannerPlaceholder()
    }
}

@Composable
private fun FunctionBar(
    onSearchClick: () -> Unit,
    onImportClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_description),
                tint = slateGrey
            )
        }
        
        IconButton(onClick = onImportClick) {
            Icon(
                painter = painterResource(R.drawable.photo),
                contentDescription = stringResource(R.string.import_description),
                tint = slateGrey
            )
        }
        
        IconButton(onClick = onNewFolderClick) {
            Icon(
                painter = painterResource(R.drawable.folder_plus_outline_normal),
                contentDescription = stringResource(R.string.new_folder_description),
                tint = slateGrey
            )
        }
        
        IconButton(onClick = onSortClick) {
            Icon(
                painter = painterResource(R.drawable.sort_alphabetical_ascending),
                contentDescription = stringResource(R.string.sort_description),
                tint = slateGrey
            )
        }
        
        IconButton(onClick = onViewModeClick) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = stringResource(R.string.view_mode_description),
                tint = slateGrey
            )
        }
        
        IconButton(onClick = onSelectClick) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.select_description),
                tint = slateGrey
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Document stack illustration
        DocumentStackIllustration(
            modifier = Modifier.size(120.dp)
        )
        
        // Guide text
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            ),
            color = blueGrey,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DocumentStackIllustration(
    modifier: Modifier = Modifier
) {
    // Use the notes_150587 drawable for empty state illustration
    Icon(
        painter = painterResource(R.drawable.notes_150587),
        contentDescription = null,
        tint = Color.Unspecified, // Keep original colors from drawable
        modifier = modifier
    )
}

@Composable
private fun AnimatedArrowAndScanButton(
    onScanButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_animation")
    
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )
    
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Arrow
        Icon(
            painter = painterResource(R.drawable.arrow_down_bold),
            contentDescription = null,
            tint = Color(0xFF4FC3F7),
            modifier = Modifier
                .size(120.dp)
                .alpha(arrowAlpha)
                .offset(y = arrowOffset.dp)
        )
        
        // Scan Button
        FloatingActionButton(
            onClick = onScanButtonClick,
            modifier = Modifier
                .size(72.dp),
            containerColor = babyBlue,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = stringResource(R.string.scan_button_description),
                tint = blueberry,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanItemCard(
    item: ScanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (!item.bDir && item.order.isNotEmpty()) {
                    val pageCount = item.order.size
                    val pageCountText = if (pageCount == 1) {
                        stringResource(R.string.pages_count_single)
                    } else {
                        stringResource(R.string.pages_count_multiple, pageCount)
                    }
                    
                    Text(
                        text = pageCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (item.bLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.locked_item_description),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AdBannerPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Gray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AdMob Banner",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEmptyPreview() {
    SimplyScannerAndroidTheme {
        MainScreen(
            scanItems = emptyList(),
            onScanButtonClick = {},
            onItemClick = {},
            onSearchClick = {},
            onImportClick = {},
            onNewFolderClick = {},
            onSortClick = {},
            onViewModeClick = {},
            onSelectClick = {},
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenWithItemsPreview() {
    SimplyScannerAndroidTheme {
        MainScreen(
            scanItems = listOf(
                ScanItem(
                    uuid = "1",
                    displayName = "Document 1",
                    bDir = false,
                    relativePath = "./2025-01-01_12_00_00.000/",
                    order = listOf("page1.jpg", "page2.jpg")
                ),
                ScanItem(
                    uuid = "2",
                    displayName = "Important Folder",
                    bDir = true,
                    relativePath = "./2025-01-01_13_00_00.000/",
                    bLock = true
                )
            ),
            onScanButtonClick = {},
            onItemClick = {},
            onSearchClick = {},
            onImportClick = {},
            onNewFolderClick = {},
            onSortClick = {},
            onViewModeClick = {},
            onSelectClick = {},
            onMenuClick = {}
        )
    }
}