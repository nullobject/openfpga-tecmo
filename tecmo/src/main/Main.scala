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

package tecmo.main

import arcadia._
import arcadia.cpu.z80._
import arcadia.gfx.VideoIO
import arcadia.mem._
import arcadida.pocket.OptionsIO
import chisel3._
import chisel3.util._
import tecmo._
import tecmo.gfx._
import tecmo.snd.SoundCtrlIO

/** Represents the main PCB. */
class Main extends Module {
  val io = IO(new Bundle {
    /** Video clock */
    val videoClock = Input(Clock())
    /** Game configuration port */
    val gameConfig = Input(GameConfig())
    /** Options port */
    val options = Input(OptionsIO())
    /** Player port */
    val player = PlayerIO()
    /** Pause flag */
    val pause = Input(Bool())
    /** Video port */
    val video = Input(VideoIO())
    /** GPU memory port */
    val gpuMemIO = Flipped(GPUMemIO())
    /** Sound control port */
    val soundCtrl = Flipped(SoundCtrlIO())
    /** Program ROM */
    val progRom = new ProgRomIO
    /** Bank ROM */
    val bankRom = new BankRomIO
  })

  // Wires
  val irq = Wire(Bool())

  // Registers
  val bankReg = RegInit(0.U(5.W))
  val fgScrollReg = RegInit(UVec2(0.U(9.W), 0.U(9.W)))
  val bgScrollReg = RegInit(UVec2(0.U(9.W), 0.U(9.W)))
  val flipReg = RegInit(false.B)

  // Main CPU
  val cpu = Module(new CPU(Config.CPU_CLOCK_DIV))
  val memMap = new MemMap(cpu.io)
  cpu.io.halt := ShiftRegister(io.pause, 2)
  cpu.io.din := DontCare
  cpu.io.int := irq
  cpu.io.nmi := false.B

  // Work RAM
  val workRam = Module(new SinglePortRam(
    addrWidth = Config.WORK_RAM_ADDR_WIDTH,
    dataWidth = Config.WORK_RAM_DATA_WIDTH
  ))

  // Character VRAM
  val charRam = Module(new TrueDualPortRam(
    addrWidthA = Config.CHAR_RAM_ADDR_WIDTH,
    dataWidthA = Config.CHAR_RAM_DATA_WIDTH,
    addrWidthB = Config.CHAR_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.CHAR_RAM_GPU_DATA_WIDTH
  ))
  charRam.io.clockB := io.videoClock

  // Foreground VRAM
  val fgRam = Module(new TrueDualPortRam(
    addrWidthA = Config.FG_RAM_ADDR_WIDTH,
    dataWidthA = Config.FG_RAM_DATA_WIDTH,
    addrWidthB = Config.FG_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.FG_RAM_GPU_DATA_WIDTH
  ))
  fgRam.io.clockB := io.videoClock

  // Background VRAM
  val bgRam = Module(new TrueDualPortRam(
    addrWidthA = Config.BG_RAM_ADDR_WIDTH,
    dataWidthA = Config.BG_RAM_DATA_WIDTH,
    addrWidthB = Config.BG_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.BG_RAM_GPU_DATA_WIDTH
  ))
  bgRam.io.clockB := io.videoClock

  // Sprite VRAM
  val spriteRam = Module(new TrueDualPortRam(
    addrWidthA = Config.SPRITE_RAM_ADDR_WIDTH,
    dataWidthA = Config.SPRITE_RAM_DATA_WIDTH,
    addrWidthB = Config.SPRITE_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.SPRITE_RAM_GPU_DATA_WIDTH
  ))
  spriteRam.io.clockB := io.videoClock

  // Palette RAM
  val paletteRam = Module(new TrueDualPortRam(
    addrWidthA = Config.PALETTE_RAM_ADDR_WIDTH,
    dataWidthA = Config.PALETTE_RAM_DATA_WIDTH,
    addrWidthB = Config.PALETTE_RAM_GPU_ADDR_WIDTH,
    dataWidthB = Config.PALETTE_RAM_GPU_DATA_WIDTH
  ))
  paletteRam.io.clockB := io.videoClock

