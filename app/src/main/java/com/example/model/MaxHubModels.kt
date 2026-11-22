package com.example.model

import java.util.UUID

data class DeveloperProfile(
    val uid: String = UUID.randomUUID().toString().take(8),
    val email: String = "",
    val name: String = "Developer",
    val profileLogoUrl: String = "",
    val bio: String = "Android & Sketchware Developer",
    val whatsapp: String = "",
    val telegram: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "email" to email,
        "name" to name,
        "profileLogoUrl" to profileLogoUrl,
        "bio" to bio,
        "whatsapp" to whatsapp,
        "telegram" to telegram,
    )

    companion object {
        fun fromMap(map: Map<*, *>): DeveloperProfile = DeveloperProfile(
            uid = (map["uid"] as? String) ?: UUID.randomUUID().toString().take(8),
            email = (map["email"] as? String) ?: "",
            name = (map["name"] as? String) ?: "Developer",
            profileLogoUrl = (map["profileLogoUrl"] as? String) ?: "",
            bio = (map["bio"] as? String) ?: "Android Developer",
            whatsapp = (map["whatsapp"] as? String) ?: "",
            telegram = (map["telegram"] as? String) ?: "",
        )
    }
}

data class MaxHubComment(
    val id: String = UUID.randomUUID().toString().take(8),
    val postId: String,
    val authorName: String,
    val authorLogo: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "postId" to postId,
        "authorName" to authorName,
        "authorLogo" to authorLogo,
        "text" to text,
        "timestampMs" to timestampMs,
    )

    companion object {
        fun fromMap(map: Map<*, *>): MaxHubComment = MaxHubComment(
            id = (map["id"] as? String) ?: UUID.randomUUID().toString().take(8),
            postId = (map["postId"] as? String) ?: "",
            authorName = (map["authorName"] as? String) ?: "User",
            authorLogo = (map["authorLogo"] as? String) ?: "",
            text = (map["text"] as? String) ?: "",
            timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        )
    }
}

data class MaxHubPost(
    val id: String = UUID.randomUUID().toString().take(8),
    val developerId: String,
    val developerName: String,
    val developerLogo: String,
    val whatsapp: String = "",
    val telegram: String = "",
    val title: String,
    val description: String,
    val price: String = "Free",
    val category: String = "Tools",
    val logoUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val likesCount: Int = 0,
    val likedByUsers: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val comments: List<MaxHubComment> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "developerId" to developerId,
        "developerName" to developerName,
        "developerLogo" to developerLogo,
        "whatsapp" to whatsapp,
        "telegram" to telegram,
        "title" to title,
        "description" to description,
        "price" to price,
        "category" to category,
        "logoUrl" to logoUrl,
        "screenshots" to screenshots,
        "likesCount" to likesCount,
        "likedByUsers" to likedByUsers,
        "commentsCount" to comments.size,
        "comments" to comments.map { it.toMap() },
        "createdAtMs" to createdAtMs,
    )

    companion object {
        fun fromMap(map: Map<*, *>): MaxHubPost {
            val rawComments = (map["comments"] as? List<*>)?.mapNotNull { it as? Map<*, *> }?.map { MaxHubComment.fromMap(it) } ?: emptyList()
            val rawScreenshots = (map["screenshots"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rawLiked = (map["likedByUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val id = (map["id"] as? String) ?: UUID.randomUUID().toString().take(8)
            return MaxHubPost(
                id = id,
                developerId = (map["developerId"] as? String) ?: "dev_1",
                developerName = (map["developerName"] as? String) ?: "Developer",
                developerLogo = (map["developerLogo"] as? String) ?: "",
                whatsapp = (map["whatsapp"] as? String) ?: "",
                telegram = (map["telegram"] as? String) ?: "",
                title = (map["title"] as? String) ?: "Awesome App",
                description = (map["description"] as? String) ?: "",
                price = (map["price"] as? String) ?: "Free",
                category = (map["category"] as? String) ?: "Tools",
                logoUrl = (map["logoUrl"] as? String) ?: "",
                screenshots = rawScreenshots,
                likesCount = (map["likesCount"] as? Number)?.toInt() ?: rawLiked.size,
                likedByUsers = rawLiked,
                commentsCount = (map["commentsCount"] as? Number)?.toInt() ?: rawComments.size,
                comments = rawComments,
                createdAtMs = (map["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            )
        }
    }
}
