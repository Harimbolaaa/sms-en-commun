package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

// Palette de couleurs pour la coloration syntaxique JSON
private val JsonKeyColor = Color(0xFFC084FC) // Mauve / Violet clair pour les clés
private val JsonStringColor = Color(0xFF4ADE80) // Vert menthe / Émeraude pour les chaînes
private val JsonNumberColor = Color(0xFFFBBF24) // Ambre / Or pour les nombres
private val JsonBooleanColor = Color(0xFFF472B6) // Rose pour les booléens
private val JsonNullColor = Color(0xFFFB7185) // Rouge corail pour null
private val JsonPunctuationColor = Color(0xFF94A3B8) // Gris ardoise pour { } [ ] : ,
private val JsonBracketsColor = Color(0xFF38BDF8) // Bleu ciel pour les accolades/crochets
private val DarkCodeBackground = Color(0xFF0F131C)
private val DarkCodeBorder = Color(0xFF1F2637)

/**
 * Formate une chaîne JSON brute en JSON indenté proprement (2 espaces).
 */
fun formatJsonString(rawJson: String): String {
    val trimmed = rawJson.trim()
    return try {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> {
                // Tenter d'extraire un bloc JSON embarqué
                val extracted = extractJsonFromText(trimmed)
                if (extracted != null) {
                    if (extracted.startsWith("{")) JSONObject(extracted).toString(2)
                    else JSONArray(extracted).toString(2)
                } else {
                    rawJson
                }
            }
        }
    } catch (_: Exception) {
        rawJson
    }
}

/**
 * Tente d'extraire un sous-bloc JSON contenu dans un texte (ex: logs avec préfixe HTTP).
 */
fun extractJsonFromText(text: String): String? {
    val trimmed = text.trim()
    val firstBrace = trimmed.indexOf('{')
    val lastBrace = trimmed.lastIndexOf('}')
    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
        val candidate = trimmed.substring(firstBrace, lastBrace + 1)
        try {
            JSONObject(candidate)
            return candidate
        } catch (_: Exception) {}
    }
    val firstBracket = trimmed.indexOf('[')
    val lastBracket = trimmed.lastIndexOf(']')
    if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
        val candidate = trimmed.substring(firstBracket, lastBracket + 1)
        try {
            JSONArray(candidate)
            return candidate
        } catch (_: Exception) {}
    }
    return null
}

/**
 * Vérifie si le texte est ou contient un format JSON valide.
 */
fun isJsonFormat(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        return try {
            if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONArray(trimmed)
            true
        } catch (_: Exception) {
            false
        }
    }
    return extractJsonFromText(trimmed) != null
}

/**
 * Construit un AnnotatedString avec coloration syntaxique complète du JSON.
 */
fun buildHighlightedJson(jsonString: String): AnnotatedString {
    val formatted = formatJsonString(jsonString)
    return buildAnnotatedString {
        var i = 0
        val len = formatted.length
        var inString = false
        var stringBuffer = StringBuilder()
        var stringStartIndex = 0
        var isKey = false

        while (i < len) {
            val c = formatted[i]

            if (inString) {
                stringBuffer.append(c)
                if (c == '\\' && i + 1 < len) {
                    // Caractère d'échappement
                    i++
                    stringBuffer.append(formatted[i])
                } else if (c == '"') {
                    inString = false
                    val fullStr = stringBuffer.toString()
                    
                    // Vérifier si le prochain caractère non-espace est ':' (ce qui indique une clé JSON)
                    var j = i + 1
                    var nextNonSpace: Char? = null
                    while (j < len) {
                        val nextC = formatted[j]
                        if (!nextC.isWhitespace()) {
                            nextNonSpace = nextC
                            break
                        }
                        j++
                    }
                    isKey = (nextNonSpace == ':')

                    if (isKey) {
                        withStyle(SpanStyle(color = JsonKeyColor, fontWeight = FontWeight.Bold)) {
                            append(fullStr)
                        }
                    } else {
                        withStyle(SpanStyle(color = JsonStringColor)) {
                            append(fullStr)
                        }
                    }
                    stringBuffer.setLength(0)
                }
                i++
                continue
            }

            if (c == '"') {
                inString = true
                stringStartIndex = i
                stringBuffer.setLength(0)
                stringBuffer.append(c)
                i++
                continue
            }

            when (c) {
                '{', '}' -> {
                    withStyle(SpanStyle(color = JsonBracketsColor, fontWeight = FontWeight.Bold)) {
                        append(c.toString())
                    }
                }
                '[', ']' -> {
                    withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
                        append(c.toString())
                    }
                }
                ':', ',' -> {
                    withStyle(SpanStyle(color = JsonPunctuationColor, fontWeight = FontWeight.SemiBold)) {
                        append(c.toString())
                    }
                }
                else -> {
                    if (c.isDigit() || c == '-' || c == '.') {
                        // Numéro
                        val numBuffer = StringBuilder()
                        while (i < len && (formatted[i].isDigit() || formatted[i] == '.' || formatted[i] == '-' || formatted[i] == 'e' || formatted[i] == 'E' || formatted[i] == '+')) {
                            numBuffer.append(formatted[i])
                            i++
                        }
                        withStyle(SpanStyle(color = JsonNumberColor, fontWeight = FontWeight.Bold)) {
                            append(numBuffer.toString())
                        }
                        continue
                    } else if (formatted.startsWith("true", i)) {
                        withStyle(SpanStyle(color = JsonBooleanColor, fontWeight = FontWeight.Bold)) {
                            append("true")
                        }
                        i += 4
                        continue
                    } else if (formatted.startsWith("false", i)) {
                        withStyle(SpanStyle(color = JsonBooleanColor, fontWeight = FontWeight.Bold)) {
                            append("false")
                        }
                        i += 5
                        continue
                    } else if (formatted.startsWith("null", i)) {
                        withStyle(SpanStyle(color = JsonNullColor, fontWeight = FontWeight.Bold)) {
                            append("null")
                        }
                        i += 4
                        continue
                    } else {
                        append(c.toString())
                    }
                }
            }
            i++
        }
    }
}

