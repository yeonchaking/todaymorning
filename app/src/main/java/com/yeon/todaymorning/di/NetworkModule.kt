package com.yeon.todaymorning.di

import com.yeon.todaymorning.data.api.BusApiService
import com.yeon.todaymorning.data.api.SubwayApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun buildOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    @Named("bus")
    fun provideBusRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("http://ws.bus.go.kr/api/rest/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideBusApiService(@Named("bus") retrofit: Retrofit): BusApiService =
        retrofit.create(BusApiService::class.java)

    @Provides
    @Singleton
    @Named("subway")
    fun provideSubwayRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("http://swopenapi.seoul.go.kr/api/subway/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideSubwayApiService(@Named("subway") retrofit: Retrofit): SubwayApiService =
        retrofit.create(SubwayApiService::class.java)
}
