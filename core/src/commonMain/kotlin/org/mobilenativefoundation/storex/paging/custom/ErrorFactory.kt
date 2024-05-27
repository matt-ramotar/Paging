package org.mobilenativefoundation.storex.paging.custom

interface ErrorFactory<E : Any> {
    fun create(throwable: Throwable): E
    fun create(message: String): E

    companion object {
        val DEFAULT = DefaultErrorFactory()
    }
}

class DefaultErrorFactory : ErrorFactory<Throwable> {
    override fun create(throwable: Throwable): Throwable = throwable
    override fun create(message: String): Throwable = Throwable(message)
}