package app.feed.storex

import app.feed.common.HomeTab
import app.feed.common.models.PostId
import kotlinx.collections.immutable.ImmutableList

typealias StoreXHomeTabState = HomeTab.State<ImmutableList<PostId?>>