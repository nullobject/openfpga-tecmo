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
import chisel3._
import chisel3.util._
import tecmo._

/** Represents a graphics layer. */
object Layer {
  val SPRITE = 0
  val CHAR = 1
  val FG = 2
  val BG = 3
  val FILL = 4
  val DEBUG = 5
}

/** Combines pixel data from the different graphics layers. */
class ColorMixer extends Module {
  val io = IO(new Bundle {
    /** Palette RAM signals */
    val paletteRam = new PaletteRamIO
    /** Sprite priority */
    val spritePriority = Input(UInt(2.W))
    /** Sprite layer data */
    val spriteData = Input(UInt(8.W))
    /** Character layer data */
    val charData = Input(UInt(8.W))
    /** Foreground layer data */
    val fgData = Input(UInt(8.W))
    /** Background layer data */
    val bgData = Input(UInt(8.W))
    /** Debug layer data */
    val debugData = Input(UInt(8.W))
    /** Pixel data */
    val dout = Output(UInt(Config.PALETTE_RAM_DATA_WIDTH.W))
  })

  // Mux the layers
  val index = ColorMixer.muxLayers(io.spritePriority, io.spriteData, io.charData, io.fgData, io.bgData, io.debugData)

  // Mux the layers
  val paletteRamAddr = MuxLookup(index, 0.U, Seq(
    Layer.SPRITE.U -> 0.U ## io.spriteData,
    Layer.CHAR.U -> 1.U ## io.charData,
    Layer.FG.U -> 2.U ## io.fgData,
    Layer.BG.U -> 3.U ## io.bgData,
    Layer.FILL.U -> 1.U ## 0.U(8.W),
    Layer.DEBUG.U -> 1.U ## io.debugData
  ))

  // Outputs
  io.paletteRam.rd := true.B // read-only
  io.paletteRam.addr := paletteRamAddr
  io.dout := RegNext(io.paletteRam.dout)
}

object ColorMixer {
  /**
   * Calculates the layer with the highest priority.
   *
   * @param spritePriority The sprite priority.
   * @param spriteData     The sprite layer data.
   * @param charData       The character layer data.
   * @param fgData         The foreground layer data.
   * @param bgData         The background layer data.
   * @param debugData      The debug layer data.
   */
  private def muxLayers(spritePriority: UInt,
                        spriteData: UInt,
                        charData: UInt,
                        fgData: UInt,
                        bgData: UInt,
                        debugData: UInt): UInt = {
    val debug = (debugData(3, 0) =/= 0.U) -> Layer.DEBUG.U
    val sprite = (spriteData(3, 0) =/= 0.U) -> Layer.SPRITE.U
    val char = (charData(3, 0) =/= 0.U) -> Layer.CHAR.U
    val fg = (fgData(3, 0) =/= 0.U) -> Layer.FG.U
    val bg = (bgData(3, 0) =/= 0.U) -> Layer.BG.U

    MuxLookup(spritePriority, 0.U, Seq(
      0.U -> MuxCase(Layer.FILL.U, Seq(debug, sprite, char, fg, bg)),
      1.U -> MuxCase(Layer.FILL.U, Seq(debug, char, sprite, fg, bg)),
      2.U -> MuxCase(Layer.FILL.U, Seq(debug, char, fg, sprite, bg)),
      3.U -> MuxCase(Layer.FILL.U, Seq(debug, char, fg, bg, sprite))
    ))
  }
}
