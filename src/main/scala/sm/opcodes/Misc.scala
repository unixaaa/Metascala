package sm
package opcodes
import sm.imm.Type
import collection.mutable
import rt.Thread

object Misc {
  case class Goto(label: Int) extends OpCode{
    def op(vt: Thread) = vt.frame.pc = label
  }

  // These guys are meant to be deprecated in java 6 and 7
  //===============================================================
  val Ret = UnusedOpCode
  val Jsr = UnusedOpCode
  //===============================================================

  case class TableSwitch(min: Int, max: Int, defaultTarget: Int, targets: Seq[Int]) extends OpCode{
    def op(vt: Thread) =  {
      val vrt.Int(top) = vt.pop
      val newPc: Int =
        if (targets.isDefinedAt(top - min)) targets(top - min)
        else defaultTarget
      vt.frame.pc = newPc
    }
  }

  case class LookupSwitch(defaultTarget: Int, keys: Seq[Int], targets: Seq[Int]) extends OpCode{
    def op(vt: Thread) =  {
      val vrt.Int(top) = vt.pop
      val newPc: Int = keys.zip(targets).toMap.get(top).getOrElse(defaultTarget: Int)
      vt.frame.pc = newPc
    }
  }

  case object IReturn extends OpCode{ def op(vt: Thread) =  vt.returnVal(Some(vt.frame.stack.pop)) }
  case object LReturn extends OpCode{ def op(vt: Thread) =  vt.returnVal(Some(vt.frame.stack.pop)) }
  case object FReturn extends OpCode{ def op(vt: Thread) =  vt.returnVal(Some(vt.frame.stack.pop)) }
  case object DReturn extends OpCode{ def op(vt: Thread) =  vt.returnVal(Some(vt.frame.stack.pop)) }
  case object AReturn extends OpCode{ def op(vt: Thread) =  vt.returnVal(Some(vt.frame.stack.pop)) }
  case object Return extends OpCode{ def op(vt: Thread) =  vt.returnVal(None) }

