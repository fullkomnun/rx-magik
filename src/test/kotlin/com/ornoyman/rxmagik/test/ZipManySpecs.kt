package com.ornoyman.rxmagik.test

import com.ornoyman.rxmagik.zipMany
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS

object ZipManySpecs : Spek({
    val testController by memoized(CachingMode.EACH_GROUP) { TestDriver() }

    describe("zipMany invoked on two source streams") {

        context("none of the source streams emit or terminate") {
            it("should not emit or terminate result stream") {
                testController.expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("first source stream errors") {
            beforeGroup {
                testController play {
                    testSubject1.playError(UnsupportedOperationException(":("))
                }
            }

            it("should not emit and terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertError(UnsupportedOperationException::class.java)
                }
            }
        }

        context("second source stream errors") {
            beforeGroup {
                testController play {
                    testSubject2.playError(UnsupportedOperationException(":("))
                }
            }

            it("should not emit and terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertError(UnsupportedOperationException::class.java)
                }
            }
        }

        context("first source stream completes") {
            beforeGroup {
                testController play {
                    testSubject1.playComplete()
                }
            }

            it("should not emit or terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("second source stream completes") {
            beforeGroup {
                testController play {
                    testSubject1.playComplete()
                }
            }

            it("should not emit or terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("only first source stream emits items and completes") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                }
            }

            it("should not emit and not terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("only first source stream emits items and completes then second stream errors") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playError(UnsupportedOperationException(":("))
                }
            }

            it("should not emit and error result stream") {
                testController expect {
                    assertNoValues()
                    assertError(UnsupportedOperationException::class.java)
                }
            }
        }

        context("only first source stream emits items and completes then second stream completes") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playComplete()
                }
            }

            it("should not emit and complete result stream") {
                testController expect {
                    assertNoValues()
                    assertComplete()
                }
            }
        }

        context("only second source stream emits items and completes") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject2.playComplete()
                }
            }

            it("should not emit and not terminate result stream") {
                testController expect {
                    assertNoValues()
                    assertNotTerminated()
                }
            }
        }

        context("only second source stream emits items and completes then first stream errors") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject2.playComplete()
                    testSubject1.playError(UnsupportedOperationException(":("))
                }
            }

            it("should not emit and error result stream") {
                testController expect {
                    assertNoValues()
                    assertError(UnsupportedOperationException::class.java)
                }
            }
        }

        context("only second source stream emits items and completes then first stream completes") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject2.playComplete()
                    testSubject1.playComplete()
                }
            }

            it("should not emit and complete result stream") {
                testController expect {
                    assertNoValues()
                    assertComplete()
                }
            }
        }

        context("first source stream emits item then second source stream emits item") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject2.playNext("hello")
                }
            }

            it("should emit [1,hello] and not terminate result stream") {
                testController expect {
                    assertNotTerminated()
                    assertValue(Pair(1, "hello"))
                }
            }
        }

        context("second source stream emits item then first source stream emits item") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject2.playNext("hello")
                }
            }

            it("should emit [1,hello] and not terminate result stream") {
                testController expect {
                    assertNotTerminated()
                    assertValue(Pair(1, "hello"))
                }
            }
        }

        context("[1, 2, hello]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject2.playNext("hello")
                }
            }

            it("should not terminate and emit [ [1,hello] [2,hello] ]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"))
                }
            }
        }

        context("[1, 2, <error1>, hello]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playError(UnsupportedOperationException(":("))
                    testSubject2.playNext("hello")
                }
            }

            it("should not emit and error") {
                testController expect {
                    assertError(UnsupportedOperationException::class.java)
                    assertNoValues()
                }
            }
        }

        context("[1, 2, <complete1>, hello]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playNext("hello")
                }
            }

            it("should not terminate and emit [ [1,hello] [2,hello] ]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"))
                }
            }
        }

        context("[1, 2, <complete1>, hello, <complete2>]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playNext("hello")
                    testSubject2.playComplete()
                }
            }

            it("should complete and emit [ [1,hello] [2,hello] ]") {
                testController expect {
                    assertComplete()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"))
                }
            }
        }

        context("[1, hello, 2]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject2.playNext("hello")
                    testSubject1.playNext(2)
                }
            }

            it("should not terminate and emit [ [1,hello] [2,hello] ]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"))
                }
            }
        }

        context("[hello, 1, 2]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                }
            }

            it("should not terminate and emit [ [1,hello]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"))
                }
            }
        }

        context("[hello, world, 1]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject1.playNext(1)
                }
            }

            it("should not terminate and emit [ [1,hello] [1,world] ]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"))
                }
            }
        }

        context("[hello, 1, world]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject2.playNext("world")
                }
            }

            it("should not terminate and emit [ [1,hello] [1,world] ]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"))
                }
            }
        }

        context("[1, hello, world]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                }
            }

            it("should not terminate and emit [ [1,hello]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"))
                }
            }
        }

        context("[1, 2, hello, world]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                }
            }

            it("should not terminate and emit [ [1,hello], [2,hello]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"))
                }
            }
        }

        context("[1, hello, world, 2 ]") {
            beforeGroup {
                testController play {
                    testSubject1.playNext(1)
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject1.playNext(2)
                }
            }

            it("should not terminate and emit [ [1,hello], [2,hello], [2,world]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(2, "hello"), Pair(2, "world"))
                }
            }
        }

        context("[hello, 1, 2, world ]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject2.playNext("world")
                }
            }

            it("should not terminate and emit [ [1,hello], [1,world], [2,world]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"), Pair(2, "world"))
                }
            }
        }

        context("[hello, 1, 2, world, <error2> ]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject2.playNext("world")
                    testSubject2.playError(UnsupportedOperationException(":("))
                }
            }

            it("should emit [ [1,hello], [1,world], [2,world]] and error") {
                testController expect {
                    assertError(UnsupportedOperationException::class.java)
                    assertValues(Pair(1, "hello"), Pair(1, "world"), Pair(2, "world"))
                }
            }
        }

        context("[hello, 1, 2, world, <complete2> ]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject2.playNext("world")
                    testSubject2.playComplete()
                }
            }

            it("should not terminate and emit [ [1,hello], [1,world], [2,world]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"), Pair(2, "world"))
                }
            }
        }

        context("[hello, 1, 2, <complete1>, world ]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playNext("world")
                }
            }

            it("should not terminate and emit [ [1,hello], [1,world], [2,world]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"), Pair(2, "world"))
                }
            }
        }

        context("[hello, 1, 2, <complete1>, world, <complete2>]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                    testSubject1.playComplete()
                    testSubject2.playNext("world")
                    testSubject2.playComplete()
                }
            }

            it("should complete and emit [ [1,hello], [1,world], [2,world]]") {
                testController expect {
                    assertComplete()
                    assertValues(Pair(1, "hello"), Pair(1, "world"), Pair(2, "world"))
                }
            }
        }

        context("[hello, world, 1, 2 ]") {
            beforeGroup {
                testController play {
                    testSubject2.playNext("hello")
                    testSubject2.playNext("world")
                    testSubject1.playNext(1)
                    testSubject1.playNext(2)
                }
            }

            it("should not terminate and emit [ [1,hello], [1,world]]") {
                testController expect {
                    assertNotTerminated()
                    assertValues(Pair(1, "hello"), Pair(1, "world"))
                }
            }
        }
    }
}) {
    class TestDriver {
        val testScheduler = TestScheduler()
        val testSubject1 = PublishSubject.create<Int>()
        val testSubject2 = PublishSubject.create<String>()
        val testObserver = zipMany(testSubject1, testSubject2,
            BiFunction<Int, String, Pair<Int, String>> { n, str -> Pair(n, str) }).test()

        infix fun expect(expectation: TestObserver<Pair<Int, String>>.() -> Unit) {
            testObserver.apply(expectation)
        }

        infix fun play(script: Play.() -> Unit) {
            Play(script)
        }

        inner class Play(script: Play.() -> Unit) {
            private var time = 1L
            val testSubject1 = this@TestDriver.testSubject1
            val testSubject2 = this@TestDriver.testSubject2

            init {
                script()
                testScheduler.advanceTimeBy(time + 1, MILLISECONDS)
            }

            fun <T> Subject<T>.playError(th: Throwable) {
                val o = Observable.error<T>(th).delay(++time, MILLISECONDS, testScheduler, true)
                o.subscribe(this)
            }

            fun <T> Subject<T>.playComplete() {
                val o = Observable.empty<T>().delay(++time, MILLISECONDS, testScheduler)
                o.subscribe(this)
            }

            fun <T> Subject<T>.playNext(v: T) {
                val o = Observable.just<T>(v).concatWith(Observable.never()).delay(
                    ++time,
                    MILLISECONDS,
                    testScheduler
                )
                o.subscribe(this)
            }
        }
    }
}