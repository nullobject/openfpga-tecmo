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

import arcadia._
import arcadia.gfx._
import arcadia.mem.SinglePortRom
import arcadida.pocket.OptionsIO
import chisel3._
import chisel3.util.MuxCase
import tecmo._

/**
 * The graphics processing unit (GPU) handles rendering the sprite and tilemap layers.
 *
 * For each pixel on the screen, the palette entry outputs from all layers are mixed together to
 * yield a final palette entry. This value is then used to look up the actual pixel color in the
 * palette RAM.
 */
class GPU extends Module {
  val io = IO(new Bundle {
    /** Program counter (debug) */
    val pc = Input(UInt(16.W))
    /** Options port */
    val options = OptionsIO()
    /** Video port */
    val video = Flipped(VideoIO())
    /** Layer control ports */
    val layerCtrl = Vec(Config.LAYER_COUNT, LayerCtrlIO())
    /** Sprite control port */
    val spriteCtrl = SpriteCtrlIO()
    /** Palette RAM port */
    val paletteRam = new PaletteRamIO
    /** RGB port */
    val rgb = Output(UInt(Config.RGB_WIDTH.W))
  })

  // Frame buffer read position
  val frameBufferPos = {
    val x = Mux(io.options.flip, ~io.video.pos.x, io.video.pos.x)
    val y = Mux(io.options.flip, ~io.video.pos.y, io.video.pos.y)
    UVec2(x, y)
  }

  // The sprite layer renders to a page in the frame buffer. At the same time, the GPU reads pixels
  // from the alternate page, and renders them to the video output.
  //
  // The frame buffer pages are flipped at the end of each frame (i.e. at the rising edge of the
  // vertical blank signal).
  val frameBuffer = Module(new FrameBuffer(Config.FRAME_BUFFER_ADDR_WIDTH, Config.FRAME_BUFFER_DATA_WIDTH))
  frameBuffer.io.portB.rd := io.video.displayEnable
  frameBuffer.io.portB.addr := frameBufferPos.y(7, 0) ## (frameBufferPos.x(7, 0) + 3.U)
  frameBuffer.io.swap := Util.toggle(Util.rising(io.video.vBlank))

  // Sprite processor
  val spriteProcessor = Module(new SpriteProcessor)
  spriteProcessor.io.ctrl <> io.spriteCtrl
  spriteProcessor.io.frameBuffer <> frameBuffer.io.portA
  spriteProcessor.io.video <> io.video

  // Character processor
  val charProcessor = Module(new LayerProcessor(LayerProcessorConfig(tileSize = 8, cols = 32, rows = 32, offset = 1)))
  charProcessor.io.ctrl <> io.layerCtrl(0)
  charProcessor.io.video <> io.video

  // Foreground processor
  val fgProcessor = Module(new LayerProcessor(LayerProcessorConfig(tileSize = 16, cols = 32, rows = 16, offset = 54)))
  fgProcessor.io.ctrl <> io.layerCtrl(1)
  fgProcessor.io.video <> io.video

  // Background processor
  val bgProcessor = Module(new LayerProcessor(LayerProcessorConfig(tileSize = 16, cols = 32, rows = 16, offset = 54)))
  bgProcessor.io.ctrl <> io.layerCtrl(2)
  bgProcessor.io.video <> io.video

  // The debug ROM contains alphanumeric character tiles
  val debugRom = Module(new SinglePortRom(
    addrWidth = Config.DEBUG_ROM_ADDR_WIDTH,
    dataWidth = Config.DEBUG_ROM_DATA_WIDTH,
    depth = 512,
    initFile = "roms/alpha.mif"
  ))

  // Debug layer
  val debugLayer = Module(new DebugLayer("PC:$%04X"))
  debugLayer.io.args := Seq(io.pc)
  debugLayer.io.enable := io.options.debug
  debugLayer.io.pos := UVec2(0.U, 232.U)
  debugLayer.io.color := 1.U
  debugLayer.io.tileRom <> debugRom.io
  debugLayer.io.video <> io.video

  // Color mixer
  val colorMixer = Module(new ColorMixer)
  colorMixer.io.paletteRam <> io.paletteRam
  colorMixer.io.spritePen := frameBuffer.io.portB.dout.asTypeOf(new PaletteEntry)
  colorMixer.io.charPen := charProcessor.io.pen
  colorMixer.io.fgPen := fgProcessor.io.pen
  colorMixer.io.bgPen := bgProcessor.io.pen
  colorMixer.io.debugPen := debugLayer.io.data.asTypeOf(new PaletteEntry)

  // Dotted border
  val dot = ((io.video.pos.x === 0.U || io.video.pos.x === 255.U) && io.video.pos.y(2) === 0.U) ||
    ((io.video.pos.y === 16.U || io.video.pos.y === 239.U) && io.video.pos.x(2) === 0.U)

  // Final pixel color
  io.rgb := MuxCase(0.U, Seq(
    (io.video.displayEnable && io.options.debug && dot) -> 0xffffff.U,
    io.video.displayEnable -> GPU.decodeRGB(colorMixer.io.dout)
  ))
}

object GPU {
  /** The width of a tile (pixels) */
  val TILE_WIDTH = 8
  /** The height of a tile (pixels) */
  val TILE_HEIGHT = 8
  /** The number of bit planes per tile */
  val TILE_BIT_PLANES = 4

  /**
   * Decodes a 24 bit RGB value from the given pixel data.
   *
   * @param data The pixel data.
   * @return A 24 bit RGB value.
   */
  private def decodeRGB(data: Bits): UInt = {
    val r = data(15, 12) ## data(15, 12)
    val g = data(11, 8) ## data(11, 8)
    val b = data(3, 0) ## data(3, 0)
    r ## g ## b
  }

  /**
   * Decodes a tile row into a sequence of pixel values.
   *
   * @param row The tile row data.
   */
  def decodeTileRow(row: Bits): Vec[Bits] =
    VecInit(Util.decode(row, TILE_WIDTH, row.getWidth / TILE_WIDTH).reverse)
}
