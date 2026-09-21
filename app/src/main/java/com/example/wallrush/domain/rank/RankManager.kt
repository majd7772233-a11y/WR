package com.example.wallrush.domain.rank

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.wallrush.data.local.PlayerProfile

@Keep
enum class RankTier(
    val tierId: String,
    val nameEn: String,
    val nameAr: String,
    val iconEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val minRating: Int,
    val maxRating: Int,
    val xpBonusPercent: Int,
    val perkTitleAr: String,
    val perkTitleEn: String
) {
    BRONZE_I("bronze_1", "Bronze I", "برونزي ١", "🥉", Color(0xFFCD7F32), Color(0xFF8C5320), Color(0xFFCD7F32), 0, 799, 0, "مبتدئ ساحات الاندفاع", "Novice Runner"),
    BRONZE_II("bronze_2", "Bronze II", "برونزي ٢", "🥉", Color(0xFFD98A3C), Color(0xFF995E26), Color(0xFFD98A3C), 800, 999, 5, "متعلم مسارات التكتيك", "Path Apprentice"),
    BRONZE_III("bronze_3", "Bronze III", "برونزي ٣", "🥉", Color(0xFFE59445), Color(0xFFA6682C), Color(0xFFE59445), 1000, 1199, 10, "عداء متمرس", "Sturdy Runner"),

    SILVER_I("silver_1", "Silver I", "فضي ١", "🥈", Color(0xFFC0C0C0), Color(0xFF7A8B99), Color(0xFFE2E8F0), 1200, 1299, 15, "مقاتل جدران معتمد", "Certified Wall Fighter"),
    SILVER_II("silver_2", "Silver II", "فضي ٢", "🥈", Color(0xFFD1D5DB), Color(0xFF8593A6), Color(0xFFF1F5F9), 1300, 1399, 20, "صائد الفرص", "Opportunity Hunter"),
    SILVER_III("silver_3", "Silver III", "فضي ٣", "🥈", Color(0xFFE5E7EB), Color(0xFF94A3B8), Color(0xFFFFFFFF), 1400, 1499, 25, "متفوق التكتيك الفضي", "Silver Tactician"),

    GOLD_I("gold_1", "Gold I", "ذهبي ١", "🥇", Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFFFBBF24), 1500, 1599, 30, "مهندس الممرات الذهبية", "Golden Corridor Crafter"),
    GOLD_II("gold_2", "Gold II", "ذهبي ٢", "🥇", Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFFCD34D), 1600, 1699, 35, "قاهر الحصار", "Siege Breaker"),
    GOLD_III("gold_3", "Gold III", "ذهبي ٣", "🥇", Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFFEF08A), 1700, 1799, 40, "فارس الساحات الذهبي", "Golden Arena Knight"),

    PLATINUM_I("platinum_1", "Platinum I", "بلاتيني ١", "💎", Color(0xFF06B6D4), Color(0xFF0891B2), Color(0xFF22D3EE), 1800, 1899, 45, "خبير المتاهات والسرعة", "Labyrinth Specialist"),
    PLATINUM_II("platinum_2", "Platinum II", "بلاتيني ٢", "💎", Color(0xFF00E5FF), Color(0xFF0284C7), Color(0xFF67E8F9), 1900, 1999, 50, "سيد التحركات الدقيقة", "Precision Master"),
    PLATINUM_III("platinum_3", "Platinum III", "بلاتيني ٣", "💎", Color(0xFF38BDF8), Color(0xFF0369A1), Color(0xFFA5F3FC), 2000, 2099, 55, "بلاتيني استراتيجي خارق", "Strategic Commander"),

    DIAMOND_I("diamond_1", "Diamond I", "ماسي ١", "🔮", Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFFA78BFA), 2100, 2199, 60, "قارئ العقول والمسارات", "Mind Reader Tactician"),
    DIAMOND_II("diamond_2", "Diamond II", "ماسي ٢", "🔮", Color(0xFFA855F7), Color(0xFF7E22CE), Color(0xFFC084FC), 2200, 2299, 65, "شبح الجدران الماسي", "Diamond Phantom"),
    DIAMOND_III("diamond_3", "Diamond III", "ماسي ٣", "🔮", Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFFE9D5FF), 2300, 2399, 70, "قاهر النخبة الماسية", "Diamond Overlord"),

    MASTER("master", "Master", "سيد الألعاب", "👑", Color(0xFFFF0055), Color(0xFF990033), Color(0xFFFF3377), 2400, 2599, 80, "أستاذ جدران النخبة", "Elite Grandmaster"),
    GRANDMASTER("grandmaster", "Grandmaster", "الأستاذ الأكبر", "🏆", Color(0xFFFFD700), Color(0xFFFF8800), Color(0xFFFFF066), 2600, 2899, 95, "أسطورة الشطرنج والحصار", "Supreme Grandmaster"),
    APEX_LEGEND("apex_legend", "Apex Legend", "الأسطورة الخارقة", "⚡", Color(0xFF00FFCC), Color(0xFF7928CA), Color(0xFF00FFFF), 2900, 99999, 120, "حاكم ساحات وول رش الأبدي", "Apex WallRush Mythic")
}

