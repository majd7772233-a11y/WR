package com.example.wallrush.domain.titles

import android.content.Context
import android.content.SharedPreferences

data class PlayerTitle(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val icon: String,
    val requiredLevel: Int = 1,
    val unlockHintEn: String,
    val unlockHintAr: String
)

object TitlesManager {

    private const val PREFS_NAME = "wallrush_titles_prefs"
    private const val KEY_EQUIPPED_TITLE = "equipped_title_id"
    private const val KEY_UNLOCKED_TITLES = "unlocked_titles_set"

    val ALL_TITLES = listOf(
        PlayerTitle(
            id = "title_novice",
            nameEn = "Wall Runner",
            nameAr = "عدّاء الجدران",
            descriptionEn = "Starting title awarded to all runners",
            descriptionAr = "اللقب الأولي الممنوح لجميع العدّائين",
            icon = "🏃",
            requiredLevel = 1,
            unlockHintEn = "Default title",
            unlockHintAr = "اللقب الافتراضي"
        ),
        PlayerTitle(
            id = "title_speed_demon",
            nameEn = "Speed Demon",
            nameAr = "شيطان السرعة",
            descriptionEn = "Awarded for lightning-fast victories",
            descriptionAr = "يُمنح للانتصارات الخاطفة السريعة",
            icon = "⚡",
            requiredLevel = 5,
            unlockHintEn = "Reach Level 5 or win in under 60s",
            unlockHintAr = "الوصول للمستوى 5 أو الفوز بأقل من 60 ثانية"
        ),
        PlayerTitle(
            id = "title_untouchable",
            nameEn = "The Untouchable",
            nameAr = "المحصّن المنيع",
            descriptionEn = "Achieved flawless dodging without taking a detour",
            descriptionAr = "تحقيق مراوغة كاملة دون أي التفاف زائد",
            icon = "🛡️",
            requiredLevel = 10,
            unlockHintEn = "Reach Level 10 or achieve Perfect Run",
            unlockHintAr = "الوصول للمستوى 10 أو إنجاز جولة كاملة مثالية"
        ),
        PlayerTitle(
            id = "title_wall_breaker",
            nameEn = "Wall Breaker",
            nameAr = "كاسر الجدران",
            descriptionEn = "Demolished walls and shattered barriers",
            descriptionAr = "تدمير الجدران وتجاوز أصعب الحواجز",
            icon = "💥",
            requiredLevel = 15,
            unlockHintEn = "Reach Level 15 or break 5 walls",
            unlockHintAr = "الوصول للمستوى 15 أو تدمير 5 جدران"
        ),
        PlayerTitle(
            id = "title_combo_master",
            nameEn = "Combo King",
            nameAr = "ملك الكومبو",
            descriptionEn = "Chain consecutive high-tier dodges",
            descriptionAr = "تنفيذ سلاسل مراوغة خارقة متتالية",
            icon = "🔥",
            requiredLevel = 20,
            unlockHintEn = "Reach Level 20 or achieve a 10x Combo",
            unlockHintAr = "الوصول للمستوى 20 أو تحقيق كومبو 10x"
        ),
        PlayerTitle(
            id = "title_architect",
            nameEn = "The Architect",
            nameAr = "المهندس المعماري",
            descriptionEn = "Master of maze design and corridor traps",
            descriptionAr = "خبير بناء المتاهات والمصائد المغلقة",
            icon = "🧱",
            requiredLevel = 25,
            unlockHintEn = "Reach Level 25 or place 50 walls",
            unlockHintAr = "الوصول للمستوى 25 أو وضع 50 جداراً"
        ),
        PlayerTitle(
            id = "title_labyrinth_king",
            nameEn = "Labyrinth King",
            nameAr = "ملك المتاهة",
            descriptionEn = "Dominates every grid size and maze layout",
            descriptionAr = "السيطرة على مختلف أبعاد المتاهات والساحات",
            icon = "👑",
            requiredLevel = 35,
            unlockHintEn = "Reach Level 35 or win 25 matches",
            unlockHintAr = "الوصول للمستوى 35 أو الفوز بـ 25 مباراة"
        ),
        PlayerTitle(
            id = "title_predictor",
            nameEn = "The Predictor",
            nameAr = "المتنبئ الخارق",
            descriptionEn = "Anticipates every enemy step before it happens",
            descriptionAr = "توقع خطوات العدو المسبقة قبل حدوثها",
            icon = "🔮",
            requiredLevel = 45,
            unlockHintEn = "Reach Level 45 or defeat Predictor AI",
            unlockHintAr = "الوصول للمستوى 45 أو هزيمة ذكاء المتنبئ"
        ),
        PlayerTitle(
            id = "title_nightmare_slayer",
            nameEn = "Nightmare Slayer",
            nameAr = "قاهر الكابوس",
            descriptionEn = "Defeated the ruthless Nightmare AI difficulty",
            descriptionAr = "هزيمة الذكاء الاصطناعي برتبة كابوس",
            icon = "💀",
            requiredLevel = 60,
            unlockHintEn = "Reach Level 60 or defeat Nightmare AI",
            unlockHintAr = "الوصول للمستوى 60 أو التغلب على الكابوس"
        ),
        PlayerTitle(
            id = "title_time_bender",
            nameEn = "Time Bender",
            nameAr = "مُتلاعب الزمن",
            descriptionEn = "Mastered the temporal rewind flow",
            descriptionAr = "إتقان الرجوع بالزمن وتعديل مسار التاريخ",
            icon = "⏳",
            requiredLevel = 75,
            unlockHintEn = "Reach Level 75",
            unlockHintAr = "الوصول للمستوى 75"
        ),
        PlayerTitle(
            id = "title_legend",
            nameEn = "The Living Legend",
            nameAr = "الأسطورة الحية",
            descriptionEn = "Reached the pinnacle of Wall Rush mastery",
            descriptionAr = "قمة المجد والاحتراف في وول رش",
            icon = "🌟",
            requiredLevel = 100,
            unlockHintEn = "Reach Level 100",
            unlockHintAr = "الوصول للمستوى الأقصى 100"
        )
    )

