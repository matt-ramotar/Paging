package app.feed.androidx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.feed.common.AppTheme
import app.feed.common.CommonAppScaffold
import app.feed.common.HomeTab
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.*
import com.slack.circuit.runtime.screen.Screen

class MainActivity : ComponentActivity() {

    private val pager = TimelineAndroidxPagerFactory().create()

    private val circuit: Circuit by lazy {
        Circuit.Builder()
            .addPresenterFactory(AppPresenterFactory(pager))
            .addUiFactory(AppUiFactory)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CircuitCompositionLocals(circuit) {
                CommonAppScaffold()
            }
        }
    }
}