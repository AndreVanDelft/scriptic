/* This file is part of Sawa, the Scriptic-Java compiler
 *
 * Sawa is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scriptic.tools.lowlevel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Instruction implements ClassFileConstants
{
  public int opc;
  public InstructionOwner   owner;
  InstructionOperand operand;
  int pc = -1;
  int index;

         int getReferenceCount() {return -1;}
  public int currentStackSize = -100; // at the time when this was created...for checking
  public void setCurrentStackSize(int currentStackSize) {this.currentStackSize = currentStackSize;}
  String currentStackSizeString() {return String.valueOf(currentStackSize);}

  void resolve(ClassEnvironment e)
  { if (operand != null) {operand.resolve(e);}}

  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
try{
    if (opc == INSTRUCTION_nop
    ||  opc == INSTRUCTION_deleted
    ||  opc == INSTRUCTION_label ) return;

    if (operand != null) operand.writePrefix(e, ce, out);
                             out.writeByte  ((byte) opc);
    if (operand != null) operand.write      (e, ce, out);
}catch(RuntimeException re) {
System.out.println("@write: "+getPresentation());
}
  }
  int size(ClassEnvironment e, CodeAttribute ce) throws ByteCodingException
  {
    if (opc == INSTRUCTION_nop 
    ||  opc == INSTRUCTION_deleted) return 0;
    if (operand == null) return 1;
    return (1 + operand.size(e, ce));
  }

  public boolean   isUnusedLabel() {return false;}
  public boolean isTryStartLabel() {return false;}
  public boolean   isTryEndLabel() {return false;}
  public boolean    isCatchLabel() {return false;}
  public static String indentString () {return "        ";}

  public String getPresentation () {return getPresentation(null);}
  public String getPresentation (ClassEnvironment e) {
     StringBuffer result = new StringBuffer();
     result.append(index);
     for (int i=result.length(); i<indentString().length(); i++)
     result.append (' ');
     if (pc >= 0) {
         String posString = String.valueOf(pc);
         for (int i=0; i<3-posString.length(); i++) result.append(' ');
         result.append (pc)
               .append(' ');
     }

     String s = currentStackSizeString();
     if (s!=null) {
       for (int i=s.length(); i<4; i++) result.append(' ');
       result.append (s);
       result.append(' ');
     }
     String opcString = Integer.toString (opc,16);
     if (opcString.length()<2) result.append(' ');
     result.append (opcString);

     result.append(' ');
     result.append (INSTRUCTION_String[opc]);
     if (operand != null) {
        result.append(' ');
        result.append (operand.getPresentation(this));
        String operandComment = operand.getCommentString(e);
        if (operandComment != null
        &&  operandComment.length() > 0) {
          for (int i=0; i<25-INSTRUCTION_String[opc].length(); i++) result.append(' ');
          result.append ("// ").append (operandComment);
        }
     }
     return result.toString();
  }

   boolean isLoadFalse() {return opc==INSTRUCTION_iconst_0;}
   boolean isLoadTrue () {return opc==INSTRUCTION_iconst_1;}
   LabelInstruction getLabelTarget () {
      if (operand == null) return null;
      return operand.getTarget();
   }

   boolean isIfInstruction() {
      switch (opc) {
      case INSTRUCTION_if_acmpne:
      case INSTRUCTION_if_acmpeq:
      case INSTRUCTION_if_icmpge:
      case INSTRUCTION_if_icmple:
      case INSTRUCTION_if_icmpgt:
      case INSTRUCTION_if_icmplt:
      case INSTRUCTION_if_icmpne:
      case INSTRUCTION_if_icmpeq:
      case INSTRUCTION_ifge:
      case INSTRUCTION_ifgt:
      case INSTRUCTION_ifne:
      case INSTRUCTION_ifle:
      case INSTRUCTION_iflt:
      case INSTRUCTION_ifeq:
      case INSTRUCTION_ifnull:
      case INSTRUCTION_ifnonnull: return  true;
      default:                    return false;
      }
   }
   void negateIfCode() {
      switch (opc) {
      case INSTRUCTION_if_acmpne: opc = INSTRUCTION_if_acmpeq; break;
      case INSTRUCTION_if_acmpeq: opc = INSTRUCTION_if_acmpne; break;
      case INSTRUCTION_if_icmpge: opc = INSTRUCTION_if_icmplt; break;
      case INSTRUCTION_if_icmple: opc = INSTRUCTION_if_icmpgt; break;
      case INSTRUCTION_if_icmpgt: opc = INSTRUCTION_if_icmple; break;
      case INSTRUCTION_if_icmplt: opc = INSTRUCTION_if_icmpge; break;
      case INSTRUCTION_if_icmpne: opc = INSTRUCTION_if_icmpeq; break;
      case INSTRUCTION_if_icmpeq: opc = INSTRUCTION_if_icmpne; break;
      case INSTRUCTION_ifge:      opc = INSTRUCTION_iflt;      break;
      case INSTRUCTION_ifgt:      opc = INSTRUCTION_ifle;      break;
      case INSTRUCTION_ifne:      opc = INSTRUCTION_ifeq;      break;
      case INSTRUCTION_ifle:      opc = INSTRUCTION_ifgt;      break;
      case INSTRUCTION_iflt:      opc = INSTRUCTION_ifge;      break;
      case INSTRUCTION_ifeq:      opc = INSTRUCTION_ifne;      break;
      case INSTRUCTION_ifnull:    opc = INSTRUCTION_ifnonnull; break;
      case INSTRUCTION_ifnonnull: opc = INSTRUCTION_ifnull;    break;
      }
   }
   boolean continuesWithNext() {
      switch (opc) {
      case INSTRUCTION_goto   : 
      case INSTRUCTION_goto_w : 
      case INSTRUCTION_ret    : 
      case INSTRUCTION_athrow : 
      case INSTRUCTION_return : 
      case INSTRUCTION_ireturn:
      case INSTRUCTION_lreturn:
      case INSTRUCTION_freturn:
      case INSTRUCTION_dreturn:
      case INSTRUCTION_areturn: return false;
      default:                  return true;
      }
   }

   boolean isReturn() {
      switch (opc) {
      case INSTRUCTION_return : 
      case INSTRUCTION_ireturn:
      case INSTRUCTION_lreturn:
      case INSTRUCTION_freturn:
      case INSTRUCTION_dreturn:
      case INSTRUCTION_areturn: return  true;
      default:                  return false;
      }
   }

   LabelInstruction[] getTargetLabels() {
     if (operand==null) return null;
     return operand.getTargetLabels();
   }
   static String INSTRUCTION_String[] = new String[258];
   static {
           INSTRUCTION_String[INSTRUCTION_bipush        ] = "bipush";
           INSTRUCTION_String[INSTRUCTION_sipush        ] = "sipush";
           INSTRUCTION_String[INSTRUCTION_ldc           ] = "ldc";
           INSTRUCTION_String[INSTRUCTION_ldc_w         ] = "ldc_w";
           INSTRUCTION_String[INSTRUCTION_ldc_2w        ] = "ldc_2w";
           INSTRUCTION_String[INSTRUCTION_aconst_null   ] = "aconst_null";
           INSTRUCTION_String[INSTRUCTION_iconst_m1     ] = "iconst_m1";
           INSTRUCTION_String[INSTRUCTION_iconst_0      ] = "iconst_0";
           INSTRUCTION_String[INSTRUCTION_iconst_1      ] = "iconst_1";
           INSTRUCTION_String[INSTRUCTION_iconst_2      ] = "iconst_2";
           INSTRUCTION_String[INSTRUCTION_iconst_3      ] = "iconst_3";
           INSTRUCTION_String[INSTRUCTION_iconst_4      ] = "iconst_4";
           INSTRUCTION_String[INSTRUCTION_iconst_5      ] = "iconst_5";
           INSTRUCTION_String[INSTRUCTION_lconst_0      ] = "lconst_0";
           INSTRUCTION_String[INSTRUCTION_lconst_1      ] = "lconst_1";
           INSTRUCTION_String[INSTRUCTION_fconst_0      ] = "fconst_0";
           INSTRUCTION_String[INSTRUCTION_fconst_1      ] = "fconst_1";
           INSTRUCTION_String[INSTRUCTION_fconst_2      ] = "fconst_2";
           INSTRUCTION_String[INSTRUCTION_dconst_0      ] = "dconst_0";
           INSTRUCTION_String[INSTRUCTION_dconst_1      ] = "dconst_1";
           INSTRUCTION_String[INSTRUCTION_iload         ] = "iload";
           INSTRUCTION_String[INSTRUCTION_iload_0       ] = "iload_0";
           INSTRUCTION_String[INSTRUCTION_iload_1       ] = "iload_1";
           INSTRUCTION_String[INSTRUCTION_iload_2       ] = "iload_2";
           INSTRUCTION_String[INSTRUCTION_iload_3       ] = "iload_3";
           INSTRUCTION_String[INSTRUCTION_lload         ] = "lload";
           INSTRUCTION_String[INSTRUCTION_lload_0       ] = "lload_0";
           INSTRUCTION_String[INSTRUCTION_lload_1       ] = "lload_1";
           INSTRUCTION_String[INSTRUCTION_lload_2       ] = "lload_2";
           INSTRUCTION_String[INSTRUCTION_lload_3       ] = "lload_3";
           INSTRUCTION_String[INSTRUCTION_fload         ] = "fload";
           INSTRUCTION_String[INSTRUCTION_fload_0       ] = "fload_0";
           INSTRUCTION_String[INSTRUCTION_fload_1       ] = "fload_1";
           INSTRUCTION_String[INSTRUCTION_fload_2       ] = "fload_2";
           INSTRUCTION_String[INSTRUCTION_fload_3       ] = "fload_3";
           INSTRUCTION_String[INSTRUCTION_dload         ] = "dload";
           INSTRUCTION_String[INSTRUCTION_dload_0       ] = "dload_0";
           INSTRUCTION_String[INSTRUCTION_dload_1       ] = "dload_1";
           INSTRUCTION_String[INSTRUCTION_dload_2       ] = "dload_2";
           INSTRUCTION_String[INSTRUCTION_dload_3       ] = "dload_3";
           INSTRUCTION_String[INSTRUCTION_aload         ] = "aload";
           INSTRUCTION_String[INSTRUCTION_aload_0       ] = "aload_0";
           INSTRUCTION_String[INSTRUCTION_aload_1       ] = "aload_1";
           INSTRUCTION_String[INSTRUCTION_aload_2       ] = "aload_2";
           INSTRUCTION_String[INSTRUCTION_aload_3       ] = "aload_3";
           INSTRUCTION_String[INSTRUCTION_istore        ] = "istore";
           INSTRUCTION_String[INSTRUCTION_istore_0      ] = "istore_0";
           INSTRUCTION_String[INSTRUCTION_istore_1      ] = "istore_1";
           INSTRUCTION_String[INSTRUCTION_istore_2      ] = "istore_2";
           INSTRUCTION_String[INSTRUCTION_istore_3      ] = "istore_3";
           INSTRUCTION_String[INSTRUCTION_lstore        ] = "lstore";
           INSTRUCTION_String[INSTRUCTION_lstore_0      ] = "lstore_0";
           INSTRUCTION_String[INSTRUCTION_lstore_1      ] = "lstore_1";
           INSTRUCTION_String[INSTRUCTION_lstore_2      ] = "lstore_2";
           INSTRUCTION_String[INSTRUCTION_lstore_3      ] = "lstore_3";
           INSTRUCTION_String[INSTRUCTION_fstore        ] = "fstore";
           INSTRUCTION_String[INSTRUCTION_fstore_0      ] = "fstore_0";
           INSTRUCTION_String[INSTRUCTION_fstore_1      ] = "fstore_1";
           INSTRUCTION_String[INSTRUCTION_fstore_2      ] = "fstore_2";
           INSTRUCTION_String[INSTRUCTION_fstore_3      ] = "fstore_3";
           INSTRUCTION_String[INSTRUCTION_dstore        ] = "dstore";
           INSTRUCTION_String[INSTRUCTION_dstore_0      ] = "dstore_0";
           INSTRUCTION_String[INSTRUCTION_dstore_1      ] = "dstore_1";
           INSTRUCTION_String[INSTRUCTION_dstore_2      ] = "dstore_2";
           INSTRUCTION_String[INSTRUCTION_dstore_3      ] = "dstore_3";
           INSTRUCTION_String[INSTRUCTION_astore        ] = "astore";
           INSTRUCTION_String[INSTRUCTION_astore_0      ] = "astore_0";
           INSTRUCTION_String[INSTRUCTION_astore_1      ] = "astore_1";
           INSTRUCTION_String[INSTRUCTION_astore_2      ] = "astore_2";
           INSTRUCTION_String[INSTRUCTION_astore_3      ] = "astore_3";
           INSTRUCTION_String[INSTRUCTION_iinc          ] = "iinc";
           INSTRUCTION_String[INSTRUCTION_newarray      ] = "newarray";
           INSTRUCTION_String[INSTRUCTION_anewarray     ] = "anewarray";
           INSTRUCTION_String[INSTRUCTION_multianewarray] = "multianewarray";
           INSTRUCTION_String[INSTRUCTION_arraylength   ] = "arraylength";
           INSTRUCTION_String[INSTRUCTION_iaload        ] = "iaload";
           INSTRUCTION_String[INSTRUCTION_laload        ] = "laload";
           INSTRUCTION_String[INSTRUCTION_faload        ] = "faload";
           INSTRUCTION_String[INSTRUCTION_daload        ] = "daload";
           INSTRUCTION_String[INSTRUCTION_aaload        ] = "aaload";
           INSTRUCTION_String[INSTRUCTION_baload        ] = "baload";
           INSTRUCTION_String[INSTRUCTION_caload        ] = "caload";
           INSTRUCTION_String[INSTRUCTION_saload        ] = "saload";
           INSTRUCTION_String[INSTRUCTION_iastore       ] = "iastore";
           INSTRUCTION_String[INSTRUCTION_lastore       ] = "lastore";
           INSTRUCTION_String[INSTRUCTION_fastore       ] = "fastore";
           INSTRUCTION_String[INSTRUCTION_dastore       ] = "dastore";
           INSTRUCTION_String[INSTRUCTION_aastore       ] = "aastore";
           INSTRUCTION_String[INSTRUCTION_bastore       ] = "bastore";
           INSTRUCTION_String[INSTRUCTION_castore       ] = "castore";
           INSTRUCTION_String[INSTRUCTION_sastore       ] = "sastore";
           INSTRUCTION_String[INSTRUCTION_nop           ] = "nop";
           INSTRUCTION_String[INSTRUCTION_pop           ] = "pop";
           INSTRUCTION_String[INSTRUCTION_pop2          ] = "pop2";
           INSTRUCTION_String[INSTRUCTION_dup           ] = "dup";
           INSTRUCTION_String[INSTRUCTION_dup2          ] = "dup2";
           INSTRUCTION_String[INSTRUCTION_dup_x1        ] = "dup_x1";
           INSTRUCTION_String[INSTRUCTION_dup2_x1       ] = "dup2_x1";
           INSTRUCTION_String[INSTRUCTION_dup_x2        ] = "dup_x2";
           INSTRUCTION_String[INSTRUCTION_dup2_x2       ] = "dup2_x2";
           INSTRUCTION_String[INSTRUCTION_swap          ] = "swap";
           INSTRUCTION_String[INSTRUCTION_iadd          ] = "iadd";
           INSTRUCTION_String[INSTRUCTION_ladd          ] = "ladd";
           INSTRUCTION_String[INSTRUCTION_fadd          ] = "fadd";
           INSTRUCTION_String[INSTRUCTION_dadd          ] = "dadd";
           INSTRUCTION_String[INSTRUCTION_isub          ] = "isub";
           INSTRUCTION_String[INSTRUCTION_lsub          ] = "lsub";
           INSTRUCTION_String[INSTRUCTION_fsub          ] = "fsub";
           INSTRUCTION_String[INSTRUCTION_dsub          ] = "dsub";
           INSTRUCTION_String[INSTRUCTION_imul          ] = "imul";
           INSTRUCTION_String[INSTRUCTION_lmul          ] = "lmul";
           INSTRUCTION_String[INSTRUCTION_fmul          ] = "fmul";
           INSTRUCTION_String[INSTRUCTION_dmul          ] = "dmul";
           INSTRUCTION_String[INSTRUCTION_idiv          ] = "idiv";
           INSTRUCTION_String[INSTRUCTION_ldiv          ] = "ldiv";
           INSTRUCTION_String[INSTRUCTION_fdiv          ] = "fdiv";
           INSTRUCTION_String[INSTRUCTION_ddiv          ] = "ddiv";
           INSTRUCTION_String[INSTRUCTION_irem          ] = "irem";
           INSTRUCTION_String[INSTRUCTION_lrem          ] = "lrem";
           INSTRUCTION_String[INSTRUCTION_frem          ] = "frem";
           INSTRUCTION_String[INSTRUCTION_drem          ] = "drem";
           INSTRUCTION_String[INSTRUCTION_ineg          ] = "ineg";
           INSTRUCTION_String[INSTRUCTION_lneg          ] = "lneg";
           INSTRUCTION_String[INSTRUCTION_fneg          ] = "fneg";
           INSTRUCTION_String[INSTRUCTION_dneg          ] = "dneg";
           INSTRUCTION_String[INSTRUCTION_ishl          ] = "ishl";
           INSTRUCTION_String[INSTRUCTION_ishr          ] = "ishr";
           INSTRUCTION_String[INSTRUCTION_iushr         ] = "iushr";
           INSTRUCTION_String[INSTRUCTION_lshl          ] = "lshl";
           INSTRUCTION_String[INSTRUCTION_lshr          ] = "lshr";
           INSTRUCTION_String[INSTRUCTION_lushr         ] = "lushr";
           INSTRUCTION_String[INSTRUCTION_iand          ] = "iand";
           INSTRUCTION_String[INSTRUCTION_land          ] = "land";
           INSTRUCTION_String[INSTRUCTION_ior           ] = "ior";
           INSTRUCTION_String[INSTRUCTION_lor           ] = "lor";
           INSTRUCTION_String[INSTRUCTION_ixor          ] = "ixor";
           INSTRUCTION_String[INSTRUCTION_lxor          ] = "lxor";
           INSTRUCTION_String[INSTRUCTION_i2l           ] = "i2l";
           INSTRUCTION_String[INSTRUCTION_i2f           ] = "i2f";
           INSTRUCTION_String[INSTRUCTION_i2d           ] = "i2d";
           INSTRUCTION_String[INSTRUCTION_l2i           ] = "l2i";
           INSTRUCTION_String[INSTRUCTION_l2f           ] = "l2f";
           INSTRUCTION_String[INSTRUCTION_l2d           ] = "l2d";
           INSTRUCTION_String[INSTRUCTION_f2i           ] = "f2i";
           INSTRUCTION_String[INSTRUCTION_f2l           ] = "f2l";
           INSTRUCTION_String[INSTRUCTION_f2d           ] = "f2d";
           INSTRUCTION_String[INSTRUCTION_d2i           ] = "d2i";
           INSTRUCTION_String[INSTRUCTION_d2l           ] = "d2l";
           INSTRUCTION_String[INSTRUCTION_d2f           ] = "d2f";
           INSTRUCTION_String[INSTRUCTION_int2byte      ] = "int2byte";
           INSTRUCTION_String[INSTRUCTION_int2char      ] = "int2char";
           INSTRUCTION_String[INSTRUCTION_int2short     ] = "int2short";
           INSTRUCTION_String[INSTRUCTION_ifeq          ] = "ifeq";
           INSTRUCTION_String[INSTRUCTION_iflt          ] = "iflt";
           INSTRUCTION_String[INSTRUCTION_ifle          ] = "ifle";
           INSTRUCTION_String[INSTRUCTION_ifne          ] = "ifne";
           INSTRUCTION_String[INSTRUCTION_ifgt          ] = "ifgt";
           INSTRUCTION_String[INSTRUCTION_ifge          ] = "ifge";
           INSTRUCTION_String[INSTRUCTION_if_icmpeq     ] = "if_icmpeq";
           INSTRUCTION_String[INSTRUCTION_if_icmpne     ] = "if_icmpne";
           INSTRUCTION_String[INSTRUCTION_if_icmplt     ] = "if_icmplt";
           INSTRUCTION_String[INSTRUCTION_if_icmpgt     ] = "if_icmpgt";
           INSTRUCTION_String[INSTRUCTION_if_icmple     ] = "if_icmple";
           INSTRUCTION_String[INSTRUCTION_if_icmpge     ] = "if_icmpge";
           INSTRUCTION_String[INSTRUCTION_lcmp          ] = "lcmp";
           INSTRUCTION_String[INSTRUCTION_fcmpl         ] = "fcmpl";
           INSTRUCTION_String[INSTRUCTION_fcmpg         ] = "fcmpg";
           INSTRUCTION_String[INSTRUCTION_dcmpl         ] = "dcmpl";
           INSTRUCTION_String[INSTRUCTION_dcmpg         ] = "dcmpg";
           INSTRUCTION_String[INSTRUCTION_if_acmpeq     ] = "if_acmpeq";
           INSTRUCTION_String[INSTRUCTION_if_acmpne     ] = "if_acmpne";
           INSTRUCTION_String[INSTRUCTION_goto          ] = "goto";
           INSTRUCTION_String[INSTRUCTION_jsr           ] = "jsr";
           INSTRUCTION_String[INSTRUCTION_ret           ] = "ret";
           INSTRUCTION_String[INSTRUCTION_ireturn       ] = "ireturn";
           INSTRUCTION_String[INSTRUCTION_lreturn       ] = "lreturn";
           INSTRUCTION_String[INSTRUCTION_freturn       ] = "freturn";
           INSTRUCTION_String[INSTRUCTION_dreturn       ] = "dreturn";
           INSTRUCTION_String[INSTRUCTION_areturn       ] = "areturn";
           INSTRUCTION_String[INSTRUCTION_return        ] = "return";
           INSTRUCTION_String[INSTRUCTION_tableswitch   ] = "tableswitch";
           INSTRUCTION_String[INSTRUCTION_lookupswitch  ] = "lookupswitch";
           INSTRUCTION_String[INSTRUCTION_putfield      ] = "putfield";
           INSTRUCTION_String[INSTRUCTION_getfield      ] = "getfield";
           INSTRUCTION_String[INSTRUCTION_putstatic     ] = "putstatic";
           INSTRUCTION_String[INSTRUCTION_getstatic     ] = "getstatic";
           INSTRUCTION_String[INSTRUCTION_invokevirtual ] = "invokevirtual";
           INSTRUCTION_String[INSTRUCTION_invokespecial ] = "invokespecial";
           INSTRUCTION_String[INSTRUCTION_invokestatic  ] = "invokestatic";
           INSTRUCTION_String[INSTRUCTION_invokeinterface] = "invokeinterface";
           INSTRUCTION_String[INSTRUCTION_athrow        ] = "athrow";
           INSTRUCTION_String[INSTRUCTION_new           ] = "new";
           INSTRUCTION_String[INSTRUCTION_checkcast     ] = "checkcast";
           INSTRUCTION_String[INSTRUCTION_instanceof    ] = "instanceof";
           INSTRUCTION_String[INSTRUCTION_monitorenter  ] = "monitorenter";
           INSTRUCTION_String[INSTRUCTION_monitorexit   ] = "monitorexit";
           INSTRUCTION_String[INSTRUCTION_breakpoint    ] = "breakpoint";
           INSTRUCTION_String[INSTRUCTION_wide          ] = "wide";
           INSTRUCTION_String[INSTRUCTION_ifnull        ] = "ifnull";
           INSTRUCTION_String[INSTRUCTION_ifnonnull     ] = "ifnonnull";
           INSTRUCTION_String[INSTRUCTION_goto_w        ] = "goto_w";
           INSTRUCTION_String[INSTRUCTION_jsr_w         ] = "jsr_w";
           INSTRUCTION_String[INSTRUCTION_label         ] = "LABEL";
           INSTRUCTION_String[INSTRUCTION_deleted       ] = "DELETED";
   }

                                // private constructor, for the 
                                // "strange" opcodes
  Instruction() {}
  Instruction(InstructionOwner owner) {this.owner = owner;}

  /**
   * Instructions with no arguments are built with
   * this constructor.
   */
  public Instruction(int opc)                         throws ByteCodingException {this (opc,(InstructionOwner)null);}
  public Instruction(int opc, InstructionOwner owner) throws ByteCodingException
  {
    this.owner = owner;
    //if (owner==null) new Exception("instruction.owner==null: "+getPresentation()).printStackTrace();
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    if (INSTRUCTION_Length[opc] > 1)
      throw new ByteCodingException
        (INSTRUCTION_String[opc] + " cannot be used without more parameters");
    operand  = null;
    this.opc = opc;
  }

  /**
   * Instructions with a known operand are built with
   * this constructor (e.g., when reading from an input stream).
   * operand may be null; it should then be set later...
   */
  public Instruction(int opc, InstructionOperand operand) throws ByteCodingException {this (opc,operand,null);}
  public Instruction(int opc, InstructionOperand operand,InstructionOwner owner)
    throws ByteCodingException
  {
    this.owner = owner;
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    if (INSTRUCTION_Length[opc] == 1)
      throw new ByteCodingException
        (INSTRUCTION_String[opc] + " cannot be used with parameters");
      this.operand = operand;
      this.opc     = opc;
  }

  /**
   * Instructions that take a single numeric argument. These are
   * INSTRUCTION_bipush,
   * INSTRUCTION_sipush,
   * INSTRUCTION_ret,
   * INSTRUCTION_iload,
   * INSTRUCTION_lload,
   * INSTRUCTION_fload,
   * INSTRUCTION_dload,
   * INSTRUCTION_aload,
   * INSTRUCTION_istore,
   * INSTRUCTION_lstore,
   * INSTRUCTION_fstore,
   * INSTRUCTION_dstore,
   * INSTRUCTION_astore,
   * INSTRUCTION_newarray
   *
   * Note that an extra wide prefix is automatically added
   * for the following instructions if the numeric argument
   * is larger than 256. Also note that while the spec makes
   * no mention of INSTRUCTION_ret as being a "wideable" opcode, thats
   * how the VM is implemented.
   *
   * INSTRUCTION_ret:
   * INSTRUCTION_iload:
   * INSTRUCTION_lload:
   * INSTRUCTION_fload:
   * INSTRUCTION_dload:
   * INSTRUCTION_aload:
   * INSTRUCTION_istore:
   * INSTRUCTION_lstore:
   * INSTRUCTION_fstore:
   * INSTRUCTION_dstore:
   * INSTRUCTION_astore:
   * 
   */

  public Instruction(int opc, int val) throws ByteCodingException {this (opc,val,null);}
  public Instruction(int opc, int val,InstructionOwner owner)
    throws ByteCodingException
  {
    this.owner = owner;
    this.opc = opc;
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    switch (opc)
      {
      case INSTRUCTION_bipush:   operand = new         ByteOperand(val); break;
      case INSTRUCTION_sipush:   operand = new        ShortOperand(val); break;
      case INSTRUCTION_newarray: operand = new UnsignedByteOperand(val); break;
      case INSTRUCTION_ret:
      case INSTRUCTION_iload:
      case INSTRUCTION_lload:
      case INSTRUCTION_fload:
      case INSTRUCTION_dload:
      case INSTRUCTION_aload:
      case INSTRUCTION_istore:
      case INSTRUCTION_lstore:
      case INSTRUCTION_fstore:
      case INSTRUCTION_dstore:
      case INSTRUCTION_astore:   operand = new UnsignedByteWideOperand(val); break;
      default: throw new ByteCodingException
          (INSTRUCTION_String[opc] + " does not take a numeric argument");
      }
  }
  /**
   * Instructions that take a Label as an argument. These are
   * INSTRUCTION_jsr,
   * INSTRUCTION_goto,
   * INSTRUCTION_if_acmpne,
   * INSTRUCTION_if_acmpeq,
   * INSTRUCTION_if_icmpge,
   * INSTRUCTION_if_icmple,
   * INSTRUCTION_if_icmpgt,
   * INSTRUCTION_if_icmplt,
   * INSTRUCTION_if_icmpne,
   * INSTRUCTION_if_icmpeq,
   * INSTRUCTION_ifge,
   * INSTRUCTION_ifgt,
   * INSTRUCTION_ifne,
   * INSTRUCTION_ifle,
   * INSTRUCTION_iflt,
   * INSTRUCTION_ifeq,
   * INSTRUCTION_ifnull,
   * INSTRUCTION_ifnonnull,
   * INSTRUCTION_goto_w,
   * INSTRUCTION_jsr_w
   */
  public Instruction(int opc, LabelInstruction target) throws ByteCodingException {this (opc,target,null);}
  public Instruction(int opc, LabelInstruction target,InstructionOwner owner)
    throws ByteCodingException
  {
    this.owner = owner;
    this.opc = opc;
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    switch(opc)
      {
      case INSTRUCTION_jsr:
      case INSTRUCTION_goto: 
      case INSTRUCTION_if_acmpne:
      case INSTRUCTION_if_acmpeq:
      case INSTRUCTION_if_icmpge:
      case INSTRUCTION_if_icmple:
      case INSTRUCTION_if_icmpgt:
      case INSTRUCTION_if_icmplt:
      case INSTRUCTION_if_icmpne:
      case INSTRUCTION_if_icmpeq:
      case INSTRUCTION_ifge:
      case INSTRUCTION_ifgt:
      case INSTRUCTION_ifne:
      case INSTRUCTION_ifle:
      case INSTRUCTION_iflt:
      case INSTRUCTION_ifeq:
      case INSTRUCTION_ifnull:
      case INSTRUCTION_ifnonnull: operand = new LabelOperand(target, this, false); break;
      case INSTRUCTION_goto_w:
      case INSTRUCTION_jsr_w:     operand = new LabelOperand(target, this, true); break;
      default: throw new ByteCodingException
          (INSTRUCTION_String[opc] + " does not take a label as its argument");
      }
  }
  /**
   * This constructor is used for instructions that take a ConstantPoolItem item
   * as their argument. These are
   * INSTRUCTION_anewarray,
   * INSTRUCTION_ldc_w,
   * INSTRUCTION_ldc2_w,
   * INSTRUCTION_new,
   * INSTRUCTION_checkcast,
   * INSTRUCTION_instanceof,
   * INSTRUCTION_getstatic,
   * INSTRUCTION_putstatic,
   * INSTRUCTION_getfield,
   * INSTRUCTION_putfield,
   * INSTRUCTION_ldc
   */
  public Instruction(int opc, ConstantPoolItem arg) throws ByteCodingException {this (opc,arg,null);}
  public Instruction(int opc, ConstantPoolItem arg,InstructionOwner owner)
    throws ByteCodingException
  {
    this.owner = owner;
    this.opc = opc;
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    switch(opc)
      {
      case INSTRUCTION_anewarray:
      case INSTRUCTION_ldc_w: 
      case INSTRUCTION_ldc_2w:
      case INSTRUCTION_new:
      case INSTRUCTION_checkcast:
      case INSTRUCTION_instanceof: operand = new CPOperand(arg);        break;
      case INSTRUCTION_ldc:        operand = new CPOperand(arg, false); break;

      case INSTRUCTION_getstatic:
      case INSTRUCTION_putstatic:
      case INSTRUCTION_getfield:
      case INSTRUCTION_putfield:  throw new ByteCodingException
          (INSTRUCTION_String[opc] + " is not a normal Instruction, but a FieldInstruction instead");

      default: throw new ByteCodingException
          (INSTRUCTION_String[opc] + " does not take a ConstantPoolItem item as an argument");
      }
  }
  boolean hasLabelOperand() {return opcodeHasLabelOperand(opc);}
  static boolean opcodeHasLabelOperand (int opc) {
    switch(opc)
      {
      case INSTRUCTION_jsr:
      case INSTRUCTION_goto: 
      case INSTRUCTION_if_acmpne:
      case INSTRUCTION_if_acmpeq:
      case INSTRUCTION_if_icmpge:
      case INSTRUCTION_if_icmple:
      case INSTRUCTION_if_icmpgt:
      case INSTRUCTION_if_icmplt:
      case INSTRUCTION_if_icmpne:
      case INSTRUCTION_if_icmpeq:
      case INSTRUCTION_ifge:
      case INSTRUCTION_ifgt:
      case INSTRUCTION_ifne:
      case INSTRUCTION_ifle:
      case INSTRUCTION_iflt:
      case INSTRUCTION_ifeq:
      case INSTRUCTION_ifnull:
      case INSTRUCTION_ifnonnull: 
      case INSTRUCTION_goto_w:
      case INSTRUCTION_jsr_w:     return true;
      default:                    return false;
      }
  }

  static Instruction readFrom (ClassEnvironment e, CodeAttribute a, DataInputStream in, LongHolder pos)
    throws IOException, ByteCodingException {

      byte        b;
      boolean isWide      = false;
      Instruction result  = null;

      long position = pos.value;
      b = in.readByte(); pos.value++; 
      if (b==INSTRUCTION_wide) {isWide=true; b = in.readByte(); pos.value++;}
      int opc = b<0? b+256: b;

      switch (opc) {
      // these have their own classes...
      case INSTRUCTION_tableswitch:    result=new     TableswitchInstruction(    TableswitchOperand.readFrom(e,in,pos)); break;
      case INSTRUCTION_lookupswitch:   result=new    LookupswitchInstruction(   LookupswitchOperand.readFrom(e,in,pos)); break;
      case INSTRUCTION_invokeinterface:result=new InvokeinterfaceInstruction(InvokeinterfaceOperand.readFrom(e,in,pos)); break;
      case INSTRUCTION_iinc:           result=new            IincInstruction(           IincOperand.readFrom(  in,pos, isWide)); break;
      case INSTRUCTION_multianewarray: result=new      MultiarrayInstruction(     MultiarrayOperand.readFrom(e,in,pos)); break;
      case INSTRUCTION_label:          result=new           LabelInstruction("LABEL UNKNOWN"); break;
      }
      if (INSTRUCTION_Length[opc] == 1) result = new Instruction (opc);

      if (result==null) {
        InstructionOperand operand = null;
  
        switch (opc) {
        case INSTRUCTION_bipush:   operand =         ByteOperand.readFrom (in, pos); break;
        case INSTRUCTION_sipush:   operand =        ShortOperand.readFrom (in, pos); break;
        case INSTRUCTION_newarray: operand = UnsignedByteOperand.readFrom (in, pos); break;
        case INSTRUCTION_ret:
        case INSTRUCTION_iload:
        case INSTRUCTION_lload:
        case INSTRUCTION_fload:
        case INSTRUCTION_dload:
        case INSTRUCTION_aload:
        case INSTRUCTION_istore:
        case INSTRUCTION_lstore:
        case INSTRUCTION_fstore:
        case INSTRUCTION_dstore:
        case INSTRUCTION_astore:   operand = UnsignedByteWideOperand.readFrom (in, pos, isWide); break;
        case INSTRUCTION_jsr:
        case INSTRUCTION_goto:     
        case INSTRUCTION_if_acmpne:
        case INSTRUCTION_if_acmpeq:
        case INSTRUCTION_if_icmpge:
        case INSTRUCTION_if_icmple:
        case INSTRUCTION_if_icmpgt:
        case INSTRUCTION_if_icmplt:
        case INSTRUCTION_if_icmpne:
        case INSTRUCTION_if_icmpeq:
        case INSTRUCTION_ifge:
        case INSTRUCTION_ifgt:
        case INSTRUCTION_ifne:
        case INSTRUCTION_ifle:
        case INSTRUCTION_iflt:
        case INSTRUCTION_ifeq:
        case INSTRUCTION_ifnull:
        case INSTRUCTION_ifnonnull: operand = LabelOperand.readFrom(in, pos, false); break;
        case INSTRUCTION_goto_w:
        case INSTRUCTION_jsr_w:     operand = LabelOperand.readFrom(in, pos, true); break;
        case INSTRUCTION_ldc_w:
        case INSTRUCTION_ldc_2w: 
        case INSTRUCTION_anewarray:
        case INSTRUCTION_invokespecial:
        case INSTRUCTION_invokestatic:
        case INSTRUCTION_invokevirtual:
        case INSTRUCTION_new:
        case INSTRUCTION_checkcast:
        case INSTRUCTION_instanceof:
        case INSTRUCTION_getstatic:
        case INSTRUCTION_putstatic:
        case INSTRUCTION_getfield:
        case INSTRUCTION_putfield:  operand = CPOperand.readFrom(e, in, pos,  true); break;
        case INSTRUCTION_ldc:       operand = CPOperand.readFrom(e, in, pos, false); break;
        }
        result = new Instruction (opc, operand);
      }
      result.pc = (int) position;
      return result;
  }

  public int deltaStackSize() {

      switch (opc) {

      case INSTRUCTION_ldc_2w      :
      case INSTRUCTION_lconst_0    : 
      case INSTRUCTION_lconst_1    : 
      case INSTRUCTION_dconst_0    : 
      case INSTRUCTION_dconst_1    : 

      case INSTRUCTION_lload       :  
      case INSTRUCTION_lload_0     :  
      case INSTRUCTION_lload_1     :  
      case INSTRUCTION_lload_2     : 
      case INSTRUCTION_lload_3     : 
      case INSTRUCTION_dload       : 
      case INSTRUCTION_dload_0     : 
      case INSTRUCTION_dload_1     : 
      case INSTRUCTION_dload_2     : 
      case INSTRUCTION_dload_3     : 

      case INSTRUCTION_dup2        : 
      case INSTRUCTION_dup2_x1     : 
      case INSTRUCTION_dup2_x2     : 

                                     return 2;

      case INSTRUCTION_bipush      :
      case INSTRUCTION_sipush      :
      case INSTRUCTION_ldc         :
      case INSTRUCTION_ldc_w       :
      case INSTRUCTION_aconst_null : 
      case INSTRUCTION_iconst_m1   : 
      case INSTRUCTION_iconst_0    : 
      case INSTRUCTION_iconst_1    : 
      case INSTRUCTION_iconst_2    : 
      case INSTRUCTION_iconst_3    : 
      case INSTRUCTION_iconst_4    : 
      case INSTRUCTION_iconst_5    : 
      case INSTRUCTION_fconst_0    : 
      case INSTRUCTION_fconst_1    : 
      case INSTRUCTION_fconst_2    : 

      case INSTRUCTION_iload       :  
      case INSTRUCTION_iload_0     :  
      case INSTRUCTION_iload_1     :  
      case INSTRUCTION_iload_2     :  
      case INSTRUCTION_iload_3     :  
      case INSTRUCTION_fload       : 
      case INSTRUCTION_fload_0     : 
      case INSTRUCTION_fload_1     : 
      case INSTRUCTION_fload_2     : 
      case INSTRUCTION_fload_3     : 
      case INSTRUCTION_aload       : 
      case INSTRUCTION_aload_0     : 
      case INSTRUCTION_aload_1     : 
      case INSTRUCTION_aload_2     : 
      case INSTRUCTION_aload_3     : 

      case INSTRUCTION_dup         : 
      case INSTRUCTION_dup_x1      : 
      case INSTRUCTION_dup_x2      : 

      case INSTRUCTION_i2l         : 
      case INSTRUCTION_i2d         : 
      case INSTRUCTION_f2l         : 
      case INSTRUCTION_f2d         : 

      case INSTRUCTION_jsr         : 
      case INSTRUCTION_jsr_w       : 

      case INSTRUCTION_new         : 

                                     return 1;

      case INSTRUCTION_iinc        : 

      case INSTRUCTION_newarray    : 
      case INSTRUCTION_anewarray   : 
      case INSTRUCTION_arraylength : 

      case INSTRUCTION_laload      : 
      case INSTRUCTION_daload      : 

      case INSTRUCTION_nop         :  
      case INSTRUCTION_swap        : 

      case INSTRUCTION_ineg        : 
      case INSTRUCTION_lneg        : 
      case INSTRUCTION_fneg        : 
      case INSTRUCTION_dneg        : 

      case INSTRUCTION_i2f         : 
      case INSTRUCTION_l2d         : 
      case INSTRUCTION_f2i         : 
      case INSTRUCTION_d2l         : 
      case INSTRUCTION_int2byte    : 
      case INSTRUCTION_int2char    : 
      case INSTRUCTION_int2short   : 

      case INSTRUCTION_goto        : 
      case INSTRUCTION_ret         : 
      case INSTRUCTION_return      : 
      case INSTRUCTION_wide        : 
      case INSTRUCTION_goto_w      : 
      case INSTRUCTION_athrow      : 

      case INSTRUCTION_checkcast   : 
      case INSTRUCTION_instanceof  : 

      case INSTRUCTION_label       : 

                                     return 0;


      case INSTRUCTION_istore      : 
      case INSTRUCTION_istore_0    : 
      case INSTRUCTION_istore_1    : 
      case INSTRUCTION_istore_2    : 
      case INSTRUCTION_istore_3    : 
      case INSTRUCTION_fstore      : 
      case INSTRUCTION_fstore_0    : 
      case INSTRUCTION_fstore_1    : 
      case INSTRUCTION_fstore_2    : 
      case INSTRUCTION_fstore_3    : 
      case INSTRUCTION_astore      : 
      case INSTRUCTION_astore_0    : 
      case INSTRUCTION_astore_1    : 
      case INSTRUCTION_astore_2    : 
      case INSTRUCTION_astore_3    : 

      case INSTRUCTION_iaload      : 
      case INSTRUCTION_faload      : 
      case INSTRUCTION_aaload      : 
      case INSTRUCTION_baload      : 
      case INSTRUCTION_caload      : 
      case INSTRUCTION_saload      : 

      case INSTRUCTION_pop         : 

      case INSTRUCTION_iadd        : 
      case INSTRUCTION_fadd        :
      case INSTRUCTION_isub        : 
      case INSTRUCTION_fsub        : 
      case INSTRUCTION_imul        : 
      case INSTRUCTION_fmul        : 
      case INSTRUCTION_idiv        : 
      case INSTRUCTION_fdiv        : 

      case INSTRUCTION_irem        : 
      case INSTRUCTION_frem        : 

      case INSTRUCTION_ishl        : 
      case INSTRUCTION_ishr        : 
      case INSTRUCTION_iushr       : 

      case INSTRUCTION_lshl        : 
      case INSTRUCTION_lshr        : 
      case INSTRUCTION_lushr       : 

      case INSTRUCTION_iand        : 
      case INSTRUCTION_ior         : 
      case INSTRUCTION_ixor        : 

      case INSTRUCTION_l2i         : 
      case INSTRUCTION_l2f         : 
      case INSTRUCTION_d2i         : 
      case INSTRUCTION_d2f         : 

      case INSTRUCTION_ifeq        : 
      case INSTRUCTION_iflt        : 
      case INSTRUCTION_ifle        : 
      case INSTRUCTION_ifne        : 
      case INSTRUCTION_ifgt        : 
      case INSTRUCTION_ifge        : 
      case INSTRUCTION_fcmpl       : 
      case INSTRUCTION_fcmpg       : 

      case INSTRUCTION_ireturn     : 
      case INSTRUCTION_freturn     : 
      case INSTRUCTION_areturn     : 

      case INSTRUCTION_ifnull      : 
      case INSTRUCTION_ifnonnull   : 

      case INSTRUCTION_monitorenter: 
      case INSTRUCTION_monitorexit :
 
                                     return -1;

      case INSTRUCTION_lstore      : 
      case INSTRUCTION_lstore_0    : 
      case INSTRUCTION_lstore_1    : 
      case INSTRUCTION_lstore_2    : 
      case INSTRUCTION_lstore_3    : 
      case INSTRUCTION_dstore      : 
      case INSTRUCTION_dstore_0    : 
      case INSTRUCTION_dstore_1    : 
      case INSTRUCTION_dstore_2    : 
      case INSTRUCTION_dstore_3    : 

      case INSTRUCTION_pop2        : 

      case INSTRUCTION_ladd        : 
      case INSTRUCTION_dadd        : 
      case INSTRUCTION_lsub        : 
      case INSTRUCTION_dsub        : 
      case INSTRUCTION_lmul        : 
      case INSTRUCTION_dmul        : 
      case INSTRUCTION_ldiv        : 
      case INSTRUCTION_ddiv        :

      case INSTRUCTION_lrem        : 
      case INSTRUCTION_drem        : 

      case INSTRUCTION_land        : 
      case INSTRUCTION_lor         : 
      case INSTRUCTION_lxor        : 

      case INSTRUCTION_if_icmpeq   : 
      case INSTRUCTION_if_icmpne   : 
      case INSTRUCTION_if_icmplt   : 
      case INSTRUCTION_if_icmpgt   : 
      case INSTRUCTION_if_icmple   : 
      case INSTRUCTION_if_icmpge   : 

      case INSTRUCTION_if_acmpeq   : 
      case INSTRUCTION_if_acmpne   : 

      case INSTRUCTION_lreturn     : 
      case INSTRUCTION_dreturn     : 

                                     return -2;

      case INSTRUCTION_iastore     : 
      case INSTRUCTION_fastore     : 
      case INSTRUCTION_aastore     : 
      case INSTRUCTION_bastore     : 
      case INSTRUCTION_castore     : 
      case INSTRUCTION_sastore     : 
      case INSTRUCTION_lcmp        : 
      case INSTRUCTION_dcmpl       : 
      case INSTRUCTION_dcmpg       : 


                                     return -3;

      case INSTRUCTION_lastore     : 
      case INSTRUCTION_dastore     : 

                                     return -4;

      case INSTRUCTION_getstatic   : return 1000+ 2; // this will be handled in 
      case INSTRUCTION_putstatic   : return 1000+-1; // subclass FieldInstruction
      case INSTRUCTION_getfield    : return 1000+ 0; // but we'll give some save
      case INSTRUCTION_putfield    : return 1000+-2; // answers anyway; to be 100% sure...

      case INSTRUCTION_invokevirtual    : // to subclass
      case INSTRUCTION_invokespecial    : // to subclass
      case INSTRUCTION_invokestatic     : // to subclass
      case INSTRUCTION_invokeinterface  : // to subclass

      case INSTRUCTION_multianewarray   : // to subclass
      case INSTRUCTION_tableswitch      : // to subclass 
      case INSTRUCTION_lookupswitch     : // to subclass

      case INSTRUCTION_breakpoint       : // invalid
      default                           :
                                          return -666;
      }
  }

  public static Instruction loadBooleanInstruction (ClassEnvironment e, boolean value, InstructionOwner owner) {
      return loadIntegerInstruction (e, value? 1: 0, owner);
  }
  public static Instruction loadIntegerInstruction (ClassEnvironment e, int value, InstructionOwner owner) {
   try {
      switch (value) {
      case -1: return new Instruction (INSTRUCTION_iconst_m1, owner);
      case  0: return new Instruction (INSTRUCTION_iconst_0 , owner);
      case  1: return new Instruction (INSTRUCTION_iconst_1 , owner);
      case  2: return new Instruction (INSTRUCTION_iconst_2 , owner);
      case  3: return new Instruction (INSTRUCTION_iconst_3 , owner);
      case  4: return new Instruction (INSTRUCTION_iconst_4 , owner);
      case  5: return new Instruction (INSTRUCTION_iconst_5 , owner);}
      if ( (byte)value==value) return new Instruction (INSTRUCTION_bipush, value, owner);
      if ((short)value==value) return new Instruction (INSTRUCTION_sipush, value, owner);
      return e.resolveInteger(value).loadInstruction (owner);
    } catch (ByteCodingException err) {err.printStackTrace();return null;}
  }

  public static Instruction loadLongInstruction (ClassEnvironment e, long value, InstructionOwner owner) {
   try {
      if (value==0) return new Instruction (INSTRUCTION_lconst_0, owner);
      if (value==1) return new Instruction (INSTRUCTION_lconst_1, owner);
      return e.resolveLong (value).loadInstruction (owner);
    } catch (ByteCodingException err) {err.printStackTrace(); return null;}
  }
  public static Instruction loadFloatInstruction (ClassEnvironment e, float value, InstructionOwner owner) {
   try {
      if (value==0) return new Instruction (INSTRUCTION_fconst_0, owner);
      if (value==1) return new Instruction (INSTRUCTION_fconst_1, owner);
      if (value==2) return new Instruction (INSTRUCTION_fconst_2, owner);
      return e.resolveFloat (value).loadInstruction (owner);
    } catch (ByteCodingException err) {err.printStackTrace(); return null;}
  }
  public static Instruction loadDoubleInstruction (ClassEnvironment e, double value, InstructionOwner owner) {
   try {
      if (value==0) return new Instruction (INSTRUCTION_dconst_0, owner);
      if (value==1) return new Instruction (INSTRUCTION_dconst_1, owner);

      return e.resolveDouble (value).loadInstruction (owner);
    } catch (ByteCodingException err) {err.printStackTrace(); return null;}
  }
  public static Instruction loadConstantPoolItemInstruction (ConstantPoolItem cpi, InstructionOwner owner) {
   try {
      return new Instruction (cpi.noOfSlots()==1? INSTRUCTION_ldc_w // optimization at a later stage...
                                                : INSTRUCTION_ldc_2w, cpi, owner);
   } catch (ByteCodingException err) {err.printStackTrace(); return null;}
  }
  public void optimize() {
     switch (opc) {
     case INSTRUCTION_aload:
       {
         int value = ((UnsignedByteWideOperand)operand).val;
         switch (value) {
         case  0: opc = INSTRUCTION_aload_0; operand  = null; break;
         case  1: opc = INSTRUCTION_aload_1; operand  = null; break;
         case  2: opc = INSTRUCTION_aload_2; operand  = null; break;
         case  3: opc = INSTRUCTION_aload_3; operand  = null; break;
         }
       }
       break;
     case INSTRUCTION_astore:
       {
         int value = ((UnsignedByteWideOperand)operand).val;
         switch (value) {
         case  0: opc = INSTRUCTION_astore_0; operand  = null; break;
         case  1: opc = INSTRUCTION_astore_1; operand  = null; break;
         case  2: opc = INSTRUCTION_astore_2; operand  = null; break;
         case  3: opc = INSTRUCTION_astore_3; operand  = null; break;
         }
       }
       break;
     case INSTRUCTION_iload:
       {
         int value = ((UnsignedByteWideOperand)operand).val;
         switch (value) {
         case  0: opc = INSTRUCTION_iload_0 ; operand  = null; break;
         case  1: opc = INSTRUCTION_iload_1 ; operand  = null; break;
         case  2: opc = INSTRUCTION_iload_2 ; operand  = null; break;
         case  3: opc = INSTRUCTION_iload_3 ; operand  = null; break;
         }
       }
       break;
     case INSTRUCTION_istore:
       {
         int value = ((UnsignedByteWideOperand)operand).val;
         switch (value) {
         case  0: opc = INSTRUCTION_istore_0 ; operand  = null; break;
         case  1: opc = INSTRUCTION_istore_1 ; operand  = null; break;
         case  2: opc = INSTRUCTION_istore_2 ; operand  = null; break;
         case  3: opc = INSTRUCTION_istore_3 ; operand  = null; break;
         }
       }
       break;
     }
  }
}


