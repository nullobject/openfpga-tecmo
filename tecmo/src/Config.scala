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
  /** The system clock frequency (Hz) */
  val CLOCK_FREQ = 96000000

  /** The width of a priority value */
  val PRIO_WIDTH = 2
  /** The width of a color code value */
  val PALETTE_WIDTH = 4
  /** The width of a palette index */
  val COLOR_WIDTH = 4

  /** The number of bits per color channel for the output RGB signal */
  val RGB_OUTPUT_BPP = 8

  val SPRITE_CODE_WIDTH = 13
  val SPRITE_SIZE_WIDTH = 7

  val PROG_ROM_ADDR_WIDTH = 16 // 64 kB
  val PROG_ROM_DATA_WIDTH = 8

  val BANK_ROM_ADDR_WIDTH = 15 // 32 kB
  val BANK_ROM_DATA_WIDTH = 8

  val SOUND_ROM_ADDR_WIDTH = 13 // 64 kB
  val SOUND_ROM_DATA_WIDTH = 8

  val WORK_RAM_ADDR_WIDTH = 12 // 4 kB
  val WORK_RAM_DATA_WIDTH = 8

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

  // Tile ROMs
  val TILE_ROM_ADDR_WIDTH = 17
  val TILE_ROM_DATA_WIDTH = 32
  val CHAR_ROM_ADDR_WIDTH = 15 // 32 kB
  val CHAR_ROM_DATA_WIDTH = 32
  val FG_ROM_ADDR_WIDTH = 17 // 128 kB
  val FG_ROM_DATA_WIDTH = 32
  val BG_ROM_ADDR_WIDTH = 17 // 128 kB
  val BG_ROM_DATA_WIDTH = 32
  val SPRITE_ROM_ADDR_WIDTH = 17 // 128 kB
  val SPRITE_ROM_DATA_WIDTH = 32
  val DEBUG_ROM_ADDR_WIDTH = 9
  val DEBUG_ROM_DATA_WIDTH = 32

  val FRAME_BUFFER_ADDR_WIDTH = 16
  val FRAME_BUFFER_DATA_WIDTH = 10

  val LINE_BUFFER_ADDR_WIDTH = 8
  val LINE_BUFFER_DATA_WIDTH = 8

  /** Scroll layer offset */
  val SCROLL_OFFSET = 56

  /** SDRAM configuration */
  val sdramConfig = sdram.Config(clockFreq = CLOCK_FREQ, burstLength = 2)

  /** Memory subsystem configuration */
  val memSysConfig = MemSysConfig(
    addrWidth = sdramConfig.addrWidth,
    dataWidth = sdramConfig.dataWidth,
    burstLength = sdramConfig.burstLength,
    slots = Seq(
//      // Main ROM
//      SlotConfig(
//        addrWidth = Config.PROG_ROM_ADDR_WIDTH,
//        dataWidth = Config.PROG_ROM_DATA_WIDTH,
//        offset = 0x00000
//      ),
//      // Bank ROM
//      SlotConfig(
//        addrWidth = Config.BANK_ROM_ADDR_WIDTH,
//        dataWidth = Config.BANK_ROM_DATA_WIDTH,
//        offset = 0x0c000
//      ),
//      // Sound ROM
//      SlotConfig(
//        addrWidth = Config.SOUND_ROM_ADDR_WIDTH,
//        dataWidth = Config.SOUND_ROM_DATA_WIDTH,
//        offset = 0x14000
//      ),
      // Character ROM
      SlotConfig(
        addrWidth = Config.CHAR_ROM_ADDR_WIDTH,
        dataWidth = Config.CHAR_ROM_DATA_WIDTH,
        offset = 0x16000
      ),
      // Foreground ROM
      SlotConfig(
        addrWidth = Config.FG_ROM_ADDR_WIDTH,
        dataWidth = Config.FG_ROM_DATA_WIDTH,
        offset = 0x1e000
      ),
      // Background ROM
      SlotConfig(
        addrWidth = Config.BG_ROM_ADDR_WIDTH,
        dataWidth = Config.BG_ROM_DATA_WIDTH,
        offset = 0x3e000
      ),
      // Sprite ROM
      SlotConfig(
        addrWidth = Config.SPRITE_ROM_ADDR_WIDTH,
        dataWidth = Config.SPRITE_ROM_DATA_WIDTH,
        offset = 0x5e000
      )
    )
  )

  /** Video timing configuration */
  val videoTimingConfig = VideoTimingConfig(
    clockFreq = 6000000,
    clockDiv = 1,
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
