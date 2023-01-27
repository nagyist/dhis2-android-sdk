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

package org.hisp.dhis.android.core.map.layer.internal.bing

import dagger.Reusable
import io.reactivex.Single
import javax.inject.Inject
import org.hisp.dhis.android.core.D2Manager
import org.hisp.dhis.android.core.arch.api.executors.internal.RxAPICallExecutor
import org.hisp.dhis.android.core.arch.handlers.internal.Handler
import org.hisp.dhis.android.core.map.layer.MapLayer
import org.hisp.dhis.android.core.map.layer.MapLayerImageryProvider
import org.hisp.dhis.android.core.map.layer.MapLayerImageryProviderArea
import org.hisp.dhis.android.core.map.layer.MapLayerPosition
import org.hisp.dhis.android.core.settings.internal.SettingService
import org.hisp.dhis.android.core.settings.internal.SystemSettingsFields
import org.hisp.dhis.android.core.systeminfo.DHISVersion
import org.hisp.dhis.android.core.systeminfo.DHISVersionManager

@Reusable
internal class BingCallFactory @Inject constructor(
    private val rxAPICallExecutor: RxAPICallExecutor,
    private val mapLayerHandler: Handler<MapLayer>,
    private val versionManager: DHISVersionManager,
    private val settingsService: SettingService,
    private val bingService: BingService
) {

    fun download(): Single<List<MapLayer>> {
        return Single.defer {
            if (versionManager.isGreaterOrEqualThan(DHISVersion.V2_34)) {
                rxAPICallExecutor.wrapSingle(settingsService.getSystemSettings(SystemSettingsFields.bingApiKey), true)
                    .flatMap { settings ->
                        settings.keyBingMapsApiKey
                            ?.let { downloadBingBasemaps(it) }
                            ?: Single.just(emptyList())
                    }
                    .map { mapLayers ->
                        mapLayerHandler.handleMany(mapLayers)
                        mapLayers
                    }
                    .onErrorReturnItem(emptyList())
            } else {
                Single.just(emptyList())
            }
        }
    }

    private fun downloadBingBasemaps(bingKey: String): Single<List<MapLayer>> {
        return Single.merge(
            BingBasemaps.list.map { b -> downloadBasemap(bingKey, b) }
        ).toList().map { it.flatten() }
    }

    private fun downloadBasemap(bingkey: String, basemap: BingBasemap): Single<List<MapLayer>> {
        return bingService.getBaseMap(getUrl(basemap.style, bingkey))
            .map { m ->
                m.resourceSets.firstOrNull()?.resources?.firstOrNull()?.let { resource ->
                    listOf(
                        MapLayer.builder()
                            .uid(basemap.id)
                            .name(basemap.name)
                            .displayName(basemap.name)
                            .style(basemap.style)
                            .mapLayerPosition(MapLayerPosition.BASEMAP)
                            .external(false)
                            .imageUrl(resource.imageUrl)
                            .subdomains(resource.imageUrlSubdomains)
                            .subdomainPlaceholder("{subdomain}")
                            .imageryProviders(
                                resource.imageryProviders.map { i ->
                                    MapLayerImageryProvider.builder()
                                        .mapLayer(basemap.id)
                                        .attribution(i.attribution)
                                        .coverageAreas(
                                            i.coverageAreas.map { ca ->
                                                MapLayerImageryProviderArea.builder()
                                                    .bbox(ca.bbox)
                                                    .zoomMax(ca.zoomMax)
                                                    .zoomMin(ca.zoomMin)
                                                    .build()
                                            }
                                        )
                                        .build()
                                }
                            )
                            .build()
                    )
                } ?: emptyList()
            }.onErrorReturnItem(emptyList())
    }

    private fun getUrl(style: String, bingKey: String): String {
        return if (D2Manager.isTestMode && !D2Manager.isRealIntegration) {
            "mockBingMaps"
        } else {
            "https://dev.virtualearth.net/REST/V1/Imagery/Metadata/$style?" +
                "output=json&include=ImageryProviders&culture=en-GB&uriScheme=https&key=$bingKey"
        }
    }
}
