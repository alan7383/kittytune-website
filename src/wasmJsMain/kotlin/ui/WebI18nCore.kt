@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import kotlinx.browser.localStorage

enum class WebLocale(
    val code: String,
    val shortLabel: String,
    val nativeName: String,
) {
    EN("en", "EN", "English"),
    FR("fr", "FR", "Français"),
    DE("de", "DE", "Deutsch"),
    HU("hu", "HU", "Magyar"),
    PT_BR("pt-BR", "PT", "Português (Brasil)"),
    RU("ru", "RU", "Русский"),
    TR("tr", "TR", "Türkçe"),
    VI("vi", "VI", "Tiếng Việt");

    companion object {
        fun fromTag(tag: String?): WebLocale = matchSupportedTagOrNull(tag) ?: EN

        internal fun matchSupportedTagOrNull(tag: String?): WebLocale? {
            val norm = tag?.trim()?.lowercase()?.replace('_', '-')?.takeIf { it.isNotEmpty() } ?: return null
            return when {
                norm == "pt-br" || norm == "pt" || norm.startsWith("pt-") || norm.startsWith("pt") -> PT_BR
                norm.startsWith("fr") -> FR
                norm.startsWith("de") -> DE
                norm.startsWith("hu") -> HU
                norm.startsWith("ru") -> RU
                norm.startsWith("tr") -> TR
                norm.startsWith("vi") -> VI
                norm.startsWith("en") -> EN
                else -> null
            }
        }
    }
}

val LocalWebLocale: ProvidableCompositionLocal<WebLocale> = compositionLocalOf { WebLocale.EN }

private const val WEB_LANG_STORAGE_KEY = "web_lang"

fun detectInitialWebLocale(): WebLocale {
    val urlLang = runCatching { urlLangParamJs() }.getOrNull()?.takeIf { it.isNotBlank() }
    if (urlLang != null) {
        return WebLocale.fromTag(urlLang)
    }

    val savedLang = runCatching { localStorage.getItem(WEB_LANG_STORAGE_KEY) }.getOrNull()?.takeIf { it.isNotBlank() }
    if (savedLang != null) {
        WebLocale.matchSupportedTagOrNull(savedLang)?.let { return it }
    }

    val browserLangs = runCatching { browserPreferredLangsCsvJs() }.getOrNull().orEmpty()
    if (browserLangs.isNotBlank()) {
        for (candidate in browserLangs.split(',')) {
            WebLocale.matchSupportedTagOrNull(candidate)?.let { return it }
        }
    }

    return WebLocale.EN
}

fun persistWebLocale(locale: WebLocale) {
    runCatching {
        localStorage.setItem(WEB_LANG_STORAGE_KEY, locale.code)
    }
    runCatching {
        setDocumentLangJs(locale.code)
    }
}

private fun urlLangParamJs(): String = js(
    """(function() {
        try {
            var params = new URLSearchParams(window.location.search || '');
            return params.get('lang') || '';
        } catch (e) {
            return '';
        }
    })()"""
)

private fun browserPreferredLangsCsvJs(): String = js(
    """(function() {
        try {
            if (window.navigator.languages && window.navigator.languages.length > 0) {
                return Array.from(window.navigator.languages).join(',');
            }
            return window.navigator.language || '';
        } catch (e) {
            return '';
        }
    })()"""
)

private fun setDocumentLangJs(lang: String) {
    js("try { if (document && document.documentElement) document.documentElement.lang = lang; } catch (e) {}")
}
