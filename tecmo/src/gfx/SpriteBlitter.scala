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

import arcadia._
import arcadia.mem._
import arcadia.util.Counter
import chisel3._
import chisel3.util._
import tecmo.Config

/** Copies a sprite to the frame buffer. */
class SpriteBlitter extends Module {
  val io = IO(new Bundle {
    /** Sprite data port */
    val spriteData = DeqIO(new Sprite)
    /** Pixel data port */
    val pixelData = DeqIO(UInt(Config.SPRITE_ROM_DATA_WIDTH.W))
    /** Frame buffer port */
    val frameBuffer = WriteMemIO(Config.FRAME_BUFFER_ADDR_WIDTH, Config.FRAME_BUFFER_DATA_WIDTH)
    /** Debug port */
    val debug = Output(new Bundle {
      val idle = Bool()
      val fetch = Bool()
      val blit = Bool()
    })
  })

  // States
  object State {
    val idle :: fetch :: blit :: Nil = Enum(3)
  }

  // Registers
  val stateReg = RegInit(State.idle)
  val spriteReg = RegEnable(io.spriteData.bits, stateReg === State.idle && io.spriteData.fire)
  val pisoReg = Reg(Vec(GPU.TILE_WIDTH, Bits(GPU.TILE_BIT_PLANES.W)))

  // Counters
  val (x, xWrap) = Counter.dynamic(spriteReg.size, stateReg === State.blit)
  val (y, yWrap) = Counter.dynamic(spriteReg.size, stateReg === State.blit && xWrap)

  // Destination position
  val destPos = {
    val xDest = Mux(spriteReg.xFlip, spriteReg.size - x - 1.U, x)
    val yDest = Mux(spriteReg.yFlip, spriteReg.size - y - 1.U, y)
    spriteReg.pos + UVec2(xDest, yDest)
  }

  // Assert the done signal when copying the last pixel
  val done = xWrap && yWrap

  // Assert the load signal when we need to load the next row of pixels (i.e. we're at the last
  // pixel in a row)
  val load = (x % GPU.TILE_WIDTH.U === (GPU.TILE_WIDTH - 1).U) && !done

  // Assert the latch signal when a tile row should be latched
  val latch = (stateReg === State.fetch || (stateReg === State.blit && load)) && io.pixelData.valid

  // Current pixel value
  val pixel = pisoReg(x)

  // Latch tile row data into the PISO
  when(latch) {
    pisoReg := GPU.decodeTileRow(io.pixelData.deq())
  } otherwise {
    io.pixelData.nodeq()
  }

  // FSM
  switch(stateReg) {
    // Wait for the VALID signal
    is(State.idle) {
      when(io.spriteData.valid) { stateReg := State.fetch }
    }

    // Fetch tile row
    is(State.fetch) {
      when(io.pixelData.valid) { stateReg := State.blit }
    }

    // Blit all rows to the frame buffer
    is(State.blit) {
      when(done) { stateReg := State.idle }
    }
  }

  // Outputs
  io.spriteData.ready := stateReg === State.idle
  io.frameBuffer.wr := stateReg === State.blit && pixel =/= 0.U && !destPos.x(8) && !destPos.y(8)
  io.frameBuffer.addr := destPos.y(7, 0) ## destPos.x(7, 0)
  io.frameBuffer.mask := DontCare
  io.frameBuffer.din := spriteReg.priority ## spriteReg.color ## pixel
  io.debug.idle := stateReg === State.idle
  io.debug.fetch := stateReg === State.fetch
  io.debug.blit := stateReg === State.blit

  printf(p"SpriteBlitter(state: $stateReg, x: $x, y: $y, pisoReg: $pisoReg, pixel: $pixel, valid: ${ io.spriteData.valid })\n")
}
