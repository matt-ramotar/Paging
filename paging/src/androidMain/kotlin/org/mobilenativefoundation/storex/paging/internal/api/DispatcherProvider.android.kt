package org.mobilenativefoundation.storex.paging.internal.api

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual data object DispatcherProvider {
    actual val io: CoroutineDispatcher = Dispatchers.IO
}

@Composable
fun A(vm: ViewModel = viewModel()){}