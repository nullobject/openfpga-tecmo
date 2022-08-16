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

package tecmo.gfx

import arcadia.mem._
import chisel3._
import chisel3.util._

/**
 * The frame buffer is used by the sprite layer for rendering.
 *
 * It contains two pages: while one page is being written, the other is being read. The pages are
 * swapped when the swap signal is asserted.
 */
class FrameBuffer(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    /** Write-only memory port */
    val portA = Flipped(WriteMemIO(addrWidth, dataWidth))
    /** Read-only memory port */
    val portB = Flipped(ReadMemIO(addrWidth, dataWidth))
    /** Swaps the pages when asserted */
    val swap = Input(Bool())
  })

  val pageA = Wire(MemIO(addrWidth, dataWidth))
  val pageB = Wire(MemIO(addrWidth, dataWidth))

  val ramA = Module(new DualPortRam(
    addrWidthA = addrWidth,
    dataWidthA = dataWidth,
    addrWidthB = addrWidth,
    dataWidthB = dataWidth
  ))
  val ramB = Module(new DualPortRam(
    addrWidthA = addrWidth,
    dataWidthA = dataWidth,
    addrWidthB = addrWidth,
    dataWidthB = dataWidth
  ))

  pageA.rd := io.portB.rd
  pageA.wr := Mux(io.swap, io.portA.wr, io.portB.rd)
  pageA.addr := Mux(io.swap, io.portA.addr, io.portB.addr)
  pageA.mask := DontCare
  pageA.din := Mux(io.swap && io.portA.wr, io.portA.din, 0.U)
  pageA.dout := ramA.io.portB.dout

  pageB.rd := io.portB.rd
  pageB.wr := Mux(!io.swap, io.portA.wr, io.portB.rd)
  pageB.addr := Mux(!io.swap, io.portA.addr, io.portB.addr)
  pageB.mask := DontCare
  pageB.din := Mux(!io.swap && io.portA.wr, io.portA.din, 0.U)
  pageB.dout := ramB.io.portB.dout

  ramA.io.portA.wr := pageA.wr
  ramA.io.portA.addr := pageA.addr
  ramA.io.portA.mask := pageA.mask
  ramA.io.portA.din := pageA.din
  ramA.io.portB.rd := pageA.rd
  ramA.io.portB.addr := pageA.addr

  ramB.io.portA.wr := pageB.wr
  ramB.io.portA.addr := pageB.addr
  ramB.io.portA.mask := pageB.mask
  ramB.io.portA.din := pageB.din
  ramB.io.portB.rd := pageB.rd
  ramB.io.portB.addr := pageB.addr

  // Output
  io.portB.dout := MuxCase(0.U, Seq(
    (!io.swap && io.portB.rd) -> pageA.dout,
    (io.swap && io.portB.rd) -> pageB.dout
  ))
}
