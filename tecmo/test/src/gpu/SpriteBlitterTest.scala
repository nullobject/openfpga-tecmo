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

trait SpriteBlitterTestHelpers {
  def waitForIdle(dut: SpriteBlitter) =
    while(!dut.io.debug.idle.peekBoolean()) { dut.clock.step() }

  def waitForFetch(dut: SpriteBlitter) =
    while(!dut.io.debug.fetch.peekBoolean()) { dut.clock.step() }

  def waitForBlit(dut: SpriteBlitter) =
    while(!dut.io.debug.blit.peekBoolean()) { dut.clock.step() }
}

class SpriteBlitterTest extends AnyFlatSpec with ChiselScalatestTester with Matchers with SpriteBlitterTestHelpers {
  behavior of "FSM"

  it should "assert the ready signal during the idle state" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      waitForIdle(dut)
      dut.io.config.ready.expect(true)
      dut.clock.step()
      dut.io.config.ready.expect(false)
    }
  }

  it should "fetch sprite data during the idle state" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      waitForFetch(dut)
    }
  }

  it should "fetch pixel data during the load state" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.pixelData.valid.poke(true)
      dut.io.pixelData.ready.expect(false)
      waitForFetch(dut)
      dut.io.pixelData.ready.expect(true)
      waitForBlit(dut)
      dut.io.pixelData.ready.expect(false)
    }
  }

  it should "return to the idle state after blitting a sprite" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)
      dut.clock.step(64) // 8x8 pixels
      dut.io.debug.idle.expect(true)
    }
  }

  behavior of "blitting"

  it should "copy pixel data to the frame buffer" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.bits.pos.x.poke(8)
      dut.io.config.bits.pos.y.poke(16)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.bits.poke("h01234567".U)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)

      // Pixel 0 (transparent)
      dut.io.frameBuffer.wr.expect(false)
      dut.io.frameBuffer.addr.expect(0x1008)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x1009)
      dut.io.frameBuffer.din.expect(1)
      dut.clock.step(5)

      // Pixel 6
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x100e)
      dut.io.frameBuffer.din.expect(6)
      dut.clock.step()

      // Pixel 7
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x100f)
      dut.io.frameBuffer.din.expect(7)
    }
  }

  it should "allow horizontal flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.bits.xFlip.poke(true)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.bits.poke("h01234567".U)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)

      // Pixel 0 (transparent)
      dut.io.frameBuffer.wr.expect(false)
      dut.io.frameBuffer.addr.expect(0x0007)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0006)
      dut.io.frameBuffer.din.expect(1)
      dut.clock.step(5)

      // Pixel 6
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0001)
      dut.io.frameBuffer.din.expect(6)
      dut.clock.step()

      // Pixel 7
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0000)
      dut.io.frameBuffer.din.expect(7)
    }
  }

  it should "allow vertical flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.bits.yFlip.poke(true)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.bits.poke("h01234567".U)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)

      // Pixel 0 (transparent)
      dut.io.frameBuffer.wr.expect(false)
      dut.io.frameBuffer.addr.expect(0x0700)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0701)
      dut.io.frameBuffer.din.expect(1)
      dut.clock.step(5)

      // Pixel 6
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0706)
      dut.io.frameBuffer.din.expect(6)
      dut.clock.step()

      // Pixel 7
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0707)
      dut.io.frameBuffer.din.expect(7)
    }
  }

  behavior of "pixel data"

  it should "fetch pixel data every 8 pixels" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)

      dut.io.pixelData.ready.expect(false)
      dut.clock.step(7)
      dut.io.pixelData.ready.expect(true)
      dut.clock.step()
      dut.io.pixelData.ready.expect(false)
    }
  }

  it should "not fetch pixel data at the last pixel" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.bits.enable.poke(true)
      dut.io.config.bits.size.poke(8)
      dut.io.config.valid.poke(true)
      dut.io.pixelData.valid.poke(true)
      waitForBlit(dut)

      dut.io.pixelData.ready.expect(false)
      dut.clock.step(63)
      dut.io.pixelData.ready.expect(false)
    }
  }
}
