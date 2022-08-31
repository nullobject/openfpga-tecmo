/*
 *   __   __     __  __     __         __
 *  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *    \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *   ______     ______       __     ______     ______     ______
 *  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 * https://joshbassett.info
 * https://twitter.com/nullobject
 * https://github.com/nullobject
 *
 * Copyright (c) 2022 Josh Bassett
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tecmo.gfx

import arcadia.Util
import arcadia.gfx.VideoIO
import arcadia.mem._
import arcadia.util.Counter
import chisel3._
import chisel3.util._
import tecmo._

/**
 * The sprite processor handles rendering sprites.
 *
 * @param numSprites The maximum number of sprites to render.
 */
class SpriteProcessor(numSprites: Int = 256) extends Module {
  val io = IO(new Bundle {
    /** Control port */
    val ctrl = SpriteCtrlIO()
    /** Frame buffer port */
    val frameBuffer = WriteMemIO(Config.FRAME_BUFFER_ADDR_WIDTH, Config.FRAME_BUFFER_DATA_WIDTH)
    /** Video port */
    val video = Flipped(VideoIO())
    /** Debug port */
    val debug = Output(new Bundle {
      val idle = Bool()
      val load = Bool()
      val check = Bool()
      val ready = Bool()
      val blit = Bool()
      val next = Bool()
      val done = Bool()
    })
  })

  // States
  object State {
    val idle :: load :: check :: ready :: blit :: next :: done :: Nil = Enum(7)
  }

  // Wires
  val effectiveRead = Wire(Bool())

  // Registers
  val stateReg = RegInit(State.idle)
  val spriteReg = RegEnable(Sprite.decode(io.ctrl.vram.dout), stateReg === State.load)
  val readPendingReg = RegInit(false.B)

  // Counters
  val (spriteCounter, spriteCounterWrap) = Counter.static(numSprites, stateReg === State.next)
  val (colCounter, colCounterWrap) = Counter.dynamic(spriteReg.cols, effectiveRead)
  val (lineCounter, lineCounterWrap) = Counter.static(GPU.TILE_HEIGHT, effectiveRead && colCounterWrap)
  val (rowCounter, rowCounterWrap) = Counter.dynamic(spriteReg.rows, effectiveRead && lineCounterWrap)

  // The FIFO is used to buffer tile ROM data to be processed by the sprite decoder
  val fifo = Module(new Queue(Bits(), SpriteProcessor.FIFO_DEPTH, useSyncReadMem = true, hasFlush = true))
  fifo.flush := stateReg === State.idle

  // Sprite blitter
  val blitter = Module(new SpriteBlitter)
  blitter.io.pixelData <> fifo.io.deq
  blitter.io.frameBuffer <> io.frameBuffer

  // Start loading data from the tile ROM for an enabled sprite. We can stop loading when all the
  // rows have been requested (i.e. the line/row/column counters have all wrapped).
  val loading = {
    val start = stateReg === State.check && spriteReg.enable
    val stop = RegNext(lineCounterWrap && rowCounterWrap && colCounterWrap)
    Util.latch(start, stop)
  }

  // Load the next row of pixels from the tile ROM when the pixel data queue is ready
  val tileRomRead = loading && !readPendingReg && fifo.io.enq.ready

  // Set effective read flag
  effectiveRead := tileRomRead && !io.ctrl.tileRom.waitReq

  // Set tile ROM address
  val tileRomAddr = SpriteProcessor.tileRomAddr(spriteReg.code, colCounter, rowCounter, lineCounter)

  // The burst pending register is asserted when there is a burst in progress
  when(io.ctrl.tileRom.valid) {
    readPendingReg := false.B
  }.elsewhen(effectiveRead) {
    readPendingReg := true.B
  }

  // Enqueue the next sprite with the sprite blitter when we are ready to do a blit
  when(stateReg === State.ready) {
    blitter.io.config.enq(spriteReg)
  } otherwise {
    blitter.io.config.noenq()
  }

  // Enqueue valid tile ROM data with the pixel data queue
  when(io.ctrl.tileRom.valid) {
    fifo.io.enq.enq(io.ctrl.tileRom.dout)
  } otherwise {
    fifo.io.enq.noenq()
  }

  // FSM
  switch(stateReg) {
    // Wait for the beginning of the frame
    is(State.idle) {
      when(io.video.vBlank) { stateReg := State.load }
    }

    // Load the sprite
    is(State.load) { stateReg := State.check }

    // Check whether the sprite is enabled
    is(State.check) {
      stateReg := Mux(spriteReg.enable, State.ready, State.next)
    }

    // Wait for the blitter to be ready
    is(State.ready) {
      when(blitter.io.config.ready) { stateReg := State.blit }
    }

    // Blit the sprite
    is(State.blit) {
      when(blitter.io.config.ready) { stateReg := State.next }
    }

    // Increment the sprite counter
    is(State.next) {
      stateReg := Mux(spriteCounterWrap, State.done, State.load)
    }

    // Wait for the end of the frame
    is(State.done) {
      when(!io.video.vBlank) { stateReg := State.idle }
    }
  }

  // Outputs
  io.ctrl.vram.rd := true.B // read-only
  io.ctrl.vram.addr := spriteCounter
  io.ctrl.tileRom.rd := tileRomRead
  io.ctrl.tileRom.addr := tileRomAddr
  io.debug.idle := stateReg === State.idle
  io.debug.load := stateReg === State.load
  io.debug.check := stateReg === State.check
  io.debug.ready := stateReg === State.ready
  io.debug.blit := stateReg === State.blit
  io.debug.next := stateReg === State.next
  io.debug.done := stateReg === State.done

  printf(p"SpriteProcessor(state: $stateReg, sprite: $spriteCounter ($spriteCounterWrap), line: $lineCounter ($lineCounterWrap), col: $colCounter ($colCounterWrap), row: $rowCounter ($rowCounterWrap), pending: $readPendingReg)\n")
}

object SpriteProcessor {
  /**
   * The depth of the tile ROM FIFO in words.
   *
   * The queue needs to be deep enough to accommodate the lines required for the largest sprite size
   * (i.e. 64x64 pixels).
   */
  val FIFO_DEPTH = 8 * 8 * 8

  /**
   * Calculates the tile ROM address for the given sprite.
   *
   * The address is composed of the sprite code for the upper bits, and the line counter for the
   * lower bits. The 4 LSB of the sprite code are combined with the tile row/column. So depending on
   * the sprite size, some of those bits will be ignored.
   *
   * @param code The sprite code.
   * @param col  The column index.
   * @param row  The row index.
   * @param line The line index.
   * @return A memory address.
   */
  private def tileRomAddr(code: UInt, col: UInt, row: UInt, line: UInt): UInt =
    Cat(
      code(12, 4),
      code(3, 0) | (row(1) ## col(1)) ## row(0) ## col(0),
      line,
      0.U(2.W)
    )
}
