package org.gorttar.annotation

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS, ANNOTATION_CLASS)
@Retention(RUNTIME)
annotation class AllOpen