data class PlayerRankInfo(
    val tier: RankTier,
    val currentRating: Int,
    val progressInTier: Float,
    val pointsToNextTier: Int,
    val nextTier: RankTier?,
    val rankBadge: String,
    val title: String
)

object RankManager {

    fun getRankTier(rating: Int): RankTier {
        val clampedRating = rating.coerceAtLeast(0)
        return RankTier.values().lastOrNull { clampedRating >= it.minRating } ?: RankTier.BRONZE_I
    }

    fun getPlayerRankInfo(profile: PlayerProfile, isArabic: Boolean = false): PlayerRankInfo {
        val tier = getRankTier(profile.ratingScore)
        val allTiers = RankTier.values()
        val currentIndex = allTiers.indexOf(tier)
        val nextTier = if (currentIndex < allTiers.size - 1) allTiers[currentIndex + 1] else null

        val pointsInTier = (profile.ratingScore - tier.minRating).coerceAtLeast(0)
        val tierSpan = if (nextTier != null) (nextTier.minRating - tier.minRating).coerceAtLeast(1) else 500
        val progress = if (nextTier == null) 1.0f else (pointsInTier.toFloat() / tierSpan).coerceIn(0f, 1f)
        val pointsToNext = if (nextTier != null) (nextTier.minRating - profile.ratingScore).coerceAtLeast(0) else 0

        return PlayerRankInfo(
            tier = tier,
            currentRating = profile.ratingScore,
            progressInTier = progress,
            pointsToNextTier = pointsToNext,
            nextTier = nextTier,
            rankBadge = tier.iconEmoji,
            title = if (isArabic) tier.nameAr else tier.nameEn
        )
    }

    /**
     * Calculates rating points (RP) gained or lost after a match.
     */
    fun calculateRatingDelta(
        isWon: Boolean,
        playerRating: Int,
        opponentRating: Int,
        moveCount: Int,
        accuracyPercent: Int
    ): Int {
        val ratingDiff = (opponentRating - playerRating).coerceIn(-400, 400)
        val expectedScore = 1.0 / (1.0 + Math.pow(10.0, -ratingDiff / 400.0))
        val actualScore = if (isWon) 1.0 else 0.0

        val kFactor = when {
            playerRating < 1200 -> 36.0 // High acceleration for beginners
            playerRating < 1800 -> 28.0
            playerRating < 2400 -> 22.0
            else -> 16.0
        }

        var delta = (kFactor * (actualScore - expectedScore)).toInt()

        if (isWon) {
            // Quality victory bonuses
            val accuracyBonus = if (accuracyPercent >= 80) 4 else if (accuracyPercent >= 70) 2 else 0
            val speedBonus = if (moveCount in 1..14) 3 else 0
            delta = (delta + accuracyBonus + speedBonus).coerceAtLeast(8)
        } else {
            // Mitigate loss penalty for close valiant games
            if (accuracyPercent >= 75) delta = (delta + 3).coerceAtMost(-5)
            delta = delta.coerceAtMost(-4)
        }

        return delta
    }
}
