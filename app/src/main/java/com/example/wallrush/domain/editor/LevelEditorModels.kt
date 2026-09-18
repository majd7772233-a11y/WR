package com.example.wallrush.domain.editor

import androidx.annotation.Keep
import com.example.wallrush.domain.model.*
import com.example.wallrush.domain.npc.NPCPersonality
import com.squareup.moshi.JsonClass
import java.util.UUID

@Keep
@JsonClass(generateAdapter = true)
data class CustomLevel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val gridSize: Int = 9, // 7, 9, 11
    val obstacles: List<Obstacle> = emptyList(),
    val presetWalls: List<Wall> = emptyList(),
    val p1Start: Position = Position(4, 8),
    val p2Start: Position = Position(4, 0),
    val wallsCount: Int = 10,
    val timeLimitSeconds: Int = 300,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val aiPersonality: NPCPersonality = NPCPersonality.THE_TACTICIAN,
    val author: String = "Player",
    val createdAt: Long = System.currentTimeMillis()
)

object CustomMapPresets {

    val PRESET_MAPS = listOf(
        CustomLevel(
            id = "preset_classic_9x9",
            name = "Classic Arena (9x9)",
            description = "Standard tournament grid with balanced symmetry",
            gridSize = 9,
            p1Start = Position(4, 8),
            p2Start = Position(4, 0),
            wallsCount = 10,
            timeLimitSeconds = 300
        ),
        CustomLevel(
            id = "preset_blitz_7x7",
            name = "Speed Duel (7x7)",
            description = "Compact 7x7 grid for ultra-fast, high-intensity dodges",
            gridSize = 7,
            wallsCount = 6,
            timeLimitSeconds = 120,
            p1Start = Position(3, 6),
            p2Start = Position(3, 0)
        ),
        CustomLevel(
            id = "preset_grand_11x11",
            name = "Grand Labyrinth (11x11)",
            description = "Sprawling arena allowing massive wall corridors",
            gridSize = 11,
            wallsCount = 16,
            timeLimitSeconds = 480,
            p1Start = Position(5, 10),
            p2Start = Position(5, 0)
        ),
        CustomLevel(
            id = "preset_pillars",
            name = "Stone Pillars",
            description = "Ancient stone columns block central movement paths",
            gridSize = 9,
            p1Start = Position(4, 8),
            p2Start = Position(4, 0),
            obstacles = listOf(
                Obstacle(2, 4),
                Obstacle(6, 4),
                Obstacle(4, 2),
                Obstacle(4, 6)
            ),
            wallsCount = 8,
            timeLimitSeconds = 300
        ),
        CustomLevel(
            id = "preset_cross_fortress",
            name = "Cross Fortress",
            description = "Central barrier cross with preset flanking corridors",
            gridSize = 9,
            p1Start = Position(4, 8),
            p2Start = Position(4, 0),
            presetWalls = listOf(
                Wall(x = 3, y = 4, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1),
                Wall(x = 5, y = 4, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_2)
            ),
            wallsCount = 10,
            timeLimitSeconds = 300
        ),
        CustomLevel(
            id = "preset_pocket_5x5",
            name = "Pocket Blitz (5x5)",
            description = "Micro arena for blistering, lightning-quick duels",
            gridSize = 5,
            p1Start = Position(2, 4),
            p2Start = Position(2, 0),
            wallsCount = 4,
            timeLimitSeconds = 60
        ),
        CustomLevel(
            id = "preset_spiral_maze",
            name = "Spiral Maze (9x9)",
            description = "Winding obstacle corridors forcing tactical detour navigation",
            gridSize = 9,
            p1Start = Position(4, 8),
            p2Start = Position(4, 0),
            obstacles = listOf(
                Obstacle(1, 2), Obstacle(2, 2), Obstacle(3, 2),
                Obstacle(5, 6), Obstacle(6, 6), Obstacle(7, 6)
            ),
            wallsCount = 10,
            timeLimitSeconds = 300
        ),
        CustomLevel(
            id = "preset_twin_castles",
            name = "Twin Fortresses (9x9)",
            description = "Symmetrical fortified bastions with defensive flanking columns",
            gridSize = 9,
            p1Start = Position(4, 8),
            p2Start = Position(4, 0),
            obstacles = listOf(
                Obstacle(2, 2), Obstacle(6, 2),
                Obstacle(2, 6), Obstacle(6, 6)
            ),
            wallsCount = 12,
            timeLimitSeconds = 300
        )
    )

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(CustomLevel::class.java)

    fun serializeToJson(level: CustomLevel): String = adapter.toJson(level)
    fun deserializeFromJson(json: String): CustomLevel? = try {
        adapter.fromJson(json.trim())
    } catch (e: Exception) {
        null
    }
}
