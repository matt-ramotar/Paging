package org.mobilenativefoundation.storex.paging.persistence


import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier

/**
 * Design Decisions:
 * 1. Abstraction: The main interface is renamed to 'DataPersistence' to accommodate various
 *    data storage mechanisms beyond traditional databases.
 * 2. Separation of Concerns: Item and Page operations are separated into distinct interfaces,
 *    allowing for more focused implementations and easier testing.
 * 3. Flexibility: The use of generic types allows for different identifier and data types,
 *    making the interfaces adaptable to various use cases.
 * 4. Asynchronous Operations: All methods are suspend functions or return Flows, accommodating
 *    both synchronous and asynchronous data sources.
 * 5. Explicit Error Handling: A custom Result type is introduced to handle operation outcomes,
 *    providing more detailed error information.
 * 6. Querying Capabilities: A simple query mechanism is introduced, allowing for more
 *    flexible data retrieval.
 *
 * These design choices aim to create a more versatile and robust persistence layer that can
 * adapt to different storage solutions (e.g., SQLite, Room, Realm, or even network-based storage)
 * while providing a consistent interface for the paging system.
 */


/**
 * Main interface for data persistence operations in the paging system.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value.
 */
interface DataPersistence<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    val itemPersistence: ItemPersistence<Id, K, V>
    val pagePersistence: PagePersistence<Id, K, V>
}
