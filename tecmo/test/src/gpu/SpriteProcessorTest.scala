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

trait SpriteLayerTestHelpers {
  def waitForLoad(dut: SpriteProcessor) =
    while(!dut.io.debug.load.peekBoolean()) { dut.clock.step() }

  def waitForCheck(dut: SpriteProcessor) =
    while(!dut.io.debug.check.peekBoolean()) { dut.clock.step() }

  def waitForReady(dut: SpriteProcessor) =
    while(!dut.io.debug.ready.peekBoolean()) { dut.clock.step() }

  def waitForBlit(dut: SpriteProcessor) =
    while(!dut.io.debug.ready.peekBoolean()) { dut.clock.step() }

  def waitForNext(dut: SpriteProcessor) =
    while(!dut.io.debug.next.peekBoolean()) { dut.clock.step() }

  def waitForDone(dut: SpriteProcessor) =
    while(!dut.io.debug.done.peekBoolean()) { dut.clock.step() }
}

class SpriteProcessorTest extends AnyFlatSpec with ChiselScalatestTester with Matchers with SpriteLayerTestHelpers {
  behavior of "FSM"

  it should "move to the load state when the VBLANK signal is deasserted" in {
    test(new SpriteProcessor) { dut =>
      // Assert VBLANK
      dut.io.video.vBlank.poke(true)
      dut.clock.step()
      dut.io.debug.idle.expect(true)

      // Deassert VBLANK
      dut.io.video.vBlank.poke(false)
      dut.clock.step()
      dut.io.debug.load.expect(true)
    }
  }

  it should "move to the check state after loading a sprite" in {
    test(new SpriteProcessor) { dut =>
      waitForLoad(dut)
      dut.clock.step()
      dut.io.debug.check.expect(true)
    }
  }

  it should "move to the next state after checking an invisible sprite" in {
    test(new SpriteProcessor) { dut =>
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.next.expect(true)
    }
  }

  it should "move to the ready state after checking a visible sprite" in {
    test(new SpriteProcessor) { dut =>
      dut.io.ctrl.vram.dout.poke(4)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.ready.expect(true)
    }
  }

  it should "assert the tile ROM read enable signal during the check state" in {
    test(new SpriteProcessor) { dut =>
      dut.io.ctrl.vram.dout.poke(4)
      waitForLoad(dut)
      dut.io.ctrl.tileRom.rd.expect(false)
      dut.clock.step()
      dut.io.ctrl.tileRom.rd.expect(true)
    }
  }

  it should "return to the idle state when the VBLANK signal is asserted" in {
    test(new SpriteProcessor(numSprites = 1)) { dut =>
      dut.io.ctrl.vram.dout.poke(4)
      dut.io.ctrl.tileRom.waitReq.poke(true)
      dut.io.ctrl.tileRom.valid.poke(true)
      waitForDone(dut)

      // Deassert VBLANK
      dut.io.video.vBlank.poke(false)
      dut.clock.step()
      dut.io.debug.done.expect(true)

      // Assert VBLANK
      dut.io.video.vBlank.poke(true)
      dut.clock.step()
      dut.io.debug.idle.expect(true)
    }
  }

  behavior of "blitting"

  it should "copy each sprite to the frame buffer" in {
    test(new SpriteProcessor(numSprites = 2)) { dut =>
      dut.io.ctrl.tileRom.waitReq.poke(true)
      dut.io.ctrl.tileRom.valid.poke(true)

      // Sprite 0
      waitForLoad(dut)
      dut.io.ctrl.vram.rd.expect(true)
      dut.io.ctrl.vram.addr.expect(0)
      dut.io.ctrl.vram.dout.poke(4)
      waitForBlit(dut)

      // Sprite 1
      waitForLoad(dut)
      dut.io.ctrl.vram.rd.expect(true)
      dut.io.ctrl.vram.addr.expect(1)
      dut.io.ctrl.vram.dout.poke(4)
      waitForDone(dut)
    }
  }

  behavior of "pixel data"

