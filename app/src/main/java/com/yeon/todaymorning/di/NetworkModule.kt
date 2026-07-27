package com.yeon.todaymorning.di

import com.yeon.todaymorning.BuildConfig
import com.yeon.todaymorning.data.api.BusApiService
import com.yeon.todaymorning.data.api.KakaoLocalApiService
import com.yeon.todaymorning.data.api.SubwayApiService
import com.yeon.todaymorning.data.api.TmapApiService
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

    // API 인증키가 로그(URL)에 노출되는 걸 막는 마스킹 로거.
    // - 버스/기타 공공 API: serviceKey=... 쿼리 파라미터
    // - 지하철 API: URL 경로에 키가 박힘 (.../api/subway/{key}/json/...)
    // 로그 문자열만 치환하며 실제 요청 URL에는 전혀 영향이 없다.
    private val serviceKeyQueryRegex = Regex("(serviceKey=)[^&\\s]+", RegexOption.IGNORE_CASE)
    private val subwayPathKeyRegex = Regex("(/api/subway/)[^/]+(/)")

    private val maskingLogger = HttpLoggingInterceptor.Logger { message ->
        val masked = message
            .replace(serviceKeyQueryRegex, "$1***")
            .replace(subwayPathKeyRegex, "$1***$2")
        HttpLoggingInterceptor.Logger.DEFAULT.log(masked)
    }

    // 릴리즈 빌드에서 API 키·응답 바디가 로그캣에 그대로 찍히는 걸 막기 위해
    // BODY(전체 요청/응답 본문)는 디버그 빌드에서만, 릴리즈는 BASIC(URL·상태코드만)으로.
    private fun buildOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor(maskingLogger).apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        })
        .build()

    @Provides @Singleton @Named("bus")
    fun provideBusRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("http://ws.bus.go.kr/api/rest/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideBusApiService(@Named("bus") retrofit: Retrofit): BusApiService =
        retrofit.create(BusApiService::class.java)

    @Provides @Singleton @Named("subway")
    fun provideSubwayRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("http://swopenapi.seoul.go.kr/api/subway/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideSubwayApiService(@Named("subway") retrofit: Retrofit): SubwayApiService =
        retrofit.create(SubwayApiService::class.java)

    @Provides @Singleton @Named("kakaoLocal")
    fun provideKakaoLocalRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideKakaoLocalApiService(@Named("kakaoLocal") retrofit: Retrofit): KakaoLocalApiService =
        retrofit.create(KakaoLocalApiService::class.java)

    @Provides @Singleton @Named("tmap")
    fun provideTmapRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://apis.openapi.sk.com/")
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideTmapApiService(@Named("tmap") retrofit: Retrofit): TmapApiService =
        retrofit.create(TmapApiService::class.java)
}
