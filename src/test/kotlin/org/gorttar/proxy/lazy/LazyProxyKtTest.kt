package org.gorttar.proxy.lazy

import assertk.Assert
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import org.gorttar.annotation.AllOpen
import org.gorttar.test.dynamicTests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.lang.reflect.Constructor
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

internal interface WhateverInterface
internal abstract class WhateverAbstract : WhateverInterface
internal sealed class WhateverSealed : WhateverAbstract()
internal open class WhateverOpen : WhateverSealed()
internal enum class WhateverEnum

@Suppress("JoinDeclarationAndAssignment")
internal class WhateverClass(val x: Nothing?) : WhateverSealed() {
    val constructorCalled: Boolean

    init {
        constructorCalled = true
    }

}

internal data class WhateverData(val x: Int, val s: String) : WhateverSealed()
internal object WhateverObject : WhateverSealed()

private interface AllOpenInterface {
    fun interfaceFun()
    var interfaceProp: String
}

private abstract class AllOpenAbstract {
    abstract fun abstractFun()
    abstract var abstractProp: String
}

private open class AllOpenInterfaceImpl : AllOpenInterface {
    override fun interfaceFun(): Unit = Unit
    override var interfaceProp: String = "AllOpenInterfaceImpl"

    @Suppress("unused")
    open fun openFun(): Unit = Unit
}

private open class AllOpenAbstractImpl : AllOpenAbstract() {
    override fun abstractFun(): Unit = Unit
    override var abstractProp: String = "AllOpenAbstractImpl"

    @Suppress("unused")
    open fun openFun(): Unit = Unit
}

@AllOpen
internal class AllOpenAnnotated {
    @Suppress("unused")
    fun openFun(): Unit = Unit

    @Suppress("unused")
    var openProp: String = "AllOpenAnnotated"
}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS")
private class NotOpenClass {
    @Suppress("unused")
    open fun openFun(): Unit = Unit

    @Suppress("unused")
    open var openProp: String = "AllOpenAnnotated"
}

private open class NotOpenFun {
    fun notOpenFun(): Unit = Unit

    @Suppress("unused")
    open var openProp: String = "AllOpenAnnotated"
}

private open class NotOpenProp {
    @Suppress("unused")
    open fun openFun(): Unit = Unit
    var notOpenProp: String = "AllOpenAnnotated"
}

private open class NotOpenParentFun : NotOpenFun()
private open class NotOpenParentProp : NotOpenProp()

@AllOpen
internal class A(val b: B) {
    override fun equals(other: Any?): Boolean = this === other ||
        other is A && this::class == other::class && b === other.b

    override fun hashCode(): Int = System.identityHashCode(b)
}

@AllOpen
internal class B(val a: A) {
    override fun equals(other: Any?): Boolean = this === other ||
        other is B && this::class == other::class && a === other.a

    override fun hashCode(): Int = System.identityHashCode(a)
}

inline fun <reified S : Any> Assert<*>.isInstanceOf(): Assert<S> = isNotNull().isInstanceOf(S::class)

internal class LazyProxyKtTest {
    @Test
    fun isOverriddenBy() = assertAll {
        class C {
            override fun equals(other: Any?): Boolean = this === other || other is C

            @Suppress("unused")
            fun equals(): Boolean = false
            fun eq(other: Any?): Boolean = equals(other)
            override fun hashCode(): Int = 0
        }


        val equals = Any::equals
        assertThat(equals.isOverriddenBy(equals.javaMethod)).isTrue()
        assertThat(
            equals.isOverriddenBy(
                C::class.declaredFunctions.single { it.name == equals.name && it.parameters.size == 2 }.javaMethod
            )
        ).isTrue()
        assertThat(equals.isOverriddenBy(null)).isFalse()
        assertThat(equals.isOverriddenBy(C::eq.javaMethod)).isFalse()
        assertThat(
            equals.isOverriddenBy(
                C::class.declaredFunctions.single { it.name == equals.name && it.parameters.size == 1 }.javaMethod
            )
        ).isFalse()
    }

    @Test
    fun lazyProxy() = assertAll {
        lateinit var bProxy: B
        val aProxy: A = lazyProxy { A(bProxy) }
        bProxy = lazyProxy { B(aProxy) }

        val a = A(bProxy)
        val b = B(aProxy)

        assertThat(aProxy).isEqualTo(aProxy)
        assertThat(bProxy).isEqualTo(bProxy)

        assertThat(aProxy).isNotEqualTo(lazyProxy { a })
        assertThat(bProxy).isNotEqualTo(lazyProxy { b })
        assertThat(lazyProxy { a }).isNotEqualTo(aProxy)
        assertThat(lazyProxy { b }).isNotEqualTo(bProxy)

        assertThat(aProxy).isNotEqualTo(a)
        assertThat(bProxy).isNotEqualTo(b)
        assertThat(a).isNotEqualTo(aProxy)
        assertThat(b).isNotEqualTo(bProxy)

        generateSequence(aProxy to bProxy) { (a, b) -> b.a to a.b }.take(20).forEach { (an, bn) ->
            assertThat(aProxy).isEqualTo(an)
            assertThat(bProxy).isEqualTo(bn)
            assertThat(an).isEqualTo(aProxy)
            assertThat(bn).isEqualTo(bProxy)

            assertThat(aProxy).isSameAs(an)
            assertThat(bProxy).isSameAs(bn)
        }
    }

