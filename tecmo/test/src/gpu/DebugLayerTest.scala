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

class DebugLayerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "video enabled"

  it should "request the correct tile from the tile ROM" in {
    test(new DebugLayer("foo")) { dut =>
      // tile 0
      dut.io.video.pos.x.poke(0)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x130)

      // tile 1
      dut.io.video.pos.x.poke(8)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x178)

      // tile 2
      dut.io.video.pos.x.poke(16)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x178)
    }
  }

  it should "request the correct row from the tile ROM" in {
    test(new DebugLayer("foo")) { dut =>
      // row 0
      dut.io.video.pos.y.poke(0)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x130)

      // row 1
      dut.io.video.pos.y.poke(1)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x131)

      // row 7
      dut.io.video.pos.y.poke(7)
      dut.clock.step()
      dut.io.tileRom.addr.expect(0x137)
    }
  }

  it should "set the RGB value" in {
    test(new DebugLayer("foo")) { dut =>
      dut.io.color.poke(15)
      dut.io.tileRom.dout.poke("hffffffff".U)
      dut.io.video.pos.x.poke(0)
      dut.clock.step()
      dut.io.data.expect(255)
    }
  }
}
