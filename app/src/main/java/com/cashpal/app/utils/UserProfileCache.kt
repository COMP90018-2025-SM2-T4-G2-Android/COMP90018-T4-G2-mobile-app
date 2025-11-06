package com.cashpal.app.utils

import com.cashpal.app.models.User

object UserProfileCache {
    @Volatile
    private var cachedProfile: User? = null

    fun get(): User? = cachedProfile

    fun update(profile: User?) {
        cachedProfile = profile
    }

    fun clear() {
        cachedProfile = null
    }
}
