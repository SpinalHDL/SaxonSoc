package saxon.board.blackice

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.blackice.peripheral.{Apb3SevenSegmentGenerator, Apb3PwmGenerator, Apb3QspiAnalogGenerator}
import saxon.board.blackice.sram._

class BlackiceSocArduinoSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val sramA = BmbSramGenerator(0x90000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val gpioB = Apb3GpioGenerator(0x50000)
  val sevenSegmentA = Apb3SevenSegmentGenerator(0x20000)
  val pwm = Apb3PwmGenerator(0x30000)
  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  val qspiAnalog = Apb3QspiAnalogGenerator(0x40000)

  cpu.setTimerInterrupt(machineTimer.interrupt)
  cpu.externalInterrupt.produce(cpu.externalInterrupt := False)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb, sramA.bmb),
    cpu.dBus -> List(ramA.bmb, sramA.bmb)
  )
}

class BlackiceSocArduino extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new BlackiceSocArduinoSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_100 = in Bool()
    val GRESET = in Bool()

    val pll = BlackicePll()
    pll.clock_in := CLOCK_100

    clockCtrl.clock.load(pll.clock_out)
    clockCtrl.reset.load(GRESET)
  }
}

object BlackiceSocArduinoSystem{
  def default(g : BlackiceSocArduinoSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimalWithCsr)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(12 KiB)
    ramA.hexInit.load("software/standalone/bootHex/build/bootHex.hex")

    sramA.layout load SramLayout(dataWidth=16, addressWidth=18)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)
    gpioB.parameter load Gpio.Parameter(width = 2, interrupt = List(0,1))
    pwm.width load(2)

    g
  }
}

object BlackiceSocArduino {
  //Function used to configure the SoC
  def default(g : BlackiceSocArduino) = g{
    import g._
    BlackiceSocArduinoSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.FALL)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceSocArduino()).toComponent()))
    BspGenerator(report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

