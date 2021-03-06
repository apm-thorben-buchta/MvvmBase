package de.trbnb.mvvmbase.commands

/**
 * The basic contract for command implementations.
 *
 * @param P The parameter type for invocation. An instance of this has to be used to call [invoke]. [Unit] may be used if no parameter is neccessary.
 * @param R The return type for invocation. An instance of this has to be returned from [invoke].
 */
interface Command<in P, out R> {

    /**
     * Determines whether this Command is enabled or not.
     *
     * @return Returns `true` if this Command is enabled, otherwise `false`.
     */
    val isEnabled: Boolean

    /**
     * Invokes the Command.
     *
     * @throws de.trbnb.mvvmbase.commands.DisabledCommandInvocationException If [isEnabled] returns `false`.
     * @return A return type instance.
     */
    operator fun invoke(param: P): R

    /**
     * Invokes the Command only if [isEnabled] equals `true`.
     *
     * @return A return type instance if [isEnabled] equals `true` before invocation, otherwise `null`.
     */
    fun invokeSafely(param: P): R? {
        return if (isEnabled) invoke(param) else null
    }

    /**
     * Adds a listener that is notified when the value of [isEnabled] might have changed.
     */
    fun addEnabledListener(listener: (Boolean) -> Unit)

    /**
     * Removes a listener that is used for listening to changes to [isEnabled].
     * A listener that is passed to this method will not be notified anymore.
     */
    fun removeEnabledListener(listener: (Boolean) -> Unit)

    /**
     * Removes all listeners that are used for listening to changes to [isEnabled].
     * No previously added listeners will be notified anymore.
     */
    fun clearEnabledListeners()
}
