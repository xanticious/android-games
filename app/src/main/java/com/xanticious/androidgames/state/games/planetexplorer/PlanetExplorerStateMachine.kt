package com.xanticious.androidgames.state.games.planetexplorer

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
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.transition.onTriggered

/** High-level Planet Explorer phases observed by the composable. */
enum class PlanetExplorerPhase { IDLE, LOADING_WORLD, EXPLORING, FIELD_BOOK, BIOME_SELECT, DISCOVERY }

private sealed class ExplorerState : DefaultState() {
    data object Idle : ExplorerState()
    data object LoadingWorld : ExplorerState()
    data object ExploringBiome : ExplorerState()
    data object FieldBookOverlay : ExplorerState()
    data object BiomeSwitcher : ExplorerState()
    data object ToolDiscovery : ExplorerState()
}

private sealed interface ExplorerEvent : Event {
    data object StartGame : ExplorerEvent
    data object WorldReady : ExplorerEvent
    data object FieldBookOpened : ExplorerEvent
    data object FieldBookClosed : ExplorerEvent
    data object BiomeSwitcherOpened : ExplorerEvent
    data object BiomeSelected : ExplorerEvent
    data object Dismissed : ExplorerEvent
    data object ToolFound : ExplorerEvent
    data object Restart : ExplorerEvent
}

class PlanetExplorerStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PlanetExplorerPhase.IDLE)
    val phase: StateFlow<PlanetExplorerPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(ExplorerState.Idle) {
            transition<ExplorerEvent.StartGame> {
                targetState = ExplorerState.LoadingWorld
                onTriggered { _phase.value = PlanetExplorerPhase.LOADING_WORLD }
            }
        }
        addState(ExplorerState.LoadingWorld) {
            transition<ExplorerEvent.WorldReady> {
                targetState = ExplorerState.ExploringBiome
                onTriggered { _phase.value = PlanetExplorerPhase.EXPLORING }
            }
        }
        addState(ExplorerState.ExploringBiome) {
            transition<ExplorerEvent.FieldBookOpened> {
                targetState = ExplorerState.FieldBookOverlay
                onTriggered { _phase.value = PlanetExplorerPhase.FIELD_BOOK }
            }
            transition<ExplorerEvent.BiomeSwitcherOpened> {
                targetState = ExplorerState.BiomeSwitcher
                onTriggered { _phase.value = PlanetExplorerPhase.BIOME_SELECT }
            }
            transition<ExplorerEvent.ToolFound> {
                targetState = ExplorerState.ToolDiscovery
                onTriggered { _phase.value = PlanetExplorerPhase.DISCOVERY }
            }
        }
        addState(ExplorerState.FieldBookOverlay) {
            transition<ExplorerEvent.FieldBookClosed> {
                targetState = ExplorerState.ExploringBiome
                onTriggered { _phase.value = PlanetExplorerPhase.EXPLORING }
            }
        }
        addState(ExplorerState.BiomeSwitcher) {
            transition<ExplorerEvent.BiomeSelected> {
                targetState = ExplorerState.ExploringBiome
                onTriggered { _phase.value = PlanetExplorerPhase.EXPLORING }
            }
            transition<ExplorerEvent.Dismissed> {
                targetState = ExplorerState.ExploringBiome
                onTriggered { _phase.value = PlanetExplorerPhase.EXPLORING }
            }
        }
        addState(ExplorerState.ToolDiscovery) {
            transition<ExplorerEvent.Dismissed> {
                targetState = ExplorerState.ExploringBiome
                onTriggered { _phase.value = PlanetExplorerPhase.EXPLORING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(ExplorerEvent.StartGame)
    fun worldReady() = machine.processEventByLaunch(ExplorerEvent.WorldReady)
    fun openFieldBook() = machine.processEventByLaunch(ExplorerEvent.FieldBookOpened)
    fun closeFieldBook() = machine.processEventByLaunch(ExplorerEvent.FieldBookClosed)
    fun openBiomeSwitcher() = machine.processEventByLaunch(ExplorerEvent.BiomeSwitcherOpened)
    fun selectBiome() = machine.processEventByLaunch(ExplorerEvent.BiomeSelected)
    fun dismiss() = machine.processEventByLaunch(ExplorerEvent.Dismissed)
    fun toolFound() = machine.processEventByLaunch(ExplorerEvent.ToolFound)
}
