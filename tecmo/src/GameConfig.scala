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

package tecmo

import chisel3._
import chisel3.util.MuxLookup

/** Represents a game configuration. */
class GameConfig extends Bundle {
  /** Character layer configuration */
  val char = new Bundle {
    /** Graphics format */
    val format = UInt(Config.GFX_FORMAT_WIDTH.W)
  }
  /** Foreground layer configuration */
  val fg = new Bundle {
    /** Graphics format */
    val format = UInt(Config.GFX_FORMAT_WIDTH.W)
  }
  /** Background layer configuration */
  val bg = new Bundle {
    /** Graphics format */
    val format = UInt(Config.GFX_FORMAT_WIDTH.W)
  }
  /** Sprite configuration */
  val sprite = new Bundle {
    /** Graphics format */
    val format = UInt(Config.GFX_FORMAT_WIDTH.W)
  }
}

object GameConfig {
  def apply() = new GameConfig

  /**
   * Returns a game configuration for the given game index.
   *
   * @param index The game index.
   */
  def apply(index: UInt): GameConfig = {
    MuxLookup(index, rygar, Seq(
      Game.GEMINI.U -> gemini,
      Game.SILKWORM.U -> silkworm
    ))
  }

  private def rygar = {
    val wire = Wire(new GameConfig)
    wire.char.format := Config.GFX_FORMAT_DEFAULT.U
    wire.fg.format := Config.GFX_FORMAT_DEFAULT.U
    wire.bg.format := Config.GFX_FORMAT_DEFAULT.U
    wire.sprite.format := Config.GFX_FORMAT_DEFAULT.U
    wire
  }

  private def gemini = {
    val wire = Wire(new GameConfig)
    wire.char.format := Config.GFX_FORMAT_DEFAULT.U
    wire.fg.format := Config.GFX_FORMAT_GEMINI.U
    wire.bg.format := Config.GFX_FORMAT_GEMINI.U
    wire.sprite.format := Config.GFX_FORMAT_GEMINI.U
    wire
  }

  private def silkworm = {
    val wire = Wire(new GameConfig)
    wire.char.format := Config.GFX_FORMAT_DEFAULT.U
    wire.fg.format := Config.GFX_FORMAT_DEFAULT.U
    wire.bg.format := Config.GFX_FORMAT_DEFAULT.U
    wire.sprite.format := Config.GFX_FORMAT_GEMINI.U
    wire
  }
}
