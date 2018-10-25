package de.trbnb.mvvmbase

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import de.trbnb.mvvmbase.events.Event
import de.trbnb.mvvmbase.events.addListener
import de.trbnb.mvvmbase.utils.findGenericSuperclass
import javax.inject.Provider

/**
 * Base class for Activities that serve as view within an MVVM structure.
 *
 * It automatically creates the binding and sets the view model as that bindings parameter.
 * Note that the parameter has to have to name "vm".
 *
 * The view model will be created by the Architecture Components, thus making sure it survives the
 * Activitys lifecycle. If an Activity is created for the first time the Loader will instantiate
 * the view model via the [viewModelProvider]. This [Provider] can either be implemented manually
 * or injected by DI.
 *
 * @param[VM] The type of the specific [ViewModel] implementation for this Activity.
 * @param[B] The type of the specific [ViewDataBinding] implementation for this Activity.
 */
abstract class MvvmBindingActivity<VM : BaseViewModel, B : ViewDataBinding> : AppCompatActivity() {

    /**
     * The [ViewDataBinding] implementation for a specific layout.
     * Will only be set in [onCreate].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected lateinit var binding: B
        private set

    /**
     * The [ViewModel] that is used for data binding.
     */
    protected val viewModel: VM by lazy {
        ViewModelProvider(viewModelStore, viewModelFactory)[viewModelClass].also { viewModel ->
            onViewModelLoaded(viewModel)
        }
    }

    /**
     * The [de.trbnb.mvvmbase.BR] value that is used as parameter for the view model in the binding.
     * Is always [de.trbnb.mvvmbase.BR.vm].
     */
    private val viewModelBindingId: Int
        get() = BR.vm

    /**
     * The layout resource ID for this Activity.
     * Is used in [onCreate] to create the [ViewDataBinding].
     */
    @get:LayoutRes
    protected abstract val layoutId: Int

    /**
     * The [Provider] implementation that is used if a new view model has to be instantiated.
     */
    abstract val viewModelProvider: Provider<VM>

    /**
     * Gets the class of the view model that an implementation uses.
     */
    private val viewModelClass: Class<VM>
        @Suppress("UNCHECKED_CAST")
        get() {
            val superClass = findGenericSuperclass<MvvmBindingActivity<VM, B>>() ?: throw IllegalStateException()
            return superClass.actualTypeArguments[0] as Class<VM>
        }

    /**
     * Creates a new view model via [viewModelProvider].
     */
    private val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return viewModelProvider.get() as T
        }
    }

    /**
     * Callback implementation that delegates the parametes to [onViewModelPropertyChanged].
     */
    private val viewModelObserver = object : Observable.OnPropertyChangedCallback(){
        @Suppress("UNCHECKED_CAST")
        override fun onPropertyChanged(sender: Observable, fieldId: Int) {
            onViewModelPropertyChanged(sender as VM, fieldId)
        }
    }

    /**
     * Called by the lifecycle.
     * Creates the [ViewDataBinding] and loads the view model.
     */
    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = initBinding()
    }

    /**
     * Creates the [ViewDataBinding].
     *
     * @return The new [ViewDataBinding] instance that fits this Activity.
     */
    private fun initBinding(): B = DataBindingUtil.setContentView<B>(this, layoutId).apply {
        setVariable(viewModelBindingId, viewModel)
    }

    /**
     * Called when the view model is loaded and is set as [viewModel].
     *
     * @param[viewModel] The [ViewModel] instance that was loaded.
     */
    @CallSuper
    protected open fun onViewModelLoaded(viewModel: VM) {
        viewModel.addOnPropertyChangedCallback(viewModelObserver)
        viewModel.onBind()
        viewModel.eventChannel.addListener(this, ::onEvent)
    }

    /**
     * Called when the view model notifies listeners that a property has changed.
     *
     * @param[viewModel] The [ViewModel] instance whose property has changed.
     * @param[fieldId] The ID of the field in the BR file that indicates which property in the view model has changed.
     */
    protected open fun onViewModelPropertyChanged(viewModel: VM, fieldId: Int) { }

    /**
     * Is called when the ViewModel sends an [Event].
     */
    protected open fun onEvent(event: Event) { }

    /**
     * Called by the lifecycle.
     * Removes the view model callback.
     * If the Activity is finishing the view model is notified.
     */
    @CallSuper
    override fun onDestroy() {
        super.onDestroy()

        viewModel.onUnbind()
        viewModel.removeOnPropertyChangedCallback(viewModelObserver)
    }
}
