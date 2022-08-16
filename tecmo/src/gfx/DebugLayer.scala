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
import arcadia.gfx.VideoIO
import arcadia.util._
import chisel3._
import chisel3.util._
import tecmo._

/**
 * Renders text on the screen.
 *
 * @param format The format string.
 */
class DebugLayer(format: String) extends Module {
  val numArgs = Format.ARG_REGEX.findAllIn(format).length
  val numRows = format.linesIterator.length
  val numCols = format.linesIterator.map(_.length).max // FIXME: should compensate for arguments

  val io = IO(new Bundle {
    /** Arguments */
    val args = Input(Vec(numArgs, UInt(16.W)))
    /** Position signals */
    val pos = Input(UVec2(9.W))
    /** Color */
    val color = Input(UInt(4.W))
    /** Tile ROM signals */
    val tileRom = new TileRomIO
    /** Video signals */
    val video = Input(new VideoIO)
    /** Pixel data output */
    val data = Output(UInt(8.W))
  })

  // Tile and pixel offsets
  val pos = io.video.pos - io.pos
  val col = (pos.x + 1.U)(log2Ceil(DebugLayer.TILE_WIDTH) + log2Ceil(numCols), log2Ceil(DebugLayer.TILE_WIDTH))
  val row = pos.y(log2Ceil(DebugLayer.TILE_HEIGHT) + log2Ceil(numRows), log2Ceil(DebugLayer.TILE_HEIGHT))
  val xOffset = pos.x(log2Ceil(DebugLayer.TILE_WIDTH) - 1, 0)
  val yOffset = pos.y(log2Ceil(DebugLayer.TILE_HEIGHT) - 1, 0)

  // Registers
  val lines = DebugLayer.decodeLines(format, numCols, io.args)
  val textReg = RegNext(lines(row))

  // Decode pixel data
  val pixel = GPU.decodeTileRow(io.tileRom.dout)(xOffset)

  // Set enable signal
  val enable =
    Util.between(io.video.pos.x, io.pos.x, io.pos.x + (DebugLayer.TILE_WIDTH * numCols - 1).U) &&
      Util.between(io.video.pos.y, io.pos.y, io.pos.y + (DebugLayer.TILE_HEIGHT * numRows - 1).U) &&
      pixel.asUInt =/= 0.U

  // Outputs
  io.tileRom.rd := true.B // read-only
  io.tileRom.addr := textReg(col) ## yOffset
  io.data := Mux(enable, io.color ## pixel, 0.U)
}

object DebugLayer {
  /** The width of a tile (pixels) */
  val TILE_WIDTH = 8
  /** The height of a tile (pixels) */
  val TILE_HEIGHT = 8
  /** The number of bits per hexadecimal digit */
  val BITS_PER_DIGIT = 4
  /** The offset of the ASCII code for the first tile character */
  val ASCII_OFFSET = 32
  /** The offset of the tile code for the numbers */
  val NUM_OFFSET = 16
  /** The offset of the tile code for the letters */
  val ALPHA_OFFSET = 33

  /**
   * Decodes a format string into a vector of lines.
   *
   * @param s The format string
   * @param numCols The number of columns.
   * @param args The arguments.
   * @return A vector of lines.
   */
  def decodeLines(s: String, numCols: Int, args: Vec[UInt]): Vec[Vec[UInt]] = {
    val tokens = Format.tokenize(s).toList
    val lines = Format.partitionWhen(tokens)(_ == NewlineToken())
    VecInit(lines.map(l => decodeLine(l, args)))
  }

  /**
   * Decodes a sequence of tokens into a vector of tile indexes.
   *
   * @param tokens The list of tokens.
   * @param args The arguments.
   * @return A vector of tile indexes.
   */
  def decodeLine(tokens: Seq[Token], args: Vec[UInt]): Vec[UInt] = {
    val indexes = tokens.flatMap {
      case t: StringToken => decodeString(t)
      case t: ArgumentToken => decodeArgument(t, args(t.index))
    }
    VecInit(indexes)
  }

  private def decodeString(token: StringToken): Seq[UInt] =
    token.text.toUpperCase.map(n => (n - ASCII_OFFSET).asUInt)

  private def decodeArgument(token: ArgumentToken, value: UInt): Seq[UInt] = {
    val width = token.width.getOrElse(value.getWidth / BITS_PER_DIGIT)
    val n = math.min(width, value.getWidth / BITS_PER_DIGIT)
    val digits = Util.decode(value, n, BITS_PER_DIGIT).map(n => hexChar(n.asUInt)).reverse
    val padding = n.until(width).map(_ => if (token.zero) 16.U else 0.U)
    padding ++ digits
  }

  private def hexChar(n: UInt): UInt = MuxCase(n, Seq(
    (n >= 0.U && n <= 9.U, n +& NUM_OFFSET.U),
    (n >= 10.U && n <= 15.U, n +& (ALPHA_OFFSET - 10).U)
  ))
}
