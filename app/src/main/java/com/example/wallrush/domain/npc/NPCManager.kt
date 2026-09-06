package com.example.wallrush.domain.npc

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.domain.model.AIDifficulty
import kotlin.random.Random

object NPCManager {

    private const val PREFS_NAME = "wallrush_npc_history"
    private const val KEY_ENCOUNTER_PREFIX = "npc_enc_"
    private const val MAX_ENCOUNTERS_PER_NPC = 3

    private val ARABIC_FIRST_NAMES = listOf(
        "عمر", "فهد", "سلطان", "زيد", "طارق", "عبدالله", "يوسف", "سارة", "نور", "مريم",
        "حمزة", "أحمد", "خالد", "بدر", "ريان", "ليلى", "سعود", "كريم", "إبراهيم", "ماجد",
        "هشام", "عمار", "فيصل", "شهد", "أنس", "حميد", "ياسر", "طلال", "سلمى", "نايف",
        "حاتم", "آية", "زياد", "باسل", "وليد", "جمال", "هند", "رغد", "سامي", "معاذ",
        "أسامة", "منصور", "مشعل", "راكان", "غادة", "جود", "ريما", "عاصم", "قصي", "ليث",
        "ضياء", "حسام", "مؤيد", "جاد", "بشار", "عصام", "أوس", "غيث", "بلال", "تامر",
        "عدنان", "شريف", "مالك", "حمادة", "فارس", "معتز", "نادين", "دانية", "لجين", "روان"
    )

    private val ARABIC_SUFFIXES = listOf(
        "_العتيبي", "_الغامدي", "_الشمري", "_القحطاني", "_الدوسري", "_التميمي", "_الشهري",
        "_الحربي", "_العنزي", "_المطيري", "_النجار", "_المصري", "_الشامي", "_البغدادي",
        "_المغربي", "_التونسي", "_الجزائري", "_الكويتي", "_اليماني", "_الاردني", "_اللبناني",
        "_99", "_77", "_94", "_96", "_07", "_05", "_2000", "_01", "_88", "_505",
        "_VIP", "_Pro", "_Gamer", "_Rush", "_KSA", "_UAE", "_DZ", "_EGY", "_Playz",
        "_Tactics", "_Master", "_Solo", "_Hunter", "_Apex", "_Fox", "_Wolf", "_King", "_Queen"
    )

    private val INTL_FIRST_NAMES = listOf(
        "Alex", "Marcus", "Elena", "Kaito", "Lucas", "Mateo", "Sophie", "Viktor", "Liam", "Chloe",
        "Kenji", "Arthur", "Sora", "Julian", "Maya", "Dmitri", "Carlos", "Emma", "Kai", "Nina",
        "Oliver", "Dev", "Hugo", "Zara", "Leo", "Isabella", "Finn", "Aria", "Sam", "Felix",
        "Gabriel", "Noa", "Nico", "Jonas", "Hanna", "Lars", "Tariq", "Oscar", "Freja", "Soren",
        "Priya", "Chen", "Yuki", "Hiro", "Aiden", "Mason", "Logan", "Elijah", "Mia", "Harper",
        "Evelyn", "Abigail", "Emily", "Noah", "Liam", "Ethan", "Zoe", "Stella", "Milo", "Theo"
    )

    private val INTL_SUFFIXES = listOf(
        "_Silva", "_Novak", "_Reid", "_Ice", "_Rush", "_Storm", "_Wolf", "_Grid", "_Plays",
        "_99", "_04", "_98", "_23", "_77", "_Pro", "_Walker", "_Chen", "_Fox", "_Runner",
        "_Strike", "_Knight", ".exe", ".grid", "99", "07", "_Live", "_Gamer", "_Craft",
        "_M", "_B", "_K", "_V", "_Tactics", "_Apex", "_Ghost", "_Shadow", "_Pixel", "_Drift"
    )

    private val COUNTRIES = listOf(
        Pair("🇸🇦", "Saudi Arabia"),
        Pair("🇦🇪", "United Arab Emirates"),
        Pair("🇪🇬", "Egypt"),
        Pair("🇯🇴", "Jordan"),
        Pair("🇲🇦", "Morocco"),
        Pair("🇩🇿", "Algeria"),
        Pair("🇰🇼", "Kuwait"),
        Pair("🇮🇶", "Iraq"),
        Pair("🇧🇭", "Bahrain"),
        Pair("🇴🇲", "Oman"),
        Pair("🇶🇦", "Qatar"),
        Pair("🇺🇸", "United States"),
        Pair("🇬🇧", "United Kingdom"),
        Pair("🇩🇪", "Germany"),
        Pair("🇫🇷", "France"),
        Pair("🇯🇵", "Japan"),
        Pair("🇧🇷", "Brazil"),
        Pair("🇨🇦", "Canada"),
        Pair("🇪🇸", "Spain"),
        Pair("🇮🇹", "Italy")
    )

