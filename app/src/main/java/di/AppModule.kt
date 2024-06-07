package di

import android.content.Context
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMeasureBoardRepository(@ApplicationContext context: Context): MeasureBoardRepository {
        return MeasureBoardRepository(context)
    }
}