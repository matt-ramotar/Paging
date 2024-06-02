package app.feed.androidx

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

data object AppPresenterFactory : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is HomeTab -> HomeTabPresenter()
            is AccountTab-> AccountTabPresenter()
            else -> null
        }
    }

}


class HomeTabPresenter() : Presenter<HomeTab.State> {

    @Composable
    override fun present(): HomeTab.State {
        return HomeTab.State("", emptyList())
    }
}

class AccountTabPresenter() : Presenter<AccountTab.State> {

    @Composable
    override fun present(): AccountTab.State {
        return AccountTab.State("")
    }
}

