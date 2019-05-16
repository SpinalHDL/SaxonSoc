package saxon

import java.awt
import java.awt.event.{ActionEvent, ActionListener, MouseEvent, MouseListener}

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import javax.swing._

import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import vexriscv.test.{JLedArray, JSwitchArray}

import scala.collection.mutable



object SaxonSim {
  def main(args: Array[String]): Unit = {
    val simSlowDown = false
//    def p = SaxonSocParameters.default
//    val flashBin = null
//    val flashBin = "software/standalone/dhrystone/build/dhrystone.bin"
//    val flashBin = "software/bootloader/up5kEvnDemo.bin"
//    val flashBin = "software/standalone/blinkAndEcho/build/blinkAndEcho.bin"
//    val flashBin = "../zephyr/zephyrSpinalHdl/samples/hello_world/build/zephyr/zephyr.bin"
//    val flashBin = "../zephyr/zephyrSpinalHdl/samples/synchronization/build/zephyr/zephyr.bin"
//    val flashBin = "../zephyr/zephyrSpinalHdl/samples/philosophers/build/zephyr/zephyr.bin"
//    val ramBin = "software/standalone/dhrystone/build/dhrystone.hex"

    SimConfig.compile(new GeneratorComponent(new SaxonSoc)).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.generator.clockCtrl.clkFrequency.get.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.generator.clockCtrl.io.clk, dut.generator.clockCtrl.io.reset)
      clockDomain.forkStimulus(systemClkPeriod)
//      clockDomain.forkSimSpeedPrinter(4)

      val tcpJtag = JtagTcp(
        jtag = dut.generator.core.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin =  dut.generator.core.uartA.uart.get.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = dut.generator.core.uartA.uart.get.rxd,
        baudPeriod = uartBaudPeriod
      )

//      val guiThread = fork{
//        val guiToSim = mutable.Queue[Any]()
//
//        var ledsValue = 0l
//        var switchValue : () => BigInt = null
//        val ledsFrame = new JFrame{
//          setLayout(new BoxLayout(getContentPane, BoxLayout.Y_AXIS))
//
//          add(new JLedArray(8){
//            override def getValue = ledsValue
//          })
//          add{
//            val switches = new JSwitchArray(8)
//            switchValue = switches.getValue
//            switches
//          }
//
//          add(new JButton("Reset"){
//            addActionListener(new ActionListener {
//              override def actionPerformed(actionEvent: ActionEvent): Unit = {
//                println("ASYNC RESET")
//                guiToSim.enqueue("asyncReset")
//              }
//            })
//            setAlignmentX(awt.Component.CENTER_ALIGNMENT)
//          })
//          setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
//          pack()
//          setVisible(true)
//
//        }
//
//        //Slow refresh
//        while(true){
//          sleep(systemClkPeriod*50000)
//
//          val dummy = if(guiToSim.nonEmpty){
//            val request = guiToSim.dequeue()
//            if(request == "asyncReset"){
//              dut.io.reset #= true
//              sleep(systemClkPeriod*32)
//              dut.io.reset #= false
//            }
//          }
//
//          dut.io.gpioA.read #= (dut.io.gpioA.write.toLong & dut.io.gpioA.writeEnable.toLong) | (switchValue())
//          ledsValue = dut.io.gpioA.write.toLong
//          ledsFrame.repaint()
//          if(simSlowDown) Thread.sleep(400)
//        }
//      }
    }
  }
}
