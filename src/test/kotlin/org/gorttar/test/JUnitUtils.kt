package org.gorttar.test

import org.junit.jupiter.api.DynamicTest
import kotlin.streams.asStream

fun <Case> dynamicTests(
    vararg cases: Case,
    assertion: Case.() -> Unit
) = cases.asSequence().dynamicTests(assertion)

fun <Case> Sequence<Case>.dynamicTests(assertion: Case.() -> Unit) =
    map { DynamicTest.dynamicTest(it.toString()) { assertion(it) } }.asStream()