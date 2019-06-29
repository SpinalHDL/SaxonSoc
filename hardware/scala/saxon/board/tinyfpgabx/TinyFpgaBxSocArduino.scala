package saxon.board.tinyfpgabx

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.blackice.IceStormInOutWrapper
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}

class TinyFpgaBxSocArduinoSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiFlash = Apb3SpiGenerator(0x30000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}


class TinyFpgaBxSocArduino extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(20 MHz)

  val system = new TinyFpgaBxSocArduinoSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_16 = in Bool()
    val GRESET = in Bool()

    val pll = TinyFpgaBxPll()
    pll.clock_in := CLOCK_16

    clockCtrl.clock.load(pll.clock_out)
    clockCtrl.reset.load(GRESET)
  }
}

object TinyFpgaBxSocArduinoSystem{
  def default(g : TinyFpgaBxSocArduinoSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(12 KiB)
    //ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")
    ramA.hexInit.load(null)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    spiFlash.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 0,
        spi = SpiXdrParameter(2, 2, 1)
      ).addFullDuplex(0,2,false).addHalfDuplex(id=1, rate=2, ddr=false, spiWidth=2),
      cmdFifoDepth = 1,
      rspFifoDepth = 1,
      cpolInit = false,
      cphaInit = false,
      modInit = 0,
      sclkToogleInit = 0,
      ssSetupInit = 0,
      ssHoldInit = 0,
      ssDisableInit = 0,
      xipConfigWritable = false,
      xipEnableInit = true,
      xipInstructionEnableInit = true,
      xipInstructionModInit = 0,
      xipAddressModInit = 0,
      xipDummyModInit = 0,
      xipPayloadModInit = 1,
      xipInstructionDataInit = 0x3B,
      xipDummyCountInit = 0,
      xipDummyDataInit = 0xFF,
      xip = SpiXdrMasterCtrl.XipBusParameters(addressWidth = 24, lengthWidth = 2)
    )

    g
  }
}


object TinyFpgaBxSocArduino {
  //Function used to configure the SoC
  def default(g : TinyFpgaBxSocArduino) = g{
    import g._
    TinyFpgaBxSocArduinoSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.FALL)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new TinyFpgaBxSocArduino()).toComponent()))
    BspGenerator("TinyFpgaBxSocArduino", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)  }
}

