package saxon.board.ulx3s

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.ulx3s.peripheral._

class Ulx3sHdmiSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val noReset = Ulx3sNoResetGenerator()
  val hdmiConsoleA = Apb3HdmiConsoleGenerator(0x20000)
  val usbKeyboardA = Apb3UsbKeyboardGenerator(0x30000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.ctrl),
    cpu.dBus -> List(ramA.ctrl, peripheralBridge.input)
  )
}

class Ulx3sHdmi extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(48 MHz)
  clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
  clockCtrl.resetSynchronous.load(true)

  val system = new Ulx3sHdmiSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val resetn = in Bool()

    val pll = HdmiPll()
    
    pll.io.clkin := clk_25mhz

    clockCtrl.clock.load(pll.io.clkout2)
    clockCtrl.reset.load(resetn)
  }

  Dependable(system, system.hdmiConsoleA){
    system.hdmiConsoleA.pixclk := clocking.pll.io.clkout3
    system.hdmiConsoleA.pixclk_x5 := clocking.pll.io.clkout0
    system.hdmiConsoleA.resetn := clocking.resetn
  }
}

object Ulx3sHdmiSystem{
  def default(g : Ulx3sHdmiSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/blinkAndConsoleEcho/build/blinkAndConsoleEcho.hex")

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}

object Ulx3sHdmi {
  //Function used to configure the SoC
  def default(g : Ulx3sHdmi) = g{
    import g._
    Ulx3sHdmiSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sHdmi()).toComponent()))
    BspGenerator("Ulx3sHdmi", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

