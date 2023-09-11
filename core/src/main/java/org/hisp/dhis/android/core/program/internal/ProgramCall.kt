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
package org.hisp.dhis.android.core.program.internal

import dagger.Reusable
import io.reactivex.Single
import org.hisp.dhis.android.core.arch.api.executors.internal.APIDownloader
import org.hisp.dhis.android.core.arch.call.factories.internal.UidsCall
import org.hisp.dhis.android.core.arch.handlers.internal.Handler
import org.hisp.dhis.android.core.common.internal.DataAccessFields
import org.hisp.dhis.android.core.program.Program
import java.lang.Boolean
import javax.inject.Inject
import kotlin.String

@Reusable
class ProgramCall @Inject internal constructor(
    private val service: ProgramService,
    handler: ProgramHandler,
    apiDownloader: APIDownloader,
) : UidsCall<Program> {
    private val handler: Handler<Program>
    private val apiDownloader: APIDownloader

    init {
        this.handler = handler
        this.apiDownloader = apiDownloader
    }

    override fun download(uids: Set<String>): Single<List<Program>> {
        val accessDataReadFilter = "access.data." + DataAccessFields.read.eq(true).generateString()
        return apiDownloader.downloadPartitioned(
            uids,
            MAX_UID_LIST_SIZE,
            handler,
        ) { partitionUids: Set<String> ->
            service.getPrograms(
                ProgramFields.allFields,
                ProgramFields.uid.`in`(partitionUids),
                accessDataReadFilter,
                Boolean.FALSE,
            )
        }
    }

    companion object {
        private const val MAX_UID_LIST_SIZE = 50
    }
}
