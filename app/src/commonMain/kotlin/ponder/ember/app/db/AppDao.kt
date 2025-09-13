package ponder.ember.app.db

class AppDao(appDatabase: AppDatabase) {
    val block: BlockDao = appDatabase.getBlockDao()
}