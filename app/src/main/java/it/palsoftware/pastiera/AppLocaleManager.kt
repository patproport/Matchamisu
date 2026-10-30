package it.palsoftware.pastiera

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object AppLocaleManager {
    fun wrapContext(base: Context): Context {
        val tag = SettingsManager.getAppLanguageTag(base)
        val requested = if (tag.isNullOrBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                base.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                base.resources.configuration.locale
            }
        } else Locale.forLanguageTag(tag)
        val locale = if (requested.language == "ja") Locale.JAPANESE else Locale.ENGLISH

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        return base.createConfigurationContext(config)
    }
}
