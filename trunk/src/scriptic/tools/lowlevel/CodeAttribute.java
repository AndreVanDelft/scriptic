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

import java.io.*;
import java.util.*;


public class CodeAttribute extends ClassFileAttribute
                        implements ClassFileConstants, AttributeOwner {

  public short      stack_size;
  short             num_locals;
  int               code_size;
  private ArrayList<Instruction> instructionsVector;
  Instruction       instructions[];
  Catchtable        catchtable;
    LineNumberTableAttribute  lineTable;
   LinePositionTableAttribute linePositionTable;
  LocalVariableTableAttribute localVariables;

  public static final int DebugInfoNone           = 0;
  public static final int DebugInfoLines          = 1;
  public static final int DebugInfoLinesPositions = 2;
  public static final int DebugInfoPseudoLines    = 3;
  int debugInfoMode = DebugInfoLinesPositions;
  int pseudoLine;

  AttributesContainer attributesContainer;
  AttributeOwner      owner;

  public static String name = "Code";
  public String getName () {return name;}

  public CodeAttribute (AttributeOwner owner, ClassEnvironment classEnv)
  {
    this.owner              = owner;
    this.num_locals         = 1;
    this.instructionsVector = new ArrayList<Instruction>();
    attributesContainer     = new AttributesContainer(this, classEnv);
    //new Exception("new CodeAttribute "+owner.getPresentation()).printStackTrace();
  }

   public void addAttribute (ClassFileAttribute attribute) {
     attributesContainer.add (attribute);
   }

  /**
   * Add an entry for the catch table
   */
  public void addCatchtableEntry (LabelInstruction start, LabelInstruction end,
                                  LabelInstruction handler, ConstantPoolClass cat)
  {
    if (catchtable == null) {
        catchtable = new Catchtable();
    }
      start.incReferencesBySwitchTablesAndCatchHandlers(); // these labels now keep positive reference counts...
        end.incReferencesBySwitchTablesAndCatchHandlers();
    handler.incReferencesBySwitchTablesAndCatchHandlers();
    catchtable.addEntry(start, end, handler, cat);
  }

  public void addInstruction(Instruction insn) throws ByteCodingException {addInstruction(insn, true);}

  public void addInstruction(Instruction insn, boolean doCountReferences) throws ByteCodingException
  {
    insertInstructionAt (insn, instructionsVector.size(), doCountReferences);
  }

  public void insertInstructionAt(Instruction insn, int index, boolean doCountReferences) throws ByteCodingException
  {
    insn.index = index;
    instructionsVector.add(index, insn);
    for (int i=index+1; i<instructionsVector.size(); i++) {
           instructionsVector.get(i).index++;
    }
    if (doCountReferences
    &&  insn.operand != null)
        insn.operand.incTargetReferenceCount();
  }

  public void removeInstruction(Instruction insn)
  {
    if (insn.hasLabelOperand()) {
        insn.operand.decTargetReferenceCount();

        if (((LabelOperand)insn.operand).target.getReferenceCount() < 0)
                new Exception ("getReferenceCount() < 0\n"
                              +insn.getPresentation())
                       .printStackTrace();
    }
    insn.opc = INSTRUCTION_deleted;
  }

  public void destroyInstructionsAt(int index, int count) throws ByteCodingException
  {
    for (int i=0; i<count; i++) {
      instructionsVector.remove(index);
    }
    for (int i=index; i<instructionsVector.size(); i++) {
           instructionsVector.get(i).index-=count;
    }
  }

  void copyInstruction (Instruction target, Instruction source) throws ByteCodingException {
      target.opc     = source.opc;
      target.operand = source.operand==null? null
                     : source.operand.copyFor(target);  // in case of a label, make a copy; otherwise offset goes wrong
      target.owner   = source.owner;
      if (target.hasLabelOperand()) {
          target.operand.incTargetReferenceCount();
      }
  }

  public boolean isEmpty() {return instructionsVector.isEmpty();}
  public void setVarSize(int num_vars) {num_locals = (short) num_vars;}
  public short   varSize()      {return num_locals;}

  public void resolve (ClassEnvironment e, AttributeOwner owner)
  {
      for (Instruction i: instructionsVector)
      {
        i.resolve(e);
      }
      if (catchtable != null) catchtable.resolve(e);
                     attributesContainer.resolve(e, owner);
  }

   public String getMnemonicCode (ClassEnvironment e) {
      decode (e);
      StringBuffer result = new StringBuffer();
      result.append (lineSeparator);
      for (int i=0; i<instructionsVector.size(); i++) {
              Instruction instruction = instructionsVector.get(i);
              //if (instruction.opc == INSTRUCTION_deleted) continue;
              result.append (instruction.getPresentation(e)).append(lineSeparator);
      }
      result.append(lineSeparator);
      return result.toString ();
   }

   public String getMnemonicCodeFromArray (ClassEnvironment e) {
      StringBuffer result = new StringBuffer();
      result.append (lineSeparator);
      for (int i=0; i<instructions.length; i++) {
              //if (instructions[i].opc == INSTRUCTION_deleted) continue;
              result.append (instructions[i].getPresentation(e)).append(lineSeparator);
      }
      result.append(lineSeparator);
      return result.toString ();
   }

   // Generic description -- hex dump+ascii dump
   public String getDescription () {
      StringBuffer result = new StringBuffer();
      result.append ( "code_size: " +  code_size).append (lineSeparator);
      result.append ("stack_size: " + stack_size).append (lineSeparator);
      result.append ("num_locals: " + num_locals).append (lineSeparator);
      if (catchtable       !=null) result.append   (catchtable       .getDescription());
                                   result.append (attributesContainer.getDescription());
      result.append (super.getDescription());
      return result.toString ();
   }

  public void decode (ClassEnvironment e) {
    if (bytes == null          ) return;
    if (!instructionsVector.isEmpty()) return;
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      stack_size = in.readShort();
      num_locals = in.readShort();
      code_size  = in.readInt  ();
      instructionsVector = new ArrayList<Instruction>();
  
      LongHolder pos = new LongHolder();
      while (pos.value < code_size) {
         try {
           addInstruction (Instruction.readFrom (e, this, in, pos), false);
         } catch (RuntimeException err) {
           System.out.println ("Error at instruction: "+instructionsVector.size());
           err.printStackTrace();
           break;
         }
      }
      int catchtableSize  = in.readShort ();
  
      if (catchtableSize > 0) {
          catchtable = new Catchtable();
          Catchtable.readFrom(e, in, catchtableSize);
      }
      attributesContainer = AttributesContainer.readFromStream (this, e, in);

      if (debugInfoMode != DebugInfoNone) {
        lineTable           = attributesContainer.   getLineNumberTable();
        localVariables      = attributesContainer.getLocalVariableTable();
        if (debugInfoMode == DebugInfoLinesPositions) {
          linePositionTable   = attributesContainer. getLinePositionTable();
        }
      }
    } catch (Exception exc) {
      System.out.println("CodeAttribute: decoding error");
      exc.printStackTrace();
    }
  }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out) throws IOException, ByteCodingException
  {
                                // First, resolve all labels and
                                // compute total size

    int                     total_size  = code_size;
    if (catchtable != null) total_size += catchtable         .size();
                            total_size += attributesContainer.size();
                            total_size += 12;    // extra headers

    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt  (total_size);
    out.writeShort(stack_size);
    out.writeShort(num_locals);
    out.writeInt  (code_size);
    for (Instruction instruction: instructionsVector)
    {
        instruction.write(e, this, out);
    }
    if  (catchtable != null)
         catchtable.write(e, this, out);
    else out.writeShort(0);
    attributesContainer.write (e, out);
  }

  public void finishForWriting (ClassEnvironment e) throws ByteCodingException {

    optimize(e);

    code_size         = 0;

    if (debugInfoMode != DebugInfoNone) {
      addAttribute (lineTable         = new   LineNumberTableAttribute());
    }
    if (debugInfoMode == DebugInfoLinesPositions) {
      addAttribute (linePositionTable = new LinePositionTableAttribute());
    }
    int n             = 0;

    for (int i=0; i<instructionsVector.size(); i++)
    {
        Instruction instruction = instructionsVector.get(i);

        switch (instruction.opc) {
        case INSTRUCTION_deleted: continue;
        case INSTRUCTION_nop    : // ??????  if (i<instructionsVector.size()-1)    // keep last nop...
                                  continue; 
        case INSTRUCTION_label  : // ?????? insn_pc.put(((LabelInstruction)instruction).id, new Integer(code_size)); break;
        default                 : //insn_pc.put(                   instruction    , new Integer(code_size));
                                  if (instruction.opc != INSTRUCTION_label) n++;
                                  instruction.pc = code_size;
        }
        code_size += instruction.size(e, this);
    }

    // create the array; more convenient for the final analyses

    instructions = new Instruction[n];
    int lastLine = -1;
    int i = 0;
    for (Instruction instruction: instructionsVector)
    {
        instruction.index = i; 

        switch (instruction.opc) {
        case INSTRUCTION_nop    :
        case INSTRUCTION_deleted:
        case INSTRUCTION_label  : break;
        default                 :
            instructions [i++] = instruction;
            if (lineTable     != null) {
               int line = 0;
               if (debugInfoMode == DebugInfoLines
               ||  debugInfoMode == DebugInfoLinesPositions) {
                   line        = e.line (instruction.owner.nameStartPosition());
               } else {
                   line = pseudoLine++;
               }
               if (lastLine   != line) {
                   lastLine    = line;
                   // System.out.println("instruction.pc: "+instruction.pc+"  line: "+line);
                   lineTable.addEntry (instruction.pc, line);
               }
               if (linePositionTable != null) {
                   int scopeStartLine       = e.line (instruction.owner.sourceStartPosition());
                   int scopeEndLine         = e.line (instruction.owner.  sourceEndPosition());
                   int primaryStartPosition = e.positionOnLine (line, instruction.owner.nameStartPosition());
                   int primaryEndPosition   = primaryStartPosition
                                            + instruction.owner.  nameEndPosition()
                                            - instruction.owner.nameStartPosition();
                   int   scopeStartPosition = e.positionOnLine (scopeStartLine, instruction.owner.sourceStartPosition());
                   int     scopeEndPosition = e.positionOnLine (  scopeEndLine, instruction.owner.  sourceEndPosition());
                   linePositionTable.addEntry(instruction.pc,
                                              line,
                                              primaryStartPosition,
                                              primaryEndPosition,
                                              scopeStartLine,
                                              scopeEndLine,
                                              scopeStartPosition,
                                              scopeEndPosition);
               }
            }
        }
    }
    checkStackSize(e);

    if (catchtable != null) catchtable.compact();
  }

  private void checkStackSize(ClassEnvironment e) throws ByteCodingException {

    stack_size = 0;
    if (instructions.length==0) return;

    checkStackSize (e, instructions[0], 0);
    if (catchtable != null) {
      for (CatchEntry c: catchtable.entries) {
        Instruction tryStart     = instructions[c.  start_pc.index];
        Instruction catchHandler = instructions[c.handler_pc.index];

        checkStackSize (e, catchHandler, tryStart.currentStackSize
                                       - tryStart.  deltaStackSize() + 1);
                           // '1' for the catch variable
      }
    }
  }

  private void checkStackSize(ClassEnvironment e, Instruction instruction, int startValue) throws ByteCodingException {

    int s = startValue;
    do {
      s += instruction.deltaStackSize();
      if (s > stack_size) stack_size = (short) s;
      if (s < 0) {
        instruction.currentStackSize = s;
        throw new ByteCodingException ("negative stack size: " + s
                           + " instruction: " + instruction.getPresentation(e)
                           + " at index: " + instruction.index
                           + lineSeparator + owner.getPresentation()
                           + lineSeparator + getMnemonicCode (e)
                           + lineSeparator + getMnemonicCodeFromArray (e));
      }
      if (instruction.currentStackSize  < 0) {
          instruction.currentStackSize  = s;
          if (instruction.isReturn() && s != 0) {
             throw new ByteCodingException ("stack size not 0 at return; instead: " + s
                           + lineSeparator + owner.getPresentation()
                           + lineSeparator + getMnemonicCode (e)
                           + lineSeparator + getMnemonicCodeFromArray (e));
          }
      }
      else if (instruction.currentStackSize != s) {
        throw new ByteCodingException ("inconsistent stack sizes: " + s
                           + " vs. " + instruction.currentStackSize
                           + " instruction: " + instruction.getPresentation()
                           + " at index: " + instruction.index
                           + lineSeparator + owner.getPresentation()
                           + lineSeparator + getMnemonicCode (e)
                           + lineSeparator + getMnemonicCodeFromArray (e)
                           + lineSeparator + attributesContainer.getDescription ()
                           + (catchtable==null?""
                           :  lineSeparator + catchtable.getDescription ())
                           );
      }
      else return; // already done...

      LabelInstruction labels[] = instruction.getTargetLabels();
      if (labels != null) { // multiple for switch instructions

         for (int i=0;  i<labels.length; i++) {
            int targetLabelIndex = labels[i].index;
            if (targetLabelIndex < instructions.length) { // could also be terminating label....
               checkStackSize (e,  instructions[targetLabelIndex], s);
            }
         }
      }
      if (!instruction.continuesWithNext()) break;

      int nextIndex  = instruction.index+1;
      if (nextIndex >= instructions.length) return;
      if (instruction.opc == INSTRUCTION_jsr
      ||  instruction.opc == INSTRUCTION_jsr_w) s--; // after jsr, same stack again...

      instruction    = instructions[nextIndex];

    } while (true);
  }

  final static boolean doTrace = false;
