package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.DeveloperProfile
import com.example.model.MaxHubPost
import com.example.ui.viewmodel.MaxHubViewModel
import kotlinx.coroutines.launch
import java.io.File

val APP_CATEGORIES = listOf(
    "All",
    "Tools",
    "Games",
    "Editing & Video",
    "UI Design",
    "Utilities",
    "Social",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxHubScreen(
    viewModel: MaxHubViewModel,
    onOpenDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val showProfileSetup by viewModel.showProfileSetupDialog.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var currentTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = All apps, 2 = News, 3 = Categories
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var selectedPostDetailId by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<MaxHubPost?>(null) }
    var showLoginPage by remember { mutableStateOf(false) }
    var showCreatePostPage by remember { mutableStateOf(false) }
    var viewingProfileUser by remember { mutableStateOf<DeveloperProfile?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            showLoginPage = true
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            viewModel.clearActionMessage()
        }
    }

    // Full Screen Routing
    if (showLoginPage) {
        LoginScreen(
            viewModel = viewModel,
            onBack = { showLoginPage = false },
            onLoggedIn = { showLoginPage = false },
        )
        return
    }

    viewingProfileUser?.let { dev ->
        DeveloperProfileScreen(
            developer = dev,
            posts = posts,
            currentUserId = currentUser?.uid,
            onBack = { viewingProfileUser = null },
            onSelectPost = { selectedPostDetailId = it.id },
            onDeletePost = { deleteCandidate = it },
        )
        return
    }

    selectedPostDetailId?.let { postId ->
        val post = posts.firstOrNull { it.id == postId }
        if (post != null) {
            PlayStoreStyleDetailScreen(
                post = post,
                currentUserId = currentUser?.uid,
                onBack = { selectedPostDetailId = null },
                onLike = { viewModel.toggleLike(post.id) },
                onOpenComments = { activeCommentPostId = post.id },
                onDelete = {
                    deleteCandidate = post
                    selectedPostDetailId = null
                },
                onDevClick = {
                    selectedPostDetailId = null
                    viewingProfileUser = DeveloperProfile(
                        uid = post.developerId,
                        name = post.developerName,
                        profileLogoUrl = post.developerLogo,
                        whatsapp = post.whatsapp,
                        telegram = post.telegram,
                    )
                },
                onWhatsApp = { num ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${num.replace(Regex("[^0-9]"), "")}")))
                    }
                },
                onTelegram = { tg ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${tg.removePrefix("@")}")))
                    }
                },
            )
            return
        }
    }

    activeCommentPostId?.let { postId ->
        val post = posts.firstOrNull { it.id == postId }
        if (post != null) {
            CommentsScreen(
                post = post,
                currentUser = currentUser,
                onBack = { activeCommentPostId = null },
                onAddComment = { text -> viewModel.addComment(postId, text) },
                onRequireAuth = {
                    activeCommentPostId = null
                    showLoginPage = true
                },
            )
            return
        }
    }

    if (showCreatePostPage) {
        CreatePostScreen(
            currentUser = currentUser,
            onBack = { showCreatePostPage = false },
            onSubmit = { title, desc, price, cat, wa, tg, logoUri, uris ->
                viewModel.createPost(title, desc, price, cat, wa, tg, logoUri, uris)
                showCreatePostPage = false
            },
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MaxHub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Community Store & Projects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentTab == 0 && searchQuery.isBlank(),
                        onClick = {
                            currentTab = 0
                            searchQuery = ""
                            selectedCategoryFilter = "All"
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                        label = { Text("All apps") },
                        selected = currentTab == 1 && searchQuery.isBlank(),
                        onClick = {
                            currentTab = 1
                            searchQuery = ""
                            selectedCategoryFilter = "All"
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Newspaper, contentDescription = null) },
                        label = { Text("News") },
                        selected = currentTab == 2,
                        onClick = {
                            currentTab = 2
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                        label = { Text("Categories") },
                        selected = currentTab == 3,
                        onClick = {
                            currentTab = 3
                            scope.launch { drawerState.close() }
                        },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Upload Project") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.requireAuth { showCreatePostPage = true }
                        },
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Image, contentDescription = null) },
                        label = { Text(if (currentUser != null) "My Profile (${currentUser!!.name})" else "Sign In") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentUser == null) {
                                showLoginPage = true
                            } else {
                                viewingProfileUser = currentUser
                            }
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.requireAuth {
                            showCreatePostPage = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Upload Project", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("maxhub_post_project_fab"),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.height(6.dp))

                // TOP BAR: 3 lines menu icon + MaxHub Title + Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu 3 lines", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "MaxHub",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(onClick = {
                        if (currentUser == null) {
                            showLoginPage = true
                        } else {
                            viewModel.showProfileSetupDialog.value = true
                        }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (currentUser?.profileLogoUrl?.isNotBlank() == true && File(currentUser!!.profileLogoUrl).exists()) {
                                AsyncImage(
                                    model = File(currentUser!!.profileLogoUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    currentUser?.name?.take(1)?.uppercase() ?: "U",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                // Search Bar with clean clear/search action
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search projects, developers...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )

                // Quick Navigation Chips (Top Header Tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = currentTab == 0 && searchQuery.isBlank(),
                        onClick = { currentTab = 0; searchQuery = "" },
                        label = { Text("Home") },
                    )
                    FilterChip(
                        selected = currentTab == 1 && searchQuery.isBlank(),
                        onClick = { currentTab = 1; searchQuery = "" },
                        label = { Text("All apps") },
                    )
                    FilterChip(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        label = { Text("News") },
                    )
                    FilterChip(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        label = { Text("Categories") },
                    )
                }

                // Search Results Screen
                if (searchQuery.isNotBlank()) {
                    val searchResults = posts.filter { p ->
                        p.title.contains(searchQuery, true) ||
                        p.description.contains(searchQuery, true) ||
                        p.developerName.contains(searchQuery, true) ||
                        p.category.contains(searchQuery, true)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Search Results For: ${searchQuery.trim()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        if (searchResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No projects found matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(bottom = 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(searchResults, key = { it.id }) { post ->
                                    MaxHubGridAppItem(
                                        post = post,
                                        onClick = { selectedPostDetailId = post.id },
                                    )
                                }
                            }
                        }
                    }
                    return@Scaffold
                }

                // Regular Tabs
                val filteredPosts = if (selectedCategoryFilter == "All") {
                    posts
                } else {
                    posts.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                }

                when (currentTab) {
                    0 -> {
                        // Home Screen with Sections
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (posts.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("No projects in MaxHub yet!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Tap 'Upload Project' below to share your first app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            } else {
                                item {
                                    MaxHubSectionRow(
                                        title = "New & Updated",
                                        posts = filteredPosts,
                                        onSelectPost = { selectedPostDetailId = it.id },
                                        onSeeMore = { currentTab = 1 },
                                    )
                                }
                                item {
                                    MaxHubSectionRow(
                                        title = "Editor's choice",
                                        posts = filteredPosts.filter { it.likesCount > 0 }.ifEmpty { posts.take(3) },
                                        onSelectPost = { selectedPostDetailId = it.id },
                                        onSeeMore = { currentTab = 1 },
                                    )
                                }
                                item {
                                    MaxHubSectionRow(
                                        title = "Trending",
                                        posts = filteredPosts.sortedByDescending { it.likesCount },
                                        onSelectPost = { selectedPostDetailId = it.id },
                                        onSeeMore = { currentTab = 1 },
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // All Apps
                        if (filteredPosts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No apps available in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(filteredPosts, key = { it.id }) { post ->
                                    MaxHubGridAppItem(
                                        post = post,
                                        onClick = { selectedPostDetailId = post.id },
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // News
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Filled.Newspaper, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(10.dp))
                                Text("MaxHub Community News", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Stay tuned for latest Android releases, MAX Studio updates, and developer announcements!", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        // Categories List
                        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { Text("Browse by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            items(APP_CATEGORIES.filter { it != "All" }) { cat ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = {
                                        selectedCategoryFilter = cat
                                        currentTab = 1
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cat, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            val count = posts.count { it.category.equals(cat, ignoreCase = true) }
                                            Text("$count projects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileSetup) {
        ProfileSetupDialog(
            profile = currentUser,
            onDismiss = { viewModel.showProfileSetupDialog.value = false },
            onSave = { name, bio, wa, tg, logo ->
                viewModel.updateProfile(name, bio, wa, tg, logo)
            },
            onLogout = {
                viewModel.logout()
                viewModel.showProfileSetupDialog.value = false
            },
        )
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePost(target.id)
                    deleteCandidate = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            },
        )
    }
}

// 3-Column Grid App Item
@Composable
fun MaxHubGridAppItem(
    post: MaxHubPost,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val logoFile = File(post.logoUrl)
            val firstScreen = post.screenshots.firstOrNull()?.let { File(it) }

            if (post.logoUrl.isNotBlank() && logoFile.exists()) {
                AsyncImage(model = logoFile, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (firstScreen != null && firstScreen.exists()) {
                AsyncImage(model = firstScreen, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        post.title.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Text(
            text = post.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("♡ ${post.likesCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("💬 ${post.commentsCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MaxHubSectionRow(
    title: String,
    posts: List<MaxHubPost>,
    onSelectPost: (MaxHubPost) -> Unit,
    onSeeMore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = onSeeMore, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp), modifier = Modifier.height(32.dp)) {
                Text("See More", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (posts.isEmpty()) {
            Text("No projects available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(posts, key = { it.id }) { post ->
                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onSelectPost(post) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            val logoFile = File(post.logoUrl)
                            val firstScreen = post.screenshots.firstOrNull()?.let { File(it) }

                            if (post.logoUrl.isNotBlank() && logoFile.exists()) {
                                AsyncImage(model = logoFile, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else if (firstScreen != null && firstScreen.exists()) {
                                AsyncImage(model = firstScreen, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        post.title.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Text(
                            post.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("♡ ${post.likesCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("💬 ${post.commentsCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// Play Store Style Detail Screen
@Composable
fun PlayStoreStyleDetailScreen(
    post: MaxHubPost,
    currentUserId: String?,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onOpenComments: () -> Unit,
    onDelete: () -> Unit,
    onDevClick: () -> Unit,
    onWhatsApp: (String) -> Unit,
    onTelegram: (String) -> Unit,
) {
    val isLiked = currentUserId != null && currentUserId in post.likedByUsers
    val isAuthor = currentUserId != null && currentUserId == post.developerId

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                if (isAuthor) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Top Header: App Logo + Title + Developer + Category
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val logoFile = File(post.logoUrl)
                    val firstScreen = post.screenshots.firstOrNull()?.let { File(it) }

                    if (post.logoUrl.isNotBlank() && logoFile.exists()) {
                        AsyncImage(model = logoFile, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (firstScreen != null && firstScreen.exists()) {
                        AsyncImage(model = firstScreen, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(post.title.take(2).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("By ${post.developerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onDevClick))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(post.category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(post.price, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Stats Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️ ${post.likesCount}", fontWeight = FontWeight.Bold)
                        Text("Likes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬 ${post.commentsCount}", fontWeight = FontWeight.Bold)
                        Text("Comments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("About Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(post.description, style = MaterialTheme.typography.bodyMedium)

            if (post.screenshots.isNotEmpty()) {
                Text("Screenshots (${post.screenshots.size}/10):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    post.screenshots.forEach { path ->
                        if (File(path).exists()) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(180.dp, 320.dp).clip(RoundedCornerShape(14.dp)),
                            )
                        }
                    }
                }
            }

            if (post.whatsapp.isNotBlank()) {
                OutlinedButton(onClick = { onWhatsApp(post.whatsapp) }, modifier = Modifier.fillMaxWidth()) {
                    Text("WhatsApp: ${post.whatsapp}")
                }
            }
            if (post.telegram.isNotBlank()) {
                OutlinedButton(onClick = { onTelegram(post.telegram) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Telegram: ${post.telegram}")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onLike, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.Red else Color.White,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isLiked) "Liked (${post.likesCount})" else "Like (${post.likesCount})")
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = onOpenComments, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Comments (${post.commentsCount})")
                }
            }
        }
    }
}

// Developer Profile Screen
@Composable
fun DeveloperProfileScreen(
    developer: DeveloperProfile,
    posts: List<MaxHubPost>,
    currentUserId: String?,
    onBack: () -> Unit,
    onSelectPost: (MaxHubPost) -> Unit,
    onDeletePost: (MaxHubPost) -> Unit,
) {
    val devPosts = posts.filter { it.developerId == developer.uid }
    val totalLikes = devPosts.sumOf { it.likesCount }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("MaxHub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Developer Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A8A)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (developer.profileLogoUrl.isNotBlank() && File(developer.profileLogoUrl).exists()) {
                        AsyncImage(model = File(developer.profileLogoUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(
                            developer.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(developer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text("User", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                    }
                }
            }

            // Stats Card
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Projects", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("${devPosts.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Likes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("$totalLikes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // About Section
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(developer.bio.ifBlank { "." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Projects Section Header
            Text("Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (devPosts.isEmpty()) {
                Text("No projects uploaded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    devPosts.chunked(3).forEach { rowPosts ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowPosts.forEach { post ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MaxHubGridAppItem(
                                        post = post,
                                        onClick = { onSelectPost(post) },
                                    )
                                }
                            }
                            repeat(3 - rowPosts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "©2026 MaxHub | User Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Comments Screen
@Composable
fun CommentsScreen(
    post: MaxHubPost,
    currentUser: DeveloperProfile?,
    onBack: () -> Unit,
    onAddComment: (String) -> Unit,
    onRequireAuth: () -> Unit,
) {
    var commentText by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Comments (${post.comments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (post.comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No comments yet. Be the first to comment!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    post.comments.forEach { c ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(c.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Write a comment...") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    if (currentUser == null) {
                        onRequireAuth()
                    } else if (commentText.isNotBlank()) {
                        onAddComment(commentText.trim())
                        commentText = ""
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                }
            }
        }
    }
}

// Create Project Post Screen with Gallery Screenshot & Logo Pickers and Cleaned Categories
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    currentUser: DeveloperProfile?,
    onBack: () -> Unit,
    onSubmit: (String, String, String, String, String, String, Uri?, List<Uri>) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("Free") }
    var selectedCategory by remember { mutableStateOf("Tools") }
    var whatsapp by remember { mutableStateOf(currentUser?.whatsapp ?: "") }
    var telegram by remember { mutableStateOf(currentUser?.telegram ?: "") }

    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    val screenshotUris = remember { mutableStateListOf<Uri>() }

    // Logo Picker from Gallery
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLogoUri = uri
        }
    }

    // Screenshots Multi-Picker from Gallery (up to 10)
    val screenshotsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val availableSlots = 10 - screenshotUris.size
            screenshotUris.addAll(uris.take(availableSlots))
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Upload Project to MaxHub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Text(
                "Upload your project with app icon, up to 10 screenshots, description, and contact info.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // App Logo Upload Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { logoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selectedLogoUri != null) {
                            AsyncImage(
                                model = selectedLogoUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Project App Icon / Logo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Choose an image from gallery to display on store card", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(onClick = { logoPickerLauncher.launch("image/*") }) {
                            Text(if (selectedLogoUri != null) "Change Logo" else "Select Logo from Gallery")
                        }
                    }
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title *") }, modifier = Modifier.fillMaxWidth())

            // Category Selection Section
            Text("Select Category *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                APP_CATEGORIES.filter { it != "All" }.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                    )
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description & Features *") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (e.g. Free or $5)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp Number (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = telegram, onValueChange = { telegram = it }, label = { Text("Telegram Username (Optional)") }, modifier = Modifier.fillMaxWidth())

            // Screenshots Gallery Picker Section (Up to 10)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Project Screenshots (${screenshotUris.size}/10)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { screenshotsPickerLauncher.launch("image/*") },
                            enabled = screenshotUris.size < 10,
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Gallery")
                        }
                    }

                    if (screenshotUris.isEmpty()) {
                        Text("No screenshots selected. Tap 'Open Gallery' to pick up to 10 screenshots.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(screenshotUris) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp, 160.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    IconButton(
                                        onClick = { screenshotUris.remove(uri) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSubmit(
                            title.trim(),
                            description.trim(),
                            price.trim(),
                            selectedCategory,
                            whatsapp.trim(),
                            telegram.trim(),
                            selectedLogoUri,
                            screenshotUris.toList(),
                        )
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Publish Project to MaxHub", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Profile Setup Dialog
@Composable
fun ProfileSetupDialog(
    profile: DeveloperProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    onLogout: () -> Unit,
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var whatsapp by remember { mutableStateOf(profile?.whatsapp ?: "") }
    var telegram by remember { mutableStateOf(profile?.telegram ?: "") }
    var logoUrl by remember { mutableStateOf(profile?.profileLogoUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Developer Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp Number (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telegram, onValueChange = { telegram = it }, label = { Text("Telegram Username (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("Profile Logo Image Path (Optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onLogout()
                    onDismiss()
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    onSave(name, bio, whatsapp, telegram, logoUrl)
                    onDismiss()
                }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
