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
import arcadia.cpu.z80._
import arcadia.gfx.VideoIO
import arcadia.mem._
import chisel3._
import chisel3.util._
import tecmo.gfx._

/** Represents the main PCB. */
class Main extends Module {
  val io = IO(new Bundle {
    /** Flip video */
    val flip = Input(Bool())
    /** Enable debug mode */
    val debug = Input(Bool())
    /** Player port */
    val player = PlayerIO()
    /** Video port */
    val video = Flipped(VideoIO())
    /** ROM port */
    val rom = new RomIO
    /** RGB output */
    val rgb = Output(RGB(Config.RGB_OUTPUT_BPP.W))
  })

  // Wires
  val irq = Wire(Bool())

  // Registers
  val bankReg = RegInit(0.U(4.W))
  val fgScrollReg = RegInit(UVec2(0.U(9.W), 0.U(9.W)))
  val bgScrollReg = RegInit(UVec2(0.U(9.W), 0.U(9.W)))

  // Z80 CPU
  val cpu = Module(new CPU)
  cpu.io.din := DontCare
  cpu.io.int := irq
  cpu.io.nmi := false.B

  // Work RAM
  val workRam = Module(new SinglePortRam(
    addrWidth = Config.WORK_RAM_ADDR_WIDTH,
    dataWidth = Config.WORK_RAM_DATA_WIDTH
  ))
  workRam.io.default()

  // Character RAM
  val charRam = Module(new TrueDualPortRam(
    addrWidthA = Config.CHAR_RAM_ADDR_WIDTH,
    dataWidthA = Config.CHAR_RAM_DATA_WIDTH,
    addrWidthB = Config.CHAR_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.CHAR_RAM_GPU_DATA_WIDTH
  ))
  charRam.io.clockB := clock
  charRam.io.portA.default()

  // Foreground RAM
  val fgRam = Module(new TrueDualPortRam(
    addrWidthA = Config.FG_RAM_ADDR_WIDTH,
    dataWidthA = Config.FG_RAM_DATA_WIDTH,
    addrWidthB = Config.FG_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.FG_RAM_GPU_DATA_WIDTH
  ))
  fgRam.io.clockB := clock
  fgRam.io.portA.default()

  // Background RAM
  val bgRam = Module(new TrueDualPortRam(
    addrWidthA = Config.BG_RAM_ADDR_WIDTH,
    dataWidthA = Config.BG_RAM_DATA_WIDTH,
    addrWidthB = Config.BG_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.BG_RAM_GPU_DATA_WIDTH
  ))
  bgRam.io.clockB := clock
  bgRam.io.portA.default()

  // Sprite RAM
  val spriteRam = Module(new TrueDualPortRam(
    addrWidthA = Config.SPRITE_RAM_ADDR_WIDTH,
    dataWidthA = Config.SPRITE_RAM_DATA_WIDTH,
    addrWidthB = Config.SPRITE_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.SPRITE_RAM_GPU_DATA_WIDTH
  ))
  spriteRam.io.clockB := clock
  spriteRam.io.portA.default()

  // Palette RAM
  val paletteRam = Module(new TrueDualPortRam(
    addrWidthA = Config.PALETTE_RAM_ADDR_WIDTH,
    dataWidthA = Config.PALETTE_RAM_DATA_WIDTH,
    addrWidthB = Config.PALETTE_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.PALETTE_RAM_GPU_DATA_WIDTH
  ))
  paletteRam.io.clockB := clock
  paletteRam.io.portA.default()

