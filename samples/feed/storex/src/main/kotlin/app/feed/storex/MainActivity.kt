package app.feed.storex

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
import app.feed.common.TimelineAndroidxPagingSource
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.*
import com.slack.circuit.runtime.screen.Screen

class MainActivity : ComponentActivity() {


    private val pager = TimelinePagerFactory().create(
        androidxPagingSourceFactory = { api ->
            TimelineAndroidxPagingSource(api, 20)
        }
    )

    private val appPresenterFactory = AppPresenterFactory(pager)

    private val circuit: Circuit by lazy {
        Circuit.Builder()
            .addPresenterFactory(appPresenterFactory)
            .addUiFactory(AppUiFactory)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val homeTab = remember { HomeTab }

            val backStack = rememberSaveableBackStack(root = homeTab)
            val navigator = rememberCircuitNavigator(backStack)
            var activeTab by remember { mutableStateOf<Screen>(HomeTab) }

            CircuitCompositionLocals(circuit) {

                AppTheme {
                    Scaffold(
                        bottomBar = {
                            BottomAppBar {
                                IconButton(onClick = {
                                    activeTab = HomeTab
                                    navigator.goTo(HomeTab)
                                }) {

                                    if (activeTab == HomeTab) {

                                        Icon(Icons.Filled.Home, "Home")
                                    } else {
                                        Icon(Icons.Outlined.Home, "Home")
                                    }

                                }
                                IconButton(onClick = {
                                    activeTab = AccountTab
                                    navigator.goTo(AccountTab)
                                }) {
                                    if (activeTab == AccountTab) {

                                        Icon(Icons.Filled.Person, "Person")
                                    } else {
                                        Icon(Icons.Outlined.Person, "Person")
                                    }
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