    private val STATUS_BIOS = listOf(
        "Always rushing forward ⚡",
        "Lover of tactical maze walls 🧱",
        "Quoridor veteran since 2021 ♟️",
        "Fast moves, quick thinker ⏱️",
        "Looking for tough matches 🔥",
        "Calculated jumps only 🎯",
        "No mercy with the barricades 🚧",
        "Mobile champion in training 📱",
        "Playing with sound on 🎧",
        "Strategy beats speed 🧠",
        "One move ahead of you 🔮",
        "Mastering wall traps 🛡️"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gets how many times the user has encountered this NPC.
     */
    fun getEncounterCount(context: Context, npcId: String): Int {
        return getPrefs(context).getInt(KEY_ENCOUNTER_PREFIX + npcId, 0)
    }

    /**
     * Records an encounter with an NPC.
     */
    fun recordEncounter(context: Context, npcId: String) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_ENCOUNTER_PREFIX + npcId, 0)
        prefs.edit().putInt(KEY_ENCOUNTER_PREFIX + npcId, current + 1).apply()
    }

    /**
     * Generates a fresh, highly realistic human-like NPC opponent.
     * Guaranteed to pick an NPC that has been encountered fewer than MAX_ENCOUNTERS_PER_NPC (3) times.
     */
    fun getRandomOpponent(context: Context, targetDifficulty: AIDifficulty? = null): NPCProfile {
        var attempts = 0
        while (attempts < 50) {
            attempts++
            val candidate = generateCandidate(targetDifficulty)
            val encounters = getEncounterCount(context, candidate.id)
            if (encounters < MAX_ENCOUNTERS_PER_NPC) {
                candidate.encountersCount = encounters
                return candidate
            }
        }
        // If thousands of matches occurred, generate a uniquely timestamped candidate
        val candidate = generateCandidate(targetDifficulty, forceUnique = true)
        candidate.encountersCount = getEncounterCount(context, candidate.id)
        return candidate
    }

    /**
     * Generates a batch of distinct public lobby room hosts.
     */
    fun generateLobbyHosts(context: Context, count: Int = 6): List<NPCProfile> {
        val list = mutableListOf<NPCProfile>()
        val usedNames = mutableSetOf<String>()
        var attempts = 0
        while (list.size < count && attempts < 100) {
            attempts++
            val npc = generateCandidate(forceUnique = true)
            if (usedNames.add(npc.name)) {
                npc.encountersCount = getEncounterCount(context, npc.id)
                list.add(npc)
            }
        }
        return list
    }

    private fun generateCandidate(targetDifficulty: AIDifficulty? = null, forceUnique: Boolean = false): NPCProfile {
        val isArabic = Random.nextBoolean()
        val baseName = if (isArabic) {
            val first = ARABIC_FIRST_NAMES.random()
            val suffix = ARABIC_SUFFIXES.random()
            "$first$suffix"
        } else {
            val first = INTL_FIRST_NAMES.random()
            val suffix = INTL_SUFFIXES.random()
            "$first$suffix"
        }

        val uniqueSuffix = if (forceUnique) "_${Random.nextInt(100, 9999)}" else ""
        val fullName = if (forceUnique && Random.nextFloat() < 0.35f) "$baseName$uniqueSuffix" else baseName
        val id = "npc_${fullName.lowercase().replace(".", "_").replace(" ", "_")}"

        val personality = when (targetDifficulty) {
            AIDifficulty.EASY -> listOf(NPCPersonality.THE_RUSHER, NPCPersonality.THE_CHAOTIC).random()
            AIDifficulty.HARD -> listOf(NPCPersonality.THE_TACTICIAN, NPCPersonality.THE_ARCHITECT, NPCPersonality.THE_COUNTER_PUNCHER).random()
            AIDifficulty.MEDIUM -> NPCPersonality.values().random()
            null -> NPCPersonality.values().random()
        }

        val rating = when (personality) {
            NPCPersonality.THE_RUSHER -> Random.nextInt(1050, 1550)
            NPCPersonality.THE_CHAOTIC -> Random.nextInt(1100, 1650)
            NPCPersonality.THE_DEFENDER -> Random.nextInt(1350, 1850)
            NPCPersonality.THE_COUNTER_PUNCHER -> Random.nextInt(1500, 2050)
            NPCPersonality.THE_ARCHITECT -> Random.nextInt(1600, 2150)
            NPCPersonality.THE_TACTICIAN -> Random.nextInt(1800, 2450)
        }

        val (minDelay, maxDelay) = when (personality) {
            NPCPersonality.THE_RUSHER -> Pair(450L, 850L)
            NPCPersonality.THE_CHAOTIC -> Pair(500L, 1600L)
            NPCPersonality.THE_DEFENDER -> Pair(750L, 1400L)
            NPCPersonality.THE_COUNTER_PUNCHER -> Pair(800L, 1500L)
            NPCPersonality.THE_TACTICIAN -> Pair(900L, 1750L)
            NPCPersonality.THE_ARCHITECT -> Pair(1000L, 1900L)
        }

        val country = if (isArabic) {
            COUNTRIES.filter { it.first in listOf("🇸🇦", "🇦🇪", "🇪🇬", "🇯🇴", "🇲🇦", "🇩🇿", "🇰🇼", "🇮🇶", "🇧🇭", "🇴🇲", "🇶🇦") }.random()
        } else {
            COUNTRIES.filter { it.first !in listOf("🇸🇦", "🇦🇪", "🇪🇬", "🇯🇴", "🇲🇦", "🇩🇿", "🇰🇼", "🇮🇶", "🇧🇭", "🇴🇲", "🇶🇦") }.random()
        }

        return NPCProfile(
            id = id,
            name = fullName,
            avatarId = Random.nextInt(0, 8),
            rating = rating,
            countryFlag = country.first,
            countryName = country.second,
            personality = personality,
            minThinkingDelayMs = minDelay,
            maxThinkingDelayMs = maxDelay,
            statusBio = STATUS_BIOS.random()
        )
    }
}
