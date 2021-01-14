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
import org.hisp.dhis.android.core.arch.db.querybuilders.internal.WhereClauseBuilder
import org.hisp.dhis.android.core.arch.db.stores.internal.IdentifiableDeletableDataObjectStore
import org.hisp.dhis.android.core.arch.db.stores.internal.IdentifiableObjectStore
import org.hisp.dhis.android.core.arch.helpers.CollectionsHelper
import org.hisp.dhis.android.core.arch.helpers.UidsHelper.excludeUids
import org.hisp.dhis.android.core.arch.helpers.UidsHelper.getUidsList
import org.hisp.dhis.android.core.arch.helpers.internal.EnumHelper
import org.hisp.dhis.android.core.common.*
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.EnrollmentInternalAccessor
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.internal.NoteToPostTransformer
import org.hisp.dhis.android.core.relationship.RelationshipCollectionRepository
import org.hisp.dhis.android.core.relationship.RelationshipHelper
import org.hisp.dhis.android.core.relationship.internal.RelationshipDHISVersionManager
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.hisp.dhis.android.core.systeminfo.DHISVersionManager
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceInternalAccessor
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

@Reusable
internal class TrackedEntityInstancePostInternalCall @Inject internal constructor(
    private val versionManager: DHISVersionManager,
    private val relationshipDHISVersionManager: RelationshipDHISVersionManager,
    private val relationshipRepository: RelationshipCollectionRepository,
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore,
    private val enrollmentStore: EnrollmentStore,
    private val eventStore: EventStore,
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore,
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore,
    private val relationshipStore: RelationshipStore,
    private val relationshipItemStore: RelationshipItemStore,
    private val noteStore: IdentifiableObjectStore<Note>
) {

    fun getPartitionsToSync(filteredTrackedEntityInstances: List<TrackedEntityInstance>?): List<List<TrackedEntityInstance>> {
        val dataValueMap = trackedEntityDataValueStore.queryTrackerTrackedEntityDataValues()
        val eventMap = eventStore.queryEventsAttachedToEnrollmentToPost()
        val enrollmentMap = enrollmentStore.queryEnrollmentsToPost()
        val attributeValueMap = trackedEntityAttributeValueStore.queryTrackedEntityAttributeValueToPost()
        val whereNotesClause = WhereClauseBuilder()
            .appendInKeyStringValues(
                DataColumns.STATE, EnumHelper.asStringList(*State.uploadableStatesIncludingError())
            )
            .build()
        val notes = noteStore.selectWhere(whereNotesClause)
        val targetTrackedEntityInstances: List<TrackedEntityInstance> =
            filteredTrackedEntityInstances ?: trackedEntityInstanceStore.queryTrackedEntityInstancesToSync()
        val trackedEntityInstancesToSync = getPagedTrackedEntityInstances(targetTrackedEntityInstances)
        val trackedEntityInstancesRecreated: MutableList<List<TrackedEntityInstance>> = ArrayList()
        for (partition in trackedEntityInstancesToSync) {
            val partitionRecreated: MutableList<TrackedEntityInstance> = ArrayList()
            for (trackedEntityInstance in partition) {
                val recreatedTrackedEntityInstance = recreateTrackedEntityInstance(
                    trackedEntityInstance, dataValueMap, eventMap, enrollmentMap, attributeValueMap, notes
                )
                partitionRecreated.add(recreatedTrackedEntityInstance)
            }
            trackedEntityInstancesRecreated.add(partitionRecreated)
            setPartitionStates(partitionRecreated, State.UPLOADING)
        }
        return trackedEntityInstancesRecreated
    }

    private fun getPagedTrackedEntityInstances(
        filteredTrackedEntityInstances: List<TrackedEntityInstance>
    ): List<List<TrackedEntityInstance>> {
        val includedUids: MutableList<String> = ArrayList()
        val partitions = CollectionsHelper.setPartition(filteredTrackedEntityInstances, DEFAULT_PAGE_SIZE)
        val partitionsWithRelationships: MutableList<List<TrackedEntityInstance>> = ArrayList()
        for (partition in partitions) {
            val partitionWithoutDuplicates = excludeUids(partition, includedUids)
            val partitionWithRelationships =
                getTrackedEntityInstancesWithRelationships(partitionWithoutDuplicates.toMutableList(), includedUids)
            partitionsWithRelationships.add(partitionWithRelationships)
            includedUids.addAll(getUidsList(partitionWithRelationships))
        }
        return partitionsWithRelationships
    }

    private fun getTrackedEntityInstancesWithRelationships(
        filteredTrackedEntityInstances: MutableList<TrackedEntityInstance>, excludedUids: List<String>
    ): List<TrackedEntityInstance> {
        val trackedEntityInstancesInDBToSync = trackedEntityInstanceStore.queryTrackedEntityInstancesToSync()
        val filteredUids: List<String?> = getUidsList(filteredTrackedEntityInstances)
        val teiUidsToPost = getUidsList(
            trackedEntityInstanceStore.queryTrackedEntityInstancesToPost()
        )
        val relatedTeisToPost: MutableList<String> = ArrayList()
        var internalRelatedTeis = filteredUids
        do {
            val relatedTeiUids = relationshipItemStore.getRelatedTeiUids(internalRelatedTeis)
            relatedTeiUids.retainAll(teiUidsToPost)
            relatedTeiUids.removeAll(filteredUids)
            relatedTeiUids.removeAll(relatedTeisToPost)
            relatedTeiUids.removeAll(excludedUids)
            relatedTeisToPost.addAll(relatedTeiUids)
            internalRelatedTeis = relatedTeiUids
        } while (!internalRelatedTeis.isEmpty())
        for (trackedEntityInstanceInDB in trackedEntityInstancesInDBToSync) {
            if (relatedTeisToPost.contains(trackedEntityInstanceInDB.uid())) {
                filteredTrackedEntityInstances.add(trackedEntityInstanceInDB)
            }
        }
        return filteredTrackedEntityInstances
    }

    private fun recreateTrackedEntityInstance(
        trackedEntityInstance: TrackedEntityInstance,
        dataValueMap: Map<String, List<TrackedEntityDataValue>>,
        eventMap: Map<String, List<Event>>,
        enrollmentMap: Map<String, List<Enrollment>>,
        attributeValueMap: Map<String, List<TrackedEntityAttributeValue>>,
        notes: List<Note>
    ): TrackedEntityInstance {
        val trackedEntityInstanceUid = trackedEntityInstance.uid()
        val enrollmentsRecreated: MutableList<Enrollment> = ArrayList()
        val enrollments = enrollmentMap[trackedEntityInstanceUid]
        val emptyAttributeValueList: List<TrackedEntityAttributeValue> = ArrayList()
        if (enrollments != null) {
            for (enrollment in enrollments) {
                val eventRecreated: MutableList<Event> = ArrayList()
                val eventsForEnrollment = eventMap[enrollment.uid()]
                val transformer = NoteToPostTransformer(versionManager)
                if (eventsForEnrollment != null) {
                    for (event in eventsForEnrollment) {
                        val dataValuesForEvent = dataValueMap[event.uid()]
                        val notesForEvent = getEventNotes(notes, event, transformer)
                        if (versionManager.is2_30) {
                            eventRecreated.add(
                                event.toBuilder()
                                    .trackedEntityDataValues(dataValuesForEvent)
                                    .notes(notesForEvent)
                                    .geometry(null)
                                    .build()
                            )
                        } else {
                            eventRecreated.add(
                                event.toBuilder()
                                    .trackedEntityDataValues(dataValuesForEvent)
                                    .notes(notesForEvent)
                                    .build()
                            )
                        }
                    }
                }
                val notesForEnrollment = getEnrollmentNotes(notes, enrollment, transformer)
                enrollmentsRecreated.add(
                    EnrollmentInternalAccessor.insertEvents(enrollment.toBuilder(), eventRecreated)
                        .notes(notesForEnrollment)
                        .build()
                )
            }
        }
        val attributeValues = attributeValueMap[trackedEntityInstanceUid]
        val dbRelationships =
            relationshipRepository.getByItem(RelationshipHelper.teiItem(trackedEntityInstance.uid()), true)
        val ownedRelationships =
            relationshipDHISVersionManager.getOwnedRelationships(dbRelationships, trackedEntityInstance.uid())
        val versionAwareRelationships =
            relationshipDHISVersionManager.to229Compatible(ownedRelationships, trackedEntityInstance.uid())
        return TrackedEntityInstanceInternalAccessor
            .insertEnrollments(
                TrackedEntityInstanceInternalAccessor
                    .insertRelationships(trackedEntityInstance.toBuilder(), versionAwareRelationships),
                enrollmentsRecreated
            )
            .trackedEntityAttributeValues(attributeValues ?: emptyAttributeValueList)
            .build()
    }

    private fun getEventNotes(notes: List<Note>, event: Event, transformer: NoteToPostTransformer): List<Note> {
        val notesForEvent: MutableList<Note> = ArrayList()
        for (note in notes) {
            if (event.uid() == note.event()) {
                notesForEvent.add(transformer.transform(note))
            }
        }
        return notesForEvent
    }

    private fun getEnrollmentNotes(
        notes: List<Note>,
        enrollment: Enrollment,
        transformer: NoteToPostTransformer
    ): List<Note> {
        val notesForEnrollment: MutableList<Note> = ArrayList()
        for (note in notes) {
            if (enrollment.uid() == note.enrollment()) {
                notesForEnrollment.add(transformer.transform(note))
            }
        }
        return notesForEnrollment
    }

    fun restorePartitionStates(partition: List<TrackedEntityInstance>) {
        setPartitionStates(partition, null)
    }

    private fun setPartitionStates(partition: List<TrackedEntityInstance>, forcedState: State?) {
        val teiMap: MutableMap<State, MutableList<String>?> = HashMap()
        val enrollmentMap: MutableMap<State, MutableList<String>?> = HashMap()
        val eventMap: MutableMap<State, MutableList<String>?> = HashMap()
        val relationshipMap: MutableMap<State, MutableList<String>?> = HashMap()
        for (instance in partition) {
            addState(teiMap, instance, forcedState)
            for (enrollment in TrackedEntityInstanceInternalAccessor.accessEnrollments(instance)) {
                addState(enrollmentMap, enrollment, forcedState)
                for (event in EnrollmentInternalAccessor.accessEvents(enrollment)) {
                    addState(eventMap, event, forcedState)
                }
            }
            for (r in TrackedEntityInstanceInternalAccessor.accessRelationships(instance)) {
                if (versionManager.is2_29) {
                    val whereClause = WhereClauseBuilder().appendKeyStringValue(CoreColumns.ID, r.id()).build()
                    val dbRelationship = relationshipStore.selectOneWhere(whereClause)
                    dbRelationship?.let { addState(relationshipMap, it, forcedState) }
                } else {
                    addState(relationshipMap, r, forcedState)
                }
            }
        }
        persistStates(teiMap, trackedEntityInstanceStore)
        persistStates(enrollmentMap, enrollmentStore)
        persistStates(eventMap, eventStore)
        persistStates(relationshipMap, relationshipStore)
    }

    private fun <O> addState(
        stateMap: MutableMap<State, MutableList<String>?>, o: O,
        forcedState: State?
    ) where O : DataObject?, O : ObjectWithUidInterface? {
        val s = getStateToSet(o, forcedState)
        if (!stateMap.containsKey(s)) {
            stateMap[s] = ArrayList()
        }
        stateMap[s]!!.add(o!!.uid())
    }

    private fun <O> getStateToSet(o: O, forcedState: State?): State where O : DataObject?, O : ObjectWithUidInterface? {
        return forcedState
            ?: if (o!!.state() == State.UPLOADING) State.TO_UPDATE else o.state()
    }

    private fun persistStates(map: Map<State, MutableList<String>?>, store: IdentifiableDeletableDataObjectStore<*>) {
        for ((key, value) in map) {
            store.setState(value!!, key)
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }
}