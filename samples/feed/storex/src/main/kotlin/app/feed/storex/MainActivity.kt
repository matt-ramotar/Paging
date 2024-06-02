package app.feed.storex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.feed.common.AppTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.*

class MainActivity : ComponentActivity() {

    private val circuit: Circuit by lazy {
        Circuit.Builder()
            .addPresenterFactory(AppPresenterFactory)
            .addUiFactory(AppUiFactory)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val homeTab = remember { HomeTab }

            val backStack = rememberSaveableBackStack(root = homeTab)
            val navigator = rememberCircuitNavigator(backStack)

            CircuitCompositionLocals(circuit) {

                AppTheme {
                    Scaffold(
                        bottomBar = {
                            BottomAppBar {
                                IconButton(onClick = {
                                    navigator.goTo(HomeTab)
                                }) {
                                    Icon(Icons.TwoTone.Home, "Home")
                                }
                                IconButton(onClick = {
                                    navigator.goTo(AccountTab)
                                }) {
                                    Icon(Icons.TwoTone.Person, "Person")
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavigableCircuitContent(
                            navigator,
                            backStack,
                            modifier = Modifier.padding(innerPadding).background(MaterialTheme.colorScheme.background),
                            decoration = NavigatorDefaults.EmptyDecoration
                        )
                    }
                }
            }
        }
    }
}