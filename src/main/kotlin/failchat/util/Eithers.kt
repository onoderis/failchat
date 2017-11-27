package failchat.util

import either.Either
import either.fold

fun <T> Either<T, T>.any() = fold({ it }, { it })
