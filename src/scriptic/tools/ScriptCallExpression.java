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


import java.io.IOException;
import java.util.ArrayList;

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.ByteCodingException;

class ScriptCallExpression extends ScriptExpression
                implements RefinementCallExpression {

   public JavaExpressionWithTarget  scriptAccessExpression;
   public StatementBlock            nativeCode;
   public MethodCallParameterList   parameterList;
   public Method                    target;
//   public Refinement                target;

   private boolean isSend    = false;
   private boolean isReceive = false;
   public  boolean isScriptChannelSend   ()  {return isSend;}
   public  boolean isScriptChannelReceive()  {return isReceive;}
   public  void    setScriptChannelSend   (boolean b)  {isSend   =b;}
   public  void    setScriptChannelReceive(boolean b)  {isReceive=b;}
   public void  setDimensionSignature(String s) {}

   private int mode = 0;
   public String getModeString() {return getModeString(this);}

   public void        setSuperMode() {       mode =       superMode;}
   public void      setVirtualMode() {       mode =     virtualMode;}
   public void      setSpecialMode() {       mode =  specialMode;}
   public void    setInterfaceMode() {       mode =   interfaceMode;}
   public void       setStaticMode() {       mode =      staticMode;}
   public boolean     isStaticMode() {return mode ==     staticMode;}
   public boolean    isSpecialMode() {return mode == specialMode;}
   public boolean      isSuperMode() {return mode ==      superMode;}
   public boolean    isVirtualMode() {return mode ==    virtualMode;}
   public boolean  isInterfaceMode() {return mode ==  interfaceMode;}

   public MethodCallParameterList   parameterList    () {return parameterList;}
   public JavaExpressionWithTarget  accessExpression () {return scriptAccessExpression;}
   public Method                    target() {return target;}
   public  void    setTarget(CompilerEnvironment env, TypeDeclaration typeDeclaration, Method t)
    throws IOException, CompilerError {
       target=t;
       int i = 0;
       if (parameterList   != null) {
           for (int j=0; j<parameterList.parameterExpressions.size(); j++,i++) {
              ScriptCallParameter scp = (ScriptCallParameter) parameterList.parameterExpressions.get (j);
              Parameter fp = t.parameters[i+1]; // ... the extra scriptic.vm.Node parameter

              if (scp.isAdapting
              ||  scp.isOutput) {

                   if (scp.expression.dataType != fp.dataType1) {
                        throw new CompilerError ("'?' parameters must have same formal and actual type; these are now: "
                                                +  fp.dataType1.getPresentation() + " and "
                                                + scp.dataType .getPresentation(),
                                                 typeDeclaration, scp.sourceStartPosition, scp.sourceEndPosition);
                   }
              }
              scp.expression = scp.expression.convertForAssignmentTo (env, fp.dataType1);
           }
       }
   }
   public int languageConstructCode () {return ScriptCallExpressionCode;}

   private ArrayList<JavaExpression> allParameters;

   public ArrayList<JavaExpression> getAllParameters (CompilerEnvironment env) {
     if (allParameters == null) {
         allParameters         = new ArrayList<JavaExpression>();
         NameExpression n      = new NameExpression();
         n.dataType            = env.scripticVmNodeType;
         n.  nameStartPosition = nameStartPosition;
         n.    nameEndPosition = nameEndPosition;
         n.sourceStartPosition = nameStartPosition;
         n.  sourceEndPosition = nameEndPosition;
         allParameters.add (n);
         if (parameterList!=null)
         for (int i=0; i<parameterList.parameterExpressions.size(); i++) {
             allParameters.add (parameterList.parameterExpressions.get(i));
         }
     }
     return allParameters;
   }
   public String getPresentationName () {
      if (   !isSend
          && !isReceive)
         return super.getPresentationName ();

      StringBuffer presentation = new StringBuffer ();
      presentation.append (super.getPresentationName ());
      presentation.append ("(");
      if (isSend)      presentation.append ("channel send");
      if (isReceive)   presentation.append ("channel receive");
      presentation.append (")");
      return presentation.toString ();
   }
   public String getDescription() {
      StringBuffer result = new StringBuffer();
      result.append(super.getDescription());
      result.append(lineSeparator).append("mode: ").append(getModeString());
      result.append(lineSeparator).append("target: ");
      if (target==null) result.append ("**UNKNOWN**");
      else result.append (target.getDescription());
      return result.toString();
   }
   Instruction invokeInstruction (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
       int invokeInstructionCode = 0;
       switch (mode) {
       case     superMode:
       case   specialMode: invokeInstructionCode = INSTRUCTION_invokespecial  ; break;
       case   virtualMode: invokeInstructionCode = INSTRUCTION_invokevirtual  ; break;
       case    staticMode: invokeInstructionCode = INSTRUCTION_invokestatic   ; break;
       case interfaceMode: invokeInstructionCode = INSTRUCTION_invokeinterface; break;
       }
       return target.invokeInstruction (env,
                                        invokeInstructionCode,
                                        constantPoolOwner,
                                        this);
   }
}

