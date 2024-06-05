package app.feed.storex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.feed.common.CommonAppScaffold
import app.feed.common.TimelineAndroidxPagingSource
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import org.mobilenativefoundation.storex.paging.StoreCompositionLocals

class MainActivity : ComponentActivity() {


    private val pager = TimelinePagerFactory().create(
        androidxPagingSourceFactory = { api ->
            TimelineAndroidxPagingSource(api, 5)
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

            StoreCompositionLocals(pager) {
                CircuitCompositionLocals(circuit) {
                    CommonAppScaffold()
                }
            }
        }
    }
}