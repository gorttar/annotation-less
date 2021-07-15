package org.gorttar.data

import org.gorttar.annotation.Kind
import org.gorttar.common.discarded
import java.util.concurrent.ConcurrentHashMap

/**
 * setter for a value of type [Kind]<[K], [A]>?
 */
@Suppress("KDocUnresolvedReference")
typealias ValueSetter<K, A> = (value: Kind<K, A>?) -> Unit

/**
 * [MutableMap] like collection for values of [Kind]<[K], *> and arbitrary key/value type
 */
interface KindedMutableMap<K : Kind<K, *>> {
    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    operator fun <A : Any> get(key: A): Kind<K, A>?

    /**
     * Returns the [ValueSetter] corresponding to the given [key]
     * [ValueSetter] based mutations are intended to short circuit type inference for [A]
     * thus ensuring that it's impossible to associate a value of type [Kind]<[K], [Int]> with a key of type [String]
     */
    operator fun <A : Any> invoke(key: A): ValueSetter<K, A>
}

/**
 * [ConcurrentHashMap] backed implementation of [KindedMutableMap]
 */
private class KindedConcurrentHashMap<K : Kind<K, *>> : KindedMutableMap<K> {
    private val data: MutableMap<Any, Kind<K, *>> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any> get(key: A): Kind<K, A>? = data[key] as Kind<K, A>?
    override fun <A : Any> invoke(key: A): ValueSetter<K, A> =
        { it?.let { data[key] = it } ?: data.remove(key).discarded }
}

fun <K : Kind<K, *>> kindedConcurrentHashMap(): KindedMutableMap<K> = KindedConcurrentHashMap()

/**
 * syntax sugar for invoking [this] [ValueSetter] on [value]
 *
 * ```kotlin:ank:playground
 * import org.gorttar.annotation.Kind
 * import org.gorttar.data.KindedMutableMap
 * import org.gorttar.data.kindedConcurrentHashMap
 * import org.gorttar.data.to
 * import org.gorttar.proxy.lazy.KindedLazy
 * import org.gorttar.proxy.lazy.fix
 * import org.gorttar.proxy.lazy.kindedLazy
 *
 * fun main() {
 *     //sampleStart
 *     val map = kindedConcurrentHashMap<KindedLazy<*>>()
 *     map("foo") to kindedLazy { "bar" } // similar to map["foo"] = kindedLazy { "bar" }
 *     //sampleEnd
 *     val a: Kind<KindedLazy<*>, String>? = map["foo"]
 *     val fixedA: KindedLazy<String>? = a?.fix()
 *     val message: String? = fixedA?.value
 *     println(message)
 * }
 * ```
 */
infix fun <K : Kind<K, *>, A> ValueSetter<K, A>.to(value: Kind<K, A>?): Unit = this(value)
fun KindedMutableMap<*>.remove(key: Any): Unit = this(key) to null
