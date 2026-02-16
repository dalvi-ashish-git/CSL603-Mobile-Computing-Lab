package com.yassineabou.calculator.util

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Extension function for Activities to create a lazy view binding delegate.
 * 
 * This function provides a convenient way to use View Binding in Activities
 * with automatic initialization and memory efficiency through lazy initialization.
 * 
 * Benefits:
 * - Type-safe view access (no findViewById needed)
 * - Lazy initialization (created only when first accessed)
 * - Thread-safe (LazyThreadSafetyMode.NONE for better performance)
 * - Reduces boilerplate code
 * 
 * Usage in Activity:
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private val binding by viewBinding(ActivityMainBinding::inflate)
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(binding.root)
 *         // Access views: binding.textView.text = "Hello"
 *     }
 * }
 * ```
 * 
 * @param factory A function that creates the binding from a LayoutInflater
 * @return A lazy delegate that provides the view binding instance
 */
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline factory: (LayoutInflater) -> T,
) =
    lazy(LazyThreadSafetyMode.NONE) {
        factory(layoutInflater)
    }

/**
 * Extension function for Fragments to create a lifecycle-aware view binding delegate.
 * 
 * This function provides View Binding for Fragments with proper lifecycle management
 * to prevent memory leaks. The binding is automatically cleaned up when the Fragment's
 * view is destroyed.
 * 
 * Key features:
 * - Lifecycle-aware (automatically nullifies binding on view destruction)
 * - Prevents memory leaks (sets binding to null in onDestroy)
 * - Type-safe view access
 * - Works with Fragment's view lifecycle
 * 
 * Memory leak prevention:
 * Fragments can outlive their views (e.g., on back stack), so we must null the binding
 * when the view is destroyed to avoid holding references to destroyed views.
 * 
 * Usage in Fragment:
 * ```kotlin
 * class CalculatorFragment : Fragment(R.layout.fragment_calculator) {
 *     private val binding by viewBinding(FragmentCalculatorBinding::bind)
 *     
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         // Access views: binding.button.setOnClickListener { }
 *     }
 *     // Binding automatically cleaned up when view is destroyed
 * }
 * ```
 * 
 * @param factory A function that creates the binding from the Fragment's root view
 * @return A ReadOnlyProperty delegate that provides and manages the view binding
 */
fun <T : ViewBinding> Fragment.viewBinding(factory: (View) -> T): ReadOnlyProperty<Fragment, T> =
    object : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
        // Nullable binding that gets cleared on view destruction
        private var binding: T? = null

        /**
         * Provides the binding instance, creating it if necessary.
         * 
         * Initialization is deferred until first access, and the lifecycle observer
         * is registered to handle cleanup automatically.
         */
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T =
            binding ?: factory(requireView()).also {
                // Only add observer if the view lifecycle is initialized
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                    viewLifecycleOwner.lifecycle.addObserver(this)
                    binding = it
                }
            }

        /**
         * Lifecycle callback that clears the binding when the Fragment's view is destroyed.
         * 
         * This is critical for preventing memory leaks in Fragments, as they can
         * exist on the back stack while their views are destroyed.
         */
        override fun onDestroy(owner: LifecycleOwner) {
            binding = null
        }
    }