/**
 * Optimize the code
 * These are possible situations that can be optimized:
 *
 * Boolean patterns
 * ================ 
 *  
 *  "if (d<f)"
 *  
 *           iflt true part
 *           bconst_false
 *           goto end
 *  true part[1]:
 *           bconst_true
 *  end[1]:
 *           if_true "..."
 *  ----------------------
 *           iflt "..."
 *  
 *
 *  "if (b1 || b2)"
 *
 *           if_false ||.false
 *           bconst_true
 *           goto ||.end
 *  ||.false[1]:
 *           load the other boolean
 *  ||.end[1]:
 *           if_true ...
 *  ------------------------------
 *           if_true ....
 *           load the other boolean
 *           if_true ...
 *
 *  "if (b1 && b2)"
 *
 *           if_true &&.true     
 *           bconst_false        
 *           goto &&.end         
 *  &&.true[1]:
 *           load the other boolean
 *  &&.end[1]:
 *           if_true ...
 *  ------------------------------
 *           if_false APPENDED_END
 *           load the other boolean
 *           if_true ...
 * APPENDED_END:
 *

 *
 *  "if (!bool) "
 *  
 *           if_false bool.false
 *           bconst_false        
 *           goto bool.end         
 *  bool.false[1]:
 *           bconst_true        
 *  bool.end[1]:
 *           if_false "..."
 *  ----------------------
 *           if_true "..."
 *  
 *
 *  "if (i<0)" // partly optimized...
 *
 *           iload_0
 *           iconst_0
 *           if_icmplt "..."
 *  ----------------------
 *           iload_0
 *           iflt ...
 *
 *  "if (a!=null)" // partly optimized...
 *
 *           aconst_null
 *           if_acmpeq ...
 *  ----------------------
 *           ifnull ...   // ifnonnull ...
 *
 * Jump Patterns
 * =============
 * goto >ret         >> ret
 * goto >return      >> return
 * goto1>goto2       >> goto
 * goto>throw        >> throw
 * goto>s; s         >> s
 * goto;   s; ...    >> goto
 * return; s; ...    >> return
 * throw;  s; ...    >> throw
 *
 *
 * Not done
 * ========
 * unused locals     >> - (??)    // too hard
 * iload 0           >> iload_0   // already generated
 * iload 8           >> bipush 8
 * iload 128         >> sipush 128
 * iload_x, iload_x  >> iload_x, dup
 * iload; istore     >> -         // will not occur
 *
 * 
 * Note: the forms "return; s; ..." and "throw;  s; ..."
 * NEED to be simplified, since the code for
 * void int() {if (b) return 0; else return 1;}
 * would otherwize have a dangling 'goto end if' after the 'return 0'...
 *
 * Carefully remove jumps to labels: decrease the label's reference count,
 * so that it may become marked as deleted
 */
  private void optimize(ClassEnvironment e) throws ByteCodingException {

    for (boolean changedSomething = true; changedSomething; ) {
 
      changedSomething       = false;
      boolean skipUntilLabel = false;

      if (doTrace) System.out.println (getMnemonicCode(e));
      for (Instruction instruction: new ArrayList<Instruction>(instructionsVector))
      {
         // changedSomething     |= instruction.optimize(e);

         //  remove unused labels
         if (instruction.isUnusedLabel()) {
             if (doTrace) System.out.println(owner.getPresentation()+" Remove unused label: "+instruction.getPresentation());
             removeInstruction (instruction);
             changedSomething     = true;
             continue;
         }

         //  ignore deleted instructions
         if (instruction.opc     == INSTRUCTION_deleted) {
             continue;
         }

         // have to skip?
         if (skipUntilLabel) {

             // stop skipping after a referenced label (NOTE tryStart and tryEnd labels)
             if  (instruction.opc == INSTRUCTION_label) {
               if(instruction.getReferenceCount() > 0
               || instruction.isCatchLabel     ()) {
                  skipUntilLabel   = false;
                  continue; // don't remove the try start and try end. Dangerous?
               }
               if (instruction.isTryEndLabel     ()) {
                  // needed since the label is in a catch table...
                  // a TryStartLabel that becomes deleted, should not enter the catch table
                  continue;
               }
             }
             if (doTrace) System.out.println(owner.getPresentation()+" skip: "+instruction.getPresentation());
             removeInstruction (instruction);
             changedSomething     = true;
             continue;
         }

         // goto <next statement>: to be deleted

         Instruction jumpInstruction = instruction;

         deleteUnneededJumpInstructions:

         while (jumpInstruction.opc  == INSTRUCTION_goto) {
                Instruction target = ((LabelOperand) jumpInstruction.operand).target;
                for (int nextIndex = jumpInstruction.index+1; nextIndex<instructionsVector.size(); nextIndex++) {

                    if (nextIndex == target.index) {
                    if (doTrace) System.out.println(owner.getPresentation()+" bypass: "+instruction.getPresentation());
                      removeInstruction (jumpInstruction);
                      changedSomething = true;
                      jumpInstruction  = target;

                      continue deleteUnneededJumpInstructions;
                    }
                    Instruction next = instructionsVector.get (nextIndex);

                    switch (next.opc) {
                    case INSTRUCTION_nop:
                    case INSTRUCTION_deleted:
                    case INSTRUCTION_label: break;
                    default:                break deleteUnneededJumpInstructions;
                    }
                }
                // throw new Exception ("Generated code: goto illegal address"
                //                +lineSeparator+owner.getPresentation(e));
                // NO; we may just have skipped a target 'nop'

                break;
         }

         // so far, no need to skip.
         // see whether we have to start skipping:
         // everything is unreachable after goto, return, throw etc; until a label...
         if (!instruction.continuesWithNext()) {

              skipUntilLabel = true;
              if (doTrace) System.out.println(owner.getPresentation()+" start skip: "+instruction.getPresentation());

              // going to a goto, return, throw etc? then take a shortcut

              while (instruction.opc    == INSTRUCTION_goto) {
                     Instruction target  =  getTargetSkipLabels(instruction);

                     if (target==null) throw new ByteCodingException ("Generated code: goto illegal address"

+lineSeparator+owner.getPresentation()+lineSeparator
+getDescription()
+lineSeparator
+lineSeparator
+"-------------------------------"
+instruction.getPresentation()
+"-------------------------------"
+lineSeparator
+lineSeparator
+getMnemonicCode(e));

                     if (target.continuesWithNext()) break;

                     if (doTrace) System.out.println(owner.getPresentation()+" shortcut: "+instruction.getPresentation());
                     removeInstruction (instruction);
                     copyInstruction   (instruction, target);
                     changedSomething = true;
              }
              continue;
         }

         if (false && instruction.isIfInstruction()) {

           // if LAB1; ...; LAB1: goto LAB2   >>   if LAB2
           Instruction target = getTargetSkipLabels (instruction);
           if (target.opc == INSTRUCTION_goto) {
               instruction.operand.decTargetReferenceCount();
               instruction.operand = target.operand.copyFor(instruction);
               instruction.operand.incTargetReferenceCount();
               changedSomething = true;
               if (doTrace) System.out.println(owner.getPresentation()+" shortened: "+instruction.getPresentation());
               continue;
           }
           int index = instruction.index;

           // if LAB1; goto LAB2; LAB1:   >>   if_negated LAB2
           Instruction next = instructionsVector.get(index+1);
           if (next.opc == INSTRUCTION_goto
           &&  target   == getNextSkipLabels (next)) {
               instruction.negateIfCode();
               instruction.operand.decTargetReferenceCount();
               instruction.operand = next.operand.copyFor(instruction);
               next.opc            = INSTRUCTION_deleted;
               changedSomething    = true;
               if (doTrace) System.out.println(owner.getPresentation()+" negated: "+instruction.getPresentation());
               continue;
           }

           // check for a lousy boolean expression pattern
           // MESSY
           if (index+6 < instructionsVector.size()) {
             Instruction loadBoolean  = instructionsVector.get (index+1);
             Instruction theGoto      = instructionsVector.get (index+2);
             Instruction label1       = instructionsVector.get (index+3);

             if (!loadBoolean.isLoadTrue ()
             &&  !loadBoolean.isLoadFalse()
             ||  theGoto.opc               != INSTRUCTION_goto
             ||  label1.getReferenceCount()!=1) {

                continue;
             }
             // so we have if_any label1; load_boolean; goto ...; label1:

             if (loadBoolean.isLoadFalse()) {
                Instruction loadTrue  = instructionsVector.get (index+4);
                Instruction label2    = instructionsVector.get (index+5);
                Instruction theIf     = instructionsVector.get (index+6);
                if (label1     .opc  == INSTRUCTION_label
                &&  label2     .opc  == INSTRUCTION_label
                && (theIf      .opc  == INSTRUCTION_ifeq
                  ||theIf      .opc  == INSTRUCTION_ifne)
                &&  theGoto    .getLabelTarget () == label2 && label2.getReferenceCount()==1
                &&  instruction.getLabelTarget () == label1
                &&  loadTrue   .isLoadTrue()           ) {

                    //           bconst_false
                    //           goto end
                    //  true part[1]:
                    //           bconst_true
                    //  end[1]:
                    //           if_true "..."

                    // reorganize fast, not using removeInstruction...
                    if (theIf.opc==INSTRUCTION_ifeq)
                    instruction.negateIfCode();
                    instruction.operand = theIf.operand;
                    ((LabelOperand)instruction.operand).source = instruction;

                    /********** this would be possible...
                    ((LabelInstruction)label1).decReferenceCount();
                    ((LabelInstruction)label2).decReferenceCount();
                    theGoto    .opc   = INSTRUCTION_deleted;
                    loadBoolean.opc   = INSTRUCTION_deleted;
                    loadTrue   .opc   = INSTRUCTION_deleted;
                    theIf      .opc   = INSTRUCTION_deleted;
                    *************************** but the following may be better: */

                    destroyInstructionsAt (index+1, 6);

                    changedSomething  = true;
                    if (doTrace) System.out.println(owner.getPresentation()+" reorganized: "+instruction.getPresentation());
                    continue;
                }
             }
         /*********************** DEACTIVATED ***************************
             LabelInstruction label2 = theGoto.getLabelTarget();

             if((instruction.opc == INSTRUCTION_ifeq && loadBoolean.isLoadTrue ()
               ||instruction.opc == INSTRUCTION_ifne && loadBoolean.isLoadFalse())
             &&   label2.index+1 < instructionsVector.size()) {

               Instruction theIf     = (Instruction) instructionsVector.elementAt (label2.index+1);
               if (theIf.opc  == INSTRUCTION_ifeq
                 ||theIf.opc  == INSTRUCTION_ifne) {

                 // at this point, we may have "if (b1 || b2)" or "if (b1 && b2)"
                 //
                 //           if_false ||.false
                 //           bconst_true
                 //           goto ||.end
                 //  ||.false[1]:
                 //           load the other boolean
                 //  ||.end[1]:
                 //           if_true ...
                 //  ------------------------------
                 //           if_true ....
                 //           load the other boolean
                 //           if_true ...
                 //
                 //           if_true &&.true     
                 //           bconst_false        
                 //           goto &&.end         
                 //  &&.true[1]:
                 //           load the other boolean
                 //  &&.end[1]:
                 //           if_true ...
                 //  ------------------------------
                 //           if_false APPENDED_END
                 //           load the other boolean
                 //           if_true ...
                 // APPENDED_END:

                 if (instruction.opc == theIf.opc) {
                    LabelInstruction appendedLabel = new LabelInstruction("appended", instruction.owner);
                    // NO NOT HERE appendedLabel.currentStackSize = instruction.currentStackSize;
                    insertInstructionAt (appendedLabel, theIf.index+1, true);
                    instruction.operand = new LabelOperand (appendedLabel, instruction, false);
                    appendedLabel.incReferenceCount (instruction);
                 }
                 else {
                    instruction.operand = theIf.operand.copyFor (instruction);
                 }
                 instruction.negateIfCode();

                 //    ********** this would be possible...
                 //((LabelInstruction)label1).decReferenceCount();
                 //((LabelInstruction)label2).decReferenceCount();
                 //theGoto    .opc   = INSTRUCTION_deleted;
                 //loadBoolean.opc   = INSTRUCTION_deleted;
                 // *************************** but the following may be better: 

                 destroyInstructionsAt (       index+1, 3);
                 destroyInstructionsAt (label2.index  , 1);

                 changedSomething  = true;
               }
             } // if (label2.index+1 < instructionsVector.size())
        ********************* END DEACTIVATED CODE *************************/

           }
           continue;
         }

         if (instruction.isLoadTrue ()
         ||  instruction.isLoadFalse()) {

           Instruction nextInstruction = getNextSkipLabels(instruction);
           while (nextInstruction.opc == INSTRUCTION_goto) {
                  nextInstruction = getTargetSkipLabels(nextInstruction);
           }
           if (nextInstruction.opc != INSTRUCTION_ifne
           &&  nextInstruction.opc != INSTRUCTION_ifeq) {
               continue;
           }
           if (instruction.isLoadTrue ()
           == (nextInstruction.opc == INSTRUCTION_ifne)) {  // load_true; if_true LAB => goto LAB
               instruction.operand  = nextInstruction.operand.copyFor (instruction);
           }
           else {   // load_false; if_true LAB => goto LAB1; if_true LAB; LAB1:
             LabelInstruction appendedLabel = new LabelInstruction("appended", instruction.owner);
             insertInstructionAt (appendedLabel, nextInstruction.index+1, true);
             instruction.operand = new LabelOperand (appendedLabel, instruction, false);
           }
           instruction.operand.incTargetReferenceCount();
           instruction.opc       = INSTRUCTION_goto;
           changedSomething      = true;
           if (doTrace) System.out.println(owner.getPresentation()+" load constant; if_true optimized: "+instruction.getPresentation());
           continue;
         }

      }
    }
    for (Instruction instruction: instructionsVector)
    {
    	instruction.optimize();
    }
  }

  /** Answer the next 'real' instruction while skipping labels
   *  and of course, skip deleted instructions
   *  If something is wrong, return null.
   */
  Instruction getNextSkipLabels (Instruction instruction) {
      Instruction result;

      for (int index = instructionsVector.indexOf (instruction)+1;
               index < instructionsVector.size();
               index++ ) {
         result = instructionsVector.get (index);
         if (result.opc != INSTRUCTION_label
         &&  result.opc != INSTRUCTION_deleted) return result;
      }
      return null;
  }

  /** the given instruction has a LabelOperand.
   *  Answer the actual target it is pointing to, so skip labels
   *  and of course, skip deleted instructions
   *  If something is wrong, return null.
   */
  Instruction getTargetSkipLabels (Instruction instruction) {
      return getNextSkipLabels (((LabelOperand) instruction.operand).target);
  }

}