/**
 * Modèle de données pour la vue Arbre / Cartes de champs structurés
 */
data class JsonFieldItem(
    val key: String,
    val value: String,
    val type: String,
    val level: Int = 0
)

/**
 * Extrait les champs clés d'un JSON pour la vue Arbre / Tableau
 */
fun parseJsonToTree(jsonStr: String): List<JsonFieldItem> {
    val items = mutableListOf<JsonFieldItem>()
    try {
        var trimmed = jsonStr.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            val extracted = extractJsonFromText(trimmed)
            if (extracted != null) {
                trimmed = extracted
            }
        }
        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            extractJsonObjectFields(root, "", items, 0)
        } else if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                val item = array.get(i)
                if (item is JSONObject) {
                    extractJsonObjectFields(item, "[$i]", items, 0)
                } else {
                    items.add(JsonFieldItem("[$i]", item.toString(), item.javaClass.simpleName, 0))
                }
            }
        }
    } catch (_: Exception) {
        // En cas d'erreur de parsing
    }
    return items
}

private fun extractJsonObjectFields(obj: JSONObject, prefix: String, list: MutableList<JsonFieldItem>, level: Int) {
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
        val value = obj.get(key)
        when (value) {
            is JSONObject -> {
                list.add(JsonFieldItem(fullKey, "{ ... }", "Object", level))
                extractJsonObjectFields(value, fullKey, list, level + 1)
            }
            is JSONArray -> {
                list.add(JsonFieldItem(fullKey, "[ ${value.length()} éléments ]", "Array", level))
                for (i in 0 until value.length()) {
                    val elem = value.get(i)
                    if (elem is JSONObject) {
                        extractJsonObjectFields(elem, "$fullKey[$i]", list, level + 1)
                    } else {
                        list.add(JsonFieldItem("$fullKey[$i]", elem.toString(), elem.javaClass.simpleName, level + 1))
                    }
                }
            }
            else -> {
                val typeName = when (value) {
                    is Number -> "Number"
                    is Boolean -> "Boolean"
                    is String -> "String"
                    JSONObject.NULL -> "null"
                    else -> value.javaClass.simpleName
                }
                list.add(JsonFieldItem(fullKey, value.toString(), typeName, level))
            }
        }
    }
}

/**
 * Composant Pretty JSON Viewer complet et interactif
 * avec onglets : "Pretty JSON Coloré", "Arbre de Champs", "Texte Brut"
 */
@Composable
fun PrettyJsonViewer(
    jsonString: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    badgeText: String = "JSON PRETTY",
    defaultViewMode: Int = 0 // 0: Pretty Coloré, 1: Arbre / Champs, 2: Brut
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var viewMode by rememberSaveable(jsonString) { mutableIntStateOf(defaultViewMode) }
    
    val formattedJson = remember(jsonString) { formatJsonString(jsonString) }
    val highlightedJson = remember(formattedJson) { buildHighlightedJson(formattedJson) }
    val treeItems = remember(jsonString) { parseJsonToTree(jsonString) }
    val isValidJson = remember(jsonString) { isJsonFormat(jsonString) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkCodeBackground,
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCodeBorder))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141926))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = badgeText,
                                color = Color(0xFFC084FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (title != null) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                // Bouton Copier
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(formattedJson))
                        Toast.makeText(context, "JSON formatté copié !", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copier",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copier", fontSize = 11.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                }
            }

            // Barre d'onglets de visualisation
            if (isValidJson) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C101A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabPillButton(
                        label = "✨ Pretty JSON",
                        selected = viewMode == 0,
                        onClick = { viewMode = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPillButton(
                        label = "🌳 Arbre Clés",
                        selected = viewMode == 1,
                        onClick = { viewMode = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPillButton(
                        label = "📄 Brut",
                        selected = viewMode == 2,
                        onClick = { viewMode = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = DarkCodeBorder, thickness = 1.dp)

            // Contenu selon le mode
            Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                when (viewMode) {
                    0 -> {
                        // Vue Colorée / Pretty Syntax
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = highlightedJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    1 -> {
                        // Vue Arbre / Tableau de champs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (treeItems.isEmpty()) {
                                Text(
                                    text = "Aucun champ analysable.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                treeItems.forEach { item ->
                                    val typeColor = when (item.type) {
                                        "Number" -> JsonNumberColor
                                        "Boolean" -> JsonBooleanColor
                                        "String" -> JsonStringColor
                                        "Object", "Array" -> JsonBracketsColor
                                        else -> Color(0xFF94A3B8)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = (item.level * 12).dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF141926),
                                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF242C40)))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = item.key,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = JsonKeyColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(typeColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = item.type,
                                                        fontSize = 9.sp,
                                                        color = typeColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = item.value,
                                                fontSize = 11.sp,
                                                color = if (item.type == "Object" || item.type == "Array") Color(0xFF94A3B8) else Color(0xFFF1F5F9),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Vue Texte Brut
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = formattedJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color(0xFFC084FC) else Color(0xFF242C40),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFFF3E8FF) else Color(0xFF94A3B8)
        )
    }
}
