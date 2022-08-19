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

package arcadia.snd

import arcadia.mem._
import chisel3._
import chisel3.util._

/**
 * The OPL is a FM sound synthesizer.
 *
 * @note This module wraps jotego's JTOPL implementation.
 * @see https://github.com/jotego/jtopl
 */
class JTOPL extends Module {
  val io = IO(new Bundle {
    /** CPU port */
    val cpu = Flipped(MemIO(16, 8))
    /** IRQ */
    val irq = Output(Bool())
    /** Audio output port */
    val audio = ValidIO(SInt(16.W))
  })

  class JTOPLExternal extends BlackBox {
    val io = IO(new Bundle {
      val rst = Input(Bool())
      val clk = Input(Bool())
      val cen = Input(Bool())
      val din = Input(Bits(8.W))
      val addr = Input(Bool())
      val cs_n = Input(Bool())
      val wr_n = Input(Bool())
      val dout = Output(Bits(8.W))
      val irq_n = Output(Bool())
      val snd = Output(SInt(16.W))
      val sample = Output(Bool())
    })

    override def desiredName = "jtopl"
  }

  val opl = Module(new JTOPLExternal)
  opl.io.rst := reset.asBool
  opl.io.clk := clock.asBool
  opl.io.cen := true.B
  opl.io.cs_n := false.B

  opl.io.wr_n := !io.cpu.wr
  opl.io.addr := io.cpu.addr(0)
  opl.io.din := io.cpu.din
  io.cpu.dout := opl.io.dout

  io.irq := !opl.io.irq_n

  io.audio.valid := opl.io.sample
  io.audio.bits := opl.io.snd
}
