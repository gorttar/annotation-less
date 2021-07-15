package org.gorttar.proxy.lazy

import org.gorttar.annotation.Kind

/**
 * [Lazy] that additionally is a [Kind]
 */
@JvmInline
value class KindedLazy<out A>(private val delegate: Lazy<A>) : KindedLazyOf<A>, Lazy<A> {
    override val value: A get() = delegate.value
    override fun isInitialized(): Boolean = delegate.isInitialized()
}

typealias KindedLazyOf<A> = Kind<KindedLazy<*>, A>

@Suppress("NOTHING_TO_INLINE")
inline fun <A> KindedLazyOf<A>.fix(): KindedLazy<A> = this as KindedLazy<A>
fun <A> kindedLazy(initializer: () -> A): KindedLazyOf<A> = KindedLazy(lazy(initializer))