  case class GetStatic(owner: Type.Cls, name: String, desc: Type) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode(
      Optimized.GetStatic(owner.cls(vt.vm).resolveStatic(owner, name))
    )

  }
  case class PutStatic(owner: Type.Cls, name: String, desc: Type) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode(
      Optimized.PutStatic(owner.cls(vt.vm).resolveStatic(owner, name))
    )

  }

  case class GetField(owner: Type.Cls, name: String, desc: Type) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode(
      Optimized.GetField(owner.cls(vt.vm).fieldList.lastIndexWhere(_.name == name))
    )
  }
  case class PutField(owner: Type.Cls, name: String, desc: Type) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode(
      Optimized.PutField(owner.cls(vt.vm).fieldList.lastIndexWhere(_.name == name))
    )

  }

  case class InvokeVirtual(owner: Type.Ref, name: String, desc: imm.Desc) extends OpCode{
    def op(vt: Thread) = {
      import vt.vm

      val index =
        owner
          .methodType
          .cls
          .methodList
          .indexWhere{ m => m.name == name && m.desc == desc }

      vt.swapOpCode(Optimized.InvokeVirtual(index, desc.args.length))
    }

  }

  def resolveDirectRef(owner: Type.Cls, name: String, desc: imm.Desc)(implicit vm: VM) = {
    val nativeId = vm.natives
      .trappedIndex
      .indexWhere(m => m._1._1.endsWith("/" + name) && m._1._2 == desc)

    val methodId = owner.cls
      .clsData
      .methods
      .indexWhere(m => m.name == name && m.desc == desc)

    if(nativeId != -1) Some(rt.Method.Native(nativeId))
    else if (methodId != -1) {
      if(owner.cls.clsData.methods(methodId).code.insns.length != 1)
        Some(owner.cls.methods(methodId))
      else
        None
    }else throw new Exception(s"Can't find method ${owner.unparse} $name ${desc.unparse}")

  }

  case class InvokeSpecial(owner: Type.Cls, name: String, desc: imm.Desc) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode{
      import vt.vm

      resolveDirectRef(owner, name, desc) match{
        case None => StackManip.Pop
        case Some(methodRef) => Optimized.InvokeSpecial(methodRef, desc.args.length)
      }

    }

  }

  case class InvokeStatic(owner: Type.Cls, name: String, desc: imm.Desc) extends OpCode{
    def op(vt: Thread) = vt.swapOpCode {
      import vt.vm
      resolveDirectRef(owner, name, desc) match{
        case None => StackManip.Pop
        case Some(methodRef) => Optimized.InvokeStatic(methodRef, desc.args.length)
      }
    }

  }

  case class InvokeInterface(owner: Type.Cls, name: String, desc: imm.Desc) extends OpCode{

    def op(vt: Thread) =  {
      import vt.vm
      val argCount = desc.args.length
      val args = vt.popArgs(argCount + 1)
      ensureNonNull(vt, args.head){
        val objType = args.head.cast[vrt.Ref].refType.methodType
        val cls = vm.ClsTable(objType)
        vt.prepInvoke(
          cls.methodMap.getOrElseUpdate((name, desc),
            cls.methodList
               .find(m => m.name == name && m.desc == desc)
               .get
          )
          ,
          args
        )
      }
    }
  }

  case class InvokeDynamic(name: String, desc: String, bsm: Object, args: Object) extends OpCode{ def op(vt: Thread) = ??? }

  case class New(desc: Type.Cls) extends OpCode{
    def op(vt: Thread) = {
      vt.vm.ClsTable(desc)
      vt.swapOpCode(
        Optimized.New(vt.vm.ClsTable.clsIndex.indexWhere(_.clsData.tpe == desc))
      )
    }

  }
  case class NewArray(typeCode: Int) extends OpCode{
    def op(vt: Thread) =  {
      val vrt.Int(count) = vt.pop

      val newArray = typeCode match{
        case 4  => vrt.Arr.Prim[Boolean](count)
        case 5  => vrt.Arr.Prim[Char](count)
        case 6  => vrt.Arr.Prim[Float](count)
        case 7  => vrt.Arr.Prim[Double](count)
        case 8  => vrt.Arr.Prim[Byte](count)
        case 9  => vrt.Arr.Prim[Short](count)
        case 10 => vrt.Arr.Prim[Int](count)
        case 11 => vrt.Arr.Prim[Long](count)
      }
      vt.push(newArray)
    }
  }
  case class ANewArray(desc: imm.Type.Ref) extends OpCode{
    def op(vt: Thread) =  {
      val vrt.Int(count) = vt.pop
      vt.push(vrt.Arr.Obj(desc, count))
    }
  }

  case object ArrayLength extends OpCode{
    def op(vt: Thread) =  {
      vt.push(vt.pop.asInstanceOf[vrt.Arr].backing.length)
    }
  }

  case object AThrow extends OpCode{
    def op(vt: Thread) =  {
      vt.throwException(vt.pop.asInstanceOf[vrt.Obj])
    }
  }
  case class CheckCast(desc: Type) extends OpCode{
    def op(vt: Thread) =  {
      import vt._

      val top = vt.pop
      vt.push(top)
      top match{
        case vrt.Null => ()
        case vrt.Unit => ()
        case (top: vrt.Ref with vrt.StackVal) if !check(top.refType, desc) =>
          vt.throwException(
            vrt.Obj("java/lang/ClassCastException",
              "detailMessage" -> s"${top.refType.unparse} cannot be converted to ${desc.unparse}"
            )
          )
        case _ => ()
      }
    }
  }
  def check(s: imm.Type, t: imm.Type)(implicit vm: VM): Boolean = {
    (s, t) match{

      case (s: Type.Cls, t: Type.Cls) => s.cls.typeAncestry.contains(t)
      case (s: Type.Arr, Type.Cls("java/lang/Object")) => true
      case (s: Type.Arr, Type.Cls("java/lang/Cloneable")) => true
      case (s: Type.Arr, Type.Cls("java/io/Serializable")) => true
      case (Type.Arr(Type.Prim(a)), Type.Arr(Type.Prim(b))) => a == b
      case (Type.Arr(sc: Type), Type.Arr(tc: Type)) => check(sc, tc)
      case _ => false
    }
  }
  case class InstanceOf(desc: Type) extends OpCode{
    def op(vt: Thread) = {

      import vt._
      import vm._
      val res = vt.pop match{
        case vrt.Null => 0
        case obj: vrt.Ref => if (check(obj.refType, desc)) 1 else 0
      }

      vt.push(res)
    }
  }
  case object MonitorEnter extends OpCode{
    def op(vt: Thread) = vt.pop
  }
  case object MonitorExit extends OpCode{
    def op(vt: Thread) = vt.pop
  }

  // Not used, because ASM folds these into the following bytecode for us
  //===============================================================
  val Wide = UnusedOpCode
  //===============================================================

  case class MultiANewArray(desc: Type.Arr, dims: Int) extends OpCode{
    def op(vt: Thread) =  {
      def rec(dims: List[Int], tpe: Type): vrt.Arr = {

        (dims, tpe) match {
          case (size :: Nil, Type.Arr(Type.Prim(c))) =>
            imm.Type.Prim.Info.charMap(c).newVirtArray(size)

          case (size :: Nil, Type.Arr(innerType: imm.Type.Ref)) =>
            new vrt.Arr.Obj(innerType, Array.fill[vrt.Val](size)(innerType.default))

          case (size :: tail, Type.Arr(innerType: imm.Type.Ref)) =>
            new vrt.Arr.Obj(innerType, Array.fill[vrt.Val](size)(rec(tail, innerType)))
        }
      }
      val (dimValues, newStack) = vt.frame.stack.splitAt(dims)
      val dimArray = dimValues.map(x => x.asInstanceOf[vrt.Int].v).toList
      val array = rec(dimArray, desc)
      vt.push(array)
    }
  }

  case class IfNull(label: Int) extends OpCode{
    def op(vt: Thread) =  {
      if (vt.pop == vrt.Null) vt.frame.pc = label
    }
  }

  case class IfNonNull(label: Int) extends OpCode{
    def op(vt: Thread) =  {
      if (vt.pop != vrt.Null) vt.frame.pc = label
    }
  }

  // Not used, because ASM converts these to normal Goto()s and Jsr()s
  //===============================================================
  val GotoW = UnusedOpCode
  val JsrW = UnusedOpCode
  //===============================================================

}
