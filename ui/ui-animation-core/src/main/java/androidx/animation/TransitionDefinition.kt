/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.animation

import kotlin.experimental.ExperimentalTypeInference

/**
 * Static specification for the transition from one state to another.
 *
 * Each property involved in the states that the transition is from and to can have an animation
 * associated with it. When such an animation is defined, the animation system will be using it
 * instead of the default [SpringAnimation] animation to animate the value change for that property.
 *
 * @sample androidx.animation.samples.TransitionSpecWith3Properties
 **/
class TransitionSpec<S> internal constructor(private val fromToPairs: Array<out Pair<S?, S?>>) {

    /**
     * Optional state where should we start switching after this transition finishing.
     */
    var nextState: S? = null

    /**
     * The interruption handling mechanism. The default interruption handling is
     * [InterruptionHandling.PHYSICS]. Meaning both value and velocity of the property will be
     * preserved as the target state (and therefore target animation value) changes.
     * [InterruptionHandling.TWEEN], which only ensures the continuity of current animation value.
     * [InterruptionHandling.UNINTERRUPTIBLE] defines a scenario where an animation is so important
     * that it cannot be interrupted, so the new state request has to be queued.
     * [InterruptionHandling.SNAP_TO_END] can be used for cases where higher priority events (such
     * as user gesture) come in and the on-going animation needs to finish immediately to give way
     * to the user events.
     */
    var interruptionHandling: InterruptionHandling = InterruptionHandling.PHYSICS

    /**
     * The default animation to use when it wasn't explicitly provided for a property
     */
    internal var defaultAnimation: DefaultTransitionAnimation = SpringTransition()

    private val propAnimation: MutableMap<PropKey<*, *>, Animation<*>> = mutableMapOf()

    internal fun <T, V : AnimationVector> getAnimationForProp(prop: PropKey<T, V>): Animation<V> {
        @Suppress("UNCHECKED_CAST")
        return (propAnimation.getOrPut(prop,
            { defaultAnimation.createDefault(prop.typeConverter) })) as Animation<V>
    }

    internal fun defines(from: S?, to: S?) =
        fromToPairs.any { it.first == from && it.second == to }

    /**
     * Associates a property with a [AnimationBuilder]
     *
     * @param builder: [AnimationBuilder] for animating [this] property value changes
     */
    infix fun <T, V : AnimationVector> PropKey<T, V>.using(builder: AnimationBuilder<T>) {
        propAnimation[this] = builder.build(typeConverter)
    }

