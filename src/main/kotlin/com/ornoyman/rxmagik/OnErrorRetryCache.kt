package com.ornoyman.rxmagik

import io.reactivex.Single
import java.util.concurrent.Semaphore

/**
 * Wraps a [Single] such that result is cached in case of success but not in case of an error.
 * Re-subscription will either return the cached result if exists
 * or retry(re-subscribe to source [Single]) otherwise.
 * Subscription while an operation is already in-progress would block until it finishes,
 * or until subscription is disposed/cancelled.
 */
class OnErrorRetryCache<T : Any> private constructor(source: Single<T>) {

    private val deferred: Single<T>
    private val singlePermit = Semaphore(1)
    private var cache: Single<T>? = null
    private var inProgress: Single<T>? = null

    init {
        deferred = Single.defer { createWhenObserverSubscribes(source) }
    }

    private fun createWhenObserverSubscribes(source: Single<T>): Single<T> =
        Single.using(
            this::acquirePermit,
            { hasAcquired ->
                return@using if (hasAcquired) tryGetCached(source) else Single.never<T>()
            }, this::releasePermit, false
        )

    private fun acquirePermit(): Boolean {
        try {
            singlePermit.acquire()
        } catch (e: InterruptedException) {
            return false // might be interrupted if disposed/cancelled
        }
        return true
    }

    private fun tryGetCached(source: Single<T>): Single<T> {
        cache?.let { return it }
        return forceFetch(source)
    }

    private fun forceFetch(source: Single<T>): Single<T> =
        source
            .doOnSuccess { this.updateCache() }
            .cache().apply { inProgress = this }

    private fun updateCache() {
        cache = inProgress
    }

    private fun releasePermit(shouldRelease: Boolean) {
        if (!shouldRelease) {
            return
        }
        inProgress = null
        singlePermit.release()
    }

    companion object {
        @JvmStatic
        fun <T : Any> from(source: Single<T>): Single<T> = OnErrorRetryCache(source).deferred
    }
}