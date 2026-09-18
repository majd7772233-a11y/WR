package com.example.wallrush.domain.powerups

import com.example.wallrush.domain.model.*
import kotlin.random.Random

data class PowerUpDefinition(
    val type: PowerUpType,
    val nameEn: String,
    val nameAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val energyCost: Int,
    val icon: String,
    val category: PowerUpCategory
)

enum class PowerUpCategory {
    DEFENSIVE,
    OFFENSIVE,
    MOVEMENT,
    TIME,
    RISKY
}

object PowerUpManager {

    val ALL_POWERUPS = listOf(
        // Defensive
        PowerUpDefinition(
            type = PowerUpType.SHIELD,
            nameEn = "Shield",
            nameAr = "الدرع الواقي",
            descriptionEn = "Blocks wall traps targeting your immediate path",
            descriptionAr = "يحميك من الحواجز التي تحاصر مسارك القادم",
            energyCost = 35,
            icon = "🛡️",
            category = PowerUpCategory.DEFENSIVE
        ),
        PowerUpDefinition(
            type = PowerUpType.PHASE,
            nameEn = "Phase Shift",
            nameAr = "اختراق الجدار",
            descriptionEn = "Phase directly through 1 placed wall on next step",
            descriptionAr = "المرور من خلال جدار واحد في حركتك القادمة",
            energyCost = 50,
            icon = "👻",
            category = PowerUpCategory.DEFENSIVE
        ),
        PowerUpDefinition(
            type = PowerUpType.INVINCIBLE,
            nameEn = "Invincibility",
            nameAr = "المناعة الشاملة",
            descriptionEn = "Complete immunity to barriers for 2 turns",
            descriptionAr = "مناعة تامة من جميع العوائق لمدة دورين",
            energyCost = 75,
            icon = "⭐",
            category = PowerUpCategory.DEFENSIVE
        ),

        // Offensive
        PowerUpDefinition(
            type = PowerUpType.WALL_BREAK,
            nameEn = "Wall Breaker",
            nameAr = "مُحطم الجدران",
            descriptionEn = "Demolishes the nearest opposing wall instantly",
            descriptionAr = "تدمير أقرب جدار للخصم فوراً وتفتيته",
            energyCost = 60,
            icon = "💥",
            category = PowerUpCategory.OFFENSIVE
        ),
        PowerUpDefinition(
            type = PowerUpType.LASER,
            nameEn = "Laser Zap",
            nameAr = "صاعقة الليزر",
            descriptionEn = "Vaporizes all walls along your current row/column",
            descriptionAr = "محو وإزالة كافة الجدران الواقعة في نفس الصف",
            energyCost = 70,
            icon = "⚡",
            category = PowerUpCategory.OFFENSIVE
        ),

        // Movement
        PowerUpDefinition(
            type = PowerUpType.DASH,
            nameEn = "Sprint Dash",
            nameAr = "اندفاع خاطف",
            descriptionEn = "Take 2 rapid steps forward along the optimal path",
            descriptionAr = "التقدم خطوتين متتاليتين في المسار الأقصر",
            energyCost = 45,
            icon = "💨",
            category = PowerUpCategory.MOVEMENT
        ),
        PowerUpDefinition(
            type = PowerUpType.TELEPORT,
            nameEn = "Blink Teleport",
            nameAr = "انتقال آني",
            descriptionEn = "Warp directly forward over any blocking wall",
            descriptionAr = "القفز فوق أي جدار حاجز مباشرة للأمام",
            energyCost = 65,
            icon = "🌀",
            category = PowerUpCategory.MOVEMENT
        ),

        // Time
        PowerUpDefinition(
            type = PowerUpType.TIME_FREEZE,
            nameEn = "Time Freeze",
            nameAr = "تجميد الوقت",
            descriptionEn = "Freezes opponent, granting you an extra free turn",
            descriptionAr = "تجميد الخصم والحصول على دور إضافي مجاني",
            energyCost = 60,
            icon = "❄️",
            category = PowerUpCategory.TIME
        ),
        PowerUpDefinition(
            type = PowerUpType.TIME_REWIND,
            nameEn = "Time Rewind",
            nameAr = "إرجاع الزمن",
            descriptionEn = "Reverses the game by 2-3 moves (AI Mode Only)",
            descriptionAr = "إرجاع المباراة للوراء بعدة خطوات (طور AI فقط)",
            energyCost = 40,
            icon = "⏪",
            category = PowerUpCategory.TIME
        ),

        // Risky
        PowerUpDefinition(
            type = PowerUpType.BERSERK,
            nameEn = "Berserk Surge",
            nameAr = "غضب هائج",
            descriptionEn = "Gain 2 instant moves, but sacrifice 2 of your walls",
            descriptionAr = "تحصل على حركتين فوريتين مقابل خسارة جدارين",
            energyCost = 50,
            icon = "⚔️",
            category = PowerUpCategory.RISKY
        ),
        PowerUpDefinition(
            type = PowerUpType.CHAOS,
            nameEn = "Chaos Shift",
            nameAr = "فوضى المتاهة",
            descriptionEn = "Randomly shifts or rotates a wall on the arena",
            descriptionAr = "تغيير موقع أو دوران جدار عشوائي في الساحة",
            energyCost = 30,
            icon = "🎲",
            category = PowerUpCategory.RISKY
        )
    )

    fun getPowerUp(type: PowerUpType): PowerUpDefinition {
        return ALL_POWERUPS.first { it.type == type }
    }

