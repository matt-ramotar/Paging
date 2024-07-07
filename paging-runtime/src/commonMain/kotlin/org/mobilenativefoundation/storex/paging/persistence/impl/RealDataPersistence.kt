package org.mobilenativefoundation.storex.paging.persistence.impl

import org.mobilenativefoundation.storex.paging.persistence.api.DataPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier

class RealDataPersistence<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    override val items: ItemPersistence<Id, K, V>,
    override val pages: PagePersistence<Id, K, V>
) : DataPersistence<Id, K, V>