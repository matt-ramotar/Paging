package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.custom.ErrorFactory

class DefaultErrorFactory : ErrorFactory<Throwable> {
    override fun create(throwable: Throwable): Throwable = throwable

    override fun create(message: String): Throwable = Throwable(message)

}