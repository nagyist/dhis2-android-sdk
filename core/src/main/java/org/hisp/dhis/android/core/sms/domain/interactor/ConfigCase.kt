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
package org.hisp.dhis.android.core.sms.domain.interactor

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxCompletable
import org.hisp.dhis.android.core.sms.domain.repository.WebApiRepository
import org.hisp.dhis.android.core.sms.domain.repository.WebApiRepository.GetMetadataIdsConfig
import org.hisp.dhis.android.core.sms.domain.repository.internal.LocalDbRepository

/**
 * Used to set initial data that is common for all sms sending tasks
 */
class ConfigCase(
    private val webApiRepository: WebApiRepository,
    private val localDbRepository: LocalDbRepository
) {

    fun smsModuleConfig(): Single<SmsConfig> {
        return Single.zip(
            localDbRepository.isModuleEnabled,
            localDbRepository.gatewayNumber,
            localDbRepository.waitingForResultEnabled,
            localDbRepository.confirmationSenderNumber,
            localDbRepository.waitingResultTimeout
        ) { moduleEnabled: Boolean, gateway: String?, waitingForResult: Boolean, resultSender: String?, resultWaitingTimeout: Int ->
            SmsConfig(
                moduleEnabled,
                gateway,
                waitingForResult,
                resultSender,
                resultWaitingTimeout
            )
        }
    }

    /**
     * Set if the SDK has to wait for the result or not.
     * @param enabled
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setWaitingForResultEnabled(enabled: Boolean): Completable {
        return localDbRepository.setWaitingForResultEnabled(enabled)
    }

    /**
     * Set the number to receive messages from. Messages from other senders will not be read.
     * @param number The sender number.
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setConfirmationSenderNumber(number: String): Completable {
        return localDbRepository.setConfirmationSenderNumber(number)
    }

    /**
     * Set the time in seconds to wait for the result.
     * @param timeoutSeconds Time in seconds.
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setWaitingResultTimeout(timeoutSeconds: Int): Completable {
        return localDbRepository.setWaitingResultTimeout(timeoutSeconds)
    }

    /**
     * Set the gateway number to send the SMS. This is a required parameter before sending SMS.
     * @param gatewayNumber The gateway numberf
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setGatewayNumber(gatewayNumber: String?): Completable {
        return if (gatewayNumber.isNullOrEmpty()) {
            Completable.error(IllegalArgumentException("Gateway number can't be empty"))
        } else localDbRepository.setGatewayNumber(gatewayNumber)
    }

    /**
     * Delete the gateway number to send the SMS.
     * @return `Completable` that completes when the configuration is changed.
     */
    fun deleteGatewayNumber(): Completable {
        return localDbRepository.deleteGatewayNumber()
    }

    /**
     * Set if SMS Module is enabled or not. It is required to enable it before using it.
     * @param enabled If the module is enabled or not
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setModuleEnabled(enabled: Boolean): Completable {
        return localDbRepository.setModuleEnabled(enabled)
    }

    /**
     * Set a new MetadataDownload configuration.
     * @param metadataIdsConfig Configuration with the metadata ids.
     * @return `Completable` that completes when the configuration is changed.
     */
    fun setMetadataDownloadConfig(metadataIdsConfig: GetMetadataIdsConfig?): Completable {
        return if (metadataIdsConfig == null) {
            Completable.error(IllegalArgumentException("Received null config"))
        } else localDbRepository.setMetadataDownloadConfig(metadataIdsConfig)
    }

    /**
     * Get the metadata download configuration.
     * @return `Single with the` metadata download configuration.
     */
    fun metadataDownloadConfig(): Single<GetMetadataIdsConfig> {
        return localDbRepository.metadataDownloadConfig
    }

    /**
     * Method to download metadata ids. This is required before using the SMS module.
     */
    fun refreshMetadataIds(): Completable {

        // TODO remove this. This is for debugging purposes

        /*     val refreshTask: Completable = metadataDownloadConfig().onErrorReturn { throwable: Throwable? ->
                 Log.d(TAG, "Can't read saved SMS metadata download config. Using default.")
                 defaultMetadataDownloadConfig()
             }.flatMap { config: GetMetadataIdsConfig? ->
                 webApiRepository.getMetadataIds(
                     config
                 )
             }.flatMapCompletable { metadata: SMSMetadata? ->
                 localDbRepository.setMetadataIds(
                     metadata
                 )
             }
             return localDbRepository.isModuleEnabled.flatMapCompletable { enabled: Boolean ->
                 if (enabled) {
                     return@flatMapCompletable refreshTask
                 } else {
                     Log.d(
                         TAG,
                         "Not refreshing SMS metadata, because sms module is disabled"
                     )
                     return@flatMapCompletable Completable.complete()
                 }
             }*/

        return rxCompletable {

            refreshMetadataIdsCoroutines()
        }

    }

    private suspend fun refreshMetadataIdsCoroutines() {
        val enabled = localDbRepository.isModuleEnabled.blockingGet()

        if (enabled) {
            val config = try {
                metadataDownloadConfig().blockingGet()

            } catch (ignored: Exception) {
                Log.d(TAG, "Can't read saved SMS metadata download config. Using default.")
                defaultMetadataDownloadConfig()
            }

            val metadata = webApiRepository.getMetadataIds(config)
            localDbRepository.setMetadataIds(metadata).blockingAwait()

        } else {
            Log.d(TAG, "Not refreshing SMS metadata, because sms module is disabled")
        }

    }

    /**
     * Callable that triggers the method [.refreshMetadataIds].
     * @return Callable object to refresh metadata ids.
     */
    internal suspend fun refreshMetadataIdsCallable() {
        try {
            Log.d(TAG, "Started SMS metadata sync.")
            refreshMetadataIds().blockingAwait()
            Log.d(TAG, "Completed SMS metadata sync.")
        } catch (ignored: Exception) {
            Log.d(TAG, "Error refreshing SMS metadata ids.")
        }

    }

    private fun defaultMetadataDownloadConfig(): GetMetadataIdsConfig {
        return GetMetadataIdsConfig()
    }


    class SmsConfig internal constructor(
        val isModuleEnabled: Boolean,
        val gateway: String?,
        val isWaitingForResult: Boolean,
        val resultSender: String?,
        val resultWaitingTimeout: Int
    )

    companion object {
        private val TAG = ConfigCase::class.java.simpleName
    }
}


