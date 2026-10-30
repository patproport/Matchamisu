package it.palsoftware.pastiera.update

data class ReleaseNotesSummary(
    val version: String,
    val title: String,
    val highlights: List<String>,
    val improvements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList(),
    val docsUrl: String = "https://pastiera.eu/"
) {
    companion object {
        fun fallback(version: String, languageTag: String = "en"): ReleaseNotesSummary {
            val language = normalizeReleaseNotesLanguage(languageTag)
            return ReleaseNotesSummary(
                version = version,
                title = when (language) {
                    "de" -> "Pastiera $version"
                    "it" -> "Pastiera $version"
                    else -> "Pastiera $version"
                },
                highlights = when (language) {
                    "de" -> listOf(
                        "Die neu gestalteten Einstellungen sind durchsuchbar, direkt verlinkbar und zuverlässiger navigierbar.",
                        "Pastiera passt sich sauberer an das gerundete Display des Titan 2 Elite an. Clicks-Tastaturen erhalten eigene Steuerungen und zuverlässigere Eingabe.",
                        "Die Bildschirmtastatur bietet eigene Themes, Presets, Software-Modifier, Zahlenreihe und bessere Barrierefreiheit."
                    )
                    "it" -> listOf(
                        "Le impostazioni ridisegnate sono ricercabili, collegabili direttamente e più affidabili da navigare.",
                        "Pastiera si adatta meglio al display arrotondato del Titan 2 Elite. Le tastiere Clicks ricevono controlli dedicati e un input più affidabile.",
                        "La tastiera su schermo offre temi, preset, modificatori software, riga numerica e accessibilità migliorata."
                    )
                    else -> listOf(
                        "Redesigned Settings are searchable, directly linkable, and more reliable to navigate.",
                        "Pastiera fits the Titan 2 Elite’s rounded display more cleanly. Clicks keyboards gain dedicated controls and more reliable input.",
                        "The on-screen keyboard adds custom themes, presets, software modifiers, a number row, and better accessibility."
                    )
                },
                improvements = when (language) {
                    "de" -> listOf(
                        "Snippets, Emoji- und Symbol-Shortcodes sowie feinere Satzzeichenregeln beschleunigen wiederkehrende Eingaben.",
                        "Vorschläge können mehrere Wörterbücher und lokal gelernte nächste Wörter verwenden.",
                        "Neue Sprachressourcen, darunter Griechisch, ergänzen aktualisierte Unicode- und Emoji-Daten."
                    )
                    "it" -> listOf(
                        "Snippet, shortcode per emoji e simboli e regole di punteggiatura più precise velocizzano l'inserimento ricorrente.",
                        "I suggerimenti possono usare più dizionari e sequenze di parole successive apprese localmente.",
                        "Nuove risorse linguistiche, incluso il greco, accompagnano dati Unicode ed emoji aggiornati."
                    )
                    else -> listOf(
                        "Snippets, emoji and symbol shortcodes, and refined punctuation rules speed up recurring input.",
                        "Suggestions can use multiple dictionaries and locally learned next-word sequences.",
                        "New language resources, including Greek, accompany updated Unicode and emoji data."
                    )
                },
                bugFixes = when (language) {
                    "de" -> listOf("Candidate- und Emoji-Oberflächen reagieren zuverlässiger; Importe, Backup-Archive und eigene Tippgeräusche werden strenger geprüft.")
                    "it" -> listOf("Le superfici dei candidati e delle emoji sono più affidabili; importazioni, archivi di backup e suoni personalizzati vengono convalidati con maggiore rigore.")
                    else -> listOf("Candidate and emoji surfaces are more reliable; imports and backup archives receive stricter validation.")
                },
                docsUrl = when (language) {
                    "de" -> "https://pastiera.eu/de/"
                    "it" -> "https://pastiera.eu/it/"
                    else -> "https://pastiera.eu/"
                }
            )
        }
    }
}

private fun normalizeReleaseNotesLanguage(languageTag: String): String {
    val language = languageTag
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
        .filter { it in 'a'..'z' }
    return language.ifBlank { "en" }
}

