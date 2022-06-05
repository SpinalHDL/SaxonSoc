package saxon.board.blackicemx

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import saxon.board.blackice._
import saxon.board.blackice.peripheral._
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.misc.plic.PlicMapping

class BlackiceMxArduinoSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val gpioB = Apb3GpioGenerator(0x01000)
  val sdramA = SdramSdrBmbGenerator(0x90000000l)
  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  val plic = Apb3PlicGenerator(0xC00000)
  val sevenSegmentA = Apb3SevenSegmentGenerator(0x20000)
  val pwm = Apb3PwmGenerator(0x30000)
  val quadrature = Apb3QuadratureGenerator(0x50000)
 
  // Create 8 IO Mux pins
  val pinA, pinB, pinC, pinD, pinE, pinF, pinG, pinH = IoGenerator()
  val pinI, pinJ, pinK, pinL, pinM, pinN, pinO, pinP = IoGenerator()
  val pinMux = Apb3IoMuxGenerator(0x40000)

  // Create a seven segment display using the mux pins
  val sevenSegmentAEnable = pinMux.createEnable(id=0)

  pinMux.addOutput(sevenSegmentA.segPins, 0, pinE, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 1, pinF, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 2, pinG, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 3, pinA, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 4, pinB, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 5, pinC, sevenSegmentAEnable)
  pinMux.addOutput(sevenSegmentA.segPins, 6, pinD, sevenSegmentAEnable)

  pinMux.addOutput(sevenSegmentA.digitPin, pinH, sevenSegmentAEnable)

  val gpioBEnable = pinMux.createEnable(id=1)
  pinMux.addConnection(gpioB.gpio, 0, pinA, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 1, pinB, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 2, pinC, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 3, pinD, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 4, pinE, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 5, pinF, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 6, pinG, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 7, pinH, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 8, pinI, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 9, pinJ, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 10, pinK, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 11, pinL, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 12, pinM, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 13, pinN, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 14, pinO, gpioBEnable)
  pinMux.addConnection(gpioB.gpio, 15, pinP, gpioBEnable)

  // PWM pins
  val pwmEnable = pinMux.createEnable(id = 2)
  pinMux.addOutput(pwm.pins, 0, pinA, pwmEnable)

  // Quadrature pins
  val quadratureEnable = pinMux.createEnable(id = 3)
  pinMux.addInput(quadrature.quadA, pinA, quadratureEnable)
  pinMux.addInput(quadrature.quadB, pinB, quadratureEnable)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.ctrl, sdramA.bmb),
    cpu.dBus -> List(ramA.ctrl, sdramA.bmb, peripheralBridge.input)
  )

  interconnect.setConnector(sdramA.bmb){(m,s) =>
    m.cmd >-> s.cmd
    m.rsp << s.rsp
  }
}


class BlackiceMxArduino extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(16 MHz)

  val system = new BlackiceMxArduinoSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25M = in Bool()
    val sdramClk = out Bool()

    val pll = BlackiceMxPll()
    pll.clock_in := clk_25M

    sdramClk := pll.sdram_clock_out

    clockCtrl.clock.load(pll.clock_out)
  }
}

object BlackiceMxArduinoSystem{
  def default(g : BlackiceMxArduinoSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    // Configure cpu
    cpu.setTimerInterrupt(machineTimer.interrupt)
    cpu.config.load(VexRiscvConfigs.muraxLike)
    cpu.enableJtag(clockCtrl)

    // Configure ram
    ramA.dataWidth.load(32)
    ramA.size.load(12 KiB)
    ramA.hexInit.load("software/standalone/bootHex/build/bootHex.hex")
    //ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")

    sdramA.layout.load(IS42S16100H.layout)
    sdramA.timings.load(IS42S16100H.timingGrade7)

    // Configure Platform Interrupt Controller
    plic.priorityWidth.load(2)
    plic.mapping.load(PlicMapping.sifive)
    plic.addTarget(cpu.externalInterrupt)

    // Configure uart
    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    uartA.connectInterrupt(plic, 1)

    // Configure gpio
    gpioA.parameter load Gpio.Parameter(width = 8)
    gpioB.parameter load Gpio.Parameter(width = 16)

    // Configure PWM
    pwm.width.load(1)

    // Configure Quadrature
    quadrature.width.load(8)

    g
  }
}


object BlackiceMxArduino {
  //Function used to configure the SoC
  def default(g : BlackiceMxArduino) = g{
    import g._
    BlackiceMxArduinoSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceMxArduino()).toComponent()))
    BspGenerator("BlackiceMxArduino", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object BlackiceMxArduinoSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new BlackiceMxArduinoSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(16 MHz)
      BlackiceMxArduinoSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)

      val tcpJtag = JtagTcp(
        jtag = dut.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin =  dut.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = dut.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val sdram = SdramModel(
        io = dut.sdramA.sdram,
        layout = dut.sdramA.logic.layout,
        clockDomain = clockDomain
      )
    }
  }
}

