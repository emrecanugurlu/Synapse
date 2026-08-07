package com.cnsoftware.synapse.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.domain.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabelsViewModel(
    private val repository: NoteRepository,
) : ViewModel() {

    val labels: StateFlow<List<Label>> = repository.observeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, color: NoteColor, icon: LabelIcon) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (labels.value.any { it.name.equals(trimmed, ignoreCase = true) }) return
        viewModelScope.launch { repository.createLabel(trimmed, color, icon) }
    }

    /** Ad, renk ve ikon tek seferde güncellenir. */
    fun update(label: Label, name: String, color: NoteColor, icon: LabelIcon) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        // Ad başka bir etikete aitse (UNIQUE kısıtı) güncelleme sessizce düşer.
        val clash = labels.value.any {
            it.id != label.id && it.name.equals(trimmed, ignoreCase = true)
        }
        if (clash) return
        if (trimmed == label.name && color == label.color && icon == label.icon) return
        viewModelScope.launch { repository.updateLabel(label.id, trimmed, color, icon) }
    }

    fun delete(label: Label) {
        viewModelScope.launch { repository.deleteLabel(label.id) }
    }

    /** Kaynak etiketin notları hedefe geçer, kaynak silinir. */
    fun merge(source: Label, target: Label) {
        viewModelScope.launch { repository.mergeLabels(source.id, target.id) }
    }
}
