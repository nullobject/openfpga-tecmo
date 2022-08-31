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

import arcadia.cpu.z80._
import arcadia.mem._
import arcadia.snd._
import arcadida.pocket.OptionsIO
import chisel3._
import chisel3.util._
import tecmo._

/** Represents the sound PCB. */
class Sound extends Module {
  val io = IO(new Bundle {
    /** Options port */
    val options = Flipped(OptionsIO())
    /** Control port */
    val ctrl = SoundCtrlIO()
    /** ROM port */
    val rom = RomIO()
    /** Audio port */
    val audio = Output(SInt(Config.AUDIO_SAMPLE_WIDTH.W))
  })

  // Wires
  val irq = Wire(Bool())

  // Registers
  val ctrlReqReg = ShiftRegister(io.ctrl.req, 2)
  val ctrlDataReg = ShiftRegister(io.ctrl.data, 2)
  val reqReg = RegEnable(true.B, false.B, ctrlReqReg)
  val dataReg = RegEnable(ctrlDataReg, ctrlReqReg)
  val pcmGainReg = RegInit(0.U(8.W))

  // Sound CPU
  val cpu = Module(new CPU(Config.SOUND_CLOCK_DIV))
  val memMap = new MemMap(cpu.io)
  cpu.io.halt := false.B
  cpu.io.din := DontCare
  cpu.io.int := irq
  cpu.io.nmi := reqReg

  // Set interface defaults
  io.rom.soundRom.default()

  // Sound RAM
  val soundRam = Module(new SinglePortRam(
    addrWidth = Config.SOUND_RAM_ADDR_WIDTH,
    dataWidth = Config.SOUND_RAM_DATA_WIDTH
  ))
  soundRam.io.default()

  // FM
  val opl = Module(new JTOPL(Config.CPU_CLOCK_FREQ, Sound.FM_SAMPLE_CLOCK_FREQ))
  irq := opl.io.irq
  opl.io.cpu.default()

  // PCM
  val pcm = Module(new JT5205(Config.CPU_CLOCK_FREQ, Sound.PCM_SAMPLE_CLOCK_FREQ))

  // PCM counter
  val pcmCounter = Module(new PCMCounter)
  pcmCounter.io.cen := pcm.io.vclk
  pcmCounter.io.wr := false.B
  pcmCounter.io.high := DontCare
  pcm.io.din := pcmCounter.io.dout
  pcmCounter.io.din := cpu.io.dout
  pcmCounter.io.rom <> io.rom.pcmRom

  /**
   * Sets the PCM address register.
   *
   * @param high The high address flag.
   */
  def setAddr(high: Boolean): Unit = {
    pcmCounter.io.wr := true.B
    pcmCounter.io.high := high.B
  }

  /**
   * Sets the PCM gain register.
   *
   * @param value The gain value.
   */
  def setGain(value: Bits): Unit = {
    pcmGainReg := value.asUInt
  }

  when(io.options.gameIndex === Game.RYGAR.U) {
    memMap(0x0000 to 0x3fff).readMem(io.rom.soundRom)
    memMap(0x4000 to 0x47ff).readWriteMem(soundRam.io)
    memMap(0x8000 to 0x8001).readWriteMem(opl.io.cpu)
    memMap(0xc000).r { (_, _) => dataReg }
    memMap(0xc000).w { (_, _, _) => setAddr(false) }
    memMap(0xd000).w { (_, _, _) => setAddr(true) }
    memMap(0xe000).w { (_, _, data) => setGain(data) }
    memMap(0xf000).w { (_, _, _) => reqReg := false.B }
  }

  // Audio mixer
  io.audio := AudioMixer.sum(Config.AUDIO_SAMPLE_WIDTH,
    RegEnable(opl.io.audio.bits, opl.io.audio.valid) -> 1,
    RegEnable(pcm.io.audio.bits, pcm.io.audio.valid) -> 0.5
  )
}

object Sound {
  /** The FM sample clock frequency (Hz) */
  val FM_SAMPLE_CLOCK_FREQ = 4_000_000
  /** The PCM sample clock frequency (Hz) */
  val PCM_SAMPLE_CLOCK_FREQ = 400_000
}
