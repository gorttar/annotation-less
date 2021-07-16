package org.gorttar.proxy.lazy

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import sun.reflect.ReflectionFactory.getReflectionFactory
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

/**
 * creates a proxy of type [T] which delegates all calls to an object lazily created by [constructor]
 * Warning! Proxy changes the behavior of [Any.equals] and [Any.hashCode] functions to identity equals/hashCode
 * in order to fulfil [Any.equals] requirements
 */
inline fun <reified T : Any> lazyProxy(
    crossinline constructor: () -> T
): T = T::class.requireAllOpen().lazyProxy(lazy { constructor() }) as T

/**
 * searches for first no argument constructor in [this] class hierarchy
 */
fun Class<*>.firstNoArgConstructor(): Constructor<*> = generateSequence(this, Class<*>::getSuperclass).map(
    Class<*>::getDeclaredConstructors
).flatMap(Array<Constructor<*>>::asSequence).firstOrNull { it.parameterCount == 0 }?.also { it.isAccessible = true }
    ?: ::Any.javaConstructor!!

/**
 * checks if [this] [KFunction] has [javaMethod] overridden by [override].
 * In other words [this] contains [override] or [override] in [this]
 */
operator fun KFunction<*>.contains(
    override: Method?
): Boolean = javaMethod == override || override != null && functionToMethodToContainsResultCache.computeIfAbsent(this) {
    ConcurrentHashMap()
}.computeIfAbsent(override) {
    javaMethod?.run { override.let { name == it.name && parameterTypes.contentEquals(it.parameterTypes) } } ?: false
}

/**
 * checks if all [memberFunctions] and [memberProperties] of [this] [KClass] are open
 * and fails otherwise
 */
fun <T : Any> KClass<T>.requireAllOpen(): KClass<T> = apply {
    require(!isFinal) { "$this is final thus can't be subclassed" }
    if (!java.isInterface) {
        require(memberFunctions.all { it.visibility == KVisibility.PRIVATE || !it.isFinal }) {
            "There are not open functions which can't be intercepted in $this: " +
                "${memberFunctions.filter { !it.isOpen }.map { "${it.name} (${it.javaMethod?.declaringClass})" }}"
        }
        require(memberProperties.all { it.visibility == KVisibility.PRIVATE || !it.isFinal }) {
            "There are not open properties which can't be intercepted in $this: " +
                "${memberProperties.filter { !it.isOpen }.map { "${it.name} (${it.javaGetter?.declaringClass})" }}"
        }
    }
}

@PublishedApi
internal fun <T : Any> KClass<out T>.lazyProxy(lazy: Lazy<T>): Any =
    kClassToProxyDataCache.computeIfAbsent(this) { kClass ->
        ByteBuddy().subclass(java).method(ElementMatchers.any()).intercept(
            InvocationHandlerAdapter.of { proxy, m, args ->
                when (m) {
                    in Any::equals -> proxy === args.first()
                    in Any::hashCode -> System.identityHashCode(proxy)
                    else -> m(kClassToProxyDataCache[kClass]!!.proxyToLazy[proxy]!!.value, *args)
                }
            }
        ).make().load(java.classLoader).loaded.noArgConstructor(this).let(::LazyProxyData)
    }.run { proxyConstructor.newInstance().also { proxyToLazy[it] = lazy } }

/**
 * Unsafe function! Returned constructor can instantiate an object of any [this] class
 * and this can break some type system warranties eg:
 * instances of a class with nulls in not nullable fields
 *
 * Use with care.
 */
internal fun <T : Any> Class<out T>.noArgConstructor(sourceClass: KClass<out T>): Constructor<*> {
    require(!sourceClass.isSubclassOf(Enum::class)) { "Shouldn't instantiate enums!" }
    require(!sourceClass.isSealed) { "Shouldn't instantiate sealed classes!" }
    require(sourceClass.objectInstance == null) { "Shouldn't instantiate objects!" }
    require(this != Nothing::class.java) { "Shouldn't instantiate Nothing!" }
    return getReflectionFactory().newConstructorForSerialization(this, firstNoArgConstructor())
}

private class LazyProxyData(val proxyConstructor: Constructor<*>) {
    val proxyToLazy = ConcurrentHashMap<Any, Lazy<*>>()
}

private val kClassToProxyDataCache = ConcurrentHashMap<KClass<*>, LazyProxyData>()
private val functionToMethodToContainsResultCache = ConcurrentHashMap<KFunction<*>, MutableMap<Method, Boolean>>()
