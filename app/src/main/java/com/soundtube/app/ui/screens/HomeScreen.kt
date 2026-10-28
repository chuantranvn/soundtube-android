package com.soundtube.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtube.app.data.model.Song
import com.soundtube.app.ui.components.QuantumExploreIcon
import com.soundtube.app.ui.components.QuantumSearchIcon
import com.soundtube.app.ui.components.YouTubeVideoCard
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.NeuButton
import com.soundtube.app.ui.theme.QuantumLoadingIndicator
import com.soundtube.app.ui.theme.QuantumMiniLoader
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary
import com.soundtube.app.ui.theme.neumorphic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedCategory: String,
    searchQuery: String,
    isSearchActive: Boolean,
    searchResults: List<Song>,
    isLoading: Boolean,
    isLoadingMore: Boolean = false,
    statusMessage: String?,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val categories = listOf("Tất cả", "Truyện / Sách nói", "Âm nhạc", "Danh sách kết hợp", "Podcast", "Tin tức")

    val listState = rememberLazyListState()

    // Bấm nút Back hệ thống khi đang mở ô tìm kiếm sẽ đóng mượt ô tìm kiếm
    BackHandler(enabled = isSearchActive) {
        focusManager.clearFocus()
        keyboardController?.hide()
        onCloseSearch()
    }

    // Tự động focus và bật bàn phím mượt mà mỗi khi mở ô tìm kiếm; ẩn bàn phím khi đóng
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            try {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Khi người dùng cuộn danh sách mà chưa nhập từ khóa tìm kiếm -> tự động ẩn gọn ô tìm kiếm
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isSearchActive && searchQuery.isBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onCloseSearch()
        }
    }

    // Tự động phát hiện khi cuộn gần chạm đáy (còn 3 phần tử) để kích hoạt tải thêm ngầm
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && !isLoadingMore) {
            onLoadMore()
        }
    }

    // Tự động cuộn lên đầu danh sách khi chuyển danh mục
    LaunchedEffect(selectedCategory) {
        if (searchResults.isNotEmpty()) {
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Ô tìm kiếm hiện/ẩn mượt mà khi bấm kính lúp (không dùng mũi tên quay lại)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 260)
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 200)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Tìm kiếm truyện, audiobook, video…",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Xóa nội dung",
                                        tint = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    if (searchQuery.isBlank()) {
                                        onCloseSearch()
                                    } else {
                                        onSearch()
                                    }
                                }
                            ) {
                                QuantumSearchIcon(size = 20.dp)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (searchQuery.isBlank()) {
                                onCloseSearch()
                            } else {
                                onSearch()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentRed
                    ),
                    // Ô tìm kiếm lõm (inset) kiểu neumorphic lượng tử với đường viền mảnh tinh tế
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        .neumorphic(
                            shape = RoundedCornerShape(24.dp),
                            elevation = 3.dp,
                            pressed = true,
                            quantumGlow = true,
                            borderWidth = 0.65.dp
                        )
                )
            }
        }

        // 2. Dải chip danh mục neumorphic (La bàn 🧭 + Tất cả + Âm nhạc + ...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Nguyên tử Lượng tử / Khám phá
            NeuButton(
                onClick = { onCategorySelected("Tất cả") },
                shape = RoundedCornerShape(12.dp),
                elevation = 3.dp,
                modifier = Modifier.size(36.dp)
            ) {
                QuantumExploreIcon(size = 20.dp)
            }

            // Các thẻ phân loại: chip nổi, khi chọn thì lõm xuống + viền lăng kính lượng tử + chấm hạt Quark
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                NeuButton(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(14.dp),
                    elevation = 3.dp,
                    selected = isSelected,
                    quantumGlow = isSelected,
                    modifier = Modifier.height(35.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = category,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentRed else TextSecondary
                        )
                    }
                }
            }
        }

        // Thông báo lỗi nếu có
        if (!statusMessage.isNullOrBlank()) {
            Text(
                text = statusMessage,
                color = AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 3. Bảng tin danh sách thẻ video lớn (YouTube Video Cards)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                QuantumLoadingIndicator(
                    message = "Đang đồng bộ bước sóng lượng tử..."
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(searchResults, key = { it.id }) { song ->
                    YouTubeVideoCard(
                        song = song,
                        onClick = { onPlaySong(song, searchResults) }
                    )
                }

                // Loading spinner lượng tử khi đang tải thêm ở chân trang
                if (isLoadingMore) {
                    item(key = "loading_more_spinner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QuantumMiniLoader()
                        }
                    }
                }
            }
        }
    }
}
