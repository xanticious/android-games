@file:DependsOn("/tmp/verify-mahjong-solitaire")

// Test multiple seeds to find one that greedy can clear
import com.xanticious.androidgames.controller.games.mahjongsolitaire.MahjongSolitaireController
import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongLayout
import kotlin.random.Random

val controller = MahjongSolitaireController()
for (seed in listOf(42L, 1L, 2L, 3L, 5L, 7L, 10L, 13L, 17L, 23L, 31L, 37L, 50L, 100L, 123L, 200L, 999L)) {
    var state = controller.newGame(MahjongLayout.TURTLE, Random(seed), true)
    var steps = 0
    while (!controller.isSolved(state) && !controller.isStuck(state) && steps < 72) {
        val pair = controller.availableMatches(state).first()
        state = controller.tryMatch(controller.selectTile(state, pair.first.id), pair.second.id)
        steps++
    }
    println("seed=$seed solved=${controller.isSolved(state)} steps=$steps stuck=${controller.isStuck(state)}")
}
