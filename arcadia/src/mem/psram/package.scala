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

package arcadia.mem

package object psram {
  /** Register select enum */
  object RegisterSelect {
    /** Refresh configuration register (RCR) */
    val RCR = 0
    /** Device ID register (DIDR) */
    val DIDR = 1
    /** Bus configuration register (BCR) */
    val BCR = 2
  }

  /** Operating mode enum */
  object OperatingMode {
    /** Synchronous burst access mode */
    val synchronous = 0
    /** Asynchronous access mode (default) */
    val asynchronous = 1
  }

  /** Initial access latency enum */
  object LatencyMode {
    /** Variable (default) */
    val variable = 0
    /** Fixed */
    val fixed = 1
  }

  /** Wait polarity enum */
  object WaitPolarity {
    /** Active low */
    val activeLow = 0
    /** Active high (default) */
    val activeHigh = 1
  }

  /** Wait configuration enum */
  object WaitConfig {
    /** Asserted during delay */
    val duringDelay = 0
    /** Asserted one clock cycle before delay (default) */
    val beforeDelay = 1
  }

  /** Drive strength enum */
  object DriveStrength {
    val full = 0
    val half = 1
    val quarter = 2
  }
}