/*
class ConfigCase(private val webApiRepository: WebApiRepository, private val localDbRepository: LocalDbRepository) {

    val smsModuleConfig: Single<SmsConfig>
        get() = Single.zip(
            localDbRepository.isModuleEnabled,
            localDbRepository.gatewayNumber,
            localDbRepository.waitingForResultEnabled,
            localDbRepository.confirmationSenderNumber,
            localDbRepository.waitingResultTimeout,
            { moduleEnabled: Boolean, gateway: String?, waitingForResult: Boolean, resultSender: String?, resultWaitingTimeout: Int ->
                SmsConfig(
                    moduleEnabled,
                    gateway,
                    waitingForResult,
                    resultSender,
                    resultWaitingTimeout
                )
            })

    */
/**
 * Set if the SDK has to wait for the result or not.
 * @param enabled
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setWaitingForResultEnabled(enabled: Boolean): Completable {
        return localDbRepository.setWaitingForResultEnabled(enabled)
    }

    */
/**
 * Set the number to receive messages from. Messages from other senders will not be read.
 * @param number The sender number.
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setConfirmationSenderNumber(number: String?): Completable {
        return localDbRepository.setConfirmationSenderNumber(number)
    }

    */
/**
 * Set the time in seconds to wait for the result.
 * @param timeoutSeconds Time in seconds.
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setWaitingResultTimeout(timeoutSeconds: Int): Completable {
        return localDbRepository.setWaitingResultTimeout(timeoutSeconds)
    }

    */
