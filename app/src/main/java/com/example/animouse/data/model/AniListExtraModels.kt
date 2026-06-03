package com.example.animouse.data.model

data class AniListExtraResponse(val data: AniListExtraData)
data class AniListExtraData(val Media: AniListExtraMedia)
data class AniListExtraMedia(val id: Int?, val trailer: Trailer?, val relations: Relations?)

data class Trailer(val id: String?, val site: String?)
data class Relations(val edges: List<RelationEdge>?)

data class RelationEdge(val relationType: String?, val node: Anime)

