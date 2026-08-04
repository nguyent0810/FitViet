package com.fitviet.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the 1i "Ngôn ngữ" choice as a real per-app locale override — Android 13+ delegates to the
 * framework `LocaleManager`; older versions use AppCompat's own persisted-locale mechanism (wired in
 * automatically once `androidx.appcompat` + `res/xml/locales_config.xml`/`android:localeConfig` are
 * present, no extra code needed). Idempotent: safe to call on every recomposition/recreation.
 */
object LocaleController {
    fun apply(isEnglish: Boolean) {
        val tag = if (isEnglish) "en" else "vi"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
