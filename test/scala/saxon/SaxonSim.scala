package saxon

import java.awt
import java.awt.event.{ActionEvent, ActionListener, MouseEvent, MouseListener}
import java.nio.file.{Files, Paths}

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import javax.swing._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.memory.sdram.{SdramCtrl, SdramInterface, SdramLayout}
import vexriscv.test.{JLedArray, JSwitchArray}

import scala.collection.mutable


case class SdramCtrlSim(io : SdramInterface,
                        layout : SdramLayout,
                        clockDomain : ClockDomain){
  var CAS = 0
  var burstLength = 0
  val banks = Array.fill(layout.bankCount)(new Bank)

  var ckeLast = false
  val readShifter = new Array[Byte](layout.bankCount*3)

  def write(address : Int, data : Byte): Unit = {
    val byteId = address & (layout.bytePerWord-1)
    val word = address / layout.bytePerWord
    val row = word >> layout.columnWidth + layout.bankWidth
    val bank = (word >> layout.columnWidth) & (layout.bankCount-1)
    val col = word & (layout.columnSize-1)
    banks(bank).data((col + row * layout.columnSize) * layout.bytePerWord + byteId) = data
  }

  def loadBin(address : Int, path : String): Unit ={
    val bin = Files.readAllBytes(Paths.get(path))
    for((v,i) <- bin.zipWithIndex) write(address = i + address, data = v)
  }

  class Bank{
    val data = new Array[Byte](layout.capacity.toInt)

    var opened = false
    var openedRow = 0

    def activate(row : Int) : Unit = {
      if(opened)
        println("SDRAM error open unclosed bank")
      openedRow = row
      opened = true
    }

    def precharge() : Unit = {
      opened = false
    }

    def write(column : Int, byteId : Int, data : Byte) : Unit = {
      if(!opened)
        println("SDRAM : write in closed bank")
      val addr = byteId + (column + openedRow * layout.columnSize) * layout.bytePerWord
      //printf("SDRAM : Write A=%08x D=%02x\n",addr,data);
      this.data(addr) = data
    }

    def read(column : Int, byteId : Int) : Byte = {
      if(!opened)
        println("SDRAM : write in closed bank")
      val addr = byteId + (column + openedRow * layout.columnSize) * layout.bytePerWord
      //printf("SDRAM : Read A=%08x D=%02x\n",addr,data[addr]);
      return data(addr);
    }
  }

  clockDomain.onSamplings{
    if(!io.CSn.toBoolean && ckeLast){
      val code = (if(io.RASn.toBoolean) 0x4 else 0) | (if(io.CASn.toBoolean) 0x2 else 0) | (if(io.WEn.toBoolean) 0x1 else 0)
      val ba = io.BA.toInt
      val addr = io.ADDR.toInt
      code match {
        case 0 =>  //Mode register set
        if(ba == 0 && (addr & 0x400) == 0){
          CAS = ((addr) >> 4) & 0x7
          burstLength = ((addr) >> 0) & 0x7
          if((addr & 0x388) != 0)
            println("SDRAM : ???")
          printf("SDRAM : MODE REGISTER DEFINITION CAS=%d burstLength=%d\n",CAS,burstLength)
        }
        case 2 =>  //Bank precharge
        if((addr & 0x400) != 0){ //all
          for(bankId <- 0 until layout.bankCount) banks(bankId).precharge()
        } else { //single
          banks(ba).precharge()
        }
        case 3 =>  //Bank activate
          banks(ba).activate(addr & 0x7FF);
        case 4 =>  //Write
        if((addr & 0x400) != 0)
          println("SDRAM : Write autoprecharge not supported")

        if(io.DQ.writeEnable.toBoolean == false)
          println("SDRAM : Write Wrong DQ direction")

        val dqWrite = io.DQ.write.toLong
        val dqm = io.DQM.toInt
        for(byteId <- 0 until layout.bytePerWord){
          if(((dqm >> byteId) & 1) == 0)
            banks(ba).write(addr, byteId ,(dqWrite >> byteId*8).toByte);
        }

        case 5 =>  //Read
        if((addr & 0x400) != 0)
        println("SDRAM : READ autoprecharge not supported")

        if(io.DQ.writeEnable.toBoolean != false)
        println("SDRAM : READ Wrong DQ direction")

        //if(io.DQM !=  config->byteCount-1)
        //println("SDRAM : READ wrong DQM")

        for(byteId <- 0 until layout.bytePerWord){
          readShifter(byteId) = banks(ba).read(addr, byteId);
        }
        case 1 =>  // Self refresh
        case 7 =>  // NOP
        case _ =>
          println("SDRAM : unknown code")
      }
    }
    ckeLast = io.CKE.toBoolean;

    if(CAS >= 2 && CAS <=3){
      var readData = 0l
      for(byteId <- 0 until layout.bankCount){
        readData |= (readShifter(byteId + (CAS-1)*layout.bytePerWord) & 0xFF) << byteId*8;
      }
      io.DQ.read #= readData
      for(latency <-  CAS-1 downto 1){  //missing CKE
        for(byteId <- 0 until layout.bytePerWord){
          readShifter(byteId+latency*layout.bytePerWord) = readShifter(byteId+(latency-1)*layout.bytePerWord);
        }
      }
    }
  }
}


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

    SimConfig.allOptimisation.compile(new GeneratorComponent(new SaxonSoc().defaultSetting())).doSimUntilVoid("test", 42){dut =>
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

      val sdram = SdramCtrlSim(
        io = dut.generator.core.sdramA.sdram.get,
        layout = dut.generator.core.sdramA.logic.layout,
        clockDomain = clockDomain
      )
      sdram.loadBin(0, "software/standalone/dhrystone/build/dhrystone.bin")

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
