//package saxon
//
//import java.awt
//import java.awt.event.{ActionEvent, ActionListener, MouseEvent, MouseListener}
//import java.nio.file.{Files, Paths}
//
//import spinal.sim._
//import spinal.core._
//import spinal.core.sim._
//import spinal.lib.generator._
//import javax.swing._
//import spinal.lib.com.jtag.sim.JtagTcp
//import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
//import spinal.lib.memory.sdram.sim.SdramModel
//import spinal.lib.memory.sdram.{SdramCtrl, SdramInterface, SdramLayout}
//import vexriscv.test.{JLedArray, JSwitchArray}
//
//import scala.collection.mutable
//
//
//
//
//object SaxonSocOnChipRamSim {
//  def main(args: Array[String]): Unit = {
//    val simSlowDown = false
//
//    val simConfig = SimConfig
//    simConfig.allOptimisation
//
//    simConfig.compile(new SaxonSocOnChipRam().defaultSetting().toComponent()).doSimUntilVoid("test", 42){dut =>
//      val systemClkPeriod = (1e12/dut.generator.clockCtrl.clkFrequency.toDouble).toLong
//      val jtagClkPeriod = systemClkPeriod*4
//      val uartBaudRate = 1000000
//      val uartBaudPeriod = (1e12/uartBaudRate).toLong
//
//      val clockDomain = ClockDomain(dut.generator.clockCtrl.clock, dut.generator.clockCtrl.reset)
//      clockDomain.forkStimulus(systemClkPeriod)
////      clockDomain.forkSimSpeedPrinter(4)
//
//
//      val tcpJtag = JtagTcp(
//        jtag = dut.generator.core.cpu.jtag,
//        jtagClkPeriod = jtagClkPeriod
//      )
//
//      val uartTx = UartDecoder(
//        uartPin =  dut.generator.core.uartA.uart.txd,
//        baudPeriod = uartBaudPeriod
//      )
//
//      val uartRx = UartEncoder(
//        uartPin = dut.generator.core.uartA.uart.rxd,
//        baudPeriod = uartBaudPeriod
//      )
//    }
//  }
//
//}
