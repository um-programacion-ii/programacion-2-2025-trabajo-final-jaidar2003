package org.example.project.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.example.project.data.EventoRepository
import org.example.project.data.VentaRepository
import org.example.project.network.createHttpClient
import org.example.project.data.AuthRepository
import org.example.project.data.local.TokenManager

expect val platformModule: Module

fun initKoin(appDeclaration: (KoinApplication.() -> Unit) = {}) {
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }
}

val commonModule = module {
    single { TokenManager(get()) }
    single { createHttpClient(get()) }
    single { EventoRepository(get()) }
    single { VentaRepository(get()) }
    single { AuthRepository(get(), get()) }
}
