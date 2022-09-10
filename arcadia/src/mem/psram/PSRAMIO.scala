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

import chisel3._

/**
 * An interface for controlling a PSRAM device.
 *
 * @param config The PSRAM configuration.
 */
class PSRAMIO(config: Config) extends Bundle {
  /** Clock enable 0 */
  val ce0_n = Output(Bool())
  /** Clock enable 1 */
  val ce1_n = Output(Bool())
  /** Address valid (active-low) */
  val adv_n = Output(Bool())
  /** Control register enable */
  val cre = Output(Bool())
  /** Write enable (active-low) */
  val we_n = Output(Bool())
  /** Output enable (active-low) */
  val oe_n = Output(Bool())
  /** Upper byte enable (active-low) */
  val ub_n = Output(Bool())
  /** Lower byte enable (active-low) */
  val lb_n = Output(Bool())
  /** Wait */
  val wait_n = Input(Bool())
  /** Address bus */
  val addr = Output(UInt(6.W))
  /** Data input bus */
  val din = Output(Bits(16.W))
  /** Data output bus */
  val dout = Input(Bits(16.W))
}

object PSRAMIO {
  def apply(config: Config) = new PSRAMIO(config)
}
