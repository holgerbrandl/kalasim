//package com.github.holgerbrandl.kdfutils
//
//import org.jetbrains.kotlinx.dataframe.DataFrame
//import org.jetbrains.kotlinx.dataframe.api.into
//import org.jetbrains.kotlinx.dataframe.api.rename
//
//
////https://stackoverflow.com/questions/60010298/how-can-i-convert-a-camel-case-string-to-snake-case-and-back-in-idiomatic-kotlin
//
//internal  fun String.camelToSnakeCase(): String {
//    return "(?<=[a-zA-Z])[A-Z]".toRegex()
//        .replace(this) {
//            "_${it.value}"
//        }
//        .lowercase()
//}
//
//
//internal fun String.snakeToCamelCase(): String {
//    return "_[a-zA-Z0-9]".toRegex()
//        .replace(this) {
//            it.value.replace("_", "")
//                .uppercase()
//        }
//}
//
//fun String.toSnakeCase() = cleanNames().camelToSnakeCase()
//
///** Implements a partial contract of https://rdrr.io/cran/janitor/man/clean_names.html */
//fun String.cleanNames(): String = replace("[#+=.,()/*: -]+".toRegex(), "_")
//    .replace("[", "")
//    .replace("]", "")
//    // remove leading and tailing underscores
//    .replace("[_]+$".toRegex(), "")
//    .replace("^[_]+".toRegex(), "")
//
//
///** Convert column names to snake_case */
//fun DataFrame<*>.renameToSnakeCase() = rename { all() }.into {
//    it.name.toSnakeCase()
//}
//
///** Convert column names to camelCase */
//fun DataFrame<*>.renameToCamelCase() = renameToSnakeCase()
//    .rename { all() }
//    .into { it.name.snakeToCamelCase() }
//
///** Convert column names to kebap-case */
//fun DataFrame<*>.renameToKebapCase() = renameToSnakeCase()
//    .rename { all() }
//    .into { it.name.replace("_", "-") }
//
//
//fun DataFrame<*>.renameToSpaces() = renameToSnakeCase()
//    .rename { all() }
//    .into { it.name.snakeCaseToSpaces() }
//
//// via chat-gpt
//fun String.snakeCaseToSpaces(): String {
//    return split("_").joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }
//}
//
