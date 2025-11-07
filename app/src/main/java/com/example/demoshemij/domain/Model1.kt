package com.example.demoshemij.domain

data class CharacterList(
    val characters: List<Map<String, CharacterData>>
)

data class CharacterData(
    val folder: String,
    val thumbnail: String,
    val animations: Map<String, AnimationData>
)

data class AnimationData(
    val thumb: String,
    val frames: List<FrameData>,
    val logic: List<LogicData>
)

data class FrameData(
    val id: Int,
    val url: String
)

data class LogicData(
    val frame: List<Int>,
    val delay: Int,
    val sequence: Any // Có thể là Int hoặc "infinity"
)