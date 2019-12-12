package com.ornoyman.rxmagik

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer

/**
 * A Transformer that takes a source observable and re-subscribes to the upstream Observable when
 * it emits.
 */
internal class RepeatWhenEmits<T> private constructor(
    private val source: Observable<*>
) : ObservableTransformer<T, T> {

    override fun apply(upstream: Observable<T>): ObservableSource<T> =
        upstream.repeatWhen { events -> events.switchMap { source } }

    companion object {
        fun <T> from(source: Observable<*>): RepeatWhenEmits<T> = RepeatWhenEmits(source)
    }
}

fun <T> Observable<T>.repeatWhenEmits(source: Observable<*>): Observable<T> =
    this.compose(RepeatWhenEmits.from(source))