package com.ornoyman.rxmagik

import io.reactivex.Observable
import io.reactivex.functions.BiFunction

/***
 * Similar to [Observable.zip] but zips emitted item
 * from one stream with all items buffered in the other(since previous emission).
 * Also, does not complete once one of the source streams completes(continues to wait for the
 * other stream).
 */
fun <T1, T2, R> zipMany(
    o1: Observable<T1>,
    o2: Observable<T2>,
    combineFunction: BiFunction<in T1, in T2, out R>
): Observable<R> {
    val r1 =
        o1.map { item: T1 ->
            Observable.just(item)
        }.concatMapEager { o: Observable<T1> ->
            Observable.zip(
                o,
                o2,
                combineFunction
            )
        }
    val r2 =
        o2.map { item: T2 ->
            Observable.just(item)
        }.concatMapEager { o: Observable<T2> ->
            Observable.zip(
                o1,
                o,
                combineFunction
            )
        }
    return r1.mergeWith(r2).distinct()
}