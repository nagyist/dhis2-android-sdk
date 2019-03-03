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

package org.hisp.dhis.android.testapp.program;

import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.data.database.SyncedDatabaseMockIntegrationShould;
import org.hisp.dhis.android.core.program.ProgramIndicator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
public class ProgramIndicatorCollectionRepositoryMockIntegrationShould extends SyncedDatabaseMockIntegrationShould {

    @Test
    public void find_all() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .get();

        assertThat(indicators.size(), is(4));
    }

    @Test
    public void filter_by_display_in_form() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byDisplayInForm()
                        .isTrue()
                        .get();
        
        assertThat(indicators.size(), is(1));
    }

    @Test
    public void filter_by_expression() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byExpression()
                        .eq("d2:yearsBetween(A{iESIqZ0R0R0},V{event_date})")
                        .get();

        assertThat(indicators.size(), is(1));
    }

    @Test
    public void filter_by_dimension_item() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byDimensionItem()
                        .eq("rXoaHGAXWy9")
                        .get();

        assertThat(indicators.size(), is(1));
    }

    @Test
    public void filter_by_filter() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byFilter()
                        .eq("#{edqlbukwRfQ.vANAXwtLwcT} < 11")
                        .get();

        assertThat(indicators.size(), is(1));
    }

    @Test
    public void filter_by_decimals() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byDecimals()
                        .eq(2)
                        .get();

        assertThat(indicators.size(), is(1));
    }

    @Test
    public void filter_by_aggregation_type() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byAggregationType()
                        .eq("AVERAGE")
                        .get();

        assertThat(indicators.size(), is(2));
    }

    @Test
    public void filter_by_program() {
        List<ProgramIndicator> indicators =
                d2.programModule().programIndicators
                        .byProgramUid()
                        .eq("lxAQ7Zs9VYR")
                        .get();

        assertThat(indicators.size(), is(4));
    }

    @Test
    public void include_legend_sets_as_children() {
        ProgramIndicator programIndicators = d2.programModule().programIndicators
                .one().getWithAllChildren();
        assertThat(programIndicators.legendSets().size(), is(1));
        assertThat(programIndicators.legendSets().get(0).name(), is("Age 15y interval"));
    }
}