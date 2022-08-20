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

import chiseltest._
import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers

trait PCMCounterTestHelpers {
  def stepCounter(dut: PCMCounter) = {
    dut.io.cen.poke(true)
    dut.clock.step()
    dut.io.cen.poke(false)
    dut.clock.step()
  }
}

class PCMCounterTest extends AnyFlatSpec with ChiselScalatestTester with Matchers with PCMCounterTestHelpers {
  it should "step the address counter" in {
    test(new PCMCounter) { dut =>
      // Set high address
      dut.io.rom.rd.expect(false)
      dut.io.high.poke(true)
      dut.io.wr.poke(true)
      dut.io.din.poke(0x13)
      dut.clock.step()

      // Set low address
      dut.io.rom.rd.expect(false)
      dut.io.high.poke(false)
      dut.io.din.poke(0x12)
      dut.clock.step()
      dut.io.wr.poke(false)

      // Load data
      dut.io.rom.dout.poke(0xab)
      dut.io.rom.rd.expect(true)
      dut.io.rom.addr.expect(0x1200)
      stepCounter(dut)
      dut.io.rom.rd.expect(true)
      dut.io.rom.addr.expect(0x1200)
      dut.io.dout.expect(0xb)
      stepCounter(dut)
      dut.io.rom.rd.expect(true)
      dut.io.rom.addr.expect(0x1201)
      dut.io.dout.expect(0xa)
      stepCounter(dut)

      // Continue to high address
      0.until(510).foreach { _ => stepCounter(dut) }
      dut.io.rom.rd.expect(true)
      dut.io.rom.addr.expect(0x1300)
      stepCounter(dut)

      // Done
      dut.io.rom.rd.expect(false)
    }
  }
}
