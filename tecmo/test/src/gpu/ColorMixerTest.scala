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

import chisel3._
import chiseltest._
import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers

class ColorMixerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "palette RAM"

  it should "assert the chip select signal" in {
    test(new ColorMixer) { dut =>
      dut.io.paletteRam.rd.expect(true)
    }
  }

  it should "set the palette RAM address" in {
    test(new ColorMixer) { dut =>
      dut.io.charData.poke("h00".U)
      dut.io.paletteRam.addr.expect("h100".U)

      dut.io.charData.poke("h01".U)
      dut.io.paletteRam.addr.expect("h101".U)

      dut.io.charData.poke("hff".U)
      dut.io.paletteRam.addr.expect("h1ff".U)
    }
  }

  it should "decode the palette RAM data" in {
    test(new ColorMixer) { dut =>
      dut.io.paletteRam.dout.poke("h1234".U)
      dut.clock.step()
      dut.io.rgb.r.expect(1)
      dut.io.rgb.g.expect(2)
      dut.io.rgb.b.expect(4)
    }
  }
}
