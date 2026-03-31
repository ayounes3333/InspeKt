package com.inspekt.di

import com.inspekt.data.repository.CollectionRepository
import com.inspekt.data.repository.HttpClientRepository
import com.inspekt.presentation.viewmodel.CollectionsViewModel
import com.inspekt.presentation.viewmodel.RequestViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun appModule(storageDir: String) = module {
    single { HttpClientRepository() }
    single { CollectionRepository(storageDir) }

    viewModelOf(::RequestViewModel)
    viewModelOf(::CollectionsViewModel)
}
