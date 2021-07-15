package org.gorttar.annotation

/**
 * [Kind]<[F], [A]> represents a generic [F]<[A]> in a way that's allowed by Kotlin.
 * To revert it back to its original form use the extension function `fix()`.
 *
 * ```kotlin:ank:playground
 * import org.gorttar.annotation.Kind
 * import org.gorttar.proxy.lazy.KindedLazy
 * import org.gorttar.proxy.lazy.fix
 * import org.gorttar.proxy.lazy.kindedLazy

 * fun main() {
 *     //sampleStart
 *     val a: Kind<KindedLazy<*>, Int> = kindedLazy { 1 }
 *     val fix: KindedLazy<Int> = a.fix()
 *     //sampleEnd
 *     println(fix.value)
 * }
 * ```
 */
interface Kind<out F : Kind<F, *>, out A>
