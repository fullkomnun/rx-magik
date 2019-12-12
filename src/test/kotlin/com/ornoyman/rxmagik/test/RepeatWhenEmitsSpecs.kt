package com.ornoyman.rxmagik.test

import com.ornoyman.rxmagik.repeatWhenEmits
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

object RepeatWhenEmitsSpecs : Spek({
    val driver by memoized { TestDriver() }

    describe(
        "An upstream of Integers that is resubscribed to upon completion whenever a trigger Unit stream emits"
    ) {

        context("[Unit]") {
            beforeEachTest {
                driver play {
                    trigger()
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[<trigger_complete>]") {
            beforeEachTest {
                driver play {
                    triggerComplete()
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[<trigger_error>]") {
            beforeEachTest {
                driver play {
                    triggerError(RuntimeException())
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[<complete>]") {
            beforeEachTest {
                driver play {
                    upstreamComplete()
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[<trigger_error>,<complete>]") {
            beforeEachTest {
                driver play {
                    triggerError(RuntimeException())
                    upstreamComplete()
                }
            }
            it("should emit [<error>]") {
                driver expect {
                    assertNoValues()
                    assertError(RuntimeException::class.java)
                }
            }
        }

        context("[<complete>,<trigger_error>]") {
            beforeEachTest {
                driver play {
                    upstreamComplete()
                    triggerError(RuntimeException())
                }
            }
            it("should emit [<error>]") {
                driver expect {
                    assertNoValues()
                    assertError(RuntimeException::class.java)
                }
            }
        }

        context("[<trigger_complete>,<complete>]") {
            beforeEachTest {
                driver play {
                    triggerComplete()
                    upstreamComplete()
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[<complete>,<trigger_complete>]") {
            beforeEachTest {
                driver play {
                    upstreamComplete()
                    triggerComplete()
                }
            }
            it("should emit []") {
                driver expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("[1,2]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                }
            }
            it("should emit [1,2]") {
                driver expect {
                    assertValues(1, 2)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<complete>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                }
            }
            it("should emit [1,2]") {
                driver expect {
                    assertValues(1, 2)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<complete>,<trigger_error>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    triggerError(RuntimeException())
                }
            }
            it("should emit [1,2]") {
                driver expect {
                    assertValues(1, 2)
                    assertError(RuntimeException::class.java)
                }
            }
        }

        context("[1,2,<complete>,Unit,3,4]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    trigger()
                    upstream(3, 4)
                }
            }
            it("should emit [1,2,3,4]") {
                driver expect {
                    assertValues(1, 2, 3, 4)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<complete>,Unit,3,4,<error>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    trigger()
                    upstream(3, 4)
                    upstreamError(RuntimeException())
                }
            }
            it("should emit [1,2,3,4,<error>]") {
                driver expect {
                    assertValues(1, 2, 3, 4)
                    assertError(RuntimeException::class.java)
                }
            }
        }

        context("[1,2,<complete>,Unit,3,4,Unit,5,<trigger_complete>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    trigger()
                    upstream(3, 4)
                    trigger()
                    upstream(5)
                    triggerComplete()
                }
            }
            it("should emit [1,2,3,4]") {
                driver expect {
                    assertValues(1, 2, 3, 4)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<complete>,Unit,3,4,<complete>,Unit,5,<trigger_complete>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    trigger()
                    upstream(3, 4)
                    upstreamComplete()
                    trigger()
                    upstream(5)
                    triggerComplete()
                }
            }
            it("should emit [1,2,3,4,5]") {
                driver expect {
                    assertValues(1, 2, 3, 4, 5)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<complete>,Unit,3,4,Unit,<complete>,5,<trigger_complete>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamComplete()
                    trigger()
                    upstream(3, 4)
                    trigger()
                    upstreamComplete()
                    upstream(5)
                    triggerComplete()
                }
            }
            it("should emit [1,2,3,4]") {
                driver expect {
                    assertValues(1, 2, 3, 4)
                    assertNotTerminated()
                }
            }
        }

        context("[1,2,<error>]") {
            beforeEachTest {
                driver play {
                    upstream(1, 2)
                    upstreamError(RuntimeException())
                }
            }
            it("should emit [1,2,<error>]") {
                driver expect {
                    assertValues(1, 2)
                    assertError(RuntimeException::class.java)
                }
            }
        }
    }
}) {
    class TestDriver {
        private val testScheduler = TestScheduler()
        private val trigger: PublishSubject<Unit> = PublishSubject.create()
        private var upstream: PublishSubject<Int> = PublishSubject.create()
        private val upstreams by lazy {
            ArrayDeque<PublishSubject<Int>>().apply { addLast(upstream) }
        }
        private val tester: TestObserver<Int> =
            Observable.defer { upstreams.pop() }
                .repeatWhenEmits(trigger).test()

        infix fun expect(expectation: TestObserver<Int>.() -> Unit) {
            tester.apply(expectation)
        }

        infix fun play(script: Play.() -> Unit) {
            Play(script)
        }

        inner class Play(script: Play.() -> Unit) {
            private var time = 1L

            init {
                script()
                testScheduler.advanceTimeBy(time + 1, MILLISECONDS)
            }

            fun upstreamError(th: Throwable) = upstream.playError(th)
            fun triggerError(th: Throwable) = trigger.playError(th)

            private fun <T> Subject<T>.playError(th: Throwable) {
                val o = Observable.error<T>(th).delay(++time, TimeUnit.MILLISECONDS, testScheduler, true)
                o.subscribe(this)
            }

            fun upstreamComplete() = upstream.playComplete()
            fun triggerComplete() = trigger.playComplete()

            private fun <T> Subject<T>.playComplete() {
                val o = Observable.empty<T>().delay(++time, TimeUnit.MILLISECONDS, testScheduler)
                o.subscribe(this)
            }

            fun upstream(vararg n: Int) = upstream.playNext(*n.toTypedArray())

            fun trigger() {
                trigger.playNext(Unit)
                this@TestDriver.upstream = PublishSubject.create()
                upstreams.addLast(this@TestDriver.upstream)
            }

            private fun <T> Subject<T>.playNext(vararg v: T) {
                val o = Observable.fromArray<T>(*v).concatWith(Observable.never()).delay(
                    ++time,
                    TimeUnit.MILLISECONDS,
                    testScheduler
                )
                o.subscribe(this)
            }
        }
    }
}