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
import arcadia.gfx.VideoIO
import chisel3._
import chisel3.util._
import tecmo._

/**
 * The layer processor handles rendering tilemap layers.
 *
 * @param tileSize The tile size in pixels.
 * @param cols     The number of columns.
 * @param rows     The number of rows.
 */
class LayerProcessor(tileSize: Int, cols: Int, rows: Int) extends Module {
  val io = IO(new Bundle {
    /** Control port */
    val ctrl = new LayerCtrlIO
    /** Video port */
    val video = Flipped(VideoIO())
    /** Flip video */
    val flip = Input(Bool())
    /** Palette entry output */
    val pen = Output(new PaletteEntry)
  })

  // Destination position
  val destPos = {
    val x = io.video.pos.x
    val y = Mux(io.flip, (~io.video.pos.y(7, 0)).asUInt - 1.U, io.video.pos.y(7, 0) + 1.U)
    UVec2(x, y) + io.ctrl.scrollPos
  }

  // Grid and pixel offsets
  val col = destPos.x(log2Ceil(tileSize) + log2Ceil(cols) - 1, log2Ceil(tileSize))
  val row = destPos.y(log2Ceil(tileSize) + log2Ceil(rows) - 1, log2Ceil(tileSize))
  val offset = {
    val x = destPos.x(log2Ceil(tileSize) - 1, 0)
    val y = destPos.y(log2Ceil(tileSize) - 1, 0)
    UVec2(x, y)
  }

  // Registers
  val ramAddrReg = Reg(UInt(Config.LAYER_RAM_GPU_ADDR_WIDTH.W))
  val romDataReg = Reg(Bits(Config.FG_ROM_DATA_WIDTH.W))
  val colorReg = Reg(UInt(Config.COLOR_WIDTH.W))
  val tileReg = Reg(new Tile)

  // Decode tile row data
  val tileRow = GPU.decodeTileRow(romDataReg)

  // The line buffer is used by the layer to render a single scanline. As one line is being written,
  // the other is being read by the GPU.
  val lineBuffer = Module(new FrameBuffer(Config.LINE_BUFFER_ADDR_WIDTH, Config.LINE_BUFFER_DATA_WIDTH))
  lineBuffer.io.portA.wr := !io.video.hBlank
  lineBuffer.io.portA.addr := Mux(io.flip, ~io.video.pos.x(7, 0), io.video.pos.x(7, 0))
  lineBuffer.io.portA.mask := DontCare
  lineBuffer.io.portA.din := colorReg ## tileRow(offset.x(2, 0))
  lineBuffer.io.portB.rd := io.video.displayEnable
  lineBuffer.io.portB.addr := io.video.pos.x(7, 0)
  lineBuffer.io.swap := io.video.pos.y(0) // swap on alternate rows

  // Set tile ROM address
  val tileRomAddr = if (tileSize == 16) {
    Cat(tileReg.code, offset.y(3), ~offset.x(3), offset.y(2, 0), 0.U(2.W))
  } else {
    Cat(tileReg.code, offset.y(2, 0), 0.U(2.W))
  }

  // Latch RAM address
  //
  // We always want to load one tile ahead of the current position to allow enough time to load the
  // tile data from memory and latch it into registers.
  when(offset.x === (tileSize - 8).U) { ramAddrReg := row ## (col + 1.U) }

  // Latch decoded tile data
  when(offset.x === (tileSize - 6).U) { tileReg := Tile.decode(io.ctrl.vram.dout) }

  // Latch row data
  when(offset.x(2, 0) === 7.U) { romDataReg := io.ctrl.tileRom.dout }

  // Latch color data
  when(offset.x === (tileSize - 1).U) { colorReg := tileReg.color }

  // Outputs
  io.ctrl.vram.rd := true.B // read-only
  io.ctrl.vram.addr := ramAddrReg
  io.ctrl.tileRom.rd := true.B // read-only
  io.ctrl.tileRom.addr := tileRomAddr
  io.pen := lineBuffer.io.portB.dout.asTypeOf(new PaletteEntry)
}