/**
 * Set the gateway number to send the SMS. This is a required parameter before sending SMS.
 * @param gatewayNumber The gateway numberf
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setGatewayNumber(gatewayNumber: String?): Completable {
        if (gatewayNumber == null || gatewayNumber.isEmpty()) {
            return Completable.error(java.lang.IllegalArgumentException("Gateway number can't be empty"))
        }
        return localDbRepository.setGatewayNumber(gatewayNumber)
    }

    */
/**
 * Delete the gateway number to send the SMS.
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun deleteGatewayNumber(): Completable {
        return localDbRepository.deleteGatewayNumber()
    }

    */
/**
 * Set if SMS Module is enabled or not. It is required to enable it before using it.
 * @param enabled If the module is enabled or not
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setModuleEnabled(enabled: Boolean): Completable {
        return localDbRepository.setModuleEnabled(enabled)
    }

    */
/**
 * Set a new MetadataDownload configuration.
 * @param metadataIdsConfig Configuration with the metadata ids.
 * @return `Completable` that completes when the configuration is changed.
 *//*

    fun setMetadataDownloadConfig(metadataIdsConfig: GetMetadataIdsConfig?): Completable {
        if (metadataIdsConfig == null) {
            return Completable.error(java.lang.IllegalArgumentException("Received null config"))
        }
        return localDbRepository.setMetadataDownloadConfig(metadataIdsConfig)
    }

    val metadataDownloadConfig: Single<GetMetadataIdsConfig>
        */
/**
 * Get the metadata download configuration.
 * @return `Single with the` metadata download configuration.
 *//*

        get() {
            return localDbRepository.metadataDownloadConfig
        }

    */
/**
 * Method to download metadata ids. This is required before using the SMS module.
 * @return `Completable` that completes when the metadata ids are downloaded.
 *//*

    fun refreshMetadataIds(): Completable {
        val refreshTask: Completable = metadataDownloadConfig.onErrorReturn({ throwable: Throwable? ->
            Log.d(
                TAG,
                "Can't read saved SMS metadata download config. Using default."
            )
            defaultMetadataDownloadConfig
        }).flatMap({ config: GetMetadataIdsConfig? ->
            webApiRepository.getMetadataIds(
                config
            )
        }).flatMapCompletable(
            { metadata: SMSMetadata? ->
                localDbRepository.setMetadataIds(
                    metadata
                )
            })
        return localDbRepository.isModuleEnabled.flatMapCompletable({ enabled: Boolean ->
            if (enabled) {
                return@flatMapCompletable refreshTask
            } else {
                Log.d(
                    TAG,
                    "Not refreshing SMS metadata, because sms module is disabled"
                )
                return@flatMapCompletable Completable.complete()
            }
        })
    }

    */
/**
 * Callable that triggers the method [.refreshMetadataIds].
 * @return Callable object to refresh metadata ids.
 *//*

    fun refreshMetadataIdsCallable(): Completable {
        return refreshMetadataIds()
            .doOnSubscribe({ d: Disposable? ->
                Log.d(
                    TAG,
                    "Started SMS metadata sync."
                )
            })
            .doOnComplete({
                Log.d(
                    TAG,
                    "Completed SMS metadata sync."
                )
            })
            .doOnError({ e: Throwable ->
                Log.d(
                    TAG,
                    e.javaClass.getSimpleName() + " Error on SMS metadata sync."
                )
            })
    }

    private val defaultMetadataDownloadConfig: GetMetadataIdsConfig
        private get() {
            return GetMetadataIdsConfig()
        }

    class SmsConfig internal constructor(
        val isModuleEnabled: Boolean,
        val gateway: String?,
        val isWaitingForResult: Boolean,
        val resultSender: String?,
        val resultWaitingTimeout: Int
    )

    companion object {
        private val TAG: String = ConfigCase::class.java.simpleName
    }
}*/
