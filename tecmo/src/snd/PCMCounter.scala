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

package tecmo.snd

import arcadia.Util
import chisel3._
import tecmo._

class PCMCounter extends Module {
  val io = IO(new Bundle {
    /** Clock enable */
    val cen = Input(Bool())
    /** Write enable */
    val wr = Input(Bool())
    /** Low/high address flag */
    val high = Input(Bool())
    /** Data input port */
    val din = Input(Bits(8.W))
    /** Data output port */
    val dout = Output(Bits(4.W))
    /** PCM ROM port */
    val rom = new SampleRomIO
  })

  // Registers
  val busyReg = RegInit(false.B)
  val addrReg = Reg(UInt(16.W))
  val highReg = Reg(UInt(16.W))
  val nibbleReg = RegInit(false.B)

  // Control signals
  val latchLow = io.wr && !io.high
  val latchHigh = io.wr && io.high
  val increment = busyReg && Util.falling(io.cen)
  val done = addrReg === highReg && nibbleReg

  // Latch low address
  when(latchLow) {
    busyReg := true.B
    addrReg := io.din ## 0.U(8.W)
  }

  // Latch high address
  when(latchHigh) {
    highReg := io.din ## 0.U(8.W)
  }

  // Increment address
  when(increment) {
    when(done) {
      busyReg := false.B
    }.otherwise {
      when(nibbleReg) { addrReg := addrReg + 1.U }
      nibbleReg := !nibbleReg
    }
  }

  // Outputs
  io.dout := Mux(addrReg(0), io.rom.dout(7, 4), io.rom.dout(3, 0))
  io.rom.rd := busyReg
  io.rom.addr := addrReg

  // Debug
  printf(p"PCMCounter(rd: ${io.rom.rd}, nibble: $nibbleReg, addr: 0x${Hexadecimal(addrReg)}, high: 0x${Hexadecimal(highReg)})\n")
}
