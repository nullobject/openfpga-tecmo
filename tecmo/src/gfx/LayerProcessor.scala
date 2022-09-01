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
 * Represents a configuration for the layer processor.
 *
 * @param tileSize The tile size in pixels.
 * @param cols     The number of columns.
 * @param rows     The number of rows.
 * @param offset   The layer offset.
 */
case class LayerProcessorConfig(tileSize: Int, cols: Int, rows: Int, offset: Int)

/**
 * The layer processor handles rendering tilemap layers.
 *
 * @param config The layer processor configuration.
 */
class LayerProcessor(config: LayerProcessorConfig) extends Module {
  val io = IO(new Bundle {
    /** Control port */
    val ctrl = LayerCtrlIO()
    /** Video port */
    val video = Flipped(VideoIO())
    /** Palette entry output */
    val pen = Output(new PaletteEntry)
  })

  // Apply the scroll and layer offsets to get the final position
  val pos = io.video.pos + io.ctrl.scroll + UVec2(config.offset.U, 0.U)

  // Tile offset
  val tileOffset = LayerProcessor.tileOffset(config, pos)

  // Control signals
  val enable = io.video.clockEnable && !io.video.vBlank
  val latchTile = enable && tileOffset.x === (config.tileSize - 7).U
  val tileRomRead = enable && tileOffset.x(2, 0) === 2.U
  val latchColor = enable && tileOffset.x === (config.tileSize - 1).U
  val latchPix = enable && tileOffset.x(2, 0) === 7.U

  // Decode tile
  val tile = Mux(io.ctrl.format === Config.GFX_FORMAT_GEMINI.U,
    Tile.decodeGemini(io.ctrl.vram.dout),
    Tile.decode(io.ctrl.vram.dout)
  )

  // Tile registers
  val tileReg = RegEnable(tile, latchTile)
  val colorReg = RegEnable(tileReg.colorCode, latchColor)
  val pixReg = RegEnable(GPU.decodeTileRow(io.ctrl.tileRom.dout), latchPix)

  // Palette entry
  val pen = PaletteEntry(0.U, colorReg, pixReg(tileOffset.x(2, 0)))

  // Outputs
  io.ctrl.vram.rd := true.B // read-only
  io.ctrl.vram.addr := LayerProcessor.vramAddr(config, pos)
  io.ctrl.tileRom.rd := tileRomRead
  io.ctrl.tileRom.addr := LayerProcessor.tileRomAddr(config, tileReg.code, tileOffset)
  io.pen := pen
}

object LayerProcessor {
  /**
   * Calculates the VRAM address for the next tile.
   *
   * @param config The layer processor configuration.
   * @param pos    The absolute position of the pixel in the tilemap.
   * @return A memory address.
   */
  private def vramAddr(config: LayerProcessorConfig, pos: UVec2): UInt = {
    val col = pos.x(log2Ceil(config.tileSize) + log2Ceil(config.cols) - 1, log2Ceil(config.tileSize))
    val row = pos.y(log2Ceil(config.tileSize) + log2Ceil(config.rows) - 1, log2Ceil(config.tileSize))
    row ## (col + 1.U)
  }

  /**
   * Calculates the pixel offset for a tile.
   *
   * @param config The layer processor configuration.
   * @param pos    The absolute position of the pixel in the tilemap.
   * @return An unsigned vector.
   */
  private def tileOffset(config: LayerProcessorConfig, pos: UVec2): UVec2 = {
    val x = pos.x(log2Ceil(config.tileSize) - 1, 0)
    val y = pos.y(log2Ceil(config.tileSize) - 1, 0)
    UVec2(x, y)
  }

  /**
   * Calculates the tile ROM address for the given tile code.
   *
   * @param config The layer processor configuration.
   * @param code   The tile code.
   * @param offset The pixel offset.
   * @return A memory address.
   */
  private def tileRomAddr(config: LayerProcessorConfig, code: UInt, offset: UVec2): UInt = {
    if (config.tileSize == 16) {
      Cat(code, offset.y(3), ~offset.x(3), offset.y(2, 0), 0.U(2.W))
    } else {
      Cat(code, offset.y(2, 0), 0.U(2.W))
    }
  }
}
