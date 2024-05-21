/*
 *  Copyright (c) 2004-2023, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.android.core.arch.api.filters.internal

import org.hisp.dhis.android.core.arch.api.fields.internal.Field
import java.util.Collections

class SingleValueFilter<T, K> (
    private val field: Field<T, K>,
    private val operator: String,
    private val values: Collection<String>
): Filter<T, K> {
    override fun field(): Field<T, K> = field
    override fun operator(): String = operator
    override fun values(): Collection<String> = values
    override fun generateString(): String {
        val builder = StringBuilder()
        builder.append(field().name())
            .append(':')
            .append(operator())
            .append(':')

        val valuesIterator: Iterator<String> = values().iterator()
        builder.append(valuesIterator.next())
        return builder.toString()
    }

    companion object {
        private fun <T, K> create(
            field: Field<T, K>,
            operator: String,
            value: String?
        ): Filter<T, K>? {
            //If the filter is incomplete, return null so the filter is not included in the request.
            if (value.isNullOrEmpty()) {
                return null
            }
            return SingleValueFilter(
                field,
                operator,
                Collections.unmodifiableCollection(listOf(value)                )
            )
        }

        @JvmStatic
        fun <T, K> gt(field: Field<T, K>, value: String?): Filter<T, K>? {
            return create(field, "gt", value)
        }

        @JvmStatic
        fun <T, K> eq(field: Field<T, K>, value: String?): Filter<T, K>? {
            return create(field, "eq", value)
        }

        @JvmStatic
        fun <T, K> like(field: Field<T, K>, value: String?): Filter<T, K>? {
            return create(field, "like", value)
        }
    }
}

