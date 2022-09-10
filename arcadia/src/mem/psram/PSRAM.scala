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

package arcadia.mem.psram

import arcadia.mem.BurstMemIO
import arcadia.mem.request.Request
import arcadia.util.Counter
import chisel3._
import chisel3.util._

/**
 * Handles reading/writing data to a PSRAM memory device.
 *
 * Read the AS1C8M16PL-70BIN PSRAM datasheet for more details.
 *
 * @param config The PSRAM configuration.
 */
class PSRAM(config: Config) extends Module {
  // Sanity check
  assert(Seq(4, 8, 16, 32).contains(config.burstLength), "PSRAM burst length must be 4, 8, 16, or 32")

  val io = IO(new Bundle {
    /** Memory port */
    val mem = Flipped(BurstMemIO(config))
    /** Device port */
    val psram = PSRAMIO(config)
    /** Debug port */
    val debug = Output(new Bundle {
      val init = Bool()
      val config = Bool()
      val idle = Bool()
      val active = Bool()
      val read = Bool()
      val write = Bool()
    })
  })

  // States
  object State {
    val init :: config :: idle :: active :: read :: write :: Nil = Enum(6)
  }

  // State register
  val nextState = Wire(UInt())
  val stateReg = RegNext(nextState, State.init)

  // Assert latch signal when a request should be latched
  val latch = stateReg =/= State.active && nextState === State.active

  // Request register
  val request = Request(io.mem.rd, io.mem.wr, io.mem.addr >> 1, 0.U, 0.U)
  val requestReg = RegEnable(request, latch)

  // Registers
  val ce0Reg = RegInit(false.B)
  val ce1Reg = RegInit(false.B)
  val creReg = RegInit(false.B)
  val advReg = RegInit(false.B)
  val oeReg = RegInit(false.B)
  val weReg = RegInit(false.B)
  val addrReg = Reg(UInt())
  val dinReg = RegNext(io.mem.din)
  val doutReg = RegNext(io.psram.dout)

  // Counters
  val (waitCounter, _) = Counter.static(config.waitCounterMax, reset = nextState =/= stateReg)

  // Control signals
  val initDone = waitCounter === (config.initWait - 1).U
  val activeDone = waitCounter =/= 0.U && io.psram.wait_n
  val readActiveDone = requestReg.rd && waitCounter =/= 0.U && io.psram.wait_n
  val writeActiveDone = requestReg.wr && waitCounter === (config.latency - 1).U
  val validReg = RegNext(stateReg === State.read)
  val burstBusy = waitCounter < (config.burstLength - 1).U
  val burstDone = (RegNext(stateReg === State.read && waitCounter === (config.burstLength - 1).U)) || (stateReg === State.write && waitCounter === (config.burstLength - 2).U)
  val readDone = stateReg === State.read && waitCounter === (config.burstLength - 1).U
  val writeDone = stateReg === State.write && waitCounter === (config.burstLength - 1).U
  val wait_n = {
    val idle = stateReg === State.idle && !request.wr
    val write = (stateReg === State.active && writeActiveDone) || (stateReg === State.write && burstBusy)
    idle || write
  }

  // Assert output enable signal after a read transaction has started
  when(stateReg === State.active && requestReg.rd) { oeReg := true.B }

  // Default to the previous state
  nextState := stateReg

  /** Writes opcode to address bus. */
  def opcode() = {
    nextState := State.config
    ce0Reg := true.B // FIXME
    creReg := true.B
    advReg := true.B
    oeReg := false.B
    weReg := false.B
    addrReg := config.opcode.head(6)
    dinReg := config.opcode.tail(6)
  }

  /** Writes bus configuration register (BCR). */
  def bcr() = {
    advReg := false.B
    weReg := true.B
  }

  /** Wait for a request. */
  def idle() = {
    nextState := State.idle
    ce0Reg := false.B // FIXME
    creReg := false.B
    advReg := false.B
    oeReg := false.B
    weReg := false.B
  }

  /** Starts a read/write transaction. */
  def active() = {
    nextState := State.active
    ce0Reg := true.B // FIXME
    advReg := true.B
    weReg := io.mem.wr
    addrReg := request.addr.head(6)
    dinReg := request.addr.tail(6)
  }

  /** Waits for read transaction. */
  def read() = {
    nextState := State.read
    advReg := false.B
//    oeReg := true.B
  }

  /** Waits for write transaction. */
  def write() = {
    nextState := State.write
    advReg := false.B
//    dinReg := requestReg.din
  }

  advReg := false.B

  // FSM
  switch(stateReg) {
    // Initialize device
    is(State.init) {
      when(initDone) { opcode() }
    }

    // Configure device
    is(State.config) {
      when(waitCounter === (config.vpWait - 1).U) {
        bcr()
      }.elsewhen(waitCounter === (config.vpWait + config.wpWait - 1).U) {
        idle()
      }
    }

    // Wait for request
    is(State.idle) {
      when(io.mem.rd || io.mem.wr) { active() }
    }

    // Start read/write request
    is(State.active) {
//      when(activeDone) {
//        when(requestReg.wr) { write() }.otherwise { read() }
//      }
      when(readActiveDone) { read() }.elsewhen(writeActiveDone) { write() }
    }

    // Wait for read
    is(State.read) {
      when(readDone) { idle() }
    }

    // Wait for write
    is(State.write) {
      when(writeDone) { idle() }
    }
  }

  // Outputs
  io.mem.dout := doutReg
  io.mem.wait_n := wait_n
  io.mem.valid := validReg
  io.mem.burstDone := burstDone
  io.psram.ce0_n := !ce0Reg
  io.psram.ce1_n := !ce1Reg
  io.psram.cre := creReg
  io.psram.adv_n := !advReg
  io.psram.oe_n := !oeReg
  io.psram.we_n := !weReg
  io.psram.ub_n := false.B
  io.psram.lb_n := false.B
  io.psram.addr := addrReg
  io.psram.din := dinReg
  io.debug.init := stateReg === State.init
  io.debug.config := stateReg === State.config
  io.debug.idle := stateReg === State.idle
  io.debug.active := stateReg === State.active
  io.debug.read := stateReg === State.read
  io.debug.write := stateReg === State.write

  // Debug
  if (sys.env.get("DEBUG").contains("1")) {
    printf(p"PSRAM(state: $stateReg, nextState: $nextState, addr: 0x${Hexadecimal(addrReg)}, waitCounter: $waitCounter)\n")
  }
}