    /**
     * Creates a [Tween] animation, initialized with [init]
     *
     * @param init Initialization function for the [Tween] animation
     */
    fun <T> tween(init: TweenBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> =
        TweenBuilder<T>().apply(init)

    /**
     * Creates a [SpringAnimation] animation, initialized with [init]
     *
     * @param init Initialization function for the [SpringAnimation] animation
     */
    fun <T> physics(init: PhysicsBuilder<T>.() -> Unit): AnimationBuilder<T> =
        PhysicsBuilder<T>().apply(init)

    /**
     * Creates a [Keyframes] animation, initialized with [init]
     *
     * @param init Initialization function for the [Keyframes] animation
     */
    fun <T> keyframes(init: KeyframesBuilder<T>.() -> Unit): KeyframesBuilder<T> =
        KeyframesBuilder<T>().apply(init)

    /**
     * Creates a [Repeatable] animation, initialized with [init]
     *
     * @param init Initialization function for the [Repeatable] animation
     */
    fun <T> repeatable(init: RepeatableBuilder<T>.() -> Unit): AnimationBuilder<T> =
        RepeatableBuilder<T>().apply(init)

    /**
     * Creates a Snap animation for immediately switching the animating value to the end value.
     */
    fun <T> snap(): AnimationBuilder<T> = SnapBuilder()
}

internal interface DefaultTransitionAnimation {
    fun <T, V : AnimationVector> createDefault(typeConverter: TwoWayConverter<T, V>): Animation<V>
}

internal class SnapTransition : DefaultTransitionAnimation {
    override fun <T, V : AnimationVector> createDefault(
        typeConverter: TwoWayConverter<T, V>
    ): Animation<V> {
        return SnapBuilder<T>().build(typeConverter)
    }
}

internal class SpringTransition : DefaultTransitionAnimation {
    override fun <T, V : AnimationVector> createDefault(
        typeConverter: TwoWayConverter<T, V>
    ): Animation<V> {
        return PhysicsBuilder<T>().build(typeConverter)
    }
}

/**
 * [TransitionDefinition] contains all the animation related configurations that will be used in
 * a state-based transition. It holds a set of [TransitionState]s and an optional set of
 * [TransitionSpec]s. It can be used in [android.ui.animation.Transition] to create a state-based
 * animation in Compose.
 *
 * Each [TransitionState] specifies how the UI should look in terms of values
 * associated with properties that differentiates the UI from one conceptual state to anther. Each
 * [TransitionState] can be considered as a snapshot of the UI in the form of property values.
 *
 * [TransitionSpec] defines how to animate from one state to another with a specific animation for
 * each property defined in the states. [TransitionSpec] can be created using [transition] method
 * inside of a [TransitionDefinition]. Currently the animations supported in a [transition] are:
 * [TransitionSpec.tween], [TransitionSpec.keyframes], [TransitionSpec.physics],
 * [TransitionSpec.snap], [TransitionSpec.repeatable]. When no [TransitionSpec] is specified,
 * the default [TransitionSpec.physics] animation will be used for all properties involved.
 * Similarly, when no animation is provided in a [TransitionSpec] for a particular property,
 * the default physics animation will be used. For each [transition], both the from and the to state
 * can be omitted. Omitting in this case is equivalent to a wildcard on the starting state or ending
 * state. When both are omitted at the same time, it means this transition applies to all the state
 * transitions unless a more specific transition have been defined.
 *
 * To create a [TransitionDefinition], there are generally 3 steps involved:
 *
 * __Step 1__: Create PropKeys. One [PropKey] is required for each property/value that needs to
 * be animated. These should be file level properties, so they are visible to
 * [TransitionDefinition] ( which will be created in step 3).
 *
 *     val radius = FloatPropKey()
 *     val alpha = FloatPropKey()
 *
 * __Step 2__ (optional): Create state names.
 *
 * This is an optional but recommended step to create a reference for different states that the
 * animation should end at. State names can be of type [T], which means they can be string,
 * integer, etc, or any custom object, so long as they are consistent.

 * It is recommended to either reuse the states that you already defined (e.g.
 * TogglableState.On, TogglableState.Off, etc) for animating those state changes, or create
 * an enum class for all the animation states.
 *
 *     enum class ButtonState {
 *         Released, Pressed, Disabled
 *     }
 *
 * __Step 3__: Create a [TransitionDefinition] using the animation DSL.
 *
 * [TransitionDefinition] is conceptually an animation configuration that defines:
 * 1) States, each of which are described as a set of values.  Each value is associated with a
 * PropKey.
 * 2) Optional transitions, for how to animate from one state to another.
 *
 * @sample androidx.animation.samples.TransitionDefSample
 *
 * Once a [TransitionDefinition] is created, [androidx.ui.animation.Transition] composable can take
 * it as an input and create a state-based transition in compose.
 *
 * @see [androidx.ui.animation.Transition]
 */
class TransitionDefinition<T> {
    internal val states: MutableMap<T, StateImpl<T>> = mutableMapOf()
    internal lateinit var defaultState: StateImpl<T>
    private val transitionSpecs: MutableList<TransitionSpec<T>> = mutableListOf()

    // TODO: Consider also having the initial defined at call site for cases where many components
    // share the same transition def
    // TODO: (Optimization) Type param in TransitionSpec requires this defaultTransitionSpec to be
    // re-created at least for each state type T. Consider dropping this T beyond initial sanity
    // check.
    private val defaultTransitionSpec = TransitionSpec<T>(arrayOf(null to null))

    /**
     * Defines all the properties and their values associated with the state with the name: [name]
     * The first state defined in the transition definition will be the default state, whose
     * property values will be used as its initial values to createAnimation from.
     *
     * Note that the first [MutableTransitionState] created with [state] in a [TransitionDefinition]
     * will be used as the initial state.
     *
     * @param name The name of the state, which can be used to createAnimation from or to this state
     * @param init Lambda to initialize a state
     */
    fun state(name: T, init: MutableTransitionState.() -> Unit) {
        val newState = StateImpl(name).apply(init)
        states[name] = newState
        if (!::defaultState.isInitialized) {
            defaultState = newState
        }
    }

