package com.anafro.kquake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.anafro.kquake.ui.MonitorScreen
import com.anafro.kquake.ui.theme.KQuakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KQuakeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MonitorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
