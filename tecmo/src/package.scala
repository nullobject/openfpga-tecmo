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

import arcadia.mem.{AsyncReadMemIO, ReadMemIO}
import tecmo.Config

package object tecmo {
  /** Game enum */
  object Game {
    /** Rygar */
    val RYGAR = 0
    /** Gemini Wing */
    val GEMINI = 1
    /** Silkworm */
    val SILKWORM = 2
  }

  /** Graphics format enum */
  object GraphicsFormat {
    /** Default graphics format */
    val GFX_FORMAT_DEFAULT = 0
    /** Gemini Wing graphics format */
    val GFX_FORMAT_GEMINI = 1
  }

  /** Program ROM IO */
  class ProgRomIO extends ReadMemIO(Config.PROG_ROM_ADDR_WIDTH, Config.PROG_ROM_DATA_WIDTH)

  /** Bank ROM IO */
  class BankRomIO extends ReadMemIO(Config.BANK_ROM_ADDR_WIDTH, Config.BANK_ROM_DATA_WIDTH)

  /** Sound ROM IO */
  class SoundRomIO extends ReadMemIO(Config.SOUND_ROM_ADDR_WIDTH, Config.SOUND_ROM_DATA_WIDTH)

  /** Sample ROM IO */
  class SampleRomIO extends ReadMemIO(Config.PCM_ROM_ADDR_WIDTH, Config.PCM_ROM_DATA_WIDTH)

  /** Tile ROM IO */
  class TileRomIO extends AsyncReadMemIO(Config.TILE_ROM_ADDR_WIDTH, Config.TILE_ROM_DATA_WIDTH)

  /** Layer RAM IO (GPU-side) */
  class LayerRamIO extends ReadMemIO(Config.LAYER_RAM_GPU_ADDR_WIDTH, Config.LAYER_RAM_GPU_DATA_WIDTH)

  /** Sprite RAM IO (GPU-side) */
  class SpriteRamIO extends ReadMemIO(Config.SPRITE_RAM_GPU_ADDR_WIDTH, Config.SPRITE_RAM_GPU_DATA_WIDTH)

  /** Palette RAM IO (GPU-side) */
  class PaletteRamIO extends ReadMemIO(Config.PALETTE_RAM_GPU_ADDR_WIDTH, Config.PALETTE_RAM_GPU_DATA_WIDTH)
}