    /**
     * Defines a transition from state [fromState] to [toState]. When animating from one state to
     * another, [TransitionAnimation] will find the most specific matching transition, and use the
     * animations defined in it for the state transition. Both [fromState] and [toState] are
     * optional. When undefined, it means a wildcard transition going from/to any state.
     *
     * @param fromState The state that the transition will be animated from
     * @param toState The state that the transition will be animated to
     * @param init Lambda to initialize the transition
     */
    fun transition(fromState: T? = null, toState: T? = null, init: TransitionSpec<T>.() -> Unit) {
        transition(fromState to toState, init = init)
    }

    /**
     * Defines a transition from state first value to the second value of the [fromToPairs].
     * When animating from one state to another, [TransitionAnimation] will find the most specific
     * matching transition, and use the animations defined in it for the state transition. Both
     * values in the pair can be null. When they are null, it means a wildcard transition going
     * from/to any state.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     * @sample androidx.animation.samples.TransitionSpecWithPairs
     *
     * @param fromToPairs The pairs of from and to states for this transition
     * @param init Lambda to initialize the transition
     */
    fun transition(vararg fromToPairs: Pair<T?, T?>, init: TransitionSpec<T>.() -> Unit) {
        val newSpec = TransitionSpec(fromToPairs).apply(init)
        transitionSpecs.add(newSpec)
    }

    /**
     * With this transition definition we are saying that every time we reach the
     * state 'from' we should immediately snap to 'to' state instead.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     *     snapTransition(State.Released to State.Pressed)
     *
     * @param fromToPairs The pairs of states for this transition
     * @param nextState Optional state where should we start switching after snap
     */
    fun snapTransition(vararg fromToPairs: Pair<T?, T?>, nextState: T? = null) =
        transition(*fromToPairs) {
            this.nextState = nextState
            defaultAnimation = SnapTransition()
        }

    internal fun getSpec(fromState: T, toState: T): TransitionSpec<T> {
        return transitionSpecs.firstOrNull { it.defines(fromState, toState) }
            ?: transitionSpecs.firstOrNull { it.defines(fromState, null) }
            ?: transitionSpecs.firstOrNull { it.defines(null, toState) }
            ?: transitionSpecs.firstOrNull { it.defines(null, null) }
            ?: defaultTransitionSpec
    }

    /**
     * Returns a state holder for the specific state [name]. Useful for the cases
     * where we don't need actual animation to be happening like in tests.
     */
    fun getStateFor(name: T): TransitionState = states.getValue(name)
}

/**
 * Creates a transition animation using the transition definition and the given clock.
 *
 * @param clock The clock source for animation to get frame time from.
 */
fun <T> TransitionDefinition<T>.createAnimation(
    clock: AnimationClockObservable,
    initState: T? = null
) = TransitionAnimation(this, clock, initState)

/**
 * Creates a [TransitionDefinition] using the [init] function to initialize it.
 *
 * @param init Initialization function for the [TransitionDefinition]
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T> transitionDefinition(@BuilderInference init: TransitionDefinition<T>.() -> Unit) =
    TransitionDefinition<T>().apply(init)

enum class InterruptionHandling {
    PHYSICS,
    SNAP_TO_END, // Not yet supported
    TWEEN, // Not yet supported
    UNINTERRUPTIBLE
}

/********************* The rest of this file is an example ***********************/
private enum class ButtonState {
    Pressed,
    Released
}

private val alpha = FloatPropKey()
private val radius = FloatPropKey()

// TODO: Support states with only part of the props defined

private val example = transitionDefinition {
    state(ButtonState.Pressed) {
        this[alpha] = 0f
        this[radius] = 200f
    }
    state(ButtonState.Released) {
        this[alpha] = 0f
        this[radius] = 60f
    }

    transition(fromState = ButtonState.Released, toState = ButtonState.Pressed) {
        alpha using keyframes {
            duration = 375
            0f at 0 // ms  // Optional
            0.4f at 75 // ms
            0.4f at 225 // ms
            0f at 375 // ms  // Optional
        }
        radius using physics {
            dampingRatio = 1.0f
        }
        interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
    }

    transition(ButtonState.Released to ButtonState.Pressed) {

        // TODO: how do we define sequential tween, alpha then radius snap
        alpha using tween {
            easing = LinearEasing
            duration = 150
            // TODO: Default behavior for transition: when the transition finishes
            // normally, all values should be snapped to the pre-defined values.
        }
    }
}
