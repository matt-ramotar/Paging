package org.mobilenativefoundation.storex.paging.persistence.impl

import org.mobilenativefoundation.storex.paging.persistence.api.DataPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence

class RealDataPersistence<ItemId: Any, PageRequestKey: Any, ItemValue: Any>(
    override val items: ItemPersistence<ItemId, PageRequestKey, ItemValue>,
    override val pages: PagePersistence<ItemId, PageRequestKey, ItemValue>
) : DataPersistence<ItemId, PageRequestKey, ItemValue>