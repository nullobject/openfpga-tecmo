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

import chisel3._
import chisel3.util._
import tecmo._

/** The color mixer combines the output from different layers to produce the final pixel. */
class ColorMixer extends Module {
  val io = IO(new Bundle {
    /** Sprite palette entry */
    val spritePen = Input(new PaletteEntry)
    /** Character layer palette entry */
    val charPen = Input(new PaletteEntry)
    /** Foreground layer palette entry */
    val fgPen = Input(new PaletteEntry)
    /** Background layer palette entry */
    val bgPen = Input(new PaletteEntry)
    /** Debug layer palette entry */
    val debugPen = Input(new PaletteEntry)
    /** Palette RAM port */
    val paletteRam = new PaletteRamIO
    /** Pixel data */
    val dout = Output(UInt(Config.PALETTE_RAM_GPU_DATA_WIDTH.W))
  })

  // Mux the layers
  val index = ColorMixer.muxLayers(io.spritePen, io.charPen, io.fgPen, io.bgPen, io.debugPen)

  // Mux the layers
  val paletteRamAddr = MuxLookup(index, 0.U)(Seq(
    ColorMixer.Priority.FILL.U -> ColorMixer.paletteRamAddr(PaletteEntry.zero, 1.U),
    ColorMixer.Priority.SPRITE.U -> ColorMixer.paletteRamAddr(io.spritePen, 0.U),
    ColorMixer.Priority.CHAR.U -> ColorMixer.paletteRamAddr(io.charPen, 1.U),
    ColorMixer.Priority.FG.U -> ColorMixer.paletteRamAddr(io.fgPen, 2.U),
    ColorMixer.Priority.BG.U -> ColorMixer.paletteRamAddr(io.bgPen, 3.U),
    ColorMixer.Priority.DEBUG.U -> ColorMixer.paletteRamAddr(io.debugPen, 1.U)
  ))

  // Outputs
  io.paletteRam.rd := true.B // read-only
  io.paletteRam.addr := paletteRamAddr
  io.dout := RegNext(io.paletteRam.dout)
}

object ColorMixer {
  /** Color mixer priority */
  object Priority {
    val FILL = 0
    val SPRITE = 1
    val CHAR = 2
    val FG = 3
    val BG = 4
    val DEBUG = 5
  }

  /**
   * Calculates the palette RAM address from the given palette entry.
   *
   * @param pen  The palette entry.
   * @param bank The palette RAM bank.
   * @return A memory address.
   */
  private def paletteRamAddr(pen: PaletteEntry, bank: UInt): UInt = bank ## pen.palette ## pen.color

  /**
   * Calculates the layer with the highest priority.
   *
   * @param spritePen The sprite palette entry.
   * @param charPen   The character layer palette entry.
   * @param fgPen     The foreground layer palette entry.
   * @param bgPen     The background layer palette entry.
   * @param debugPen  The debug layer palette entry.
   * @return The index of the layer with the highest priority.
   */
  private def muxLayers(spritePen: PaletteEntry,
                        charPen: PaletteEntry,
                        fgPen: PaletteEntry,
                        bgPen: PaletteEntry,
                        debugPen: PaletteEntry): UInt = {
    val sprite = (spritePen.color =/= 0.U) -> Priority.SPRITE.U
    val char = (charPen.color =/= 0.U) -> Priority.CHAR.U
    val fg = (fgPen.color =/= 0.U) -> Priority.FG.U
    val bg = (bgPen.color =/= 0.U) -> Priority.BG.U
    val debug = (debugPen.color =/= 0.U) -> Priority.DEBUG.U

    MuxLookup(spritePen.priority, 0.U)(Seq(
      0.U -> MuxCase(Priority.FILL.U, Seq(debug, sprite, char, fg, bg)),
      1.U -> MuxCase(Priority.FILL.U, Seq(debug, char, sprite, fg, bg)),
      2.U -> MuxCase(Priority.FILL.U, Seq(debug, char, fg, sprite, bg)),
      3.U -> MuxCase(Priority.FILL.U, Seq(debug, char, fg, bg, sprite))
    ))
  }
}
