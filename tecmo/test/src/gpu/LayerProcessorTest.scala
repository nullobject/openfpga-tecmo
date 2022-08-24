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

import chiseltest._
import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers

trait LayerProcessorTestHelpers {
  def mkLayerProcessor =
    new LayerProcessor(LayerProcessorConfig(
      tileSize = 8,
      cols = 32,
      rows = 32,
      offset = 0
    ))
}

class LayerProcessorTest extends AnyFlatSpec with ChiselScalatestTester with Matchers with LayerProcessorTestHelpers {
  behavior of "RAM"

  it should "assert the read signal" in {
    test(mkLayerProcessor) { dut =>
      dut.io.ctrl.vram.rd.expect(true)
    }
  }

  it should "set the RAM address" in {
    test(mkLayerProcessor) { dut =>
      // Col 0
      dut.io.video.pos.x.poke(0)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x001)

      // Col 1
      dut.io.video.pos.x.poke(8)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x002)

      // Col 30
      dut.io.video.pos.x.poke(240)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x01f)

      // Col 31
      dut.io.video.pos.x.poke(248)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x000)

      // Row 0
      dut.io.video.pos.x.poke(0)
      dut.io.video.pos.y.poke(0)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x001)

      // Row 1
      dut.io.video.pos.y.poke(8)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x021)

      // Row 30
      dut.io.video.pos.y.poke(240)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x3c1)

      // Row 31
      dut.io.video.pos.y.poke(248)
      dut.clock.step()
      dut.io.ctrl.vram.addr.expect(0x3e1)
    }
  }

  behavior of "ROM"

  it should "assert the read signal" in {
    test(mkLayerProcessor) { dut =>
      dut.io.video.clockEnable.poke(true)
      dut.io.video.pos.x.poke(2)
      dut.io.ctrl.tileRom.rd.expect(true)
    }
  }

  it should "set the ROM address" in {
    test(mkLayerProcessor) { dut =>
      dut.io.video.clockEnable.poke(true)
      dut.io.video.pos.x.poke(1)

      // Offset 0
      dut.io.video.pos.y.poke(0)
      dut.io.ctrl.vram.dout.poke(0x1234)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x4680)

      // Offset 1
      dut.io.video.pos.y.poke(1)
      dut.io.ctrl.vram.dout.poke(0x1234)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x4684)

      // Offset 6
      dut.io.video.pos.y.poke(6)
      dut.io.ctrl.vram.dout.poke(0x1234)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x4698)

      // Offset 7
      dut.io.video.pos.y.poke(7)
      dut.io.ctrl.vram.dout.poke(0x1234)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x469c)
    }
  }
}
