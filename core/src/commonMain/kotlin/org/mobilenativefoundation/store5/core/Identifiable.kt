package org.mobilenativefoundation.store5.core

/**
 * TODO(): Document
 */
interface Identifiable<Id : Comparable<Id>> {
    val id: Id
}