/*--------------------------------------------------------------------
check Definite Assignments = compute the number of forward references 
                                                to each label instruction
                             for each local variable 
                               check Definite Assignment for it

check Definite Assignment for a variable
= boolean definitelyAssigned = false
  for each label instruction set DACount=0
  for all instructions in order
    if !DefinitelyAssigned
       if the variable is     used: ERROR
       if the variable is assigned: definitelyAssigned = true
    else 
       if the instruction may go to forward labels: the labels.DACount--
       if the instruction is a label 
       && label.DACount != label.the number of forward references: definitelyAssigned = false

--------------------------------------------------------------------
wipe out Unnecessary Assignments = do while did wipe out Unnecessary Assignments


did wipe out Unnecessary Assignments
= boolean wipedOutSome = false
  for each local variable 
     wipedOutSome |= did wipe out Unnecessary Assignments for it
  return wipedOutSome


did wipe out Unnecessary Assignments for a variable
= boolean isUsed   = false
  boolean wipedOut = false
  for all instructions in backwards order
    if !isUsed
       if the variable is     used: isUsed   = true
       if the variable is assigned: wipedOut = true, convert instruction to pop
    else 
       if the instruction refers to a label: the label.DACount--
       if the instruction is a label: label.variableIsUsed = isUsed
       && label.DACount != label.the number of forward references: DefinitelyAssigned = false
  return wipedOut
--------------------------------------------------------------------
*/
