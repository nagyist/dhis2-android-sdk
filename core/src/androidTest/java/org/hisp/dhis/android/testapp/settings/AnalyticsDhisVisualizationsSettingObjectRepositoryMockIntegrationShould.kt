/*
 *  Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.android.testapp.settings

import com.google.common.truth.Truth.assertThat
import org.hisp.dhis.android.core.utils.integration.mock.BaseMockIntegrationTestFullDispatcher
import org.junit.Test

class AnalyticsDhisVisualizationsSettingObjectRepositoryMockIntegrationShould :
    BaseMockIntegrationTestFullDispatcher() {

    @Test
    fun find_analytics_settings() {
        val analyticsDhisVisualizationsSetting = d2
            .settingModule()
            .analyticsSetting()
            .visualizationsSettings()
            .blockingGet()

        assertThat(analyticsDhisVisualizationsSetting.home().size).isEqualTo(2)
        assertThat(
            analyticsDhisVisualizationsSetting.home().first().visualizations().first().name(),
        ).isNotEmpty()

        assertThat(analyticsDhisVisualizationsSetting.program().size).isEqualTo(1)
        assertThat(analyticsDhisVisualizationsSetting.dataSet().size).isEqualTo(1)
    }

    @Test
    fun find_analytics_settings_by_program() {
        val programSettings = d2
            .settingModule()
            .analyticsSetting()
            .visualizationsSettings()
            .blockingGetByProgram("IpHINAT79UW")

        assertThat(programSettings?.size).isEqualTo(1)
        assertThat(programSettings?.first()?.visualizations()?.size).isEqualTo(2)
        assertThat(
            programSettings?.first()?.visualizations()?.first()?.uid(),
        ).isEqualTo("PYBH8ZaAQnC")
    }

    @Test
    fun find_analytics_settings_by_data_set() {
        val programSettings = d2
            .settingModule()
            .analyticsSetting()
            .visualizationsSettings()
            .blockingByDataSet("BfMAe6Itzgt")

        assertThat(programSettings?.size).isEqualTo(1)
        assertThat(programSettings?.first()?.visualizations()?.size).isEqualTo(1)
        assertThat(
            programSettings?.first()?.visualizations()?.first()?.uid(),
        ).isEqualTo("FAFa11yFeFe")
    }
}
