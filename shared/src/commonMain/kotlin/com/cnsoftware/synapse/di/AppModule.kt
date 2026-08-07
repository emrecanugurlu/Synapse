package com.cnsoftware.synapse.di

import com.cnsoftware.synapse.data.NoteRepositoryImpl
import com.cnsoftware.synapse.db.DatabaseDriverFactory
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.ui.archive.ArchiveViewModel
import com.cnsoftware.synapse.ui.editor.NoteEditorViewModel
import com.cnsoftware.synapse.ui.labels.LabelsViewModel
import com.cnsoftware.synapse.ui.notelist.NoteListViewModel
import com.cnsoftware.synapse.ui.trash.TrashViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    single { NoteDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<NoteRepository> { NoteRepositoryImpl(get()) }
    single { AppSettings(get()) }

    viewModelOf(::NoteListViewModel)
    viewModelOf(::ArchiveViewModel)
    viewModelOf(::TrashViewModel)
    viewModelOf(::LabelsViewModel)
    viewModel { params ->
        NoteEditorViewModel(
            noteId = params.getOrNull<String>(),
            repository = get(),
            reminderScheduler = get(),
            shareService = get(),
            settings = get(),
            startAsChecklist = params.getOrNull<Boolean>() ?: false,
        )
    }
}

/**
 * `platformModule` her platformdan (Android/iOS) DatabaseDriverFactory'yi
 * sağlayan modülü geçirir. Böylece bu dosya tamamen platform bağımsız kalır.
 */
fun initKoin(platformModule: Module, appDeclaration: (KoinApplication.() -> Unit)? = null) {
    startKoin {
        appDeclaration?.invoke(this)
        modules(platformModule, commonModule)
    }
}
