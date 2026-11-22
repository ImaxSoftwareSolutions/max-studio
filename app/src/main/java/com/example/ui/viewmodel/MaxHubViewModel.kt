package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DeveloperProfile
import com.example.model.MaxHubComment
import com.example.model.MaxHubPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MaxHubViewModel(private val context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val hubDir: File = File(File(appContext.filesDir, ".maxstudio"), "MaxHub")

    private val _posts = MutableStateFlow<List<MaxHubPost>>(emptyList())
    val posts: StateFlow<List<MaxHubPost>> = _posts.asStateFlow()

    private val _currentUser = MutableStateFlow<DeveloperProfile?>(null)
    val currentUser: StateFlow<DeveloperProfile?> = _currentUser.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _newPostNotification = MutableStateFlow<MaxHubPost?>(null)
    val newPostNotification: StateFlow<MaxHubPost?> = _newPostNotification.asStateFlow()

    var showAuthDialog = MutableStateFlow(false)
    var showProfileSetupDialog = MutableStateFlow(false)
    var showCreatePostSheet = MutableStateFlow(false)
    var pendingActionAfterAuth: (() -> Unit)? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!hubDir.exists()) hubDir.mkdirs()

                // Load Profile
                val profFile = File(hubDir, "profile.json")
                if (profFile.exists()) {
                    val obj = JSONObject(profFile.readText())
                    _currentUser.value = DeveloperProfile.fromMap(jsonToMap(obj))
                } else {
                    _currentUser.value = null
                }

                // Load Posts (clearing existing uploaded projects as requested)
                val postsFile = File(hubDir, "posts.json")
                if (postsFile.exists()) {
                    postsFile.delete()
                }
                _posts.value = emptyList()
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearNewPostNotification() {
        _newPostNotification.value = null
    }

    fun requireAuth(action: () -> Unit) {
        if (_currentUser.value != null) {
            action()
        } else {
            pendingActionAfterAuth = action
            showAuthDialog.value = true
        }
    }

    fun performGoogleSignIn(email: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = DeveloperProfile(
                uid = "user_${System.currentTimeMillis()}",
                email = email.ifBlank { "user@gmail.com" },
                name = name.ifBlank { "Android Developer" },
                bio = "Sketchware & Android App Developer",
            )
            _currentUser.value = prof
            saveProfileDisk(prof)
            withContext(Dispatchers.Main) {
                showAuthDialog.value = false
                showProfileSetupDialog.value = true
                _actionMessage.value = "Google verified! Complete your profile details."
                pendingActionAfterAuth?.invoke()
                pendingActionAfterAuth = null
            }
        }
    }

    fun updateProfile(name: String, bio: String, whatsapp: String, telegram: String, logoUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentUser.value ?: DeveloperProfile()
            var savedLogo = current.profileLogoUrl
            if (!logoUri.isNullOrBlank()) {
                runCatching {
                    val resDir = File(appContext.filesDir, ".maxstudio/resources")
                    if (!resDir.exists()) resDir.mkdirs()
                    val dest = File(resDir, "profile_${System.currentTimeMillis()}.png")
                    appContext.contentResolver.openInputStream(Uri.parse(logoUri))?.use { input ->
                        FileOutputStream(dest).use { output -> input.copyTo(output) }
                    }
                    savedLogo = dest.absolutePath
                }
            }
            val updated = current.copy(
                name = name.ifBlank { current.name },
                bio = bio,
                whatsapp = whatsapp,
                telegram = telegram,
                profileLogoUrl = savedLogo,
            )
            _currentUser.value = updated
            saveProfileDisk(updated)
            withContext(Dispatchers.Main) {
                showProfileSetupDialog.value = false
                _actionMessage.value = "Profile saved successfully!"
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentUser.value = null
            File(hubDir, "profile.json").delete()
            withContext(Dispatchers.Main) {
                _actionMessage.value = "Logged out successfully"
            }
        }
    }

    fun createPost(
        title: String,
        description: String,
        price: String,
        category: String,
        whatsapp: String,
        telegram: String,
        logoUri: Uri?,
        imageUris: List<Uri>,
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val resDir = File(appContext.filesDir, ".maxstudio/resources")
            if (!resDir.exists()) resDir.mkdirs()

            var savedLogoPath = ""
            if (logoUri != null) {
                runCatching {
                    val destLogo = File(resDir, "logo_${System.currentTimeMillis()}.png")
                    appContext.contentResolver.openInputStream(logoUri)?.use { input ->
                        FileOutputStream(destLogo).use { output -> input.copyTo(output) }
                    }
                    savedLogoPath = destLogo.absolutePath
                }
            }

            val savedImagePaths = mutableListOf<String>()
            // Support up to 10 screenshots
            imageUris.take(10).forEach { uri ->
                runCatching {
                    val dest = File(resDir, "hub_${System.currentTimeMillis()}_${savedImagePaths.size}.png")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dest).use { output -> input.copyTo(output) }
                    }
                    savedImagePaths.add(dest.absolutePath)
                }
            }

            val newPost = MaxHubPost(
                developerId = user.uid,
                developerName = user.name,
                developerLogo = user.profileLogoUrl,
                whatsapp = whatsapp.ifBlank { user.whatsapp },
                telegram = telegram.ifBlank { user.telegram },
                title = title.ifBlank { "Untitled App" },
                description = description,
                price = price.ifBlank { "Free" },
                category = category.ifBlank { "Tools" },
                logoUrl = savedLogoPath,
                screenshots = savedImagePaths,
            )

            val updatedList = listOf(newPost) + _posts.value
            _posts.value = updatedList
            savePostsDisk(updatedList)

            withContext(Dispatchers.Main) {
                showCreatePostSheet.value = false
                _newPostNotification.value = newPost
                _actionMessage.value = "Project posted successfully to MaxHub!"
            }
        }
    }

    fun deletePost(postId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val target = _posts.value.firstOrNull { it.id == postId }
            if (target != null && target.developerId == user.uid) {
                val updated = _posts.value.filterNot { it.id == postId }
                _posts.value = updated
                savePostsDisk(updated)
                withContext(Dispatchers.Main) {
                    _actionMessage.value = "Project deleted successfully"
                }
            } else {
                withContext(Dispatchers.Main) {
                    _actionMessage.value = "You can only delete your own projects!"
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = _currentUser.value
        if (user == null) {
            requireAuth { toggleLike(postId) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _posts.value.map { p ->
                if (p.id == postId) {
                    val alreadyLiked = user.uid in p.likedByUsers
                    val newLikedList = if (alreadyLiked) p.likedByUsers - user.uid else p.likedByUsers + user.uid
                    p.copy(
                        likedByUsers = newLikedList,
                        likesCount = newLikedList.size,
                    )
                } else p
            }
            _posts.value = updated
            savePostsDisk(updated)
        }
    }




    fun addComment(postId: String, text: String) {
        val user = _currentUser.value
        if (user == null) {
            requireAuth { addComment(postId, text) }
            return
        }
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val comment = MaxHubComment(
                postId = postId,
                authorName = user.name,
                authorLogo = user.profileLogoUrl,
                text = text.trim(),
            )
            val updated = _posts.value.map { p ->
                if (p.id == postId) {
                    val newComments = p.comments + comment
                    p.copy(
                        comments = newComments,
                        commentsCount = newComments.size,
                    )
                } else p
            }
            _posts.value = updated
            savePostsDisk(updated)
        }
    }

    private fun saveProfileDisk(prof: DeveloperProfile) {
        runCatching {
            if (!hubDir.exists()) hubDir.mkdirs()
            File(hubDir, "profile.json").writeText(mapToJson(prof.toMap()).toString(2))
        }
    }

    private fun savePostsDisk(list: List<MaxHubPost>) {
        runCatching {
            if (!hubDir.exists()) hubDir.mkdirs()
            val arr = JSONArray()
            list.forEach { arr.put(mapToJson(it.toMap())) }
            File(hubDir, "posts.json").writeText(arr.toString(2))
        }
    }

    private fun mapToJson(map: Map<*, *>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            val key = k?.toString() ?: return@forEach
            obj.put(key, anyToJsonValue(v))
        }
        return obj
    }

    private fun anyToJsonValue(value: Any?): Any = when (value) {
        is Map<*, *> -> mapToJson(value)
        is List<*> -> JSONArray().apply { value.forEach { put(anyToJsonValue(it)) } }
        null -> JSONObject.NULL
        else -> value
    }

    private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonValueToAny(obj.opt(key))
        }
        return map
    }

    private fun jsonValueToAny(value: Any?): Any? = when (value) {
        is JSONObject -> jsonToMap(value)
        is JSONArray -> buildList { for (i in 0 until value.length()) add(jsonValueToAny(value.opt(i))) }
        JSONObject.NULL -> null
        else -> value
    }
}
