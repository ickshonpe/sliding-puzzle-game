package com.slidepuzzle.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import java.util.Random

inline fun repeat2(outer: Int, inner: Int, action: (Int, Int) -> Unit) {
    repeat(outer) { i ->
        repeat(inner) { j ->
            action(i, j)
        }
    }
}

inline fun <X, Y> for2(outer: Iterable<X>, inner: Iterable<Y>, action: (X, Y) -> Unit) {
    for (x in outer) {
        for (y in inner) {
            action(x, y)
        }
    }
}

data class Point2(val x: Int, val y: Int) {
    fun down() = this.copy(y = y - 1)
    fun up() = this.copy(y = y + 1)
    fun left() = this.copy(x = x - 1)
    fun right() = this.copy(x = x + 1)
}

class Board(val width: Int, val height: Int) {
    private val data: Array<Array<Point2>>

    init {
        data = Array(width, { x -> Array(height, { y -> Point2(x, y) }) })

    }

    operator fun contains(p: Point2) = p.x in 0 until width && p.y in 0 until height

    operator fun set(p: Point2, value: Point2) {
        data[p.x][p.y] = value
    }

    operator fun set(x: Int, y: Int, value: Point2) {
        data[x][y] = value
    }

    operator fun get(p: Point2) = data[p.x][p.y]
    operator fun get(x: Int, y: Int) = data[x][y]
    fun swapTiles(p: Point2, q: Point2) {
        val temp = this[p]
        this[p] = this[q]
        this[q] = temp
    }

    fun printDebug() {
        println("Board size: $width x $height")
        repeat2(width, height) { x, y ->
            print("${data[x][y]}\t")
        }
        println()
    }

    fun isCompleted(): Boolean {
        repeat2(width, height) { x, y ->
            val current = Point2(x, y)
            if (this[current] != current) {
                return false
            }
        }
        return true
    }

    fun shuffle(moves: Int) {
        val random = Random()
        var cursor = Point2(0, 0)
        var count = moves
        while (count > 0) {
            val next =
                    when (random.nextInt(4)) {
                        0 -> cursor.left()
                        1 -> cursor.right()
                        2 -> cursor.down()
                        3 -> cursor.up()
                        else -> throw IndexOutOfBoundsException()
                    }
            if (this.contains(next)) {
                this.swapTiles(cursor, next)
                cursor = next
                count -= 1
            }
        }
    }
}



class SlidePuzzleGame : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var img: Texture
    lateinit var camera: OrthographicCamera
    private var cursor = Point2(0, 0)
    private val puzzleWidth = 4
    private val puzzleHeight = 4
    private val tileGap = 4
    private val board = Board(puzzleWidth, puzzleHeight)
    private var slideCoolDown = 0f

    override fun create() {
        batch = SpriteBatch()
        img = Texture("badlogic.jpg")
        board.shuffle(1000)
        val w = img.width + (puzzleWidth + 1) * tileGap
        val h = img.height + (puzzleHeight + 1) * tileGap
        Gdx.graphics.setWindowedMode(w, h)
        camera = OrthographicCamera(w.toFloat(), h.toFloat())
        camera.translate(w / 2f, h / 2f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    override fun render() {
        val timeDelta = Gdx.graphics.deltaTime
        slideCoolDown -= timeDelta
        Gdx.gl.glClearColor(0f, 0f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        val tileWidth = img.width / board.width
        val tileHeight = img.height / board.height

        batch.begin()
        repeat2(board.width, board.height) { i, j ->
            val current = Point2(i, j)
            if (cursor != current) {
                val x = i * (tileWidth + tileGap) + tileGap
                val y = j * (tileHeight + tileGap) + tileGap
                val tile = board[current]
                val sourceX = tile.x * tileWidth
                val sourceY = (puzzleWidth - 1 - tile.y) * tileWidth
                batch.draw(img, x.toFloat(), y.toFloat(), tileWidth.toFloat(), tileHeight.toFloat(), sourceX, sourceY, tileWidth, tileHeight, false, false)

            }
        }
        if (slideCoolDown < 0f) {
            val target = when {
                Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP) -> {
                    cursor.down()
                }
                Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)-> {
                    cursor.up()
                }
                Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT) -> {
                    cursor.right()
                }
                Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT) -> {
                    cursor.left()
                }
                else ->
                    null
            }
            if (target != null && board.contains(target)) {
                board.swapTiles(cursor, target)
                cursor = target
                slideCoolDown = 0.3f
            }
        }
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }
}
