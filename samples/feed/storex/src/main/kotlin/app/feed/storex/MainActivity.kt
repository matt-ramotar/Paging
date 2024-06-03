package app.feed.storex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.feed.common.AppTheme
import app.feed.common.TimelineAndroidxPagingSource
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.*
import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.storex.paging.StoreCompositionLocals

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


            StoreCompositionLocals(pager) {
                CircuitCompositionLocals(circuit) {
                    AppTheme {
                        Scaffold(
                            containerColor = Color.White,
                            bottomBar = {
                                BottomAppBar(
                                    containerColor = Color.White,

                                    ) {

                                    IconButton(onClick = {
                                        activeTab = HomeTab
                                        navigator.goTo(HomeTab)
                                    }) {

                                        if (activeTab == HomeTab) {

                                            Icon(
                                                painterResource(app.feed.common.R.drawable.home),
                                                "Home",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                painterResource(app.feed.common.R.drawable.home),
                                                "Home",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                    }

                                    IconButton(onClick = {
                                        activeTab = AccountTab
                                        navigator.goTo(AccountTab)
                                    }) {
                                        Icon(
                                            painterResource(app.feed.common.R.drawable.discover),
                                            "Person",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        activeTab = AccountTab
                                        navigator.goTo(AccountTab)
                                    }) {
                                        Icon(
                                            painterResource(app.feed.common.R.drawable.notifications),
                                            "Person",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        activeTab = AccountTab
                                        navigator.goTo(AccountTab)
                                    }) {
                                        if (activeTab == AccountTab) {

                                            Icon(
                                                painterResource(app.feed.common.R.drawable.user),
                                                "Person",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                painterResource(app.feed.common.R.drawable.user),
                                                "Person",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }


                                }
                            }
                        ) { innerPadding ->
                            NavigableCircuitContent(
                                navigator,
                                backStack,
                                modifier = Modifier.padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background),
                                decoration = NavigatorDefaults.EmptyDecoration
                            )
                        }
                    }
                }
            }
        }
    }
}