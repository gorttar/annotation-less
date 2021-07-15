package org.gorttar.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.gorttar.annotation.Kind
import org.junit.jupiter.api.Test

private typealias WrapOf<A> = Kind<Wrap<*>, A>

private class Wrap<A>(val a: A) : WrapOf<A>

private fun <A> WrapOf<A>.fix(): Wrap<A> = this as Wrap<A>

private const val intKey = 1
private const val intValue = 2
private const val stringKey = "foo"
private const val stringValue = "bar"

internal class KindedMutableMapTest {
    private val map = kindedConcurrentHashMap<Wrap<*>>().also {
        it(intKey) to Wrap(intValue)
        it(stringKey) to Wrap(stringValue)
    }

    @Test
    fun `invoke - to - get combination should work`() {
        val wrappedInt: WrapOf<Int>? = map[intKey]
        assertThat(wrappedInt?.fix()?.a).isEqualTo(intValue)

        val wrappedString: WrapOf<String>? = map[stringKey]
        assertThat(wrappedString?.fix()?.a).isEqualTo(stringValue)

        assertThat(map["baz"]).isNull()
        assertThat(map[3]).isNull()
    }

    @Test
    fun `assign to null should remove key`() {
        map(stringKey) to null
        assertThat(map[stringKey]).isNull()
    }

    @Test
    fun remove() {
        map.remove(intKey)
        assertThat(map[intKey]).isNull()
    }
}