    fun getEquippedTitle(context: Context): PlayerTitle {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_EQUIPPED_TITLE, "title_novice") ?: "title_novice"
        return ALL_TITLES.firstOrNull { it.id == id } ?: ALL_TITLES.first()
    }

    fun setEquippedTitle(context: Context, titleId: String): Boolean {
        if (!isTitleUnlocked(context, titleId)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EQUIPPED_TITLE, titleId).apply()
        return true
    }

    fun isTitleUnlocked(context: Context, titleId: String): Boolean {
        val title = ALL_TITLES.firstOrNull { it.id == titleId } ?: return false
        val currentLevel = com.example.wallrush.domain.progression.ProgressionManager.calculateLevelFromXp(
            com.example.wallrush.domain.progression.ProgressionManager.getTotalXp(context)
        )
        if (currentLevel >= title.requiredLevel) return true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unlockedSet = prefs.getStringSet(KEY_UNLOCKED_TITLES, emptySet()) ?: emptySet()
        return unlockedSet.contains(titleId)
    }

    fun unlockTitle(context: Context, titleId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unlockedSet = prefs.getStringSet(KEY_UNLOCKED_TITLES, emptySet())?.toMutableSet() ?: mutableSetOf()
        unlockedSet.add(titleId)
        prefs.edit().putStringSet(KEY_UNLOCKED_TITLES, unlockedSet).apply()
    }

    fun getAllUnlockedTitleIds(context: Context): Set<String> {
        val currentLevel = com.example.wallrush.domain.progression.ProgressionManager.calculateLevelFromXp(
            com.example.wallrush.domain.progression.ProgressionManager.getTotalXp(context)
        )
        val levelUnlocked = ALL_TITLES.filter { currentLevel >= it.requiredLevel }.map { it.id }.toSet()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manualUnlocked = prefs.getStringSet(KEY_UNLOCKED_TITLES, emptySet()) ?: emptySet()
        return levelUnlocked + manualUnlocked
    }
}
