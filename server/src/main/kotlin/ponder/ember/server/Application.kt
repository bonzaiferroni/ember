package ponder.ember.server

import io.ktor.server.application.*
import klutch.server.configureSecurity
import ponder.ember.server.plugins.configureApiRoutes
import ponder.ember.server.plugins.configureCors
import ponder.ember.server.plugins.configureDatabases
import ponder.ember.server.plugins.configureLogging
import ponder.ember.server.plugins.configureSerialization
import ponder.ember.server.plugins.configureWebSockets

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureCors()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureApiRoutes()
    configureWebSockets()
    configureLogging()
}