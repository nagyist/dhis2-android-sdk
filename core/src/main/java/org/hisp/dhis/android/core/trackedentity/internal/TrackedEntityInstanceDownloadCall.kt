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
package org.hisp.dhis.android.core.trackedentity.internal

import dagger.Reusable
import io.reactivex.*
import java.util.HashSet
import javax.inject.Inject
import org.hisp.dhis.android.core.arch.api.executors.internal.RxAPICallExecutor
import org.hisp.dhis.android.core.arch.api.paging.internal.ApiPagingEngine
import org.hisp.dhis.android.core.arch.api.paging.internal.Paging
import org.hisp.dhis.android.core.arch.api.payload.internal.Payload
import org.hisp.dhis.android.core.arch.call.D2Progress
import org.hisp.dhis.android.core.arch.call.internal.D2ProgressManager
import org.hisp.dhis.android.core.arch.handlers.internal.Handler
import org.hisp.dhis.android.core.arch.helpers.internal.BooleanWrapper
import org.hisp.dhis.android.core.arch.repositories.collection.ReadOnlyWithDownloadObjectRepository
import org.hisp.dhis.android.core.program.internal.ProgramDataDownloadParams
import org.hisp.dhis.android.core.program.internal.ProgramOrganisationUnitLastUpdated
import org.hisp.dhis.android.core.relationship.internal.RelationshipDownloadAndPersistCallFactory
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemRelatives
import org.hisp.dhis.android.core.systeminfo.DHISVersionManager
import org.hisp.dhis.android.core.systeminfo.SystemInfo
import org.hisp.dhis.android.core.systeminfo.internal.SystemInfoModuleDownloader
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.user.internal.UserOrganisationUnitLinkStore

