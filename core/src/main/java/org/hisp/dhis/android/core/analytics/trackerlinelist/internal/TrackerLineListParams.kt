/*
 *  Copyright (c) 2004-2024, University of Oslo
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

package org.hisp.dhis.android.core.analytics.trackerlinelist.internal

import org.hisp.dhis.android.core.analytics.trackerlinelist.TrackerLineListItem

internal data class TrackerLineListParams(
    val trackerVisualization: String?,
    val outputType: TrackerLineListOutputType?,
    val programId: String?,
    val programStageId: String?,
    val columns: List<TrackerLineListItem>,
    val filters: List<TrackerLineListItem>,
) {
    operator fun plus(other: TrackerLineListParams): TrackerLineListParams {
        return other.copy(
            outputType = other.outputType ?: outputType,
            programId = other.programId ?: programId,
            programStageId = other.programStageId ?: programStageId,
            columns = other.columns.fold(columns) { list, item -> updateInList(list, item) },
            filters = other.filters.fold(filters) { list, item -> updateInList(list, item) },
        )
    }

    companion object {
        fun updateInList(items: List<TrackerLineListItem>, newItem: TrackerLineListItem): List<TrackerLineListItem> {
            val otherItems = items.filterNot { it.id == newItem.id }
            return otherItems + newItem
        }
    }
}
