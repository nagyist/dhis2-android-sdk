/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.core.event.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.hisp.dhis.android.core.organisationunit.OrganisationUnitMode;

import java.util.Date;
import java.util.List;

@AutoValue
abstract class EventQueryBundle {

    @NonNull
    abstract List<String> orgUnitList();

    @NonNull
    abstract List<String> programList();

    @NonNull
    abstract OrganisationUnitMode ouMode();

    @Nullable
    abstract Date lastUpdatedStartDate();

    @Nullable
    abstract String eventStartDate();

    @NonNull
    abstract Integer limit();

    static Builder builder() {
        return new AutoValue_EventQueryBundle.Builder()
                .ouMode(OrganisationUnitMode.SELECTED);
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder orgUnitList(List<String> orgUnitList);

        abstract Builder programList(List<String> programList);

        abstract Builder ouMode(OrganisationUnitMode ouMode);

        abstract Builder lastUpdatedStartDate(Date lastUpdatedStartDate);

        abstract Builder eventStartDate(String eventStartDate);

        abstract Builder limit(Integer limit);

        abstract EventQueryBundle build();
    }
}
