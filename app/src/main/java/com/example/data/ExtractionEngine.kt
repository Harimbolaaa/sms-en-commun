package com.example.data

import com.example.data.db.RegexRuleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Modèle de préréglage Regex
 */
data class RegexPreset(
    val id: String,
    val title: String,
    val pattern: String,
    val description: String,
    val sampleMessage: String
)

/**
 * Résultat d'extraction d'un modèle unitaire
 */
data class SingleExtractionResult(
    val ruleId: String,
    val ruleTitle: String,
    val isMatch: Boolean,
    val extractedValues: List<String>,
    val pattern: String
)

/**
 * Résultat du traitement d'extraction Regex multi-règles (supporte 1 ou plusieurs règles groupées cochées)
 */
data class ExtractionResult(
    val isMatch: Boolean,
    val primaryValue: String?,
    val allExtractedValues: List<String> = emptyList(),
    val matchedRules: List<String> = emptyList(),
    val ruleBreakdown: Map<String, List<String>> = emptyMap(),
    val matchedPattern: String? = null,
    val errorMessage: String? = null
)

/**
 * Moteur d'évaluation des règles de filtrage d'expéditeur et d'extraction Regex multi-modèles groupés.
 */
object ExtractionEngine {

    /**
     * Vérifie si l'expéditeur fait partie de la liste autorisée.
     */
    fun isSenderAllowed(
        sender: String,
        allowedSendersStr: String,
        isFilterEnabled: Boolean
    ): Boolean {
        if (!isFilterEnabled || allowedSendersStr.isBlank()) {
            return true
        }

        val cleanSender = sender.trim().lowercase()
        val allowedList = allowedSendersStr
            .split(",", ";", "\n")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (allowedList.isEmpty()) return true

        return allowedList.any { pattern ->
            cleanSender == pattern || 
            cleanSender.contains(pattern) || 
            pattern.contains(cleanSender)
        }
    }

    /**
     * Applique TOUS les modèles cochés sur le message et regroupe automatiquement
     * toutes les données extraites sans que l'utilisateur ait besoin de créer une formule complexe !
     */
    fun applyMultipleRules(
        text: String,
        checkedRules: List<RegexRuleEntity>,
        isExtractionEnabled: Boolean
    ): ExtractionResult {
        if (!isExtractionEnabled || checkedRules.isEmpty()) {
            return ExtractionResult(
                isMatch = true,
                primaryValue = text,
                allExtractedValues = listOf(text),
                matchedRules = listOf("Brut (Pas de filtre)"),
                ruleBreakdown = mapOf("Texte Brut" to listOf(text)),
                matchedPattern = "AUCUN"
            )
        }

        val allExtractedList = mutableListOf<String>()
        val matchedRuleTitles = mutableListOf<String>()
        val breakdown = mutableMapOf<String, List<String>>()
        val patternsUsed = mutableListOf<String>()

        for (rule in checkedRules) {
            val singleResult = extractFromSinglePattern(text, rule.pattern, rule.id, rule.title)
            if (singleResult.isMatch && singleResult.extractedValues.isNotEmpty()) {
                allExtractedList.addAll(singleResult.extractedValues)
                matchedRuleTitles.add(rule.title)
                breakdown[rule.title] = singleResult.extractedValues
                patternsUsed.add(rule.pattern)
            }
        }

        val distinctExtracted = allExtractedList.distinct()

        return if (distinctExtracted.isNotEmpty()) {
            ExtractionResult(
                isMatch = true,
                primaryValue = distinctExtracted.first(),
                allExtractedValues = distinctExtracted,
                matchedRules = matchedRuleTitles,
                ruleBreakdown = breakdown,
                matchedPattern = patternsUsed.joinToString(" | ")
            )
        } else {
            ExtractionResult(
                isMatch = false,
                primaryValue = null,
                allExtractedValues = emptyList(),
                matchedRules = emptyList(),
                ruleBreakdown = emptyMap(),
                matchedPattern = checkedRules.joinToString(" | ") { it.pattern },
                errorMessage = "Aucune correspondance trouvée pour les ${checkedRules.size} modèle(s) coché(s)"
            )
        }
    }

