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
  val pos = io.video.pos + io.ctrl.scrollPos

  // Tile offset
  val tileOffset = LayerProcessor.tileOffset(tileSize, pos)

  // Latch signals
  val latchTile = tileOffset.x === (tileSize - 6).U
  val latchColor = tileOffset.x === (tileSize - 1).U
  val latchPix = tileOffset.x(2, 0) === 7.U

  // Tile registers
  val tileReg = RegEnable(Tile.decode(io.ctrl.vram.dout), latchTile)
  val colorReg = RegEnable(tileReg.colorCode, latchColor)
  val pixReg = RegEnable(GPU.decodeTileRow(io.ctrl.tileRom.dout), latchPix)

  // Palette entry
  val pen = PaletteEntry(0.U, colorReg, pixReg(tileOffset.x(2, 0)))

  // Outputs
  io.ctrl.vram.rd := true.B // read-only
  io.ctrl.vram.addr :=  LayerProcessor.vramAddr(tileSize, cols, rows, pos)
  io.ctrl.tileRom.rd := true.B // read-only
  io.ctrl.tileRom.addr := LayerProcessor.tileRomAddr(tileSize, tileReg.code, tileOffset)
  io.pen := pen
}

object LayerProcessor {
  /**
   * Calculates the VRAM address for the next tile.
   *
   * @param tileSize The tile size in pixels.
   * @param cols     The number of columns.
   * @param rows     The number of rows.
   * @param pos      The absolute position of the pixel in the tilemap.
   * @return A memory address.
   */
  private def vramAddr(tileSize: Int, cols: Int, rows: Int, pos: UVec2): UInt = {
    val col = pos.x(log2Ceil(tileSize) + log2Ceil(cols) - 1, log2Ceil(tileSize))
    val row = pos.y(log2Ceil(tileSize) + log2Ceil(rows) - 1, log2Ceil(tileSize))
    row ## (col + 1.U)
  }

  /**
   * Calculates the pixel offset for a tile.
   *
   * @param tileSize The tile size in pixels.
   * @param pos      The absolute position of the pixel in the tilemap.
   * @return An unsigned vector.
   */
  private def tileOffset(tileSize: Int, pos: UVec2): UVec2 = {
    val x = pos.x(log2Ceil(tileSize) - 1, 0)
    val y = pos.y(log2Ceil(tileSize) - 1, 0)
    UVec2(x, y)
  }

  /**
   * Calculates the tile ROM address for the given tile code.
   *
   * @param tileSize The tile size in pixels.
   * @param code     The tile code.
   * @param offset   The pixel offset.
   * @return A memory address.
   */
  private def tileRomAddr(tileSize: Int, code: UInt, offset: UVec2): UInt = {
    if (tileSize == 16) {
      Cat(code, offset.y(3), ~offset.x(3), offset.y(2, 0), 0.U(2.W))
    } else {
      Cat(code, offset.y(2, 0), 0.U(2.W))
    }
  }
}
