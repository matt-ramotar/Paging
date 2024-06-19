package org.mobilenativefoundation.storex.paging

/**
 * TODO(): Document
 */
interface Identifiable<Id : Identifier<*>> {
    val id: Id
}