  // GPU
  val gpu = Module(new GPU)
  gpu.io.flip := io.flip
  gpu.io.debug := io.debug
  gpu.io.pc := cpu.io.regs.pc
  gpu.io.paletteRam <> paletteRam.io.portB
  gpu.io.debugRom <> io.rom.debugRom
  gpu.io.charCtrl.vram <> charRam.io.portB
  gpu.io.charCtrl.tileRom <> io.rom.charRom
  gpu.io.charCtrl.scrollPos := UVec2(0.U, 0.U)
  gpu.io.fgCtrl.vram <> fgRam.io.portB
  gpu.io.fgCtrl.tileRom <> io.rom.fgRom
  gpu.io.fgCtrl.scrollPos := fgScrollReg + UVec2(Config.SCROLL_OFFSET.U, 0.U)
  gpu.io.bgCtrl.vram <> bgRam.io.portB
  gpu.io.bgCtrl.tileRom <> io.rom.bgRom
  gpu.io.bgCtrl.scrollPos := bgScrollReg + UVec2(Config.SCROLL_OFFSET.U, 0.U)
  gpu.io.spriteCtrl.vram <> spriteRam.io.portB
  gpu.io.spriteCtrl.tileRom <> io.rom.spriteRom
  gpu.io.video <> io.video
  io.rgb := gpu.io.rgb

  // Trigger an interrupt request on the falling edge of the vertical blank signal.
  //
  // Once the IRQ has been accepted by the CPU, it is acknowledged by activating the IO request
  // signal during the M1 cycle. This clears the interrupt, and the cycle starts over.
  irq := Util.latch(Util.falling(io.video.vBlank), cpu.io.m1 && cpu.io.iorq)

  // Memory map
  val memMap = new MemMap(cpu.io)
  memMap(0x0000 to 0xbfff).readMem(io.rom.progRom)
  memMap(0xc000 to 0xcfff).readWriteMem(workRam.io)
  memMap(0xd000 to 0xd7ff).readWriteMemT(charRam.io.portA) { addr =>
    // Rotate the address to keep the tile codes and colors contiguous in memory. Normally they are
    // split across the lower and upper halves of the character RAM.
    Util.rotateLeft(addr(Config.CHAR_RAM_ADDR_WIDTH - 1, 0))
  }
  memMap(0xd800 to 0xdbff).readWriteMemT(fgRam.io.portA) { addr =>
    // Rotate the address to keep the tile codes and colors contiguous in memory. Normally they are
    // split across the lower and upper halves of the foreground RAM.
    Util.rotateLeft(addr(Config.FG_RAM_ADDR_WIDTH - 1, 0))
  }
  memMap(0xdc00 to 0xdfff).readWriteMemT(bgRam.io.portA) { addr =>
    // Rotate the address to keep the tile codes and colors contiguous in memory. Normally they are
    // split across the lower and upper halves of the background RAM.
    Util.rotateLeft(addr(Config.BG_RAM_ADDR_WIDTH - 1, 0))
  }
  memMap(0xe000 to 0xe7ff).readWriteMem(spriteRam.io.portA)
  memMap(0xe800 to 0xefff).readWriteMem(paletteRam.io.portA)
  memMap(0xf000 to 0xf7ff).readMemT(io.rom.bankRom) { addr =>
    // Select the current bank
    bankReg ## addr(10, 0)
  }
  memMap(0xf800).r { (_, _) => Cat(io.player.up, io.player.down, io.player.right, io.player.left) }
  memMap(0xf801).r { (_, _) => Cat(io.player.buttons(2), io.player.buttons(1), io.player.buttons(0)) }
  memMap(0xf804).r { (_, _) => Cat(io.player.coin, 0.U, io.player.start, 0.U) }
  memMap(0xf800 to 0xf802).w { (_, offset, data) =>
    switch(offset) {
      is(0.U) { fgScrollReg.x := fgScrollReg.x(8) ## data }
      is(1.U) { fgScrollReg.x := data(0) ## fgScrollReg.x(7, 0) }
      is(2.U) { fgScrollReg.y := data }
    }
  }
  memMap(0xf803 to 0xf805).w { (_, offset, data) =>
    switch(offset) {
      is(0.U) { bgScrollReg.x := bgScrollReg.x(8) ## data }
      is(1.U) { bgScrollReg.x := data(0) ## bgScrollReg.x(7, 0) }
      is(2.U) { bgScrollReg.y := data }
    }
  }
  memMap(0xf808).w { (_, _, data) => bankReg := data(6, 3) }
}
