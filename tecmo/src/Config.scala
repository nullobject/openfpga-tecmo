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

import arcadia.gfx.VideoTimingConfig
import arcadia.mem._

object Config {
  /** System clock frequency (Hz) */
  val CLOCK_FREQ = 96_000_000D

  /** CPU clock frequency (Hz) */
  val CPU_CLOCK_FREQ = 12_000_000D
  /** CPU clock divider */
  val CPU_CLOCK_DIV = 2 // 6 MHz
  /** Sound clock divider */
  val SOUND_CLOCK_DIV = 3 // 4 MHz

  /** Video clock frequency (Hz) */
  val VIDEO_CLOCK_FREQ = 6_000_000D
  /** Video clock divider */
  val VIDEO_CLOCK_DIV = 1 // 6 MHz

  /** The width of a priority value */
  val PRIO_WIDTH = 2
  /** The width of a color code value */
  val PALETTE_WIDTH = 4
  /** The width of a palette index */
  val COLOR_WIDTH = 4

  /** The width of audio sample values */
  val AUDIO_SAMPLE_WIDTH = 16

  /** The width of the final RGB values */
  val RGB_WIDTH = 24

  val PROG_ROM_ADDR_WIDTH = 16 // 64 kB
  val PROG_ROM_DATA_WIDTH = 8

  val BANK_ROM_ADDR_WIDTH = 15 // 64 kB
  val BANK_ROM_DATA_WIDTH = 8

  val SOUND_ROM_ADDR_WIDTH = 15 // 32 kB
  val SOUND_ROM_DATA_WIDTH = 8

  val PCM_ROM_ADDR_WIDTH = 15 // 32 kB
  val PCM_ROM_DATA_WIDTH = 8

  val WORK_RAM_ADDR_WIDTH = 12 // 4 kB
  val WORK_RAM_DATA_WIDTH = 8

  val SOUND_RAM_ADDR_WIDTH = 11 // 2kB
  val SOUND_RAM_DATA_WIDTH = 8

  val LAYER_RAM_GPU_ADDR_WIDTH = 10
  val LAYER_RAM_GPU_DATA_WIDTH = 16

  val CHAR_RAM_ADDR_WIDTH = 11 // 2 kB
  val CHAR_RAM_DATA_WIDTH = 8
  val CHAR_RAM_GPU_ADDR_WIDTH = 10
  val CHAR_RAM_GPU_DATA_WIDTH = 16

  val FG_RAM_ADDR_WIDTH = 10 // 1 kB
  val FG_RAM_DATA_WIDTH = 8
  val FG_RAM_GPU_ADDR_WIDTH = 9
  val FG_RAM_GPU_DATA_WIDTH = 16

  val BG_RAM_ADDR_WIDTH = 10 // 1 kB
  val BG_RAM_DATA_WIDTH = 8
  val BG_RAM_GPU_ADDR_WIDTH = 9
  val BG_RAM_GPU_DATA_WIDTH = 16

  val SPRITE_RAM_ADDR_WIDTH = 11 // 2 kB
  val SPRITE_RAM_DATA_WIDTH = 8
  val SPRITE_RAM_GPU_ADDR_WIDTH = 8 // 2 kB
  val SPRITE_RAM_GPU_DATA_WIDTH = 64

  val PALETTE_RAM_ADDR_WIDTH = 11 // 2 kB
  val PALETTE_RAM_DATA_WIDTH = 8
  val PALETTE_RAM_GPU_ADDR_WIDTH = 10
  val PALETTE_RAM_GPU_DATA_WIDTH = 16

  /** The number of tilemap layers */
  val LAYER_COUNT = 3

  // Tile ROMs
  val TILE_ROM_ADDR_WIDTH = 18
  val TILE_ROM_DATA_WIDTH = 32
  val CHAR_ROM_ADDR_WIDTH = 15 // 32 kB
  val CHAR_ROM_DATA_WIDTH = 32
  val FG_ROM_ADDR_WIDTH = 18 // 256 kB
  val FG_ROM_DATA_WIDTH = 32
  val BG_ROM_ADDR_WIDTH = 18 // 256 kB
  val BG_ROM_DATA_WIDTH = 32
  val SPRITE_ROM_ADDR_WIDTH = 18 // 256 kB
  val SPRITE_ROM_DATA_WIDTH = 32
  val DEBUG_ROM_ADDR_WIDTH = 9
  val DEBUG_ROM_DATA_WIDTH = 32

  val FRAME_BUFFER_ADDR_WIDTH = 16
  val FRAME_BUFFER_DATA_WIDTH = 10

  /** PSRAM configuration */
  val psramConfig = psram.Config(clockFreq = CLOCK_FREQ, burstLength = 4)

  /** Memory subsystem configuration */
  val memSysConfig = MemSysConfig(
    addrWidth = psramConfig.addrWidth,
    dataWidth = psramConfig.dataWidth,
    burstLength = psramConfig.burstLength,
    slots = Seq(
      // Main ROM
      SlotConfig(
        addrWidth = Config.PROG_ROM_ADDR_WIDTH,
        dataWidth = Config.PROG_ROM_DATA_WIDTH,
        offset = 0x00000
      ),
      // Bank ROM
      SlotConfig(
        addrWidth = Config.BANK_ROM_ADDR_WIDTH,
        dataWidth = Config.BANK_ROM_DATA_WIDTH,
        offset = 0x10000
      ),
      // Character ROM
      SlotConfig(
        addrWidth = Config.CHAR_ROM_ADDR_WIDTH,
        dataWidth = Config.CHAR_ROM_DATA_WIDTH,
        offset = 0x20000
      ),
      // Foreground ROM
      SlotConfig(
        addrWidth = Config.FG_ROM_ADDR_WIDTH,
        dataWidth = Config.FG_ROM_DATA_WIDTH,
        offset = 0x30000
      ),
      // Background ROM
      SlotConfig(
        addrWidth = Config.BG_ROM_ADDR_WIDTH,
        dataWidth = Config.BG_ROM_DATA_WIDTH,
        offset = 0x70000
      ),
      // Sprite ROM
      SlotConfig(
        addrWidth = Config.SPRITE_ROM_ADDR_WIDTH,
        dataWidth = Config.SPRITE_ROM_DATA_WIDTH,
        offset = 0xb0000
      ),
      // Sound ROM
      SlotConfig(
        addrWidth = Config.SOUND_ROM_ADDR_WIDTH,
        dataWidth = Config.SOUND_ROM_DATA_WIDTH,
        offset = 0xf0000
      ),
      // PCM ROM
      SlotConfig(
        addrWidth = Config.PCM_ROM_ADDR_WIDTH,
        dataWidth = Config.PCM_ROM_DATA_WIDTH,
        offset = 0xf8000
      )
    )
  )

  /** Video timing configuration */
  val videoTimingConfig = VideoTimingConfig(
    clockFreq = VIDEO_CLOCK_FREQ,
    clockDiv = VIDEO_CLOCK_DIV,
    hFreq = 15625, // Hz
    vFreq = 59.19, // Hz
    hDisplay = 256,
    vDisplay = 224,
    hFrontPorch = 40,
    vFrontPorch = 16,
    hRetrace = 32,
    vRetrace = 8,
    vOffset = 16
  )
}
