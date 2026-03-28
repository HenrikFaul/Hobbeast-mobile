package com.hobbeast.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hobbeast.app.data.model.*

private val gson = Gson()

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    location = location,
    address = address,
    latitude = latitude,
    longitude = longitude,
    organizerId = organizerId,
    organizerName = organizerName,
    imageUrl = imageUrl,
    category = category,
    tags = gson.toJson(tags),
    maxCapacity = maxCapacity,
    attendeeCount = attendeeCount,
    isPrivate = isPrivate,
    isFree = isFree,
    price = price,
    source = source.name.lowercase(),
    externalId = externalId,
    externalUrl = externalUrl,
    isTrending = isTrending,
    isFeatured = isFeatured,
    isEarlyAccess = isEarlyAccess,
    participationState = participationState.name.lowercase(),
    reminderSet = reminderSet,
    communityPulseScore = communityPulseScore,
)

fun EventEntity.toModel(): Event = Event(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    location = location,
    address = address,
    latitude = latitude,
    longitude = longitude,
    organizerId = organizerId,
    organizerName = organizerName,
    imageUrl = imageUrl,
    category = category,
    tags = runCatching {
        gson.fromJson<List<String>>(tags, object : TypeToken<List<String>>() {}.type)
    }.getOrDefault(emptyList()),
    maxCapacity = maxCapacity,
    attendeeCount = attendeeCount,
    isPrivate = isPrivate,
    isFree = isFree,
    price = price,
    source = EventSource.values().firstOrNull { it.name.lowercase() == source } ?: EventSource.HOBBEAST,
    externalId = externalId,
    externalUrl = externalUrl,
    isTrending = isTrending,
    isFeatured = isFeatured,
    isEarlyAccess = isEarlyAccess,
    participationState = ParticipationState.values().firstOrNull { it.name.lowercase() == participationState }
        ?: ParticipationState.NONE,
    reminderSet = reminderSet,
    communityPulseScore = communityPulseScore,
)

fun UserProfile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    email = email,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    interests = gson.toJson(interests),
    location = location,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    locationSharing = locationSharing,
    isOrganizer = isOrganizer,
    profileVisibility = profileVisibility.name.lowercase(),
)

fun ProfileEntity.toModel(): UserProfile = UserProfile(
    id = id,
    email = email,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    interests = runCatching {
        gson.fromJson<List<String>>(interests, object : TypeToken<List<String>>() {}.type)
    }.getOrDefault(emptyList()),
    location = location,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    locationSharing = locationSharing,
    isOrganizer = isOrganizer,
    profileVisibility = ProfileVisibility.values().firstOrNull { it.name.lowercase() == profileVisibility }
        ?: ProfileVisibility.PUBLIC,
)
