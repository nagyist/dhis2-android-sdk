/*
 * Copyright (c) 2016, University of Oslo
 *
 * All rights reserved.
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

package org.hisp.dhis.client.sdk.android.flow;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import org.hisp.dhis.client.sdk.android.common.meta.DbDhis;
import org.hisp.dhis.client.sdk.models.common.MergeStrategy;
import org.hisp.dhis.client.sdk.models.common.base.IdentifiableObject;


@Table(databaseName = DbDhis.NAME)
public final class Category$Flow extends BaseIdentifiableObject$Flow {

    @Column
    String dataDimension;

    @Column
    String dataDimensionType;

    @Column
    String dimension;

    public Category$Flow() {
    }

    public String getDataDimension() {
        return dataDimension;
    }

    public void setDataDimension(String dataDimension) {
        this.dataDimension = dataDimension;
    }

    public String getDataDimensionType() {
        return dataDimensionType;
    }

    public void setDataDimensionType(String dataDimensionType) {
        this.dataDimensionType = dataDimensionType;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    @Override
    public void mergeWith(IdentifiableObject identifiableObject, MergeStrategy mergeStrategy) {

    }
}
