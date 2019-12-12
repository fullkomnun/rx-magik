package com.ornoyman.rxmagik.test

import com.ornoyman.rxmagik.OnErrorRetryCache
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.atomic.AtomicInteger

object OnErrorRetryCacheSpecs : Spek({
    describe("a single is wrapped using 'OnErrorRetryCache' and subscribed to by 3 subscribers") {

        (1..50).forEach { testIndex ->

            context("first operation completes successfully $testIndex") {
                it("should cache result and emit same for all subscribers $testIndex") {
                    val results = listOf(
                        Single.just(1),
                        Single.just(2),
                        Single.just(3)
                    )
                    val counter = AtomicInteger(0)
                    val target = OnErrorRetryCache.from(Single.defer { results[counter.getAndIncrement()] })
                        .subscribeOn(Schedulers.io())
                    val tester1 = target.test()
                    val tester2 = target.test()
                    val tester3 = target.test()
                    tester1.awaitTerminalEvent(5, SECONDS)
                    tester2.awaitTerminalEvent(5, SECONDS)
                    tester3.awaitTerminalEvent(5, SECONDS)

                    tester1.assertValue(1)
                    tester1.assertNoErrors()
                    tester1.assertComplete()

                    tester2.assertValue(1)
                    tester2.assertNoErrors()
                    tester2.assertComplete()

                    tester3.assertValue(1)
                    tester3.assertNoErrors()
                    tester3.assertComplete()
                }
            }

            context("first operation fails, second operation succeeds $testIndex") {
                it("should emit error for first, then retry for second and return cached for third $testIndex") {
                    val results = listOf<Single<Int>>(
                        Single.error<Int>(IllegalStateException()),
                        Single.just(2),
                        Single.just(3)
                    )
                    val counter = AtomicInteger(0)
                    val target = OnErrorRetryCache.from(
                        Single.defer { results[counter.getAndIncrement()] })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.single())
                    val testers = mutableListOf<TestObserver<Int>>()
                    val tester1: TestObserver<Int> = TestObserver()
                    target.doOnTerminated {
                        testers.add(tester1); println("doFinally #1")
                    }.subscribe(tester1)
                    val tester2: TestObserver<Int> = TestObserver()
                    target.doOnTerminated {
                        testers.add(tester2); println("doFinally #2")
                    }.subscribe(tester2)
                    val tester3: TestObserver<Int> = TestObserver()
                    target.doOnTerminated {
                        testers.add(tester3); println("doFinally #3")
                    }.subscribe(tester3)
                    tester1.awaitTerminalEvent(5, SECONDS)
                    tester2.awaitTerminalEvent(5, SECONDS)
                    tester3.awaitTerminalEvent(5, SECONDS)
                    println("wait is over")

                    testers[0].apply {
                        assertNoValues()
                        assertError(IllegalStateException::class.java)
                        assertNotComplete()
                    }

                    testers[1].apply {
                        assertValue(2)
                        assertNoErrors()
                        assertComplete()
                    }

                    testers[2].apply {
                        assertValue(2)
                        assertNoErrors()
                        assertComplete()
                    }
                }
            }

            context(
                "first operation fails, second operation fails, third operation succeeds $testIndex"
            ) {
                it("should emit error for first and second, retry and emit result for third $testIndex") {
                    val results = listOf(
                        Single.error(IllegalStateException()),
                        Single.error(IllegalStateException()), Single.just(3)
                    )
                    val counter = AtomicInteger(0)
                    val target = OnErrorRetryCache.from(Single.defer { results[counter.getAndIncrement()] })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.single())
                    val testers = mutableListOf<TestObserver<Int>>()
                    val tester1: TestObserver<Int> = TestObserver()
                    target.doOnTerminated { testers.add(tester1); println("doFinally #1") }.subscribe(tester1)
                    val tester2: TestObserver<Int> = TestObserver()
                    target.doOnTerminated { testers.add(tester2); println("doFinally #2") }.subscribe(tester2)
                    val tester3: TestObserver<Int> = TestObserver()
                    target.doOnTerminated { testers.add(tester3); println("doFinally #3") }.subscribe(tester3)
                    tester1.awaitTerminalEvent(5, SECONDS)
                    tester2.awaitTerminalEvent(5, SECONDS)
                    tester3.awaitTerminalEvent(5, SECONDS)
                    println("wait is over")

                    testers[0].apply {
                        assertNoValues()
                        assertError(IllegalStateException::class.java)
                        assertNotComplete()
                    }

                    testers[1].apply {
                        assertNoValues()
                        assertError(IllegalStateException::class.java)
                        assertNotComplete()
                    }

                    testers[2].apply {
                        assertValue(3)
                        assertNoErrors()
                        assertComplete()
                    }
                }
            }

            context(
                "first operation canceled, seconds operation succeeds $testIndex"
            ) {
                it("should either cache result for first(prior to cancel) or retry for second $testIndex") {
                    val results = listOf(
                        Single.just(1)/*.delay(100, MILLISECONDS, Schedulers.single())*/,
                        Single.just(2), Single.just(3)
                    )
                    val counter = AtomicInteger(0)
                    val target = OnErrorRetryCache.from(Single.defer { results[counter.getAndIncrement()] })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.single())
                    val tester1: TestObserver<Int> = TestObserver()
                    target.subscribe(tester1)
                    //Thread.sleep(100)
                    tester1.cancel()
                    val tester2: TestObserver<Int> = TestObserver()
                    target.subscribe(tester2)
                    //tester1.awaitTerminalEvent()
                    tester2.awaitTerminalEvent(5, SECONDS)
                    println("wait is over")

                    tester1.apply {
                        assertOf { it.isCancelled }
                        if (valueCount() == 0) {
                            assertNoValues()
                            assertNotTerminated()
                        } else {
                            assertValue(1)
                            assertComplete()
                            assertNoErrors()
                        }
                    }

                    tester2.apply {
                        assertValue { it in (1..2) }
                        assertComplete()
                        assertNoErrors()
                    }
                }
            }
        }
    }
})

private inline fun <T : Any> Single<T>.doOnTerminated(crossinline action: () -> Unit): Single<T> =
    this.doOnSuccess { action() }
        .doOnError { action() }