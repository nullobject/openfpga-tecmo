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

import arcadia.Util
import arcadia.cpu.z80._
import arcadia.mem._
import arcadia.snd._
import chisel3._
import chisel3.util._

/** A bundle that contains ports to control the sound PCB. */
class SoundCtrlIO extends Bundle {
  val req = Input(Bool())
  val data = Input(Bits(8.W))
}

class Sound extends Module {
  val io = IO(new Bundle {
    /** Control port */
    val ctrl = new SoundCtrlIO
    /** Sound ROM port */
    val rom = new SoundRomIO
    /** Audio port */
    val audio = Output(SInt(Config.AUDIO_SAMPLE_WIDTH.W))
  })

  // Wires
  val irq = Wire(Bool())

  // Registers
  val ctrlReqReg = ShiftRegister(io.ctrl.req, 2)
  val ctrlDataReg = ShiftRegister(io.ctrl.data, 2)

  val latch = Util.rising(ctrlReqReg)
  val nmiReg = RegEnable(true.B, false.B, latch)
  val dataReg = RegEnable(ctrlDataReg, latch)

  // Sound CPU
  val cpu = Module(new CPU)
  cpu.io.din := DontCare
  cpu.io.int := irq
  cpu.io.nmi := nmiReg

  // Sound RAM
  val soundRam = Module(new SinglePortRam(
    addrWidth = Config.SOUND_RAM_ADDR_WIDTH,
    dataWidth = Config.SOUND_RAM_DATA_WIDTH
  ))
  soundRam.io.default()

  // FM
  val opl = Module(new JTOPL)
  irq := opl.io.irq
  opl.io.cpu.default()

  // Memory map
  val memMap = new MemMap(cpu.io)
  memMap(0x0000 to 0x3fff).readMem(io.rom)
  memMap(0x4000 to 0x47ff).readWriteMem(soundRam.io)
  memMap(0x8000 to 0x8001).readWriteMem(opl.io.cpu)
  memMap(0xc000 to 0xc000).r { (_, _) => dataReg }
  memMap(0xc000 to 0xc000).nopw() // PCM LO
  memMap(0xd000 to 0xd000).nopw() // PCM HI
  memMap(0xe000 to 0xe000).nopw() // PCM VOL
  memMap(0xf000 to 0xf000).w { (_, _, _) => nmiReg := false.B }

  // Outputs
  io.audio := RegEnable(opl.io.audio.bits, opl.io.audio.valid)
}
