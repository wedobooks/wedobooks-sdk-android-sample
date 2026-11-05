package io.wedobooks.sdk.library.wedobookssdksampleapp.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.wedobooks.sdk.models.WdbHighlight
import io.wedobooks.sdk.models.WdbHighlightColor
import io.wedobooks.sdk.models.WdbHighlightCallback
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.highlightsDataStore by preferencesDataStore(name = "sample_highlights")

/**
 * Keeps a copy of reader highlights on the device, as one JSON array per book,
 * and has the reader show that copy instead of the SDK's stored highlights.
 */
class LocalHighlightCallback(context: Context) : WdbHighlightCallback {
    private val dataStore = context.applicationContext.highlightsDataStore

    override suspend fun onLoadHighlights(materialId: String): List<WdbHighlight> =
        dataStore.data.first()[keyFor(materialId)]?.let(::decode).orEmpty()

    override suspend fun onSave(highlight: WdbHighlight) {
        update(highlight.materialId) { highlights ->
            highlights.filterNot { it.id == highlight.id } + highlight
        }
    }

    override suspend fun onDelete(highlight: WdbHighlight) {
        update(highlight.materialId) { highlights ->
            highlights.filterNot { it.id == highlight.id }
        }
    }

    private suspend fun update(
        materialId: String,
        transform: (List<WdbHighlight>) -> List<WdbHighlight>,
    ) {
        val key = keyFor(materialId)
        dataStore.edit { preferences ->
            preferences[key] = encode(transform(preferences[key]?.let(::decode).orEmpty()))
        }
    }

    private fun keyFor(materialId: String) = stringPreferencesKey("highlights_$materialId")
}

private fun encode(highlights: List<WdbHighlight>): String = JSONArray().apply {
    highlights.forEach { highlight ->
        put(
            JSONObject()
                .put("id", highlight.id)
                .put("materialId", highlight.materialId)
                .put("cfi", highlight.cfi)
                .put("selectionText", highlight.selectionText)
                .put("color", highlight.color.name)
                .put("note", highlight.note)
                .put("sectionIndex", highlight.sectionIndex)
        )
    }
}.toString()

private fun decode(json: String): List<WdbHighlight> {
    val array = JSONArray(json)
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        WdbHighlight(
            id = item.getString("id"),
            materialId = item.getString("materialId"),
            cfi = item.getString("cfi"),
            selectionText = item.getString("selectionText"),
            color = WdbHighlightColor.valueOf(item.getString("color")),
            note = if (item.isNull("note")) null else item.getString("note"),
            sectionIndex = item.getInt("sectionIndex"),
        )
    }
}
