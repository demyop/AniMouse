package com.example.animouse.data.model

// Главная обертка ответа
data class DiscoveryResponse(
    val data: DiscoveryData
)

// Внутри лежат наши 3 списка с теми именами, которые мы зададим в GraphQL
data class DiscoveryData(
    val trending: AniListPage?,
    val upcoming: AniListPage?,
    val top: AniListPage?
)

// Сама страница, внутри которой лежит список аниме
data class AniListPage(
    val media: List<Anime>?
)