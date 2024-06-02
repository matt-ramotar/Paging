package app.feed.androidx

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui

data object AppUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is HomeTab -> HomeTabUi
            is AccountTab -> AccountTabUi
            else -> null
        }
    }
}

data object HomeTabUi : Ui<HomeTab.State> {
    @Composable
    override fun Content(state: HomeTab.State, modifier: Modifier) {
        Text("AndroidX - Home")
    }

}

data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("AndroidX - Account")
    }

}