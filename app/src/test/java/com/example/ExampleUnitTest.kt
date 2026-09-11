package com.example

import com.example.data.ExtractionEngine
import com.example.data.db.QueueItemEntity
import com.example.data.db.RegexRuleEntity
import com.example.network.NetworkDispatcher
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests unitaires avec Robolectric validant :
 * 1. L'extraction multi-modèles groupés
 * 2. Le filtrage d'expéditeur
 * 3. La garantie d'envoi du SMS source complet
 * 4. La gestion des En-têtes HTTP de sécurité et de la file d'attente (Queue Retry)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    private val sampleRules = listOf(
        RegexRuleEntity(
            id = "rule_amount",
            title = "Montant & Devise",
            pattern = "([0-9]+[.,]?[0-9]*\\s*(?:€|\\$|EUR|USD))",
            description = "Montant",
            sampleMessage = "89.50 €",
            isChecked = true
        ),
        RegexRuleEntity(
            id = "rule_otp",
            title = "Code OTP",
            pattern = "(?:code|otp|secret)[^0-9]*([0-9]{4,8})",
            description = "Code OTP",
            sampleMessage = "code 493021",
            isChecked = true
        ),
        RegexRuleEntity(
            id = "rule_ref",
            title = "Commande REF",
            pattern = "(?:ref|cmd|commande)[^A-Z0-9]*([A-Z0-9-]{4,16})",
            description = "Référence",
            sampleMessage = "REF-98421",
            isChecked = true
        )
    )

    @Test
    fun testMultiModelGrouping_extractsAllCheckedDataSimultaneously() {
        val rawMessage = "Achat de 89.50 € validé. Votre code secret est 493021 pour la commande REF-98421."
        val result = ExtractionEngine.applyMultipleRules(
            text = rawMessage,
            checkedRules = sampleRules,
            isExtractionEnabled = true
        )

        assertTrue("Le résultat doit matcher", result.isMatch)
        assertEquals(3, result.allExtractedValues.size)
        assertTrue(result.allExtractedValues.contains("89.50 €"))
        assertTrue(result.allExtractedValues.contains("493021"))
        assertTrue(result.allExtractedValues.contains("REF-98421"))
        assertEquals(3, result.matchedRules.size)
    }

    @Test
    fun testSenderFiltering_correctlyBlocksOrAllows() {
        val allowedSenders = "BANQUE, +33612345678, ORANGE"

        assertTrue(ExtractionEngine.isSenderAllowed("BANQUE", allowedSenders, true))
        assertTrue(ExtractionEngine.isSenderAllowed("banque_notif", allowedSenders, true))
        assertTrue(ExtractionEngine.isSenderAllowed("+33612345678", allowedSenders, true))
        assertFalse(ExtractionEngine.isSenderAllowed("SPAMMER_999", allowedSenders, true))
        
        // Si filtre désactivé, tout passe
        assertTrue(ExtractionEngine.isSenderAllowed("SPAMMER_999", allowedSenders, false))
    }

    @Test
    fun testJsonPayload_guaranteesSourceAndOriginalSms() {
        val source = "BANQUE_POPULAIRE"
        val originalSms = "Achat de 89.50 € validé. Code secret: 493021"

        val multiResult = ExtractionEngine.applyMultipleRules(
            text = originalSms,
            checkedRules = sampleRules,
            isExtractionEnabled = true
        )

        val jsonString = ExtractionEngine.buildCustomJsonPayload(
            source = source,
            originalContent = originalSms,
            extractionResult = multiResult,
            sendOnlyExtracted = false
        )

        val json = JSONObject(jsonString)
        assertEquals("unique()", json.getString("documentId"))
        val data = json.getJSONObject("data")
        assertEquals(source, data.getString("sender"))
        assertEquals(originalSms, data.getString("rawMessage"))
        assertTrue(data.has("createdAtUnix"))
    }

    @Test
    fun testMalagasySmsExtraction_correctlyExtractsAllFields() {
        val sampleSms = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553"
        val rules = com.example.data.RegexRuleRepository.DEFAULT_RULES.filter { it.isChecked }
        
        val result = ExtractionEngine.applyMultipleRules(
            text = sampleSms,
            checkedRules = rules,
            isExtractionEnabled = true
        )

        assertTrue(result.isMatch)
        assertTrue("Devrait extraire le montant 500 Ar", result.allExtractedValues.contains("500 Ar"))
        assertTrue("Devrait extraire le numéro 0386135942", result.allExtractedValues.contains("0386135942"))
        assertTrue("Devrait extraire la référence 5661921553", result.allExtractedValues.contains("5661921553"))
        assertTrue("Devrait extraire l'expéditeur Mioranirina", result.allExtractedValues.contains("Mioranirina"))
        assertTrue("Devrait extraire le solde 504 Ar", result.allExtractedValues.contains("504 Ar"))

        val payload = ExtractionEngine.buildCustomJsonPayload(
            source = "YAS",
            originalContent = sampleSms,
            extractionResult = result
        )
        val json = JSONObject(payload)
        assertEquals("unique()", json.getString("documentId"))
        
        val data = json.getJSONObject("data")
        assertEquals("YAS", data.getString("sender"))
        assertEquals(sampleSms, data.getString("rawMessage"))
        assertEquals(500L, data.getLong("parsedAmount"))
        assertEquals("5661921553", data.getString("referenceId"))
        assertEquals("MVOLA", data.getString("operator"))
        assertTrue(data.getLong("createdAtUnix") > 0L)
    }

    @Test
    fun testQueueItemEntity_creationAndFields() {
        val queueItem = QueueItemEntity(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            targetUrl = "https://example.com/api/webhook",
            payloadJson = "{\"source\":\"BANQUE\",\"content\":\"Code 1234\"}",
            source = "BANQUE",
            contentLength = 9,
            status = "FAILED",
            attemptCount = 1,
            lastError = "HTTP 500: Internal Server Error",
            headerName = "X-SMS-Gateway-Token",
            headerValue = "MonSuperSecret123!"
        )

        assertEquals("FAILED", queueItem.status)
        assertEquals("X-SMS-Gateway-Token", queueItem.headerName)
        assertEquals("MonSuperSecret123!", queueItem.headerValue)
        assertEquals(1, queueItem.attemptCount)
        assertEquals("HTTP 500: Internal Server Error", queueItem.lastError)
    }
}
