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

import arcadia.mem._
import arcadia.mem.arbiter.BurstMemArbiter
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
    val rom = Flipped(BurstWriteMemIO(config.addrWidth, config.dataWidth))
    /** Input port */
    val in = Flipped(MixedVec(config.slots.map(slot => AsyncReadMemIO(slot.addrWidth, slot.dataWidth))))
    /** Output port */
    val out = BurstMemIO(config.addrWidth, config.dataWidth)
    /** Enable memory system */
    val enable = Input(Bool())
  })

  // Arbiter
  val arbiter = Module(new BurstMemArbiter(config.slots.size + 1, config.addrWidth, config.dataWidth))
  arbiter.io.in(0) <> io.rom.asBurstMemIO
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
    slot.io.enable := io.enable
    slot.io.in <> io.in(i)
    slot.io.out.mapAddr(_ + slotConfig.offset.U).asBurstMemIO <> arbiter.io.in(i + 1)
  }
}
