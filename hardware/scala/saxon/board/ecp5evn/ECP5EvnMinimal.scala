package saxon.board.ecp5evn

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}

class ECP5EvnMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}

class ECP5EvnMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(50 MHz)

  val system = new ECP5EvnMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_12mhz = in Bool()
    val resetN = in Bool()

    val pll = CorePll()
    pll.io.clkin := clk_12mhz

    clockCtrl.clock.load(pll.io.clkout0)
    clockCtrl.reset.load(resetN)
  }
}

object ECP5EvnMinimalSystem{
  def default(g : ECP5EvnMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.withJtag.load(false)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}

object ECP5EvnMinimal {
  //Function used to configure the SoC
  def default(g : ECP5EvnMinimal) = g{
    import g._
    ECP5EvnMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new ECP5EvnMinimal()).toComponent()))
    BspGenerator("ECP5EvnMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

