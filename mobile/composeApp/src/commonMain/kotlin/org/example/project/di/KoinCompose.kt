package org.example.project.di

import androidx.compose.runtime.Composable

// Multiplatform Composable helper to get Koin instances from common code.
@Composable
expect inline fun <reified T> koinInject(): T
