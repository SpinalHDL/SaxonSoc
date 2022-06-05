package saxon

import spinal.core._
import vexriscv.ip._
import vexriscv._
import vexriscv.plugin._

object VexRiscvConfigs {
    val withMemoryStage = false
    val executeRf = true
    val hardwareBreakpointsCount  = 0
    val bootloaderBin : String = null

    def linux: VexRiscvConfig = linux(0x80000000l)
    def linux(resetVector: Long, ioRegion : UInt => Bool = _(31 downto 28) === 0xF) = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = true,
      List(
        new IBusCachedPlugin(
          resetVector = resetVector,
          compressedGen = false,
          prediction = STATIC,
          injectorStage = false,
          config = InstructionCacheConfig(
            cacheSize = 4096*1,
            bytePerLine = 32,
            wayCount = 1,
            addressWidth = 32,
            cpuDataWidth = 32,
            memDataWidth = 32,
            catchIllegalAccess = true,
            catchAccessFault = true,
            asyncTagMemory = false,
            twoCycleRam = false,
            twoCycleCache = true
          ),
          memoryTranslatorPortConfig = MmuPortConfig(
            portTlbSize = 4
          )
        ),
        new DBusCachedPlugin(
          dBusCmdMasterPipe = true,
          dBusCmdSlavePipe = true,
          dBusRspSlavePipe = true,
          config = new DataCacheConfig(
            cacheSize         = 4096*1,
            bytePerLine       = 32,
            wayCount          = 1,
            addressWidth      = 32,
            cpuDataWidth      = 32,
            memDataWidth      = 32,
            catchAccessError  = true,
            catchIllegal      = true,
            catchUnaligned    = true,
            withLrSc = true,
            withAmo = true
            //          )
          ),
          memoryTranslatorPortConfig = MmuPortConfig(
            portTlbSize = 4
          )
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = true
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false
        ),
        new FullBarrelShifterPlugin(earlyInjection = false),
        new HazardSimplePlugin(
          bypassExecute           = true,
          bypassMemory            = true,
          bypassWriteBack         = true,
          bypassWriteBackBuffer   = true,
          pessimisticUseSrc       = false,
          pessimisticWriteRegFile = false,
          pessimisticAddressMatch = false
        ),
        new MulPlugin,
        new MulDivIterativePlugin(
          genMul = false,
          genDiv = true,
          mulUnrollFactor = 32,
          divUnrollFactor = 1
        ),
        new CsrPlugin(CsrPluginConfig.linuxMinimal(0x80000020l).copy(ebreakGen = false)),

        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = true,
          fenceiGenAsAJump = false
        ),
        new MmuPlugin(
          ioRange = (x => x(31 downto 28) === 0x1)
        ),
        new YamlPlugin("cpu0.yaml")
      )
    )


  def ulx3sLinux(resetVector: Long) = VexRiscvConfig(
    withMemoryStage = true,
    withWriteBackStage = true,
    List(
      new IBusCachedPlugin(
        resetVector = resetVector,
        compressedGen = false,
        prediction = NONE,
        injectorStage = false,
        relaxedPcCalculation = false,
        config = InstructionCacheConfig(
          cacheSize = 4096*1,
          bytePerLine = 64,
          wayCount = 1,
          addressWidth = 32,
          cpuDataWidth = 32,
          memDataWidth = 32,
          catchIllegalAccess = true,
          catchAccessFault = true,
          asyncTagMemory = false,
          twoCycleRam = true,
          twoCycleCache = true
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DBusCachedPlugin(
        dBusCmdMasterPipe = true,
        dBusCmdSlavePipe = true,
        dBusRspSlavePipe = true,
        config = new DataCacheConfig(
          cacheSize         = 4096*1,
          bytePerLine       = 64,
          wayCount          = 1,
          addressWidth      = 32,
          cpuDataWidth      = 32,
          memDataWidth      = 32,
          catchAccessError  = true,
          catchIllegal      = true,
          catchUnaligned    = true,
          withLrSc = true,
          withAmo = true
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = true
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.ASYNC,
        zeroBoot = true
      ),
      new IntAluPlugin,
      new SrcPlugin(
        separatedAddSub = false
      ),
      new FullBarrelShifterPlugin(earlyInjection = false),
      new HazardSimplePlugin(
        bypassExecute           = true,
        bypassMemory            = true,
        bypassWriteBack         = true,
        bypassWriteBackBuffer   = true,
        pessimisticUseSrc       = false,
        pessimisticWriteRegFile = false,
        pessimisticAddressMatch = false
      ),
      new MulDivIterativePlugin(
        genMul = true,
        genDiv = true,
        mulUnrollFactor = 4,
        divUnrollFactor = 1
      ),
      new CsrPlugin(CsrPluginConfig.linuxMinimal(0x80000020l).copy(ebreakGen = false)),

      new BranchPlugin(
        earlyBranch = false,
        catchAddressMisaligned = true,
        fenceiGenAsAJump = false
      ),
      new MmuPlugin(
        ioRange = (x => x(31 downto 28) === 0x1)
      ),
      new YamlPlugin("cpu0.yaml")
    )
  )

  def linuxTest(resetVector : BigInt = 0x80000000l, openSbi : Boolean = false) = VexRiscvConfig(
    withMemoryStage = true,
    withWriteBackStage = true,
    List(
//      new SingleInstructionLimiterPlugin(),
      new IBusCachedPlugin(
        resetVector = resetVector,
        compressedGen = false,
        prediction = STATIC,
        injectorStage = false,
        config = InstructionCacheConfig(
          cacheSize = 4096*2,
          bytePerLine = 64,
          wayCount = 2,
          addressWidth = 32,
          cpuDataWidth = 32,
          memDataWidth = 32,
          catchIllegalAccess = true,
          catchAccessFault = true,
          asyncTagMemory = false,
          twoCycleRam = false,
          twoCycleCache = true
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DBusCachedPlugin(
        dBusCmdMasterPipe = true,
        dBusCmdSlavePipe = true,
        dBusRspSlavePipe = true,
        config = new DataCacheConfig(
          cacheSize         = 4096*2,
          bytePerLine       = 64,
          wayCount          = 2,
          addressWidth      = 32,
          cpuDataWidth      = 32,
          memDataWidth      = 32,
          catchAccessError  = true,
          catchIllegal      = true,
          catchUnaligned    = true,
          withLrSc = true,
          withAmo = true
          //          )
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = true
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.ASYNC,
        zeroBoot = true,
        x0Init = false
      ),
      new IntAluPlugin,
      new SrcPlugin(
        separatedAddSub = false
      ),
      new FullBarrelShifterPlugin(earlyInjection = false),
      new HazardSimplePlugin(
        bypassExecute           = true,
        bypassMemory            = true,
        bypassWriteBack         = true,
        bypassWriteBackBuffer   = true,
        pessimisticUseSrc       = false,
        pessimisticWriteRegFile = false,
        pessimisticAddressMatch = false
      ),
      new MulPlugin,
      new MulDivIterativePlugin(
        genMul = false,
        genDiv = true,
        mulUnrollFactor = 32,
        divUnrollFactor = 1
      ),
      new CsrPlugin((if(openSbi)CsrPluginConfig.openSbi(0,Riscv.misaToInt("imas")) else CsrPluginConfig.linuxMinimal(null)).copy(ebreakGen = false, wfiOutput = true, mtvecAccess = CsrAccess.READ_WRITE)), //mtvecAccess read required by freertos

      new BranchPlugin(
        earlyBranch = false,
        catchAddressMisaligned = true,
        fenceiGenAsAJump = false
      ),
      new MmuPlugin(
        ioRange = (x => x(31 downto 28) === 0x1)
      ),
      new YamlPlugin("cpu0.yaml")
    )
  )

  def cachedTest(resetVector : BigInt = 0x80000000l) = VexRiscvConfig(
    withMemoryStage = true,
    withWriteBackStage = true,
    List(
      new IBusCachedPlugin(
        resetVector = resetVector,
        compressedGen = false,
        prediction = vexriscv.plugin.NONE,
        injectorStage = true,
        relaxedPcCalculation = false,
        config = InstructionCacheConfig(
          cacheSize = 4096*1,
          bytePerLine = 32,
          wayCount = 1,
          addressWidth = 32,
          cpuDataWidth = 32,
          memDataWidth = 32,
          catchIllegalAccess = true,
          catchAccessFault = true,
          asyncTagMemory = false,
          twoCycleRam = true,
          twoCycleCache = true
        )
      ),
      new DBusCachedPlugin(
        dBusCmdMasterPipe = true,
        dBusCmdSlavePipe = true,
        dBusRspSlavePipe = true,
        relaxedMemoryTranslationRegister = true,
        config = new DataCacheConfig(
          cacheSize         = 4096*1,
          bytePerLine       = 32,
          wayCount          = 1,
          addressWidth      = 32,
          cpuDataWidth      = 32,
          memDataWidth      = 32,
          catchAccessError  = true,
          catchIllegal      = true,
          catchUnaligned    = true,
          withLrSc = true,
          withAmo = true,
          earlyWaysHits = true
          //          )
        )
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = true
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.ASYNC,
        zeroBoot = true,
        x0Init = false
      ),
      new IntAluPlugin,
      new SrcPlugin(
        separatedAddSub = true
      ),
      new FullBarrelShifterPlugin(earlyInjection = false),
      new HazardSimplePlugin(
        bypassExecute           = true,
        bypassMemory            = true,
        bypassWriteBack         = true,
        bypassWriteBackBuffer   = true,
        pessimisticUseSrc       = false,
        pessimisticWriteRegFile = false,
        pessimisticAddressMatch = false
      ),
      new MulPlugin(
        inputBuffer = true
      ),
      new MulDivIterativePlugin(
        genMul = false,
        genDiv = true,
        mulUnrollFactor = 32,
        divUnrollFactor = 1
      ),
      new CsrPlugin(CsrPluginConfig.small(null).copy(ebreakGen = false, mtvecAccess         = CsrAccess.WRITE_ONLY)),

      new BranchPlugin(
        earlyBranch = false,
        catchAddressMisaligned = true,
        fenceiGenAsAJump = false
      ),
      new StaticMemoryTranslatorPlugin(
        ioRange      = _(31 downto 28) === 0x1
      ),
      new YamlPlugin("cpu0.yaml")
    )
  )
  def linuxIce40 = VexRiscvConfig(
    withMemoryStage = true,
    withWriteBackStage = true,
    List(
      new IBusCachedPlugin(
        resetVector = 0x80000000l,
        compressedGen = false,
        prediction = plugin.NONE,
        injectorStage = false,
        config = InstructionCacheConfig(
          cacheSize = 4096*1,
          bytePerLine = 32,
          wayCount = 1,
          addressWidth = 32,
          cpuDataWidth = 32,
          memDataWidth = 32,
          catchIllegalAccess = true,
          catchAccessFault = true,
          asyncTagMemory = false,
          twoCycleRam = false,
          twoCycleCache = true
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DBusCachedPlugin(
        dBusCmdMasterPipe = false,
        dBusCmdSlavePipe = false,
        dBusRspSlavePipe = false,
        config = new DataCacheConfig(
          cacheSize         = 4096,
          bytePerLine       = 32,
          wayCount          = 1,
          addressWidth      = 32,
          cpuDataWidth      = 32,
          memDataWidth      = 32,
          catchAccessError  = true,
          catchIllegal      = true,
          catchUnaligned    = true,
          withLrSc = true,
          withAmo = false
          //          )
        ),
        memoryTranslatorPortConfig = MmuPortConfig(
          portTlbSize = 4
        )
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = true
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.SYNC,
        zeroBoot = true,
        x0Init = false
      ),
      new IntAluPlugin,
      new SrcPlugin(
        executeInsertion = true,
        separatedAddSub = false
      ),
      new LightShifterPlugin(),
//      new FullBarrelShifterPlugin(earlyInjection = false),
      new HazardSimplePlugin(
        bypassExecute           = true,
        bypassMemory            = true,
        bypassWriteBack         = true,
        bypassWriteBackBuffer   = true,
        pessimisticUseSrc       = false,
        pessimisticWriteRegFile = false,
        pessimisticAddressMatch = false
      ),
//      new MulPlugin,
//      new MulDivIterativePlugin(
//        genMul = false,
//        genDiv = true,
//        mulUnrollFactor = 32,
//        divUnrollFactor = 1
//      ),
      new CsrPlugin(CsrPluginConfig.linuxMinimal(null).copy(ebreakGen = false)),

      new BranchPlugin(
        earlyBranch = false,
        catchAddressMisaligned = true,
        fenceiGenAsAJump = false
      ),
      new MmuPlugin(
        ioRange = (x => x(31 downto 28) === 0x1)
      ),
      new YamlPlugin("cpu0.yaml")
    )
  )


    def cacheLessRegular = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = true,
      List(
        new IBusSimplePlugin(
          resetVector = 0x80000000l,
          cmdForkOnSecondStage = false,
          cmdForkPersistence = true
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true,
          x0Init = false,
          readInExecute = false,
          syncUpdateOnStall = true
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = false,
          decodeAddSub = false
        ),
        //      new LightShifterPlugin(),
        new FullBarrelShifterPlugin(earlyInjection = false),
        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = false,
          fenceiGenAsAJump = true
        ),
        new HazardSimplePlugin(
          bypassExecute = true,
          bypassMemory = true,
          bypassWriteBack = true,
          bypassWriteBackBuffer = true
        ),
        new MulPlugin,
        new MulDivIterativePlugin(
          genMul = false
        ),
        new CsrPlugin(new CsrPluginConfig(
          catchIllegalAccess = false,
          mvendorid = null,
          marchid = null,
          mimpid = null,
          mhartid = null,
          misaExtensionsInit = 0,
          misaAccess = CsrAccess.NONE,
          mtvecAccess = CsrAccess.WRITE_ONLY,
          mtvecInit = null,
          mepcAccess = CsrAccess.READ_WRITE,
          mscratchGen = false,
          mcauseAccess = CsrAccess.READ_ONLY,
          mbadaddrAccess = CsrAccess.NONE,
          mcycleAccess = CsrAccess.NONE,
          minstretAccess = CsrAccess.NONE,
          ecallGen = true,
          ebreakGen = false,
          wfiGenAsWait = false,
          wfiGenAsNop = true,
          ucycleAccess = CsrAccess.NONE
        )),
        new YamlPlugin("cpu0.yaml")
      )
    )


    def minimal = VexRiscvConfig(
      withMemoryStage = withMemoryStage,
      withWriteBackStage = false,
      List(
        new IBusSimplePlugin(
          resetVector = 0x80000000l,
          cmdForkOnSecondStage = false,
          cmdForkPersistence = true
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true,
          x0Init = false,
          readInExecute = executeRf,
          syncUpdateOnStall = true
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = executeRf,
          decodeAddSub = false
        ),
        new LightShifterPlugin(),
        new BranchPlugin(
          earlyBranch = true,
          catchAddressMisaligned = false,
          fenceiGenAsAJump = true
        ),
        new HazardSimplePlugin(
          bypassExecute = false,
          bypassWriteBackBuffer = false
        ),

        new YamlPlugin("cpu0.yaml")
      )
    )

    def minimalWithCsr = {
      val c = minimal
      c.plugins += new CsrPlugin(new CsrPluginConfig(
        catchIllegalAccess = false,
        mvendorid = null,
        marchid = null,
        mimpid = null,
        mhartid = null,
        misaExtensionsInit = 0,
        misaAccess = CsrAccess.NONE,
        mtvecAccess = CsrAccess.WRITE_ONLY,
        mtvecInit = null,
        mepcAccess = CsrAccess.READ_WRITE,
        mscratchGen = false,
        mcauseAccess = CsrAccess.READ_ONLY,
        mbadaddrAccess = CsrAccess.NONE,
        mcycleAccess = CsrAccess.NONE,
        minstretAccess = CsrAccess.NONE,
        ecallGen = true,
        ebreakGen = false,
        wfiGenAsWait = false,
        wfiGenAsNop = true,
        ucycleAccess = CsrAccess.NONE
      ))
      c
    }
 
    def muraxLike = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = true,
      List(
        new IBusSimplePlugin(
          resetVector = 0x80000000l,
          cmdForkOnSecondStage = false,
          cmdForkPersistence = true,
          prediction = plugin.NONE,
          catchAccessFault = false,
          compressedGen = false
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false,
          earlyInjection = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = false
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = false
        ),
        new LightShifterPlugin(),
        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = false
        ),
        new HazardSimplePlugin(
          bypassExecute = false,
          bypassMemory = false,
          bypassWriteBack = false,
          bypassWriteBackBuffer = false,
          pessimisticUseSrc = false,
          pessimisticWriteRegFile = false,
          pessimisticAddressMatch = false
        ),
        new YamlPlugin("cpu0.yaml"),
        new CsrPlugin(new CsrPluginConfig(
          catchIllegalAccess = false,
          mvendorid = null,
          marchid = null,
          mimpid = null,
          mhartid = null,
          misaExtensionsInit = 0,
          misaAccess = CsrAccess.NONE,
          mtvecAccess = CsrAccess.WRITE_ONLY,
          mtvecInit = null,
          mepcAccess = CsrAccess.READ_WRITE,
          mscratchGen = false,
          mcauseAccess = CsrAccess.READ_ONLY,
          mbadaddrAccess = CsrAccess.NONE,
          mcycleAccess = CsrAccess.NONE,
          minstretAccess = CsrAccess.NONE,
          ecallGen = true,
          ebreakGen = false,
          wfiGenAsWait = false,
          wfiGenAsNop = true,
          ucycleAccess = CsrAccess.NONE
        ) 
      )
    )
  )

  object xip{
    def fast: VexRiscvConfig = fast(0x20000000l + (1 MiB).toLong)
    def fast(resetVector: Long) = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = true,
      List(
        new IBusCachedPlugin(
          resetVector = resetVector,
          compressedGen = false,
          prediction = plugin.NONE,
          injectorStage = false,
          config = InstructionCacheConfig(
            cacheSize = 4096,
            bytePerLine = 32,
            wayCount = 1,
            addressWidth = 32,
            cpuDataWidth = 32,
            memDataWidth = 32,
            catchIllegalAccess = false,
            catchAccessFault = false,
            asyncTagMemory = false,
            twoCycleRam = false,
            twoCycleCache = false
          )
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false,
          earlyInjection = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true,
          x0Init = false
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = false
        ),
        new LightShifterPlugin(),
        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = false
        ),
        new HazardSimplePlugin(
          bypassExecute = false,
          bypassMemory = false,
          bypassWriteBack = false,
          bypassWriteBackBuffer = false
        ),
        new YamlPlugin("cpu0.yaml")
      )
    )

    def fastWithCsr: VexRiscvConfig = fastWithCsr(0x20000000l + (1 MiB).toLong)
    def fastWithCsr(resetVector: Long) = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = true,
      List(
        new IBusCachedPlugin(
          resetVector = resetVector,
          compressedGen = false,
          prediction = plugin.NONE,
          injectorStage = false,
          config = InstructionCacheConfig(
            cacheSize = 4096,
            bytePerLine = 32,
            wayCount = 1,
            addressWidth = 32,
            cpuDataWidth = 32,
            memDataWidth = 32,
            catchIllegalAccess = false,
            catchAccessFault = false,
            asyncTagMemory = false,
            twoCycleRam = false,
            twoCycleCache = false
          )
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false,
          earlyInjection = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true,
          x0Init = false
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = false
        ),
        new LightShifterPlugin(),
        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = false
        ),
        new HazardSimplePlugin(
          bypassExecute = false,
          bypassMemory = false,
          bypassWriteBack = false,
          bypassWriteBackBuffer = false
        ),
        new CsrPlugin(new CsrPluginConfig(
          catchIllegalAccess = false,
          mvendorid = null,
          marchid = null,
          mimpid = null,
          mhartid = null,
          misaExtensionsInit = 0,
          misaAccess = CsrAccess.NONE,
          mtvecAccess = CsrAccess.WRITE_ONLY,
          mtvecInit = null,
          mepcAccess = CsrAccess.READ_WRITE,
          mscratchGen = false,
          mcauseAccess = CsrAccess.READ_ONLY,
          mbadaddrAccess = CsrAccess.NONE,
          mcycleAccess = CsrAccess.NONE,
          minstretAccess = CsrAccess.NONE,
          ecallGen = true,
          ebreakGen = false,
          wfiGenAsWait = false,
          wfiGenAsNop = true,
          ucycleAccess = CsrAccess.NONE
        )),
        new YamlPlugin("cpu0.yaml")
      )
    )
  }
}

