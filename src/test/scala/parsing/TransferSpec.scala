package parsing

import java.io.File

import org.scalatest.Ignore


@Ignore class TransferSpec extends BaseSpec {

  val fileName = "simple.vhd"
  val absFileName = System.getProperty("user.home") + File.separator + s"src/vhdl_parser/src/main/scala/parsing/files/${fileName}"
  val transfer = new Transfer(absFileName)

  "header" should "have header" in {
    transfer.header should be("theory vhdl_simpl\nimports Main vhdl_component vhdl_syntax_complex\n")
  }

  "entity" should "parse" in {
    val tree = transfer.parser.design_file()
    val logVisitor = new TVisitor
    logVisitor.visit(tree)

    logVisitor.defs should contain("""definition scantest:: "variable" where "scantest ≡ (''scantest'', vhdl_integer, (val_i 0))"""")
  }

}
