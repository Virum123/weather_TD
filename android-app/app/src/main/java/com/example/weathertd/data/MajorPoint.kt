package com.example.weathertd.data

data class MajorPoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object MajorPointCatalog {
    val all: List<MajorPoint> = listOf(
        MajorPoint(
            id = "gangnam",
            name = "Gangnam",
            latitude = 37.497952,
            longitude = 127.027619,
        ),
        MajorPoint(
            id = "hongdae",
            name = "Hongdae",
            latitude = 37.556350,
            longitude = 126.922651,
        ),
        MajorPoint(
            id = "jamsil",
            name = "Jamsil",
            latitude = 37.513261,
            longitude = 127.100133,
        ),
        MajorPoint(
            id = "yeouido",
            name = "Yeouido",
            latitude = 37.521624,
            longitude = 126.924191,
        ),
        MajorPoint(
            id = "gwanghwamun",
            name = "Gwanghwamun",
            latitude = 37.575876,
            longitude = 126.976849,
        ),
    )

    val fallback: MajorPoint = all.first()
}
