package com.blecenter.blecenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blecenter.blecenter.ui.theme.BleCenterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val bleManager = BleManager(applicationContext)

        setContent {
            BleCenterTheme {
                App(bleManager)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BleCenterTheme {
        // Preview requires a real BleManager instance
        // Note: This preview may not work without a real context
        // Consider removing this preview or providing a mock implementation
    }
}