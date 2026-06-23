package com.xanticious.androidgames.model.games.mapcommon

/** Integer grid cell. x grows east, y grows south. */
data class GridCell(val x: Int, val y: Int) {
    fun move(direction: GridDirection, steps: Int = 1): GridCell = GridCell(
        x = x + direction.dx * steps,
        y = y + direction.dy * steps
    )
}

data class GridSize(val columns: Int, val rows: Int) {
    init {
        require(columns > 0) { "columns must be positive" }
        require(rows > 0) { "rows must be positive" }
    }

    val cells: List<GridCell>
        get() = (0 until rows).flatMap { y -> (0 until columns).map { x -> GridCell(x, y) } }

    fun contains(cell: GridCell): Boolean = cell.x in 0 until columns && cell.y in 0 until rows

    fun cardinalNeighbors(cell: GridCell): List<GridCell> = GridDirection.entries
        .map { cell.move(it) }
        .filter { contains(it) }
}

enum class GridDirection(val dx: Int, val dy: Int, val label: String) {
    NORTH(0, -1, "north"),
    SOUTH(0, 1, "south"),
    EAST(1, 0, "east"),
    WEST(-1, 0, "west")
}
