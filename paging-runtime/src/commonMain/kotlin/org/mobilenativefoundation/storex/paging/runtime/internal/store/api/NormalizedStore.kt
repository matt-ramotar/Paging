package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier

/**
 * This file contains the interfaces for a normalized store used in paging operations.
 *
 * Design Decision:
 * The original NormalizedStore interface has been separated into multiple smaller interfaces
 * (ItemStore, PageStore, StoreInvalidation) with NormalizedStore extending all of them.
 * This design decision was made based on the following considerations:
 *
 * 1. Single Responsibility Principle: Each interface now has a more focused set of responsibilities.
 * 2. Interface Segregation Principle: Clients can depend only on the interfaces they need.
 * 3. Improved Testability: Smaller interfaces are easier to mock and test in isolation.
 * 4. Flexibility: This structure allows for more flexible implementations and potential optimizations.
 * 5. Reduced Coupling: Separating concerns reduces dependencies between different parts of the system.
 *
 * While this separation introduces some additional complexity, the benefits in terms of
 * maintainability, testability, and adherence to SOLID principles outweigh this drawback.
 * The NormalizedStore interface is retained as a composition of the smaller interfaces,
 * providing a single point of access for clients that need all functionalities.
 *
 * This design allows for future extensions or optimizations of specific parts of the store
 * while maintaining a cohesive API for the entire normalized store functionality.
 */

/**
 * Represents a normalized store for paging data, combining item, page, and invalidation operations.
 *
 * This interface extends ItemStore, PageStore, and StoreInvalidation, providing a comprehensive
 * set of operations for managing paged data in a normalized format.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value, which must be Identifiable by Id.
 */
internal interface NormalizedStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> :
    ItemStore<Id, K, V>,
    PageStore<Id, K, V>,
    StoreInvalidation


