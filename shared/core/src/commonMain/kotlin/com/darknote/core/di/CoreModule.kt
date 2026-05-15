package com.darknote.core.di

import org.koin.dsl.module

val coreModule = module {
    // Core domain models and interfaces only
    // Implementations will be provided by platform-specific modules
}
