package org.mobilenativefoundation.storex.paging

/**
 * TODO(): Document
 */
interface Identifiable<Id : Comparable<Id>> {
    val id: Quantifiable<Id>
}