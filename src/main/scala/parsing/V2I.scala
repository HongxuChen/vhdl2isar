package parsing

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.language.implicitConversions

object V2I {

  type RangeTy = (String, String, String)
  val defaultRange: RangeTy = ("???", "???", "???")

  val logger = LoggerFactory.getLogger(this.getClass)

  def VHDLize(vhdlType: String) = s"vhdl_${vhdlType}"

  class TypeInfo {
    type RecordInfoTy = Seq[(String, VSubtypeIndication)]
    val typeDeclTbl = mutable.Map.empty[String, RecordInfoTy]

    def addNewType(id: String, items: RecordInfoTy): Unit = {
      typeDeclTbl += (id -> items)
    }

    val knownListType = Set("div32_in_type", "div32_out_type")

    def isListType(valType: String) = {
      if (knownListType.contains(valType)) true
      else if (typeDeclTbl.contains(valType)) true
      else false
    }

    def isVectorType(valType: String) = valType.contains("_vector")

    def decorate(rawIdType: String, valType: String) = {
      if (isListType(valType)) s"${rawIdType} list" else rawIdType
    }

    def _guessScalarInitVal(valType: String): IVariable = valType match {
      case "integer" => IVariable("val_i", "0")
      case "real" => IVariable("val_r", "0.0")
      case "character" => IVariable("val_c", "(CHR ''0'')")
      case "boolean" => IVariable("val_b", "True)")
      case "std_ulogic" => IVariable("val_c", "(CHR ''0'')")
      case "std_logic" => IVariable("val_c", "(CHR ''0'')")
      case _ => IVariable("TODO", "Scalar unknown")
    }

    def getScalarInitVal(valType: String, expOption: Option[VExp]): IExp = expOption match {
      case Some(exp) => {
        val expRepr = exp.repr
        if (expRepr.contains("???")) {
          logger.warn(s"unknown exp, guessing")
          IExp_con(valType, _guessScalarInitVal(valType))
        } else {
          //        repr -> IExpr
          IVariable("TODO", "Scalar repr")
        }
      }
      case None => IExp_con(valType, _guessScalarInitVal(valType))
    }

    def _guessListInitVals(rawType: String): List[IScalarOrVecIval] = {
      val recordInfo = typeDeclTbl(rawType)
      val iVals = for {
        (itemId, itemTyInfo) <- recordInfo
      } yield {
        val valType = itemTyInfo.selectedName
        if (isListType(valType)) {
          val initVals = _guessListInitVals(valType)
          //      TODO    IValue shoud have other forms
          logger.info(s"list-list: $initVals")
          IScalarOrVecIval(itemId, valType, null)
        } else {
          val initVal = if (isVectorType(valType)) {
            val range = itemTyInfo.getRange.getOrElse(defaultRange)
            _guessVectorInitVal(valType, range)
          } else {
            _guessScalarInitVal(valType)
          }
          IScalarOrVecIval(itemId, valType, initVal)
        }
      }
      iVals.toList
    }

    def _guessVectorInitVal(valType: String, r: RangeTy): IExp = {
      require(valType.endsWith("_vector"), "vector")
      val genCmd = valType.substring(0, valType.length - "_vector".length) + "_vec_gen"
      val valListType = if (r._2 == "to") "val_list" else if (r._2 == "downto") "val_rlist" else "???"
      if (List("val_lsit", "val_rlist").contains(valListType)) {
        val iVarChar = IVariable("val_c", "(CHR ''0'')")
        IVariable(valListType, s"(${genCmd} ${Math.abs(r._1.toInt - r._3.toInt) + 1} ${iVarChar})")
      } else {
        IVariable(valListType, s"???")
      }
    }

    def getListInitVals(valType: String, expOption: Option[VExp]): List[IScalarOrVecIval] = {
      require(isListType(valType), s"${valType} should be composite")
      expOption match {
        case Some(vExp) => {
          val expRepr = vExp.repr
          if (expRepr.contains("???")) {
            logger.warn(s"unknown composite exp, guessing")
            _guessListInitVals(valType)
          } else {
            logger.info("comoposite exp, TODO")
            List.empty
          }
        }
        case None => _guessListInitVals(valType)
      }
    }
  }

  //  for type rather than  value
  case class TVRecordItem(id: String, valType: String, range: Seq[VExplicitRange])

  case class TVRecord(id: String, items: Seq[TVRecordItem])

}

