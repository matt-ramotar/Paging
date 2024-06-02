package org.mobilenativefoundation.storex.paging

/**
 * TODO(): Document
 */
interface Identifiable<Id : Comparable<Id>, Q : Quantifiable<Id>> {
    val id: Q
}