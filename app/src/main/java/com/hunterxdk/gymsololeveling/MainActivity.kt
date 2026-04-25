package com.hunterxdk.gymsololeveling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hunterxdk.gymsololeveling.core.navigation.GymLevelsNavGraph
import com.hunterxdk.gymsololeveling.core.ui.OfflineBanner
import com.hunterxdk.gymsololeveling.ui.theme.GymLevelsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymLevelsTheme {
                Column(Modifier.fillMaxSize()) {
                    OfflineBanner()
                    GymLevelsNavGraph()
                }
            }
        }
    }
}
