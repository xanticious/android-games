package com.xanticious.androidgames.state.games.deckbuildersuperhero

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.transition.onTriggered

/** High-level game phase exposed to the composable. */
enum class DeckBuilderPhase {
    /** Before the game has started — shows hero-select screen. */
    IDLE,
    /** Hero selection underway; human picks their archetype. */
    HERO_SELECT,
    /**
     * Game is being initialised (decks shuffled, villain placed).
     * Transitions to [HERO_TURN] automatically once setup is complete.
     */
    SETUP,
    /** Human hero's interactive turn (play cards, recruit, attack). */
    HERO_TURN,
    /** Villain scheme advances; AI turns are processed in this phase. */
    VILLAIN_PHASE,
    /** Team defeated the villain — victory panel shown below the board. */
    WON,
    /** Villain scheme completed before villain was defeated — defeat panel. */
    LOST
}

private sealed class SHState : DefaultState() {
    data object Idle         : SHState()
    data object HeroSelect   : SHState()
    data object Setup        : SHState()
    data object HeroTurn     : SHState()
    data object VillainPhase : SHState()
    data object Won          : SHState()
    data object Lost         : SHState()
}

private sealed interface SHEvent : Event {
    data object GameStarted      : SHEvent
    data object HeroesAssigned   : SHEvent
    data object SetupComplete    : SHEvent
    data object TurnEnded        : SHEvent
    data object VillainDefeated  : SHEvent
    data object SchemeCompleted  : SHEvent
    data object ContinueGame     : SHEvent
    data object Replay           : SHEvent
}

/**
 * KStateMachine driving [DeckBuilderPhase] transitions for Deck Builder
 * Superhero.
 *
 * Inject [scope] with `CoroutineScope(Dispatchers.Unconfined)` in unit tests
 * to avoid requiring the Android main dispatcher.
 */
class DeckBuilderStateMachine(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(DeckBuilderPhase.IDLE)
    val phase: StateFlow<DeckBuilderPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(SHState.Idle) {
            transition<SHEvent.GameStarted> {
                targetState = SHState.HeroSelect
                onTriggered { _phase.value = DeckBuilderPhase.HERO_SELECT }
            }
        }
        addState(SHState.HeroSelect) {
            transition<SHEvent.HeroesAssigned> {
                targetState = SHState.Setup
                onTriggered { _phase.value = DeckBuilderPhase.SETUP }
            }
        }
        addState(SHState.Setup) {
            transition<SHEvent.SetupComplete> {
                targetState = SHState.HeroTurn
                onTriggered { _phase.value = DeckBuilderPhase.HERO_TURN }
            }
        }
        addState(SHState.HeroTurn) {
            transition<SHEvent.TurnEnded> {
                targetState = SHState.VillainPhase
                onTriggered { _phase.value = DeckBuilderPhase.VILLAIN_PHASE }
            }
        }
        addState(SHState.VillainPhase) {
            transition<SHEvent.VillainDefeated> {
                targetState = SHState.Won
                onTriggered { _phase.value = DeckBuilderPhase.WON }
            }
            transition<SHEvent.SchemeCompleted> {
                targetState = SHState.Lost
                onTriggered { _phase.value = DeckBuilderPhase.LOST }
            }
            transition<SHEvent.ContinueGame> {
                targetState = SHState.HeroTurn
                onTriggered { _phase.value = DeckBuilderPhase.HERO_TURN }
            }
        }
        addState(SHState.Won) {
            transition<SHEvent.Replay> {
                targetState = SHState.HeroSelect
                onTriggered { _phase.value = DeckBuilderPhase.HERO_SELECT }
            }
        }
        addState(SHState.Lost) {
            transition<SHEvent.Replay> {
                targetState = SHState.HeroSelect
                onTriggered { _phase.value = DeckBuilderPhase.HERO_SELECT }
            }
        }
    }

    fun startGame()         = machine.processEventByLaunch(SHEvent.GameStarted)
    fun heroesAssigned()    = machine.processEventByLaunch(SHEvent.HeroesAssigned)
    fun setupComplete()     = machine.processEventByLaunch(SHEvent.SetupComplete)
    fun endTurn()           = machine.processEventByLaunch(SHEvent.TurnEnded)
    fun villainDefeated()   = machine.processEventByLaunch(SHEvent.VillainDefeated)
    fun schemeCompleted()   = machine.processEventByLaunch(SHEvent.SchemeCompleted)
    fun continueGame()      = machine.processEventByLaunch(SHEvent.ContinueGame)
    fun replay()            = machine.processEventByLaunch(SHEvent.Replay)
}
