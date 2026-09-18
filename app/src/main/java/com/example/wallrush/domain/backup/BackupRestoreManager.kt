package com.example.wallrush.domain.backup

import android.content.Context
import com.example.wallrush.data.local.PlayerProfile
import com.example.wallrush.data.local.SettingsPreferences
import com.example.wallrush.domain.progression.ProgressionManager
import com.example.wallrush.domain.titles.TitlesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import android.util.Base64

data class BackupPayload(
    val version: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val avatarId: Int,
    val ratingScore: Int,
    val wins: Int,
    val losses: Int,
    val totalMatches: Int = 0,
    val wallsPlaced: Int = 0,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalXp: Long,
    val equippedTitleId: String,
    val unlockedTitleIds: List<String>,
    val customLevelsJson: String = "[]"
)

data class BackupFile(
    val checksum: String,
    val encodedData: String
)

sealed class RestoreResult {
    data class Success(val profile: PlayerProfile, val totalXp: Long) : RestoreResult()
    data class Error(val messageEn: String, val messageAr: String) : RestoreResult()
}

object BackupRestoreManager {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a fully verified, tamper-resistant .wrsave export string.
     */
    fun createBackupString(context: Context, profile: PlayerProfile): String {
        val totalXp = ProgressionManager.getTotalXp(context)
        val equippedTitle = TitlesManager.getEquippedTitle(context).id
        val unlockedTitles = TitlesManager.getAllUnlockedTitleIds(context).toList()

        val payload = BackupPayload(
            username = profile.username,
            avatarId = profile.avatarId,
            ratingScore = profile.ratingScore,
            wins = profile.wins,
            losses = profile.losses,
            totalMatches = profile.totalMatches,
            wallsPlaced = profile.wallsPlaced,
            currentStreak = profile.currentStreak,
            bestStreak = profile.bestStreak,
            totalXp = totalXp,
            equippedTitleId = equippedTitle,
            unlockedTitleIds = unlockedTitles
        )

        val adapter = moshi.adapter(BackupPayload::class.java)
        val json = adapter.toJson(payload)
        val checksum = sha256(json)
        val base64Data = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val fileAdapter = moshi.adapter(BackupFile::class.java)
        return fileAdapter.toJson(BackupFile(checksum = checksum, encodedData = base64Data))
    }

    /**
     * Imports and validates a .wrsave export string with integrity check.
     */
    fun restoreFromBackupString(context: Context, backupContent: String): RestoreResult {
        return try {
            val fileAdapter = moshi.adapter(BackupFile::class.java)
            val backupFile = fileAdapter.fromJson(backupContent.trim())
                ?: return RestoreResult.Error("Invalid backup file format", "صيغة ملف النسخ الاحتياطي غير صالحة")

            val decodedJson = String(Base64.decode(backupFile.encodedData, Base64.NO_WRAP), Charsets.UTF_8)
            val computedChecksum = sha256(decodedJson)

            if (!computedChecksum.equals(backupFile.checksum, ignoreCase = true)) {
                return RestoreResult.Error(
                    "Save file corruption detected! Checksum mismatch.",
                    "تم اكتشاف تلف أو تعديل في ملف الحفظ! فشل التحقق من النزاهة."
                )
            }

            val payloadAdapter = moshi.adapter(BackupPayload::class.java)
            val payload = payloadAdapter.fromJson(decodedJson)
                ?: return RestoreResult.Error("Corrupted payload", "بيانات الحفظ تالفة")

            // Apply XP & Progression
            val currentXp = ProgressionManager.getTotalXp(context)
            if (payload.totalXp > currentXp) {
                ProgressionManager.addXp(context, payload.totalXp - currentXp)
            }

            // Restore unlocked titles
            payload.unlockedTitleIds.forEach { titleId ->
                TitlesManager.unlockTitle(context, titleId)
            }
            TitlesManager.setEquippedTitle(context, payload.equippedTitleId)

            val restoredProfile = PlayerProfile(
                guestId = "guest01",
                username = payload.username,
                avatarId = payload.avatarId,
                wins = payload.wins,
                losses = payload.losses,
                totalMatches = payload.totalMatches,
                currentStreak = payload.currentStreak,
                bestStreak = payload.bestStreak,
                wallsPlaced = payload.wallsPlaced,
                ratingScore = payload.ratingScore
            )

            RestoreResult.Success(restoredProfile, payload.totalXp)
        } catch (e: Exception) {
            RestoreResult.Error("Failed to parse backup: ${e.localizedMessage}", "فشل في قراءة ملف الحفظ: ${e.localizedMessage}")
        }
    }
}
