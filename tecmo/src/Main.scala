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
import arcadia.pocket.Bridge
import chisel3._
import chisel3.experimental.FlatIO
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

/**
 * The top-level module.
 *
 * This module abstracts the rest of the arcade hardware from platform-specific things (e.g. memory
 * subsystem) that are not part of the original arcade hardware design.
 */
class Main extends Module {
  val io = FlatIO(new Bundle {
    /** Core reset */
    val coreReset = Input(Bool())
    /** Video clock */
    val videoClock = Input(Clock())
    /** SDRAM port */
    val sdram = SDRAMIO(Config.sdramConfig)
    /** Video port */
    val video = VideoIO()
    /** RGB output */
    val rgb = Output(RGB(Config.COLOR_WIDTH.W))
    /** Bridge port */
    val bridge = Bridge()
    /** Player port */
    val player = PlayerIO()
  })

  // SDRAM controller
  val sdram = Module(new SDRAM(Config.sdramConfig))
  sdram.io.sdram <> io.sdram

  // Memory subsystem
  val memSys = Module(new MemSys(Config.memSysConfig))
  memSys.io.prog.rom <> io.bridge.rom
  memSys.io.prog.done := io.bridge.done
  memSys.io.out <> sdram.io.mem

  // The debug ROM contains alphanumeric character tiles
  val debugRom = Module(new SinglePortRom(
    addrWidth = Config.DEBUG_ROM_ADDR_WIDTH,
    dataWidth = Config.DEBUG_ROM_DATA_WIDTH,
    depth = 512,
    initFile = "roms/alpha.mif"
  ))

  // Video timing
  val videoTiming = withClock(io.videoClock) { Module(new VideoTiming(Config.videoTimingConfig)) }
  videoTiming.io.offset := SVec2(0.S, 0.S)
  val video = videoTiming.io.timing

  // Tecmo board
  val tecmo = withClockAndReset(io.videoClock, io.coreReset) { Module(new Tecmo) }
  tecmo.io.rom.progRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(0)).asReadMemIO
  tecmo.io.rom.bankRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(1)).asReadMemIO
  tecmo.io.rom.charRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(2)).asReadMemIO
  tecmo.io.rom.fgRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(3)).asReadMemIO
  tecmo.io.rom.bgRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(4)).asReadMemIO
  tecmo.io.rom.spriteRom <> DataFreezer.freeze(io.videoClock, memSys.io.in(5))
  tecmo.io.rom.debugRom <> debugRom.io
  tecmo.io.video <> video
  tecmo.io.rgb <> io.rgb
  tecmo.io.player <> io.player
  tecmo.io.flip := false.B
  tecmo.io.debug := true.B

  val black = RGB(0.U(Config.COLOR_WIDTH.W), 0.U(Config.COLOR_WIDTH.W), 0.U(Config.COLOR_WIDTH.W))

  // Video output
  io.video <> RegNext(video)
  io.rgb <> RegNext(Mux(video.displayEnable, tecmo.io.rgb, black))
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/core"),
    Seq(ChiselGeneratorAnnotation(() => new Main))
  )
}
