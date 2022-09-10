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

import arcadia._
import arcadia.gfx._
import arcadia.mem._
import arcadia.mem.sdram.{SDRAM, SDRAMIO}
import arcadia.pocket.{Bridge, BridgeIO}
import chisel3._
import chisel3.experimental.FlatIO
import main.Main
import tecmo.snd.Sound

/**
 * The top-level module.
 *
 * This module abstracts the rest of the arcade hardware from platform-specific things (e.g. memory
 * subsystem) that are not part of the original arcade hardware design.
 */
class Tecmo extends Module {
  val io = FlatIO(new Bundle {
    /** CPU reset */
    val cpuReset = Input(Bool())
    /** Bridge clock */
    val bridgeClock = Input(Clock())
    /** CPU clock */
    val cpuClock = Input(Clock())
    /** Video clock */
    val videoClock = Input(Clock())
    /** Bridge port */
    val bridge = BridgeIO()
    /** Player port */
    val player = PlayerIO()
    /** Video port */
    val video = VideoIO()
    /** Audio port */
    val audio = Output(SInt(Config.AUDIO_SAMPLE_WIDTH.W))
    /** RGB output */
    val rgb = Output(UInt(Config.RGB_WIDTH.W))
    /** SDRAM port */
    val sdram = SDRAMIO(Config.sdramConfig)
  })

  // SDRAM controller
  val sdram = Module(new SDRAM(Config.sdramConfig))
  sdram.io.sdram <> io.sdram

  // Pocket bridge controller
  val bridge = Module(new Bridge(
    addrWidth = Config.sdramConfig.addrWidth,
    dataWidth = Config.sdramConfig.dataWidth,
    burstLength = Config.sdramConfig.burstLength
  ))
  bridge.io.bridgeClock := io.bridgeClock
  bridge.io.bridge <> io.bridge

  // Memory subsystem
  val memSys = Module(new MemSys(Config.memSysConfig))
  memSys.io.enable := io.bridge.done
  memSys.io.rom <> bridge.io.download
  memSys.io.out <> sdram.io.mem

  // Video timing
  val videoTiming = withClock(io.videoClock) { Module(new VideoTiming(Config.videoTimingConfig)) }
  videoTiming.io.offset := SVec2(0.S, 0.S)
  val video = videoTiming.io.timing

  // Main PCB
  val main = withClockAndReset(io.cpuClock, io.cpuReset) { Module(new Main) }
  main.io.videoClock := io.videoClock
  main.io.gameConfig := GameConfig(bridge.io.options.gameIndex)
  main.io.options := bridge.io.options
  main.io.player := io.player
  main.io.pause := io.bridge.pause
  main.io.video := video
  main.io.rom.progRom <> Crossing.freeze(io.cpuClock, memSys.io.in(0)).asReadMemIO
  main.io.rom.bankRom <> Crossing.freeze(io.cpuClock, memSys.io.in(1)).asReadMemIO
  main.io.rom.layerTileRom(0) <> Crossing.freeze(io.videoClock, memSys.io.in(2))
  main.io.rom.layerTileRom(1) <> Crossing.freeze(io.videoClock, memSys.io.in(3))
  main.io.rom.layerTileRom(2) <> Crossing.freeze(io.videoClock, memSys.io.in(4))
  main.io.rom.spriteTileRom <> Crossing.freeze(io.videoClock, memSys.io.in(5))

  // Sound PCB
  val sound = withClockAndReset(io.cpuClock, io.cpuReset) { Module(new Sound) }
  sound.io.options := bridge.io.options
  sound.io.ctrl <> main.io.soundCtrl
  sound.io.rom.soundRom <> Crossing.freeze(io.cpuClock, memSys.io.in(6)).asReadMemIO
  sound.io.rom.pcmRom <> Crossing.freeze(io.cpuClock, memSys.io.in(7))

  val rgb = Mux(video.displayEnable,
    main.io.rgb,
    bridge.io.options.scalerMode.pad(10) ## 0.U(13.W)
  )

  // Outputs
  io.video := RegNext(video)
  io.rgb := RegNext(rgb)
  io.audio := sound.io.audio
}
