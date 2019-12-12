package com.ornoyman.rxmagik.test

import com.ornoyman.rxmagik.scanMap
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

object ScanMapSpecs : Spek({
    val testController by memoized(CachingMode.EACH_GROUP) { TestDriver() }

    describe(
        "scanMap invoked on source stream, each source emission defines number of next fibonacci pairs to emit"
    ) {
        context("source emits []") {
            it("should emits [${fibSeq(0)}]") {
                testController expect {
                    assertValueSequence(fibSeq(0))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [<error>]") {
            beforeGroup {
                testController play {
                    testSubject.playError(IOException("some error"))
                }
            }

            it("should emit [${fibSeq(0)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(0))
                    assertError(IOException::class.java)
                }
            }
        }

        context("source emits [<complete>]") {
            beforeGroup {
                testController play {
                    testSubject.playComplete()
                }
            }

            it("should emit [${fibSeq(0)}, <complete>]") {
                testController expect {
                    assertValueSequence(fibSeq(0))
                    assertComplete()
                }
            }
        }

        context("source emits [-1]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(-1)
                }
            }

            it("should emit [${fibSeq(0)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(0))
                    assertError(java.lang.IllegalArgumentException::class.java)
                }
            }
        }

        context("source emits [1]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(1)
                }
            }

            it("should emit [${fibSeq(1)}]") {
                testController expect {
                    assertValueSequence(fibSeq(1))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [1, <error>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(1)
                    testSubject.playError(IllegalAccessException("some error"))
                }
            }

            it("should emit [${fibSeq(1)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(1))
                    assertError(IllegalAccessException::class.java)
                }
            }
        }

        context("source emits [1, <complete>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(1)
                    testSubject.playComplete()
                }
            }

            it("should emit [${fibSeq(1)}, <complete>]") {
                testController expect {
                    assertValueSequence(fibSeq(1))
                    assertComplete()
                }
            }
        }

        context("source emits [1, -1]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(1, -1)
                }
            }

            it("should emit [${fibSeq(1)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(1))
                    assertError(java.lang.IllegalArgumentException::class.java)
                }
            }
        }

        context("source emits [3]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3)
                }
            }

            it("should emit [${fibSeq(3)}]") {
                testController expect {
                    assertValueSequence(fibSeq(3))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [3, <error>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3)
                    testSubject.playError(UnsupportedOperationException("some error"))
                }
            }

            it("should emit [${fibSeq(3)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(3))
                    assertError(UnsupportedOperationException::class.java)
                }
            }
        }

        context("source emits [3, <complete>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3)
                    testSubject.playComplete()
                }
            }

            it("should emit [${fibSeq(3)}, <complete>]") {
                testController expect {
                    assertValueSequence(fibSeq(3))
                    assertComplete()
                }
            }
        }

        context("source emits [3, -1]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3, -1)
                }
            }

            it("should emit [${fibSeq(3)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(3))
                    assertError(IllegalArgumentException::class.java)
                }
            }
        }

        context("source emits [3, 5]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3, 5)
                }
            }

            it("should emit [${fibSeq(8)}]") {
                testController expect {
                    assertValueSequence(fibSeq(8))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [3, 5] but max pair allowed is [${fib(1)}]") {
            beforeGroup {
                testController.maxPair = fib(1)
                testController play {
                    testSubject.playNext(3, 5)
                }
            }

            it("should emit [${fibSeq(1)}]") {
                testController expect {
                    assertValueSequence(fibSeq(1))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [3, 5] but max pair allowed is [${fib(4)}]") {
            beforeGroup {
                testController.maxPair = fib(4)
                testController play {
                    testSubject.playNext(3, 5)
                }
            }

            it("should emit [${fibSeq(4)}]") {
                testController expect {
                    assertValueSequence(fibSeq(4))
                    assertNotTerminated()
                }
            }
        }

        context("source emits [3, 5, <error>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3, 5)
                    testSubject.playError(IllegalStateException("some error"))
                }
            }

            it("should emit [${fibSeq(8)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(8))
                    assertError(IllegalStateException::class.java)
                }
            }
        }

        context("source emits [3, 5, <complete>]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3, 5)
                    testSubject.playComplete()
                }
            }

            it("should emit [${fibSeq(8)}, <complete>]") {
                testController expect {
                    assertValueSequence(fibSeq(8))
                    assertComplete()
                }
            }
        }

        context("source emits [3, 5, -1]") {
            beforeGroup {
                testController play {
                    testSubject.playNext(3, 5, -1)
                }
            }

            it("should emit [${fibSeq(8)}, <error>]") {
                testController expect {
                    assertValueSequence(fibSeq(8))
                    assertError(IllegalArgumentException::class.java)
                }
            }
        }
    }
}) {

    class TestDriver {
        val testScheduler = TestScheduler()
        val testSubject = PublishSubject.create<Int>()
        var maxPair: Pair<Int, Int> = Pair(Int.MAX_VALUE, Int.MAX_VALUE)

        fun nextFibonacci(
            fibonacciPair: Pair<Int, Int>, maxSteps: Int,
            steps: Int = 1
        ): Observable<Pair<Int, Int>> {
            validatePositive(maxSteps)
            return if (steps > maxSteps) Observable.empty<Pair<Int, Int>>() else Observable.just(
                Pair(
                    fibonacciPair.second,
                    fibonacciPair.first + fibonacciPair.second
                )
            ).concatMap { fib ->
                Observable.just(fib).delay {
                    Observable.timer(if (it <= maxPair) 0 else 50, MILLISECONDS, testScheduler)
                }.concatWith(nextFibonacci(fib, maxSteps, steps + 1))
            }
        }

        val testObserver = testSubject.scanMap(Observable.just(Pair(0, 1)),
            BiFunction { prev: Pair<Int, Int>, n: Int ->
                nextFibonacci(prev, n)
            }).test()

        infix fun expect(expectation: TestObserver<Pair<Int, Int>>.() -> Unit) {
            testObserver.apply(expectation)
        }

        infix fun play(script: Play.() -> Unit) {
            Play(script)
        }

        inner class Play(script: Play.() -> Unit) {
            private var time = 1L
            val testSubject = this@TestDriver.testSubject

            init {
                script()
                testScheduler.advanceTimeBy(time + 1, MILLISECONDS)
            }

            fun <T> Subject<T>.playError(th: Throwable) {
                val o = Observable.error<T>(th).delay(++time, TimeUnit.MILLISECONDS, testScheduler, true)
                o.subscribe(this)
            }

            fun <T> Subject<T>.playComplete() {
                val o = Observable.empty<T>().delay(++time, TimeUnit.MILLISECONDS, testScheduler)
                o.subscribe(this)
            }

            fun <T> Subject<T>.playNext(vararg v: T) {
                val o = Observable.fromArray(*v).concatWith(Observable.never()).delay(
                    ++time,
                    TimeUnit.MILLISECONDS,
                    testScheduler
                )
                o.subscribe(this)
            }
        }
    }
}

private operator fun Pair<Int, Int>.compareTo(other: Pair<Int, Int>): Int {
    return this.second.compareTo(other.second)
}

fun fib(n: Int): Pair<Int, Int> {
    return fibSeq(n).last()
}

fun fibSeq(n: Int): List<Pair<Int, Int>> {
    validateNonNegative(n)
    return (1..n).fold(listOf(Pair(0, 1)),
        { lst, _ -> lst + Pair(lst.last().second, lst.last().first + lst.last().second) })
}

fun validatePositive(n: Int) {
    if (n <= 0) {
        throw IllegalArgumentException()
    }
}

fun validateNonNegative(n: Int) {
    if (n < 0) {
        throw IllegalArgumentException()
    }
}