  // Set interface defaults
  io.progRom.default()
  io.bankRom.default()
  workRam.io.default()
  charRam.io.portA.default()
  fgRam.io.portA.default()
  bgRam.io.portA.default()
  spriteRam.io.portA.default()
  paletteRam.io.portA.default()

  // GPU memory
  io.gpuMemIO.pc := cpu.io.regs.pc
  io.gpuMemIO.layer(0).vram <> charRam.io.portB
  io.gpuMemIO.layer(0).scroll := UVec2(0.U, 0.U)
  io.gpuMemIO.layer(1).vram <> fgRam.io.portB
  io.gpuMemIO.layer(1).scroll := fgScrollReg
  io.gpuMemIO.layer(2).vram <> bgRam.io.portB
  io.gpuMemIO.layer(2).scroll := bgScrollReg
  io.gpuMemIO.sprite.vram <> spriteRam.io.portB
  io.gpuMemIO.paletteRam <> paletteRam.io.portB

  // Trigger an interrupt request on the falling edge of the vertical blank signal.
  //
  // Once the IRQ has been accepted by the CPU, it is acknowledged by activating the IO request
  // signal during the M1 cycle. This clears the interrupt, and the cycle starts over.
  irq := Util.latch(Util.falling(io.video.vBlank), cpu.io.m1 && cpu.io.iorq)

  // Sound control
  io.soundCtrl.req := false.B
  io.soundCtrl.data := cpu.io.dout

  /**
   * Maps video RAM to the given address range.
   *
   * The address is rotated to keep the tile codes and colors contiguous in memory. In the arcade
   * hardware they are split between the lower and upper halves of the VRAM.
   *
   * @param r   The address range.
   * @param mem The VRAM memory interface.
   */
  def vramMap(r: Range, mem: MemIO): Unit = {
    memMap(r).readWriteMemT(mem) { addr => Util.rotateLeft(addr(mem.addrWidth - 1, 0)) }
  }

