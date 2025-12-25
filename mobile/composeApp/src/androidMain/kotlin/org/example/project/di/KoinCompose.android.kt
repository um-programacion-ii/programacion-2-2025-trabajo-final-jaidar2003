package org.example.project.di

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.get

@Composable
actual inline fun <reified T> koinInject(): T = get()
