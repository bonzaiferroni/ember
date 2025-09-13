package ponder.ember.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform