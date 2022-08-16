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
 * Renders text on the screen.
 *
 * @param numCols The number of columns.
 * @param numRows The number of rows.
 */
class DebugLayer(numCols: Int, numRows: Int) extends Module {
  val io = IO(new Bundle {
    /** Tile ROM signals */
    val tileRom = new TileRomIO
    /** Video signals */
    val video = Input(new VideoIO)
    /** Text */
    val text = Input(Vec(numCols, UInt(8.W)))
    /** Position signals */
    val pos = Input(UVec2(9.W))
    /** Color */
    val color = Input(UInt(4.W))
    /** Pixel data output */
    val data = Output(UInt(8.W))
  })

  // Registers
  val textReg = RegNext(io.text)

  // Tile and pixel offsets
  val delta = io.video.pos - io.pos
  val col = delta.x(log2Ceil(GPU.TILE_WIDTH) + log2Ceil(numCols) - 1, log2Ceil(GPU.TILE_WIDTH))
  val xOffset = delta.x(log2Ceil(GPU.TILE_WIDTH) - 1, 0)
  val yOffset = delta.y(log2Ceil(GPU.TILE_HEIGHT) - 1, 0)

  // Set enable signal
  val enable =
    Util.between(io.video.pos.x, io.pos.x, io.pos.x + (GPU.TILE_WIDTH * numCols - 1).U) &&
      Util.between(io.video.pos.y, io.pos.y, io.pos.y + (GPU.TILE_HEIGHT * numRows - 1).U)

  // Decode tile row data
  val tileRow = GPU.decodeTileRow(io.tileRom.dout)

  // Outputs
  io.tileRom.rd := true.B
  io.tileRom.addr := textReg(col) ## yOffset
  io.data := Mux(enable, io.color ## tileRow(xOffset), 0.U)
}