    /**
     * Applique une formule regex unitaire
     */
    fun applyRegex(
        text: String,
        patternStr: String,
        isRegexEnabled: Boolean
    ): ExtractionResult {
        if (!isRegexEnabled || patternStr.isBlank()) {
            return ExtractionResult(
                isMatch = true,
                primaryValue = text,
                allExtractedValues = listOf(text),
                matchedRules = listOf("Brut"),
                matchedPattern = "BRUT"
            )
        }

        val res = extractFromSinglePattern(text, patternStr, "custom", "Règle active")
        return if (res.isMatch) {
            ExtractionResult(
                isMatch = true,
                primaryValue = res.extractedValues.firstOrNull(),
                allExtractedValues = res.extractedValues,
                matchedRules = listOf("Règle active"),
                ruleBreakdown = mapOf("Règle active" to res.extractedValues),
                matchedPattern = patternStr
            )
        } else {
            ExtractionResult(
                isMatch = false,
                primaryValue = null,
                allExtractedValues = emptyList(),
                matchedPattern = patternStr,
                errorMessage = "Aucune correspondance trouvée"
            )
        }
    }

    private fun extractFromSinglePattern(
        text: String,
        patternStr: String,
        ruleId: String,
        ruleTitle: String
    ): SingleExtractionResult {
        return try {
            val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)
            val matcher = pattern.matcher(text)
            val values = mutableListOf<String>()

            while (matcher.find()) {
                val groupCount = matcher.groupCount()
                if (groupCount > 0) {
                    for (i in 1..groupCount) {
                        val groupVal = matcher.group(i)?.trim()
                        if (!groupVal.isNullOrEmpty()) {
                            values.add(groupVal)
                        }
                    }
                } else {
                    val fullMatch = matcher.group(0)?.trim()
                    if (!fullMatch.isNullOrEmpty()) {
                        values.add(fullMatch)
                    }
                }
            }

            SingleExtractionResult(
                ruleId = ruleId,
                ruleTitle = ruleTitle,
                isMatch = values.isNotEmpty(),
                extractedValues = values.distinct(),
                pattern = patternStr
            )
        } catch (e: Exception) {
            SingleExtractionResult(
                ruleId = ruleId,
                ruleTitle = ruleTitle,
                isMatch = false,
                extractedValues = emptyList(),
                pattern = patternStr
            )
        }
    }

    /**
     * Construit le JSON final conforme au format Appwrite Database Collection Document.
     * Structure requise :
     * {
     *   "documentId": "unique()",
     *   "data": {
     *     "sender": "YAS",
     *     "rawMessage": "...",
     *     "parsedAmount": 500,
     *     "referenceId": "5661921553",
     *     "operator": "MVOLA",
     *     "createdAtUnix": 1787214594767
     *   }
     * }
     */
    fun buildCustomJsonPayload(
        source: String,
        originalContent: String,
        extractionResult: ExtractionResult,
        sendOnlyExtracted: Boolean = false,
        customFieldKey: String = "content",
        clientPhone: String? = null,
        clientUserId: String? = null
    ): String {
        val root = JSONObject()
        val dataObj = JSONObject()
        val timestamp = System.currentTimeMillis()

        // 1. Détection des champs clés à partir des modèles extraits
        var extractedPhone: String? = null
        var extractedRef: String? = null
        var extractedAmountStr: String? = null
        var extractedSenderName: String? = null
        var extractedBalance: String? = null

        extractionResult.ruleBreakdown.forEach { (modelName, vals) ->
            val lowerModel = modelName.lowercase()
            val firstVal = vals.firstOrNull()
            if (firstVal != null) {
                if (lowerModel.contains("téléphone") || lowerModel.contains("telephone") || lowerModel.contains("phone") || lowerModel.contains("03")) {
                    extractedPhone = firstVal
                } else if (lowerModel.contains("référence") || lowerModel.contains("reference") || lowerModel.contains("ref") || lowerModel.contains("transaction")) {
                    extractedRef = firstVal
                } else if (lowerModel.contains("montant") || lowerModel.contains("crédit") || lowerModel.contains("credit") || lowerModel.contains("amount")) {
                    extractedAmountStr = firstVal
                } else if (lowerModel.contains("nom") || lowerModel.contains("contact") || lowerModel.contains("expéditeur") || lowerModel.contains("expediteur")) {
                    extractedSenderName = firstVal
                } else if (lowerModel.contains("solde") || lowerModel.contains("balance")) {
                    extractedBalance = firstVal
                }
            }
        }

        // Fallbacks détection Regex si non identifiés via les titres
        if (extractedPhone == null) {
            val phoneMatcher = Pattern.compile("((?:\\+261|0)3[23478][0-9]{7})").matcher(originalContent)
            if (phoneMatcher.find()) {
                extractedPhone = phoneMatcher.group(1) ?: phoneMatcher.group(0)
            }
        }
        if (extractedRef == null) {
            val refMatcher = Pattern.compile("(?i)(?:Ref|Reference|Référence|TransID|ID)\\s*[:\\-\\s]\\s*([A-Za-z0-9]+)").matcher(originalContent)
            if (refMatcher.find()) {
                extractedRef = refMatcher.group(1)
            }
        }
        if (extractedAmountStr == null) {
            val amountMatcher = Pattern.compile("(?:recu|reçu|montant|crédit|credit)\\s*:\\s*([0-9]+(?:\\s*[0-9]+)?\\s*(?:Ar|Ariary|MGA))", Pattern.CASE_INSENSITIVE).matcher(originalContent)
            if (amountMatcher.find()) {
                extractedAmountStr = amountMatcher.group(1)
            }
        }

        // 2. Parsing numérique du montant (ex: 500 pour "500 Ar")
        val parsedAmountNumber: Long = if (extractedAmountStr != null) {
            val digits = extractedAmountStr!!.replace(Regex("[^0-9]"), "")
            digits.toLongOrNull() ?: 0L
        } else {
            val genericAmountMatcher = Pattern.compile("([0-9]+(?:\\s*[0-9]+)?)\\s*(?:Ar|Ariary|MGA)", Pattern.CASE_INSENSITIVE).matcher(originalContent)
            if (genericAmountMatcher.find()) {
                genericAmountMatcher.group(1).replace(" ", "").toLongOrNull() ?: 0L
            } else {
                0L
            }
        }

        // 3. Détection intelligente de l'opérateur (MVOLA, AIRTEL, ORANGE, YAS)
        val upperSource = source.uppercase()
        val upperContent = originalContent.uppercase()
        val detectedOperator: String = when {
            upperSource.contains("MVOLA") || upperContent.contains("MVOLA") -> "MVOLA"
            upperSource.contains("YAS") || upperContent.contains("CREDIT YAS") || upperContent.contains("SOLDE YAS") -> "MVOLA"
            upperSource.contains("TELMA") || upperContent.contains("TELMA") -> "MVOLA"
            upperSource.contains("ORANGE") || upperContent.contains("ORANGE MONEY") -> "ORANGE"
            upperSource.contains("AIRTEL") || upperContent.contains("AIRTEL MONEY") -> "AIRTEL"
            upperSource.startsWith("034") || upperSource.startsWith("038") || upperSource.startsWith("+26134") || upperSource.startsWith("+26138") -> "MVOLA"
            upperSource.startsWith("032") || upperSource.startsWith("+26132") -> "ORANGE"
            upperSource.startsWith("033") || upperSource.startsWith("+26133") -> "AIRTEL"
            else -> if (upperSource.isNotBlank()) upperSource else "MVOLA"
        }

        // 4. Construction de l'objet Appwrite
        root.put("documentId", "unique()")

        dataObj.put("sender", if (source.isNotBlank()) source else "YAS")
        dataObj.put("rawMessage", originalContent)
        dataObj.put("parsedAmount", parsedAmountNumber)
        dataObj.put("referenceId", extractedRef ?: (extractedPhone ?: ""))
        dataObj.put("operator", detectedOperator)
        dataObj.put("direction", "RECEIVED")
        dataObj.put("createdAtUnix", timestamp)
        if (!clientPhone.isNullOrBlank()) {
            dataObj.put("clientPhone", clientPhone)
        }
        if (!clientUserId.isNullOrBlank()) {
            dataObj.put("clientUserId", clientUserId)
        }

        root.put("data", dataObj)

        return root.toString(2)
    }
}
