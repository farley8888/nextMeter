package com.vismo.nextgenmeter.util

import android.content.Context
import android.os.Build
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleHelper @Inject constructor(
    private val appContext: Context
) {

    fun setLocale(languageCode: String?) {
        // Fallback to default locale if languageCode is null
        val locale = languageCode?.let { Locale(it) } ?: Locale.getDefault()

        Locale.setDefault(locale)

        val configuration = appContext.resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)  // API 17+ use setLocale
        } else {
            configuration.locale = locale
        }
        appContext.resources.updateConfiguration(configuration, appContext.resources.getDisplayMetrics())
    }

    fun getLocale(): Locale {
        val configuration = appContext.resources.configuration

        // API 24+ uses getLocales
        return configuration.locales[0]  // Returns the first Locale in the list
    }
}
