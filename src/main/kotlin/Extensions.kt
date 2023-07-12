import kotlin.reflect.KClass

inline fun <R> (() -> R).multiCatch(vararg exceptions: KClass<out Throwable>, thenDo: (Throwable) -> R): R {
    return try {
        this()
    } catch (ex: Exception) {
        if (ex::class in exceptions) thenDo(ex) else throw ex
    }
}