package org.mobilenativefoundation.storex.paging.runtime

interface Identifiable<Id : Identifier<*>> {
    val id: Id
}