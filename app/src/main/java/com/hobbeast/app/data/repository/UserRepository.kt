package com.hobbeast.app.data.repository

import com.hobbeast.app.data.model.*
import com.hobbeast.app.data.remote.SupabaseDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val supabase: SupabaseDataSource,
) {
    suspend fun getCurrentProfile(): Result<UserProfile> = runCatching {
        val uid = supabase.currentUserId() ?: throw Exception("Nem vagy bejelentkezve")
        supabase.getUserProfile(uid)
    }

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> = runCatching {
        supabase.updateProfile(profile)
    }

    suspend fun saveInterests(interests: List<String>): Result<Unit> = runCatching {
        val uid = supabase.currentUserId() ?: return@runCatching
        val existing = supabase.getUserProfile(uid)
        supabase.updateProfile(existing.copy(interests = interests))
    }

    suspend fun signOut() = supabase.signOut()
}
