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
import tecmo.Config

/** Represent an entry in a color palette and a priority. */
class PaletteEntry extends Bundle {
  /** Priority */
  val priority = UInt(Config.PRIO_WIDTH.W)
  /** Palette index */
  val palette = UInt(Config.PALETTE_WIDTH.W)
  /** Color index */
  val color = UInt(Config.COLOR_WIDTH.W)
}

object PaletteEntry {
  /**
   * Constructs a new palette entry.
   *
   * @param priority The priority value.
   * @param palette  The palette index.
   * @param color    The color index.
   */
  def apply(priority: Bits, palette: Bits, color: Bits): PaletteEntry = {
    val wire = Wire(new PaletteEntry)
    wire.priority := priority
    wire.palette := palette
    wire.color := color
    wire
  }

  /** Returns an empty palette entry. */
  def zero: PaletteEntry = apply(0.U, 0.U, 0.U)
}
