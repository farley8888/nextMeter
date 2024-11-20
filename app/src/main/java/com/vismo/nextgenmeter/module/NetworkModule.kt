package com.vismo.nextgenmeter.module

import com.vismo.nextgenmeter.api.service.MeterOApiService
import com.vismo.nextgenmeter.api.interceptor.LoggingInterceptor
import com.vismo.nextgenmeter.api.service.MeterApiService
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository
import com.vismo.nextgenmeter.repository.MeterApiRepository
import com.vismo.nextgenmeter.repository.MeterOApiRepository
import com.vismo.nextgenmeter.util.Constant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit1

@Qualifier
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit2

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Singleton
    @Provides
    @ApiRetrofit1
    fun providesRetrofit(): Retrofit {
        val okHttpBuilder = okhttp3.OkHttpClient.Builder()

        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        okHttpBuilder.addInterceptor(httpLoggingInterceptor)
        okHttpBuilder.addInterceptor(LoggingInterceptor())

        return Retrofit.Builder()
            .baseUrl(Constant.BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpBuilder.build())
            .build()
    }

    @Singleton
    @Provides
    @ApiRetrofit2
    fun providesRetrofitForOApiUrls(): Retrofit {
        val okHttpBuilder = okhttp3.OkHttpClient.Builder()

        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        okHttpBuilder.addInterceptor(httpLoggingInterceptor)
        okHttpBuilder.addInterceptor(LoggingInterceptor())

        return Retrofit.Builder()
            .baseUrl(Constant.BASE_OAPI_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpBuilder.build())
            .build()

    }

    @Singleton
    @Provides
    fun providesMeterApi(@ApiRetrofit1 retrofit: Retrofit): MeterApiService {
        return retrofit.create(MeterApiService::class.java)
    }

    @Singleton
    @Provides
    fun providesMeterOApi(@ApiRetrofit2 retrofit: Retrofit): MeterOApiService {
        return retrofit.create(MeterOApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideOMeterApiRepository(
        meterOApiService: MeterOApiService,
    ): MeterOApiRepository {
        return MeterOApiRepository(
            meterOApiService = meterOApiService,
        )
    }

    @Singleton
    @Provides
    fun provideMeterApiRepository(
        meterApiService: MeterApiService,
        firebaseAuthRepository: FirebaseAuthRepository
    ): MeterApiRepository {
        return MeterApiRepository(
            firebaseAuthRepository = firebaseAuthRepository,
            meterApiService = meterApiService,
        )
    }

}