    /**
     * Executes the power-up effect and returns the modified GameState.
     */
    fun applyPowerUp(
        state: GameState,
        playerId: PlayerId,
        powerUpType: PowerUpType
    ): Pair<GameState, String> {
        val player = state.getPlayer(playerId)
        val definition = getPowerUp(powerUpType)

        if (player.energy < definition.energyCost) {
            return Pair(state, "Not enough Energy!")
        }

        val updatedPlayer = player.copy(
            energy = (player.energy - definition.energyCost).coerceAtLeast(0)
        )

        var newState = when (playerId) {
            PlayerId.PLAYER_1 -> state.copy(player1 = updatedPlayer)
            PlayerId.PLAYER_2 -> state.copy(player2 = updatedPlayer)
            PlayerId.PLAYER_3 -> state.copy(player3 = updatedPlayer)
            PlayerId.PLAYER_4 -> state.copy(player4 = updatedPlayer)
        }

        // Add event to history
        val newHistory = newState.eventHistory + GameEvent.PowerUpUsed(
            player = playerId,
            powerUp = powerUpType,
            moveNumber = newState.moveCount
        )
        newState = newState.copy(eventHistory = newHistory)

        var message = "Activated ${definition.nameEn}!"

        when (powerUpType) {
            PowerUpType.SHIELD -> {
                newState = updatePlayerState(newState, playerId) { it.copy(shieldTurns = it.shieldTurns + 2) }
                message = "Shield active for 2 turns!"
            }
            PowerUpType.PHASE -> {
                newState = updatePlayerState(newState, playerId) { it.copy(phasePassCharges = it.phasePassCharges + 1) }
                message = "Phase shift primed! Step through any wall."
            }
            PowerUpType.INVINCIBLE -> {
                newState = updatePlayerState(newState, playerId) { it.copy(shieldTurns = it.shieldTurns + 3, phasePassCharges = it.phasePassCharges + 2) }
                message = "Invincibility surge activated!"
            }
            PowerUpType.WALL_BREAK -> {
                if (newState.walls.isNotEmpty()) {
                    // Destroy closest wall to opponent or player
                    val targetWall = newState.walls.minByOrNull { w ->
                        kotlin.math.abs(w.x - player.position.x) + kotlin.math.abs(w.y - player.position.y)
                    } ?: newState.walls.last()
                    newState = newState.copy(walls = newState.walls - targetWall)
                    message = "Barrier shattered!"
                } else {
                    message = "No walls on the arena to break."
                }
            }
            PowerUpType.LASER -> {
                val playerRow = player.position.y
                val wallsRemoved = newState.walls.filter { it.y == playerRow || it.y == playerRow - 1 }
                newState = newState.copy(walls = newState.walls - wallsRemoved.toSet())
                message = "Laser cleared ${wallsRemoved.size} walls!"
            }
            PowerUpType.DASH -> {
                // Dash 1 step closer to goal row
                val goalRow = com.example.wallrush.domain.engine.RuleEngine.getGoalRow(playerId, newState.rules.mode)
                val path = com.example.wallrush.domain.engine.PathFinder.findShortestPath(player.position, goalRow, newState.walls)
                if (path.size > 1) {
                    val nextStep = path[1]
                    newState = updatePlayerState(newState, playerId) { it.copy(position = nextStep) }
                    message = "Dashed forward!"
                }
            }
            PowerUpType.TELEPORT -> {
                val goalRow = com.example.wallrush.domain.engine.RuleEngine.getGoalRow(playerId, newState.rules.mode)
                val targetY = if (goalRow == 8) (player.position.y + 2).coerceAtMost(8) else (player.position.y - 2).coerceAtLeast(0)
                val target = Position(player.position.x, targetY)
                newState = updatePlayerState(newState, playerId) { it.copy(position = target) }
                message = "Teleported ahead!"
            }
            PowerUpType.TIME_FREEZE -> {
                // Skips opponent turn by keeping turn on current player
                message = "Opponent frozen! Free extra turn."
            }
            PowerUpType.TIME_REWIND -> {
                // Handled in ViewModel's executeRewind()
                message = "Rewinding time..."
            }
            PowerUpType.BERSERK -> {
                val remainingWalls = (player.remainingWalls - 2).coerceAtLeast(0)
                val goalRow = com.example.wallrush.domain.engine.RuleEngine.getGoalRow(playerId, newState.rules.mode)
                val path = com.example.wallrush.domain.engine.PathFinder.findShortestPath(player.position, goalRow, newState.walls)
                val newPos = if (path.size > 2) path[2] else if (path.size > 1) path[1] else player.position
                newState = updatePlayerState(newState, playerId) { it.copy(position = newPos, remainingWalls = remainingWalls) }
                message = "Berserk surge launched!"
            }
            PowerUpType.CHAOS -> {
                if (newState.walls.isNotEmpty()) {
                    val randomWall = newState.walls.random()
                    val shiftedOrientation = if (randomWall.orientation == WallOrientation.HORIZONTAL) WallOrientation.VERTICAL else WallOrientation.HORIZONTAL
                    val shifted = randomWall.copy(orientation = shiftedOrientation)
                    newState = newState.copy(walls = (newState.walls - randomWall) + shifted)
                    message = "Chaos shifted a barrier!"
                }
            }
            else -> {}
        }

        return Pair(newState, message)
    }

    private fun updatePlayerState(state: GameState, playerId: PlayerId, transform: (PlayerState) -> PlayerState): GameState {
        return when (playerId) {
            PlayerId.PLAYER_1 -> state.copy(player1 = transform(state.player1))
            PlayerId.PLAYER_2 -> state.copy(player2 = transform(state.player2))
            PlayerId.PLAYER_3 -> state.copy(player3 = state.player3?.let(transform))
            PlayerId.PLAYER_4 -> state.copy(player4 = state.player4?.let(transform))
        }
    }
}
