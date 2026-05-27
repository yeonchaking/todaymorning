package com.yeon.todaymorning.di

import android.content.Context
import com.yeon.todaymorning.data.db.AppDatabase
import com.yeon.todaymorning.data.db.MissionDao
import com.yeon.todaymorning.data.repository.MissionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideMissionDao(db: AppDatabase): MissionDao {
        return db.missionDao()
    }

    @Provides
    @Singleton
    fun provideMissionRepository(dao: MissionDao): MissionRepository {
        return MissionRepository(dao)
    }
}
