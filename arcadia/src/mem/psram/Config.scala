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

package arcadia.mem.psram

import arcadia.mem.BusConfig
import chisel3._
import chisel3.util.log2Ceil

/**
 * Represents the PSRAM memory configuration.
 *
 * @param clockFreq   The SDRAM clock frequency (Hz).
 * @param addrWidth   The width of the address bus.
 * @param dataWidth   The width of the data bus.
 * @param burstLength The number of words to be transferred during a read/write.
 * @param burstWrap   Set to true when the burst is wrapped, false otherwise.
 * @param latency     The delay in clock cycles, between the start of a read/write operation and the
 *                    first data value transferred.
 * @param tPU         The device initialization delay (ns).
 * @param tVP         Address valid pulse width (ns).
 * @param tWP         Write pulse width (ns).
 */
case class Config(clockFreq: Double,
                  addrWidth: Int = 22,
                  dataWidth: Int = 16,
                  burstLength: Int = 1,
                  burstWrap: Boolean = false,
                  latency: Int = 4,
                  tPU: Double = 150_000,
                  tVP: Double = 5,
                  tWP: Double = 45) extends BusConfig {
  /** The clock period (ns). */
  val clockPeriod = 1 / clockFreq * 1_000_000_000D
  /** The number of clock cycles to during initialization. */
  val initWait = (tPU / clockPeriod).ceil.toLong
  /** The number of clock cycles to during address valid pulse. */
  val vpWait = (tVP / clockPeriod).ceil.toLong
  /** The number of clock cycles to during write pulse. */
  val wpWait = (tWP / clockPeriod).ceil.toLong
  /** The maximum value of the wait counter. */
  val waitCounterMax = 1 << log2Ceil(Seq(initWait, vpWait, wpWait).max)

  /** The opcode to write to the bus configuration register (BCR). */
  def opcode: UInt =
    0.U(2.W) ## // reserved
      RegisterSelect.BCR.U(2.W) ##
      0.U(2.W) ## // reserved
      OperatingMode.synchronous.U(1.W) ##
      LatencyMode.variable.U(1.W) ##
      (latency - 1).U(3.W) ##
      WaitPolarity.activeLow.U(1.W) ##
      0.U(1.W) ## // reserved
      WaitConfig.beforeDelay.U(1.W) ##
      0.U(2.W) ## // reserved
      DriveStrength.half.U(2.W) ##
      !burstWrap.B ##
      (log2Ceil(burstLength / 4) + 1).U(3.W)
}
