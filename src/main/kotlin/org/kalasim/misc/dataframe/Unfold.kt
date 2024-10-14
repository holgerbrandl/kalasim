//package org.kalasim.misc.dataframe
//
//import org.jetbrains.kotlinx.dataframe.DataFrame
//import org.jetbrains.kotlinx.dataframe.api.*
//import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
//import kotlin.reflect.KCallable
//import kotlin.reflect.KProperty
//import kotlin.reflect.full.starProjectedType
//
////
//@JvmName("unfoldByProperty")
//inline fun  <reified T> DataFrame<*>.unfoldByProperty(
//    columnName: String,
//    properties: List<KCallable<*>>,
//    keep: Boolean = true,
//    addPrefix: Boolean = false,
//) = unfold<T>(columnName, properties.map{it.name}, keep, addPrefix)
//
//
//inline fun <reified T> DataFrame<*>.unfold(
//    column: String,
//    properties: List<String>? = detectPropertiesByReflection<T>().map { it.name },
//    keep: Boolean = false,
//    addPrefix: Boolean = false,
//): DataFrame<*> {
//    val unfold = inferType().select(column)
//        .unfold { cols { it.name() == column } }
//        .flatten()
//        .run {
//            if(properties != null) {
//                select { properties.toColumnSet() }
//            } else this
//        }
//
//    val withPrefix = if(addPrefix) {
//        unfold.rename(*unfold.columnNames().map { it to column + "_" + it }
//            .toTypedArray())
//    } else unfold
//
//    val df = if(!keep) remove(column) else this
//
//    // fix ambiguous names
//    val unfoldUnique = withPrefix.rename(
//        *withPrefix.columnNames()
//            .map {
//                it to (if(df.columnNames()
//                        .contains(it)
//                ) column + "_" + it else it)
//            }
//            .toTypedArray()
//    )
//
//    return df.add(unfoldUnique)
//}
//
//
////todo move to internal namespace to prevent API clutter
//inline fun <reified T> detectPropertiesByReflection(): List<KCallable<*>> {
//    val members = T::class.members
//
//    val propsOrGetters = members.filter {
//        //        it.parameters.isEmpty() // -> wrong because self pointer needs to be provided
//        when (it) {
//            is KProperty -> true
//            else -> {
//                val starProjectedType = T::class.starProjectedType
//                it.parameters.size == 1 && it.parameters.first().type == starProjectedType
//            }
//        }
//    }
//
//    return propsOrGetters.filterNot { it.name.run { equals("toString") || equals("hashCode") || matches("component[1-9][0-9]*".toRegex()) } }
//}