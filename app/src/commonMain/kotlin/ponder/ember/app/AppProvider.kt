package ponder.ember.app

import kabinet.utils.Environment
import ponder.ember.app.db.AppDao
import ponder.ember.app.db.AppDatabase

object AppProvider {
    var db: AppDatabase? = null
    var env: Environment? = null

    val appDao by lazy { AppDao(db ?: error("db not initialized")) }
}