package org.example.project.di

import androidx.compose.runtime.Composable

@Composable
actual inline fun <reified T> koinInject(): T {
    throw NotImplementedError("koinInject is not implemented on iOS yet")
}
