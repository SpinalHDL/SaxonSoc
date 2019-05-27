package saxon

import java.awt
import java.awt.event.{ActionEvent, ActionListener, MouseEvent, MouseListener}
import java.nio.file.{Files, Paths}

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.generator._
import spinal.lib.generator.Handle._
import javax.swing._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.memory.sdram.sim.SdramModel
import spinal.lib.memory.sdram.{SdramCtrl, SdramInterface, SdramLayout}
import vexriscv.test.{JLedArray, JSwitchArray}

import scala.collection.mutable




object SaxonSimSdramSim {
  def main(args: Array[String]): Unit = {
    val simSlowDown = false

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new SaxonSocSdram().defaultSetting().toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 1000000
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)
//      clockDomain.forkSimSpeedPrinter(4)

//      fork{
//        while(true){
//          sleep(systemClkPeriod*1000000)
//          println("\nsimTime : " + simTime())
//        }
//      }
      fork{
//        disableSimWave()
//        sleep(600e9.toLong)
//        enableSimWave()
//        sleep(systemClkPeriod*1000000)
//        simFailure()

        while(true){
          disableSimWave()
          sleep(systemClkPeriod*500000)
          enableSimWave()
          sleep(systemClkPeriod*100)
        }
      }




      val tcpJtag = JtagTcp(
        jtag = dut.system.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin =  dut.system.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = dut.system.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val sdram = SdramModel(
        io = dut.system.sdramA.sdram,
        layout = dut.system.sdramA.logic.layout,
        clockDomain = clockDomain
      )
//      sdram.loadBin(0, "software/standalone/dhrystone/build/dhrystone.bin")

//      val linuxPath = "ext/VexRiscv/src/test/resources/VexRiscvRegressionData/sim/linux/rv32ima/"
      val linuxPath = "../buildroot/output/images/"
      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      sdram.loadBin(0x00400000, linuxPath + "Image")
      sdram.loadBin(0x00BF0000, linuxPath + "dtb")
      sdram.loadBin(0x00C00000, linuxPath + "rootfs.cpio")
    }
  }

}