@Suppress("LongParameterList")
@Reusable
internal class TrackedEntityInstanceDownloadCall @Inject constructor(
    private val rxCallExecutor: RxAPICallExecutor,
    private val programOrganisationUnitLastUpdatedHandler: Handler<ProgramOrganisationUnitLastUpdated>,
    private val userOrganisationUnitLinkStore: UserOrganisationUnitLinkStore,
    private val systemInfoModuleDownloader: SystemInfoModuleDownloader,
    private val systemInfoRepository: ReadOnlyWithDownloadObjectRepository<SystemInfo>,
    private val trackedEntityInstanceQueryBuilderFactory: TrackedEntityInstanceQueryBuilderFactory,
    private val relationshipDownloadAndPersistCallFactory: RelationshipDownloadAndPersistCallFactory,
    private val persistenceCallFactory: TrackedEntityInstancePersistenceCallFactory,
    private val versionManager: DHISVersionManager,
    private val endpointCallFactory: TrackedEntityInstancesEndpointCallFactory,
    private val apiCallExecutor: RxAPICallExecutor,
    private val lastUpdatedManager: TrackedEntityInstanceLastUpdatedManager
) {

    // TODO use scheduler for parallel download
    // private final Scheduler teiDownloadScheduler = Schedulers.from(Executors.newFixedThreadPool(6));

    fun download(params: ProgramDataDownloadParams): Observable<D2Progress> {
        val observable = Observable.defer {
            val progressManager = D2ProgressManager(null)
            val programOrganisationUnitSet: MutableSet<ProgramOrganisationUnitLastUpdated> = HashSet()
            if (userOrganisationUnitLinkStore.count() == 0) {
                return@defer Observable.just(
                    progressManager.increaseProgress(TrackedEntityInstance::class.java, true)
                )
            } else {
                val relatives = RelationshipItemRelatives()
                return@defer Observable.concat(
                    systemInfoModuleDownloader.downloadWithProgressManager(progressManager),
                    downloadTeis(progressManager, params, programOrganisationUnitSet, relatives),
                    downloadRelationships(progressManager, relatives),
                    updateResource(progressManager, programOrganisationUnitSet)
                )
            }
        }
        return rxCallExecutor.wrapObservableTransactionally(observable, true)
    }

    private fun downloadTeis(
        progressManager: D2ProgressManager,
        params: ProgramDataDownloadParams,
        programOrganisationUnitSet: MutableSet<ProgramOrganisationUnitLastUpdated>,
        relatives: RelationshipItemRelatives
    ): Observable<D2Progress> {
        return Observable.defer {
            val teiQueryBuilders = trackedEntityInstanceQueryBuilderFactory
                .getTeiQueryBuilders(params)
            val teiDownloadObservable = Observable.fromIterable(teiQueryBuilders)
                .flatMap { teiQueryBuilder: TeiQuery.Builder -> getTrackedEntityInstancesWithPaging(teiQueryBuilder) }
            // TODO .subscribeOn(teiDownloadScheduler);
            val serverDate = systemInfoRepository.blockingGet().serverDate()
            val isFullUpdate = params.program() == null
            val overwrite = params.overwrite()
            teiDownloadObservable.flatMapSingle { teiList: List<TrackedEntityInstance> ->
                persistenceCallFactory.persistTEIs(teiList, isFullUpdate, overwrite, relatives)
                    .doOnComplete {
                        programOrganisationUnitSet.addAll(
                            TrackedEntityInstanceHelper.getProgramOrganisationUnitTuple(teiList, serverDate)
                        )
                    }
                    .toSingle {
                        progressManager.increaseProgress(
                            TrackedEntityInstance::class.java, false
                        )
                    }
            }
        }
    }

    private fun downloadRelationships(
        progressManager: D2ProgressManager,
        relatives: RelationshipItemRelatives
    ): Observable<D2Progress> {
        return Observable.defer {
            val completable =
                if (versionManager.is2_29) Completable.complete()
                else relationshipDownloadAndPersistCallFactory.downloadAndPersist(relatives)
            completable.andThen(
                Observable.just(
                    progressManager.increaseProgress(
                        TrackedEntityInstance::class.java, true
                    )
                )
            )
        }
    }

    private fun getTrackedEntityInstancesWithPaging(
        teiQueryBuilder: TeiQuery.Builder
    ): Observable<List<TrackedEntityInstance>> {
        val baseQuery = teiQueryBuilder.build()
        val pagingList = ApiPagingEngine.getPaginationList(baseQuery.pageSize(), baseQuery.limit())
        val allOkay = BooleanWrapper(true)
        return Observable
            .fromIterable(pagingList)
            .flatMapSingle { paging: Paging ->
                teiQueryBuilder.page(paging.page()).pageSize(paging.pageSize())
                apiCallExecutor.wrapSingle(endpointCallFactory.getCall(teiQueryBuilder.build()), true)
                    .map { payload: Payload<TrackedEntityInstance> ->
                        TeiListWithPaging(
                            true,
                            limitTeisForPage(payload.items(), paging),
                            paging
                        )
                    }
                    .onErrorResumeNext {
                        allOkay.set(false)
                        Single.just(TeiListWithPaging(false, emptyList(), paging))
                    }
            }
            .takeUntil { res: TeiListWithPaging ->
                res.isSuccess && (
                    res.paging.isLastPage ||
                        res.teiList.size < res.paging.pageSize()
                    )
            }
            .map { tuple: TeiListWithPaging -> tuple.teiList }
            .doOnComplete {
                if (allOkay.get()) {
                    lastUpdatedManager.update(teiQueryBuilder.build())
                }
            }
    }

    private fun limitTeisForPage(
        pageTrackedEntityInstances: List<TrackedEntityInstance>,
        paging: Paging
    ): List<TrackedEntityInstance> {
        return if (paging.isLastPage &&
            pageTrackedEntityInstances.size > paging.previousItemsToSkipCount()
        ) {
            val toIndex = Math.min(
                pageTrackedEntityInstances.size,
                paging.pageSize() - paging.posteriorItemsToSkipCount()
            )
            pageTrackedEntityInstances.subList(paging.previousItemsToSkipCount(), toIndex)
        } else {
            pageTrackedEntityInstances
        }
    }

    private fun updateResource(
        progressManager: D2ProgressManager,
        programOrganisationUnitSet: Set<ProgramOrganisationUnitLastUpdated>
    ): Observable<D2Progress> {
        return Single.fromCallable {
            programOrganisationUnitLastUpdatedHandler.handleMany(programOrganisationUnitSet)
            progressManager.increaseProgress(TrackedEntityInstance::class.java, true)
        }.toObservable()
    }

    private data class TeiListWithPaging constructor(
        val isSuccess: Boolean,
        val teiList: List<TrackedEntityInstance>,
        val paging: Paging
    )
}
