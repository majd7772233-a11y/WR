package com.example.wallrush.domain.npc

import androidx.annotation.Keep
import com.example.wallrush.domain.model.AIDifficulty

@Keep
enum class NPCPersonality(
    val titleAr: String,
    val titleEn: String,
    val emoji: String,
    val badgeAr: String,
    val badgeEn: String,
    val playStyleAr: String,
    val playStyleEn: String,
    val defaultDelayMs: Long,
    val aggressionLevel: Int // 1 (Passive) to 10 (Hyper-Aggressive)
) {
    THE_RUSHER(
        titleAr = "المهاجم المندفع",
        titleEn = "The Relentless Rusher",
        emoji = "⚡🏃",
        badgeAr = "اندفاع هجومي",
        badgeEn = "Speed Striker",
        playStyleAr = "ينطلق بسرعة فائقة نحو خط النهاية عبر أقصر مسار ممكن، متجاهلاً وضع الجدران إلا عند الحافة القصوى للهزيمة.",
        playStyleEn = "Sprints relentlessly towards the goal row along the absolute shortest path, placing walls only at the brink of defeat.",
        defaultDelayMs = 280L,
        aggressionLevel = 10
    ),
    THE_DEFENDER(
        titleAr = "المدافع الحصين",
        titleEn = "The Fortress Defender",
        emoji = "🏰🛡️",
        badgeAr = "حصن دفاعي",
        badgeEn = "Fortress",
        playStyleAr = "يقيم جداراً دفاعياً متيناً ويغلق الممرات فور اقتراب الخصم من منتصف الملعب، ثم يتقدم بحذر شديد.",
        playStyleEn = "Constructs robust defensive barricades and seals corridors early, advancing cautiously with maximum protection.",
        defaultDelayMs = 450L,
        aggressionLevel = 3
    ),
    THE_ARCHITECT(
        titleAr = "المهندس الماكر",
        titleEn = "The Maze Architect",
        emoji = "🌀📐",
        badgeAr = "هندسة متاهات",
        badgeEn = "Labyrinth Crafter",
        playStyleAr = "يبني متاهة معقدة من الجدران المتداخلة لإجبار الخصم على قطع مسافة مضاعفة عبر حواف اللوحة.",
        playStyleEn = "Crafts intricate serpentine labyrinths that force the opponent on punishing detours across the perimeter.",
        defaultDelayMs = 550L,
        aggressionLevel = 6
    ),
    THE_TACTICIAN(
        titleAr = "التكتيكي المحترف",
        titleEn = "The Grandmaster Tactician",
        emoji = "♟️🎯",
        badgeAr = "حسابات دقيقة",
        badgeEn = "Grandmaster",
        playStyleAr = "يحسب رياضياً الفارق بين مساره ومسار الخصم، ويوازن بين الحركات التكتيكية واستهلاك الجدران بأعلى دقة.",
        playStyleEn = "Mathematically optimizes path differentials, conserving walls and striking precisely when tempo advantage peaks.",
        defaultDelayMs = 500L,
        aggressionLevel = 7
    ),
    THE_PREDICTOR(
        titleAr = "قارئ الأفكار",
        titleEn = "The Mind Reader",
        emoji = "🔮🧠",
        badgeAr = "استباق ذكي",
        badgeEn = "Predictive AI",
        playStyleAr = "يتعلم مسارك المفضل ويقطع مسار الهروب الجانبي قبل أن تخطو إليه خطوة واحدة.",
        playStyleEn = "Analyzes your historical evasion biases and preemptively closes your preferred escape flank.",
        defaultDelayMs = 480L,
        aggressionLevel = 7
    ),
    THE_TRICKSTER(
        titleAr = "المخادع المباغت",
        titleEn = "The Elusive Trickster",
        emoji = "🎭🃏",
        badgeAr = "حيل وفخاخ",
        badgeEn = "Mind Games",
        playStyleAr = "يوهمك بفتح ممر سهل ثم يغلقه فجأة بجدارين متوازيين ليحبسك في ممر جانبي ضيق.",
        playStyleEn = "Baits you into open corridors then snaps them shut with deceptive parallel wall traps.",
        defaultDelayMs = 400L,
        aggressionLevel = 8
    ),
    THE_SPEED_DEMON(
        titleAr = "شبح السرعة",
        titleEn = "The Speed Phantom",
        emoji = "🏎️💨",
        badgeAr = "برق خاطف",
        badgeEn = "Instant Reflexes",
        playStyleAr = "يتخذ قراراته في أجزاء من الثانية بحركات وقفزات جانبية خاطفة تشتت تركيز الخصم وتسلبه المبادرة.",
        playStyleEn = "Makes split-second moves with blistering pace, executing rapid hops and sudden perimeter flanks.",
        defaultDelayMs = 180L,
        aggressionLevel = 9
    ),
    THE_COUNTER_PUNCHER(
        titleAr = "القناص المتربص",
        titleEn = "The Counter-Striker",
        emoji = "🎯⚡",
        badgeAr = "هجوم مضاد",
        badgeEn = "Counter Attacker",
        playStyleAr = "ينتظر تجاوزك لمنتصف اللوحة، ثم يسقط جداراً خلفك أو أمامك مباشرة يقلب موازين اللقاء في لحظة.",
        playStyleEn = "Lures you across the midfield, then drops a devastating wall behind or ahead to crush your momentum.",
        defaultDelayMs = 420L,
        aggressionLevel = 8
    ),
    THE_MERCILESS(
        titleAr = "المنتقم الذي لا يرحم",
        titleEn = "The Merciless Slayer",
        emoji = "💀⚔️",
        badgeAr = "لا رحمة",
        badgeEn = "No Mercy",
        playStyleAr = "يعاقب كل خطوة غير مثالية بجدار حصار قاسٍ ويحاصرك دون أي تردد حتى الاستسلام.",
        playStyleEn = "Ruthlessly punishes every sub-optimal step with brutal wall locks, leaving zero room for mistakes.",
        defaultDelayMs = 520L,
        aggressionLevel = 10
    ),
    THE_CHAOTIC(
        titleAr = "الفوضوي الغامض",
        titleEn = "The Chaotic Glitcher",
        emoji = "🌪️🎲",
        badgeAr = "غير متوقع",
        badgeEn = "Unpredictable",
        playStyleAr = "حركات متعرجة غير تقليدية وجدران مفاجئة في أماكن غير متوقعة تكسر جميع الخطط الكلاسيكية.",
        playStyleEn = "Unorthodox zig-zags and surprising wall placements that shatter traditional Quoridor textbook openings.",
        defaultDelayMs = 350L,
        aggressionLevel = 6
    );

    fun getPersonalityReaction(isWallPlaced: Boolean, isWinning: Boolean, isArabic: Boolean): String {
        return if (isArabic) {
            when (this) {
                THE_RUSHER -> if (isWallPlaced) "⚡ لن توقفني أي حواجز!" else "🏃 سأصل قبلك بخطوة!"
                THE_DEFENDER -> if (isWallPlaced) "🏰 هذا الطريق مغلق تماماً!" else "🛡️ خطوة محسوبة وحصن منيع."
                THE_ARCHITECT -> if (isWallPlaced) "🌀 مرحباً بك في متاهتي!" else "📐 كل جدار في مكانه الصحيح."
                THE_TACTICIAN -> if (isWallPlaced) "♟️ حسابات دقيقة وتفوق تكتيكي." else "🎯 أفضل نقلة على اللوحة."
                THE_PREDICTOR -> if (isWallPlaced) "🔮 كنت أعلم أنك ستذهب هناك!" else "🧠 مسارك القادم مكشوف بالكامل."
                THE_TRICKSTER -> if (isWallPlaced) "🎭 وقعت في الفخ ببساطة!" else "🃏 توقع ما لا يمكن توقعه!"
                THE_SPEED_DEMON -> if (isWallPlaced) "🏎️ أسرع من البرق!" else "💨 لن تلحق بي أبداً!"
                THE_COUNTER_PUNCHER -> if (isWallPlaced) "🎯 ضربة مضادة في التوقيت المثالي!" else "⚡ اللعبة أصبحت في يدي."
                THE_MERCILESS -> if (isWallPlaced) "💀 لا مجال للهروب الآن." else "⚔️ خطؤك الأخير كلفك المباراة."
                THE_CHAOTIC -> if (isWallPlaced) "🎲 فوضى خلاقة تحكم الساحة!" else "🌪️ من يستطيع التنبؤ بهذا؟"
            }
        } else {
            when (this) {
                THE_RUSHER -> if (isWallPlaced) "⚡ No barrier can slow me down!" else "🏃 One step ahead of you!"
                THE_DEFENDER -> if (isWallPlaced) "🏰 This path is completely sealed!" else "🛡️ Solid defense wins games."
                THE_ARCHITECT -> if (isWallPlaced) "🌀 Welcome to my custom maze!" else "📐 Geometric precision."
                THE_TACTICIAN -> if (isWallPlaced) "♟️ Pure tactical calculation." else "🎯 Optimal positional move."
                THE_PREDICTOR -> if (isWallPlaced) "🔮 I foresaw your next 3 steps!" else "🧠 Your thoughts are open book."
                THE_TRICKSTER -> if (isWallPlaced) "🎭 Stepped right into the trap!" else "🃏 Always expect the unexpected!"
                THE_SPEED_DEMON -> if (isWallPlaced) "🏎️ Faster than lightning!" else "💨 You cannot catch the phantom!"
                THE_COUNTER_PUNCHER -> if (isWallPlaced) "🎯 Perfect counter strike!" else "⚡ Momentum is mine now."
                THE_MERCILESS -> if (isWallPlaced) "💀 Zero room for error." else "⚔️ Checkmate in motion."
                THE_CHAOTIC -> if (isWallPlaced) "🎲 Embrace the absolute chaos!" else "🌪️ Unpredictable by design."
            }
        }
    }
}

@Keep
data class NPCProfile(
    val id: String,
    val name: String,
    val avatarId: Int,
    val rating: Int,
    val countryFlag: String,
    val countryName: String,
    val personality: NPCPersonality,
    val minThinkingDelayMs: Long,
    val maxThinkingDelayMs: Long,
    var encountersCount: Int = 0,
    val statusBio: String = ""
) {
    fun toAIDifficulty(): AIDifficulty {
        return when {
            rating >= 2400 -> AIDifficulty.INSANE
            rating >= 2100 -> AIDifficulty.NIGHTMARE
            rating >= 1800 -> AIDifficulty.EXPERT
            rating >= 1500 -> AIDifficulty.HARD
            rating >= 1100 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.EASY
        }
    }
}
