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
import arcadia.mem._
import arcadia.mem.arbiter.BurstMemArbiter
import arcadia.mem.buffer.BurstBuffer
import arcadia.mem.request.WriteRequest
import arcadia.pocket.Bridge
import chisel3._
import chisel3.util._

/**
 * Represents a memory slot configuration.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 * @param depth     The number of entries in the cache.
 * @param offset    The offset of the output address.
 */
case class SlotConfig(addrWidth: Int, dataWidth: Int, depth: Int = 16, offset: Int = 0)

/**
 * Represents a memory system configuration.
 *
 * @param addrWidth   The address bus width.
 * @param dataWidth   The data bus width.
 * @param burstLength The number of words to be transferred during a read/write.
 * @param slots       The slots to be multiplexed.
 */
case class MemSysConfig(addrWidth: Int, dataWidth: Int, burstLength: Int, slots: Seq[SlotConfig])

/**
 * Multiplexes multiple memory devices to a single memory interface.
 *
 * @param config The memory system configuration.
 */
class MemSys(config: MemSysConfig) extends Module {
  val io = IO(new Bundle {
    val prog = new Bundle {
      /** Download clock */
      val clock = Input(Clock())
      /** ROM download port */
      val rom = Flipped(AsyncWriteMemIO(Bridge.ADDR_WIDTH, Bridge.DATA_WIDTH))
      /** Asserted when the ROM has finished downloading */
      val done = Input(Bool())
    }
    /** Input port */
    val in = Flipped(MixedVec(config.slots.map(slot => AsyncReadMemIO(slot.addrWidth, slot.dataWidth))))
    /** Output port */
    val out = BurstMemIO(config.addrWidth, config.dataWidth)
    /** Asserted when the memory system is ready */
    val ready = Output(Bool())
  })

  // The FIFO is used to buffer download data
  val fifo = withClock(io.prog.clock) { Module(new DualClockFIFO(new WriteRequest(UInt(Bridge.ADDR_WIDTH.W), Bits(Bridge.DATA_WIDTH.W)), MemSys.FIFO_DEPTH)) }
  fifo.io.readClock := clock
  fifo.io.enq.bits := WriteRequest(io.prog.rom)
  fifo.io.enq.valid := io.prog.rom.wr
  io.prog.rom.waitReq := !fifo.io.enq.ready

  // The download buffer is used to buffer ROM data from the bridge, so that complete words are
  // written to memory.
  val downloadBuffer = Module(new BurstBuffer(buffer.Config(
    inAddrWidth = Bridge.ADDR_WIDTH,
    inDataWidth = Bridge.DATA_WIDTH,
    outAddrWidth = config.addrWidth,
    outDataWidth = config.dataWidth,
    burstLength = config.burstLength
  )))
  downloadBuffer.io.in.addr := fifo.io.deq.bits.addr
  downloadBuffer.io.in.din := fifo.io.deq.bits.din
  downloadBuffer.io.in.mask := Fill(Bridge.DATA_WIDTH / 8, 1.U)
  downloadBuffer.io.in.wr := fifo.io.deq.valid
  fifo.io.deq.ready := !downloadBuffer.io.in.waitReq

  // Arbiter
  val arbiter = Module(new BurstMemArbiter(config.slots.size + 1, config.addrWidth, config.dataWidth))
  arbiter.io.in(0) <> downloadBuffer.io.out.asBurstMemIO
  arbiter.io.out <> io.out

  // Slots
  for ((slotConfig, i) <- config.slots.zipWithIndex) {
    val slot = Module(new cache.ReadCache(cache.Config(
      inAddrWidth = slotConfig.addrWidth,
      inDataWidth = slotConfig.dataWidth,
      outAddrWidth = config.addrWidth,
      outDataWidth = config.dataWidth,
      lineWidth = config.burstLength,
      depth = slotConfig.depth,
      wrapping = true
    )))
    slot.io.enable := io.ready
    slot.io.in <> io.in(i)
    slot.io.out.mapAddr(_ + slotConfig.offset.U).asBurstMemIO <> arbiter.io.in(i + 1)
  }

  // Latch ready flag
  io.ready := Util.latchSync(io.prog.done)
}

object MemSys {
  /** The depth of the download FIFO in words. */
  val FIFO_DEPTH = 16
}
