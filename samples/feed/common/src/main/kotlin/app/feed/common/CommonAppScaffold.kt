package app.feed.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen

@Composable
fun CommonAppScaffold() {

    val homeTab = remember { HomeTab }

    val backStack = rememberSaveableBackStack(root = homeTab)
    val navigator = rememberCircuitNavigator(backStack)
    var activeTab by remember { mutableStateOf<Screen>(HomeTab) }

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