  it should "fetch pixel data for a 8x8 sprite" in {
    test(new SpriteProcessor(numSprites = 1)) { dut =>
      dut.io.ctrl.vram.rd.expect(true)
      dut.io.ctrl.vram.addr.expect(0)
      dut.io.ctrl.vram.dout.poke("h0000000000002314".U)
      dut.io.ctrl.tileRom.waitReq.poke(true)
      waitForCheck(dut)

      // Line 0
      dut.io.ctrl.tileRom.rd.expect(true)
      dut.io.ctrl.tileRom.addr.expect(0x2460)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2460)
      dut.io.ctrl.tileRom.waitReq.poke(false)
      dut.io.ctrl.tileRom.valid.poke(true)
      dut.clock.step()

      // Line 1
      dut.io.ctrl.tileRom.addr.expect(0x2464)
      dut.clock.step()

      // Skip 5 lines
      dut.clock.step(5)

      // Line 7
      dut.io.ctrl.tileRom.addr.expect(0x247c)
      dut.clock.step()

      dut.io.ctrl.tileRom.rd.expect(false)
    }
  }

  it should "fetch pixel data for a 16x16 sprite" in {
    test(new SpriteProcessor(numSprites = 1)) { dut =>
      dut.io.ctrl.vram.rd.expect(true)
      dut.io.ctrl.vram.addr.expect(0)
      dut.io.ctrl.vram.dout.poke("h0000000000012314".U)
      dut.io.ctrl.tileRom.waitReq.poke(true)
      waitForCheck(dut)

      // Line 0
      dut.io.ctrl.tileRom.rd.expect(true)
      dut.io.ctrl.tileRom.addr.expect(0x2400)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2400)
      dut.io.ctrl.tileRom.waitReq.poke(false)
      dut.io.ctrl.tileRom.valid.poke(true)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2420)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(12)

      // Line 7
      dut.io.ctrl.tileRom.addr.expect(0x241c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x243c)
      dut.clock.step()

      // Line 8
      dut.io.ctrl.tileRom.addr.expect(0x2440)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2460)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(12)

      // Line 15
      dut.io.ctrl.tileRom.addr.expect(0x245c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x247c)
      dut.clock.step()

      dut.io.ctrl.tileRom.rd.expect(false)
    }
  }

  it should "fetch pixel data for a 32x32 sprite" in {
    test(new SpriteProcessor(numSprites = 1)) { dut =>
      dut.io.ctrl.vram.rd.expect(true)
      dut.io.ctrl.vram.addr.expect(0)
      dut.io.ctrl.vram.dout.poke("h0000000000022314".U)
      dut.io.ctrl.tileRom.waitReq.poke(true)
      waitForCheck(dut)

      // Line 0
      dut.io.ctrl.tileRom.rd.expect(true)
      dut.io.ctrl.tileRom.addr.expect(0x2400)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2400)
      dut.io.ctrl.tileRom.waitReq.poke(false)
      dut.io.ctrl.tileRom.valid.poke(true)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2420)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2480)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24a0)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(24)

      // Line 7
      dut.io.ctrl.tileRom.addr.expect(0x241c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x243c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x249c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24bc)
      dut.clock.step()

      // Line 8
      dut.io.ctrl.tileRom.addr.expect(0x2440)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2460)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24c0)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24e0)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(24)

      // Line 15
      dut.io.ctrl.tileRom.addr.expect(0x245c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x247c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24dc)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x24fc)
      dut.clock.step()

      // Line 16
      dut.io.ctrl.tileRom.addr.expect(0x2500)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2520)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2580)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25a0)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(24)

      // Line 23
      dut.io.ctrl.tileRom.addr.expect(0x251c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x253c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x259c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25bc)
      dut.clock.step()

      // Line 24
      dut.io.ctrl.tileRom.addr.expect(0x2540)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x2560)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25c0)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25e0)
      dut.clock.step()

      // Skip 6 lines
      dut.clock.step(24)

      // Line 31
      dut.io.ctrl.tileRom.addr.expect(0x255c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x257c)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25dc)
      dut.clock.step()
      dut.io.ctrl.tileRom.addr.expect(0x25fc)
      dut.clock.step()

      dut.io.ctrl.tileRom.rd.expect(false)
    }
  }
}
