/* This file is part of Sawa, the Scriptic-Java compiler
 * Copyright (C) 2009 Andre van Delft
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

package scriptic.tools;


import java.io.*;
import scriptic.tools.lowlevel.*;


/**
 * Flow analysis?
 * code generation and optimization
 * write class file
 * dispose method bodies
 *
 * arg2: passes switch statement???
 *
 * local varable 0 == this for instance functions
 *
 * process(expression):
 *   if (expression.isLValue) push expression.reference
 *   else                       push expression.value
 */

/// ?? hoe class + instance variabelen te adresseren?

public class ByteCodeEmitter extends ScripticParseTreeEnumerator
               implements scriptic.tokens.ScripticTokens, ClassFileConstants {

   public ByteCodeEmitter (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
   }

   CodeAttribute   codeAttribute;

   /** mostly/allways classType */
   ClassType constantPoolOwner;

   void handleByteCodingException (ByteCodingException e) {
       System.out.println (codeAttribute.getDescription()+lineSeparator+codeAttribute.getMnemonicCode (constantPoolOwner));
       e.printStackTrace();
   }

   InstructionOwner previousOwner;

   void emit (Instruction insn) {
      if (insn == null) {
          // new Exception ("emit(null)").printStackTrace();
          return;
      }
      try {
        codeAttribute.addInstruction(insn);
        previousOwner = insn.owner;
      } catch (ByteCodingException e) {handleByteCodingException(e);
      }
   }
   void emit (Instruction insns[]) {
      for (int i=0; i<insns.length; i++) emit (insns[i]);
   }

   void emitStoreLocal(LocalVariable v, InstructionOwner owner) throws IOException {
     try {
       emit (new Instruction (v.getDataType (env).storeInstructionCode(), v.slot, owner));
     } 
     catch (ByteCodingException e) {handleByteCodingException(e);}
  }

   void emitLoadLocal(LocalVariable v, InstructionOwner owner) throws IOException  {
     try {
       emit (new Instruction (v.getDataType (env).loadInstructionCode(), v.slot, owner));
     } catch (ByteCodingException e) {handleByteCodingException(e);}
   }

   // load a nested array with strings and numbers
   // depth: the minimum guaranteed array nesting depth,
   // needed for the appropriate descriptor for the anewarray instruction
   void emitLoadNestedArray (Object array, InstructionOwner owner, int depth) {
    try {
     boolean doTrace = false;
     if (doTrace) System.out.print ("{");
     if (array instanceof int[]) {
        if (depth>1) {throw new ByteCodingException("INTERNAL ARRAY GENERATION ERROR: received int[] while depth = "+depth);}
        int a [] = (int[])array;
        emitLoadInteger (a.length, owner);
        emit            (new Instruction (INSTRUCTION_newarray, IntType.theOne.getArrayTypeNumber(), owner));
        for (int i=0; i<a.length; i++) {
            if (doTrace) {if (i>0) System.out.print (",");System.out.print (a[i]);}
            if (a[i] != 0) {
                emitCode        (INSTRUCTION_dup    , owner);
                emitLoadInteger (  i                , owner);
                emitLoadInteger (a[i]               , owner);
                emitCode        (INSTRUCTION_iastore, owner);
            }
        }
     } else if (array instanceof long[]) {
        if (depth>1) {throw new ByteCodingException("INTERNAL ARRAY GENERATION ERROR: received long[] while depth = "+depth);}
        long a [] = (long[])array;
        emitLoadInteger (a.length, owner);
        emit            (new Instruction (INSTRUCTION_newarray, LongType.theOne.getArrayTypeNumber(), owner));
        for (int i=0; i<a.length; i++) {
            if (doTrace) {if (i>0) System.out.print (",");System.out.print (a[i]);}
            if (a[i] != 0) {
                emitCode    (INSTRUCTION_dup    , owner);
                emitLoadInteger (i              , owner);
                emit (loadLongInstruction (a[i] , owner));
                emitCode    (INSTRUCTION_lastore, owner);
            }
        }
     } else { // Objects: String or other arrays...
        DataType accessType = env.javaLangObjectType;
        if (depth>1) {
           accessType = accessType.makeArray (depth-1);
        }
        Instruction instruction = new Instruction (INSTRUCTION_anewarray,
                                           accessType.classRef(constantPoolOwner),
                                           owner);
        Object a [] = (Object[])array;
        emitLoadInteger (a.length, owner);
        emit            (instruction);

        for (int i=0; i<a.length; i++) {
            Object obj = a[i];

            if (obj== null) {
                continue;
            }
            emitCode        (INSTRUCTION_dup, owner);
            emitLoadInteger (  i            , owner);
            if (obj instanceof String) {
                if (depth>0) {throw new ByteCodingException("INTERNAL ARRAY GENERATION ERROR: received String while depth = "+depth);}
                if (doTrace) {if (i>0) System.out.print (",");System.out.print ((String) obj);}
                emitLoadString ((String) obj, owner);
            }
            else { // a nested array...
                emitLoadNestedArray (obj, owner, depth-1);
            }
            emitCode        (INSTRUCTION_aastore, owner);
        }
     }
     if (doTrace) System.out.println ("}");
    } catch (ByteCodingException e) {handleByteCodingException (e);}
   }

   // for loading an index of parameter in the script method, apply the appropriate offset.
   void emitLoadIndexOrParameterInScriptMethod (BasicScriptDeclaration decl, LocalVariableOrParameter v, InstructionOwner owner) 
   throws ByteCodingException, IOException {
      int offset = decl.isStatic()? 0: 1;
      if (!decl.isMainScript()) offset++; // most script funtions have an extra parameter
      emitCode (v.getDataType(env).loadInstructionCode(), offset+v.slot, owner);
   }

   // load value of script index variable or parameter
   // with wrapper object: Byte, ..., Double (not for reference)
   void emitLoadAsObject  (BasicScriptDeclaration decl, BasicVariableDeclaration vdecl) throws ByteCodingException, IOException {
     LocalVariableOrParameter v = (LocalVariableOrParameter) vdecl.targetField();
     if (v.dataType1.isReference()) {
       emitLoadIndexOrParameterInScriptMethod (decl, v, vdecl);
       return;
     }
     try {
       ClassType w = v.dataType1.wrapperClass(env);
       Method winit = env.mustResolveMethod (w, "<init>",
                                             "("+v.dataType1.getSignature()+")V");
       emit          (new Instruction(INSTRUCTION_new, w.classRef(constantPoolOwner), vdecl));
       emitCode      (INSTRUCTION_dup, vdecl);
       emitLoadIndexOrParameterInScriptMethod (decl, v, vdecl);
       emit          (winit.invokeInstruction (env, INSTRUCTION_invokespecial, constantPoolOwner, vdecl));
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, vdecl);}
   }

   void emitLoadConstant (JavaExpression e) {
      ConstantValue c = e.constantValue;
      try {
        if (c.isNull()) emit (new Instruction (INSTRUCTION_aconst_null, e));
        else            emit (c.loadInstructions (constantPoolOwner, e));
      } catch (ByteCodingException err) {err.printStackTrace();}
   }
   void emitLoadString (String string, InstructionOwner owner) {
      try {
        emit (constantPoolOwner.resolveString (string).loadInstruction (owner));
      } catch (ByteCodingException err) {err.printStackTrace();}
   }
   void emitLoadNull (InstructionOwner owner) {emitCode (INSTRUCTION_aconst_null, owner);}

   void emitJumpInstruction (int code, LabelInstruction label, InstructionOwner owner) {
     try {
      emit (new Instruction (code, label, owner));
     } catch (ByteCodingException e) {handleByteCodingException(e);}
   }

   void emitIfNonNull (LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_ifnonnull,l,o);}
   void emitIfNull    (LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_ifnull,l,o);}
   void emitGoto      (LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_goto  ,l,o);}
   void emitIfEqual   (LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_ifeq  ,l,o);}
   void emitIfNotEqual(LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_ifne  ,l,o);}
   void emitJSR       (LabelInstruction l, InstructionOwner o) {emitJumpInstruction(INSTRUCTION_jsr   ,l,o);}
   void emitIfTrue    (LabelInstruction l, InstructionOwner o) {emitIfNotEqual(l,o);}
   void emitIfFalse   (LabelInstruction l, InstructionOwner o) {emitIfEqual   (l,o);}
   void emitReturn    (ReturnStatement statement) {
       int code = ( statement                 ==null
                 || statement.returnExpression==null)? INSTRUCTION_return
                  : statement.returnExpression.dataType.returnInstructionCode();
       emitCode (code, statement);
   }
   void emitReturn    (MethodDeclaration owner) {
       emitCode (INSTRUCTION_return, owner);
   }
   void emitLoadFalse (InstructionOwner owner) {emitLoadInteger (0, owner);}
   void emitLoadTrue  (InstructionOwner owner) {emitLoadInteger (1, owner);}

   void emitRet(LocalVariable v, InstructionOwner owner) {emitCode (INSTRUCTION_ret, v.slot, owner);}

   void emitCode (int code, InstructionOwner owner) {
     try {
       emit (new Instruction (code, owner));
     } catch (ByteCodingException e) {handleByteCodingException(e);}
   }
   void emitCode (int code, int i, InstructionOwner owner) {
     try {
       emit (new Instruction (code, i, owner));
     } catch (ByteCodingException e) {handleByteCodingException(e);}
   }

   void emitCode (int code, ConstantPoolItem c, InstructionOwner owner) {
     try {
       emit (new Instruction (code, c, owner));
     } catch (ByteCodingException e) {handleByteCodingException(e);}
   }
   void emitLoadNodeParameter (FieldDeclaration decl, InstructionOwner owner, boolean isScriptStatic) {
        emitCode (INSTRUCTION_aload, isScriptStatic? 0: 1, owner);
   }
 
   void emitThrow         (InstructionOwner owner) {emitCode (INSTRUCTION_athrow, owner);}
   void emitLoadIntegerAs (int value, DataType d, InstructionOwner owner) {
      emitLoadInteger (value, owner);
      if (!d.isInt()) emitCode (IntType.theOne.convertToInstructionCode (d), owner);
   }
   void emitLoadInteger (int value, InstructionOwner owner) {
      emit (loadIntegerInstruction (value, owner));
   }

  public Instruction loadBooleanInstruction (boolean value, InstructionOwner owner) {
  return Instruction.loadBooleanInstruction (constantPoolOwner, value, owner);
  }
  public Instruction loadIntegerInstruction (int value, InstructionOwner owner) {
  return Instruction.loadIntegerInstruction (constantPoolOwner, value, owner);
  }
  public Instruction loadLongInstruction (long value, InstructionOwner owner) {
  return Instruction.loadLongInstruction (constantPoolOwner, value, owner);
  }
  public Instruction loadFloatInstruction (float value, InstructionOwner owner) {
  return Instruction.loadFloatInstruction (constantPoolOwner, value, owner);
  }
  public Instruction loadDoubleInstruction (double value, InstructionOwner owner) {
  return Instruction.loadDoubleInstruction (constantPoolOwner, value, owner);
  }
   void emitPop (DataType d, InstructionOwner owner) {
      int instructionCode = d.popInstructionCode();
      if (instructionCode>=0) emitCode (instructionCode, owner);
   }

   void emitLoadThis    (InstructionOwner owner) {
       emitCode (INSTRUCTION_aload_0, owner);
   }
   void emitLoadThisFor (JavaExpressionWithTarget exp) throws CompilerError, IOException, ByteCodingException {
      emitLoadThis (exp);

      ClassType owner = exp.target.ownerClass(env);
      ClassType ct    = classType;

//System.out.println("emitLoadThisFor, owner: "+owner.nameWithSlashes
//+" ct: "+ct.nameWithSlashes
//+" exp: "+exp.getPresentation()
//+" exp.target: "+exp.target.getPresentation());

      while (!ct.isSubtypeOf (env, owner)) {

/****
System.out.println("emitLoadThisFor, owner: "+owner.nameWithSlashes
+" ct: "+ct.nameWithSlashes
+" exp: "+exp.getPresentation());
****/
          LocalOrNestedClassType lct = (LocalOrNestedClassType) ct;
          lct.setNeedForParentReference(); // better: in getEnclosingInstance...
          emit (lct.enclosingInstance.loadInstructions (env, constantPoolOwner, exp));
          ct = lct.parent();
      }
   }
   void emitLoadThisFor (RefinementCallExpression exp) throws CompilerError, IOException, ByteCodingException {
      emitLoadThis (exp);
      ClassType owner = exp.target().ownerClass(env);
      ClassType ct    = classType;
      while (!ct.isSubtypeOf (env, owner)) {
/*****
System.out.println("emitLoadThisFor, owner: "+owner.nameWithSlashes
+" ct: "+ct.nameWithSlashes
+" exp: "+((MethodCallExpression)exp).getPresentation());
****/
          LocalOrNestedClassType lct = (LocalOrNestedClassType) ct;
          lct.setNeedForParentReference(); // better: in getEnclosingInstance...
          emit (lct.enclosingInstance.loadInstructions (env, constantPoolOwner, exp));
          ct = lct.parent();
      }
   }

   /** two longs, floats or doubles are on the stack.
    * Compare according to token, and push 0 (false) or 1 (true)
    */

   void emitCompare (DataType dataType, int token, InstructionOwner owner) {

       boolean doGreater = token == GreaterThanToken
                        || token == GreaterOrEqualToken;
      
       emitCode (dataType.cmpInstructionCode (doGreater), owner);

       LabelInstruction trueLabel = new LabelInstruction("true part", owner);
       emitJumpInstruction (dataType.binaryInstructionCode(token), trueLabel, owner);
       emitLoadInteger (0, owner);

       LabelInstruction endLabel = new LabelInstruction("end", owner);
       emitGoto (endLabel, owner);

       emit(trueLabel);
       emitLoadInteger (1, owner);
       emit(endLabel);
   }
   /** two integers are on the stack.
    * Compare according to token, and push 0 (false) or 1 (true)
    * @param token
    */
   void emitCompareIntegers (int token, InstructionOwner owner) {
       LabelInstruction trueLabel = new LabelInstruction("true part", owner);
       emitJumpInstruction (IntType.theOne.binaryInstructionCode(token), trueLabel, owner);
       emitLoadInteger (0, owner);

       LabelInstruction endLabel = new LabelInstruction("end", owner);
       emitGoto (endLabel, owner);

       emit(trueLabel);
       emitLoadInteger (1, owner);
       emit(endLabel);
   }
   /** two reference values are on the stack.
    * Compare according to token (== or !=), and push 0 (false) or 1 (true)
    * @param token
    */
   void emitCompareReferences (int token, InstructionOwner owner) {
       LabelInstruction trueLabel = new LabelInstruction("true part", owner);
       int code = token==EqualsToken? INSTRUCTION_if_acmpeq
                                    : INSTRUCTION_if_acmpne;
       emitJumpInstruction (code, trueLabel, owner);
       emitLoadInteger (0, owner);

       LabelInstruction endLabel = new LabelInstruction("end", owner);
       emitGoto (endLabel, owner);

       emit(trueLabel);
       emitLoadInteger (1, owner);
       emit(endLabel);
   }
   void emitCheckCast (DataType c, InstructionOwner owner) {
        emitCode (INSTRUCTION_checkcast , constantPoolOwner.resolveClass (c), owner);
   }
   void emitInstanceof (DataType c, InstructionOwner owner) {
        emitCode (INSTRUCTION_instanceof, constantPoolOwner.resolveClass (c), owner);
   }

   void emitTraceString (String s, InstructionOwner owner) {
      try {
      Variable v = env.javaLangSystemType.resolveMemberVariable (env, "out");
        emit (v.loadInstructions (env, constantPoolOwner, owner));
        emitLoadString (s, owner);
        Method m = env.javaIoPrintStreamType.findMethod (env, "println", "(Ljava/lang/String;)V");
        emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, owner));
      } catch (ByteCodingException      e) {handleByteCodingException(e);
      } catch (IOException   e) {e.printStackTrace();
      }
   }
}

