package com.blecenter.blecenter

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    val bleManager = BleManager()
    App(bleManager)
}