    @Test
    fun `instantiate WhateverOpen`() =
        assertThat { WhateverOpen::class.java.instantiate() }.isSuccess().isInstanceOf<WhateverOpen>().given { }

    @Test
    fun `instantiate WhateverClass`() = assertThat { WhateverClass::class.java.instantiate() }.isSuccess().given {
        assertThat(it).isInstanceOf<WhateverClass>()
        assertThat(it.constructorCalled).isFalse()
        assertThat(it.x).isNull()
    }

    @Test
    fun `instantiate WhateverData`() = assertThat { WhateverData::class.java.instantiate() }.isSuccess().given {
        assertThat(it).isInstanceOf<WhateverData>()
        assertThat(it.x).isZero()
        /** see [instantiate] disclaimer */
        assertThat(it.s).isNull()
    }

    @Test
    fun `instantiate WhateverEnum`() = assertThat { WhateverEnum::class.java.instantiate() }.isFailure()
        .hasMessage("Shouldn't instantiate enums!")

    @Test
    fun `instantiate WhateverSealed`() = assertThat {
        WhateverClass::class.java.instantiate<WhateverSealed>()
    }.isFailure().hasMessage("Shouldn't instantiate sealed classes!")

    @Test
    fun `instantiate WhateverObject`() = assertThat { WhateverObject::class.java.instantiate() }.isFailure()
        .hasMessage("Shouldn't instantiate objects!")

    @Test
    fun `instantiate Nothing`() = assertThat { Nothing::class.java.instantiate() }.isFailure()
        .hasMessage("Shouldn't instantiate Nothing!")

    @TestFactory
    fun firstNoArgConstructor() = dynamicTests(
        firstNoArgConstructorCase<WhateverInterface>(::Any.javaConstructor),
        firstNoArgConstructorCase<WhateverAbstract>(),
        firstNoArgConstructorCase<WhateverSealed>(),
        firstNoArgConstructorCase<WhateverOpen>(),
        firstNoArgConstructorCase<WhateverEnum>(::Any.javaConstructor),
        firstNoArgConstructorCase<WhateverClass>(WhateverSealed::class.java.declaredConstructors.first()),
        firstNoArgConstructorCase<WhateverData>(WhateverSealed::class.java.declaredConstructors.first()),
        firstNoArgConstructorCase<WhateverObject>()
    ) { assertThat(clazz.firstNoArgConstructor()).isEqualTo(expected) }

    @TestFactory
    fun `requireAllOpen success`() = dynamicTests(
        successCase<AllOpenInterface>(),
        successCase<AllOpenAbstract>(),
        successCase<AllOpenInterfaceImpl>(),
        successCase<AllOpenAbstractImpl>(),
        successCase<AllOpenAnnotated>()
    ) { assertThat { kClass.requireAllOpen() }.isSuccess().isEqualTo(kClass) }

    @TestFactory
    fun `requireAllOpen fail`() = dynamicTests(
        failCase<NotOpenClass> { "$it is final thus can't be subclassed" },
        failCase<NotOpenFun> {
            "There are not open functions which can't be intercepted in $it: [${NotOpenFun::notOpenFun.name} ($it)]"
        },
        failCase<NotOpenProp> {
            "There are not open properties which can't be intercepted in $it: [${NotOpenProp::notOpenProp.name} ($it)]"
        },
        failCase<NotOpenParentFun> {
            "There are not open functions which can't be intercepted in $it: " +
                "[${NotOpenFun::notOpenFun.name} (${NotOpenFun::class})]"
        },
        failCase<NotOpenParentProp> {
            "There are not open properties which can't be intercepted in $it: " +
                "[${NotOpenProp::notOpenProp.name} (${NotOpenProp::class})]"
        }
    ) {
        assertThat { kClass.requireAllOpen() }.isFailure().isInstanceOf<IllegalArgumentException>().hasMessage(message)
    }

    private inline fun <reified T : Any> successCase() = RequireAllOpenSuccessCase(T::class)
    private inline fun <reified T : Any> failCase(message: (KClass<T>) -> String) =
        T::class.let { RequireAllOpenFailCase(it, message(it)) }

    private inline fun <reified T : Any> firstNoArgConstructorCase(
        expected: Constructor<*>? = T::class.java.declaredConstructors.first()
    ) = FirstNoArgConstructorCase(T::class.java, expected)

    private class FirstNoArgConstructorCase(val clazz: Class<*>, val expected: Constructor<*>?) {
        override fun toString(): String = "firstNoArgConstructor[clazz=${clazz.simpleName}, expected=$expected]"
    }

    private class RequireAllOpenSuccessCase(val kClass: KClass<*>) {
        override fun toString(): String = "requireAllOpen success[class=${kClass.simpleName}]"
    }

    private class RequireAllOpenFailCase(val kClass: KClass<*>, val message: String) {
        override fun toString(): String = "requireAllOpen fail[class=${kClass.simpleName}]"
    }
}