  /**
   * Sets the scroll register.
   *
   * @param offset The address offset.
   * @param data   The data to be written.
   * @param reg    The scroll register.
   */
  def setScroll(offset: UInt, data: Bits, reg: UVec2): Unit = {
    switch(offset) {
      is(0.U) { reg.x := reg.x(8) ## data }
      is(1.U) { reg.x := data(0) ## reg.x(7, 0) }
      is(2.U) { reg.y := data }
    }
  }

  when(io.options.gameIndex === Game.RYGAR.U) {
    memMap(0x0000 to 0xbfff).readMem(io.progRom)
    memMap(0xc000 to 0xcfff).readWriteMem(workRam.io)
    vramMap(0xd000 to 0xd7ff, charRam.io.portA)
    vramMap(0xd800 to 0xdbff, fgRam.io.portA)
    vramMap(0xdc00 to 0xdfff, bgRam.io.portA)
    memMap(0xe000 to 0xe7ff).readWriteMem(spriteRam.io.portA)
    memMap(0xe800 to 0xefff).readWriteMem(paletteRam.io.portA)
    memMap(0xf000 to 0xf7ff).readMemT(io.bankRom) { addr => bankReg ## addr(10, 0) }
    memMap(0xf800).r { (_, _) => Cat(io.player.up, io.player.down, io.player.right, io.player.left) }
    memMap(0xf801).r { (_, _) => Cat(io.player.buttons(1), io.player.buttons(0)) }
    memMap(0xf802).nopr() // JOY 1
    memMap(0xf803).nopr() // BUTTONS 1
    memMap(0xf804).r { (_, _) => Cat(io.player.coin, 0.U, io.player.start, 0.U) }
    memMap(0xf806 to 0xf807).nopr() // DIP 0
    memMap(0xf808 to 0xf809).nopr() // DIP 1
    memMap(0xf800 to 0xf802).w { (_, offset, data) => setScroll(offset, data, fgScrollReg) }
    memMap(0xf803 to 0xf805).w { (_, offset, data) => setScroll(offset, data, bgScrollReg) }
    memMap(0xf806).w { (_, _, _) => io.soundCtrl.req := true.B }
    memMap(0xf807).w { (_, _, data) => flipReg := data }
    memMap(0xf808).w { (_, _, data) => bankReg := data(7, 3) }
  }

  when(io.options.gameIndex === Game.GEMINI.U) {
    memMap(0x0000 to 0xbfff).readMem(io.progRom)
    memMap(0xc000 to 0xcfff).readWriteMem(workRam.io)
    vramMap(0xd000 to 0xd7ff, charRam.io.portA)
    vramMap(0xd800 to 0xdbff, fgRam.io.portA)
    vramMap(0xdc00 to 0xdfff, bgRam.io.portA)
    memMap(0xe000 to 0xe7ff).readWriteMem(paletteRam.io.portA)
    memMap(0xe800 to 0xefff).readWriteMem(spriteRam.io.portA)
    memMap(0xf000 to 0xf7ff).readMemT(io.bankRom) { addr => bankReg ## addr(10, 0) }
    memMap(0xf800).r { (_, _) => Cat(io.player.up, io.player.down, io.player.right, io.player.left) }
    memMap(0xf801).r { (_, _) => Cat(io.player.buttons(0), io.player.buttons(1)) }
    memMap(0xf802).nopr() // JOY 1
    memMap(0xf803).nopr() // BUTTONS 1
    memMap(0xf805).r { (_, _) => Cat(0.U, io.player.coin, 0.U, io.player.start) }
    memMap(0xf806 to 0xf807).nopr() // DIP 0
    memMap(0xf808 to 0xf809).nopr() // DIP 1
    memMap(0xf800 to 0xf802).w { (_, offset, data) => setScroll(offset, data, fgScrollReg) }
    memMap(0xf803 to 0xf805).w { (_, offset, data) => setScroll(offset, data, bgScrollReg) }
    memMap(0xf806).w { (_, _, _) => io.soundCtrl.req := true.B }
    memMap(0xf807).w { (_, _, data) => flipReg := data }
    memMap(0xf808).w { (_, _, data) => bankReg := data(7, 3) }
  }

  when(io.options.gameIndex === Game.SILKWORM.U) {
    memMap(0x0000 to 0xbfff).readMem(io.progRom)
    vramMap(0xc000 to 0xc3ff, bgRam.io.portA)
    vramMap(0xc400 to 0xc7ff, fgRam.io.portA)
    vramMap(0xc800 to 0xcfff, charRam.io.portA)
    memMap(0xd000 to 0xdfff).readWriteMem(workRam.io)
    memMap(0xe000 to 0xe7ff).readWriteMem(spriteRam.io.portA)
    memMap(0xe800 to 0xefff).readWriteMem(paletteRam.io.portA)
    memMap(0xf000 to 0xf7ff).readMemT(io.bankRom) { addr => bankReg ## addr(10, 0) }
    memMap(0xf800).r { (_, _) => Cat(io.player.up, io.player.down, io.player.right, io.player.left) }
    memMap(0xf801).r { (_, _) => Cat(io.player.buttons(2), io.player.buttons(0), io.player.buttons(1)) }
    memMap(0xf802).nopr() // JOY 1
    memMap(0xf803).nopr() // BUTTONS 1
    memMap(0xf806 to 0xf807).nopr() // DIP 0
    memMap(0xf808 to 0xf809).nopr() // DIP 1
    memMap(0xf80f).r { (_, _) => Cat(0.U, io.player.coin, 0.U, io.player.start) }
    memMap(0xf800 to 0xf802).w { (_, offset, data) => setScroll(offset, data, fgScrollReg) }
    memMap(0xf803 to 0xf805).w { (_, offset, data) => setScroll(offset, data, bgScrollReg) }
    memMap(0xf806).w { (_, _, _) => io.soundCtrl.req := true.B }
    memMap(0xf807).w { (_, _, data) => flipReg := data }
    memMap(0xf808).w { (_, _, data) => bankReg := data(7, 3) }
  }
}
