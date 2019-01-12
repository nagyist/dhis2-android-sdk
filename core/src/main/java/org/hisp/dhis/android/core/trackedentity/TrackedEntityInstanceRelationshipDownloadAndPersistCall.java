package org.hisp.dhis.android.core.trackedentity;

import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.D2InternalModules;
import org.hisp.dhis.android.core.arch.api.executors.APICallExecutor;
import org.hisp.dhis.android.core.arch.api.executors.APICallExecutorImpl;
import org.hisp.dhis.android.core.calls.Call;
import org.hisp.dhis.android.core.common.D2CallExecutor;
import org.hisp.dhis.android.core.common.SyncCall;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.maintenance.D2Error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import retrofit2.Retrofit;

public final class TrackedEntityInstanceRelationshipDownloadAndPersistCall
        extends SyncCall<List<TrackedEntityInstance>> {

    private final DatabaseAdapter databaseAdapter;
    private final Retrofit retrofit;
    private final APICallExecutor apiCallExecutor;
    private final D2InternalModules internalModules;

    private final Collection<String> trackedEntityInstanceUids;

    private TrackedEntityInstanceRelationshipDownloadAndPersistCall(
            @NonNull DatabaseAdapter databaseAdapter,
            @NonNull Retrofit retrofit,
            @NonNull APICallExecutor apiCallExecutor,
            @NonNull D2InternalModules internalModules,
            @NonNull Collection<String> trackedEntityInstanceUids) {
        this.databaseAdapter = databaseAdapter;
        this.retrofit = retrofit;
        this.apiCallExecutor = apiCallExecutor;
        this.internalModules = internalModules;
        this.trackedEntityInstanceUids = trackedEntityInstanceUids;
    }

    @Override
    public List<TrackedEntityInstance> call() throws D2Error {
        setExecuted();

        if (trackedEntityInstanceUids == null) {
            throw new IllegalArgumentException("UID list is null");
        }

        List<TrackedEntityInstance> teis = new ArrayList<>();
        D2CallExecutor executor = new D2CallExecutor(databaseAdapter);
        for (String uid: trackedEntityInstanceUids) {
            Call<TrackedEntityInstance> teiCall = TrackedEntityInstanceDownloadByUidEndPointCall
                    .create(retrofit, apiCallExecutor, uid, TrackedEntityInstanceFields.asRelationshipFields);
            teis.add(executor.executeD2Call(teiCall));
        }

        executor.executeD2Call(TrackedEntityInstanceRelationshipPersistenceCall.create(databaseAdapter,
                internalModules, teis));

        return teis;
    }

    public static Call<List<TrackedEntityInstance>> create(@NonNull DatabaseAdapter databaseAdapter,
                                                           @NonNull Retrofit retrofit,
                                                           @NonNull D2InternalModules internalModules,
                                                           @NonNull Collection<String> trackedEntityInstanceUids) {
        return new TrackedEntityInstanceRelationshipDownloadAndPersistCall(
                databaseAdapter,
                retrofit,
                APICallExecutorImpl.create(databaseAdapter),
                internalModules,
                trackedEntityInstanceUids
        );
    }
}
