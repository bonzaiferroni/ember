package ponder.ember

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform