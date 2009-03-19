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

import scriptic.tools.lowlevel.ByteCodingException;
import scriptic.tools.lowlevel.SourceFileAttribute;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                      BasicScriptDeclaration                     */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

abstract class BasicScriptDeclaration 
         extends RefinementDeclaration
         implements scriptic.tokens.ScripticParseTreeCodes, scriptic.tokens.ScripticTokens {

   public ScriptExpression    scriptExpression;
   public int                 sequenceNumber = -1;
   public boolean isSparseActivation;
   public ScriptTemplateVariable  templateVariable; 
   public ScriptTemplateGetMethod templateGetMethod; 

   public ScriptMethod            scriptMethod;
   public ScriptCodeMethod        codeMethod;
   public Method              codeTarget  () {return codeMethod;}

   /* --------------------------- Constructors ------------------------- */

   public BasicScriptDeclaration () {}
   public BasicScriptDeclaration (BasicScriptDeclaration anotherDeclaration) { 
      super ((RefinementDeclaration) anotherDeclaration);
      this.scriptExpression   = anotherDeclaration.scriptExpression;
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isAnyKindofScriptDeclaration () {
      return true;
   }
   
   public boolean isMainScript () {
      return (   isScriptDeclaration ()
              && modifiers.isPublic ()
              && modifiers.isStatic ()
              && "main".equals (getName())
              && parameterList != null
              && parameterList.isMainMethodParameterList ());
    }

   public int getSequenceNumber       () {return sequenceNumber;}
   public int getFormalIndexCount() {
		return 0;	
  }

   public boolean hasFormalParameters () {
      return (   parameterList != null 
              && parameterList.parameters.size () > 0);
   }

   /** return the parameter index of a given parameter,
    *  as applicable for isForced etc.
    *  Example: for a[10 i](int p) b[](int q, int r)
    *       the result for parameters p, q and r would be: 0, 1 and 2
    */
   int paramIndexOf (NormalOrOldScriptParameter p) {
/******************
      if (parameterList.parameters.indexOf(p.source)==-1) {
         System.out.println ("Parameter not found: "+p.getDescription()+" - "+p.getClass());
         for (int i=0; i<parameterList.parameters.size(); i++) {
           FormalIndexOrParameter q = (FormalIndexOrParameter) parameterList.parameters.elementAt(i);
           System.out.println ("Parameter@"+i+": "+q.getDescription()+" - "+q.getClass());
         }
      }
************/
      return parameterList.parameters.indexOf(p.source);
   }
   public Method makeTarget (CompilerEnvironment env) {
       target = scriptMethod = new ScriptMethod(this);
       scriptMethod.source         = this;
       scriptMethod.name           = name;
       scriptMethod.owner          = typeDeclaration.target;
       scriptMethod.modifierFlags  = modifiers.modifierFlags;
       if (isDeprecated) scriptMethod.setDeprecated();

       if (typeDeclaration.isInterface())scriptMethod.modifierFlags |= AbstractFlag;

       int nParameters             = parameterList==null?0: parameterList.parameters.size();
       int nNodeParameters         = isMainScript()? 0: 1;
       int nTargetParameters       = nNodeParameters
                                   + nParameters;
       scriptMethod.parameters     = new Parameter [nTargetParameters];

       if (!isMainScript ()) {
         Parameter firstParameter    = new Parameter();
         firstParameter.dataType1    = env.scripticVmNodeType;
         firstParameter.name         = "_n_";
         scriptMethod.parameters[0]  = firstParameter;
       }

       if (nParameters > 0) {
         for (int i=0; i<parameterList.parameters.size(); i++) {
             MethodParameterDeclaration parameterDeclaration
               = parameterList.parameters.get(i);
             ScriptParameter parameter = parameterDeclaration.makeTargetForScript(env, i);
             scriptMethod   .parameters[nNodeParameters+i] = parameter;
             parameter.slot= parameter.oldVersion.slot= i;
         }
       }
       scriptMethod.getSignature (env);
       return scriptMethod;
   }

   /* ---------------------------- Presentation ------------------------ */

   public String getPresentation () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getPresentationName ());
      presentation.append (' ');
      presentation.append (getNameSignature());
      return presentation.toString ();
   }
   public String getNameSignature () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getName ());
      return presentation.toString ();
   }

   public String getParameterSignature (ClassesEnvironment env) {

      if (target==null) return ""; // needed for communications "a,b" etc.
      return target.getParameterSignature(env);
   }
   /* ------------------------- Code generation ------------------------ */


   public static final int PaddedNumberStringLength = 3;

   /* Sequence number, padded with zeros */
   public String getZeroPaddedString (int number) {
      if (number < 0)
         return new String ();

      String numberString = String.valueOf (number);
      if (numberString.length() >= PaddedNumberStringLength)
         return numberString;

      char [ ] numberChars = new char [ PaddedNumberStringLength ];
      for (int i = 0; i < numberChars.length ; i++)
         numberChars [i] = '0';
      numberString.getChars (0, numberString.length(),
                             numberChars, 
                             numberChars.length - numberString.length());
      return new String (numberChars);
   }

   /* Sequence number, padded with zeros */
   public String getSequenceNumberString () {
      return getZeroPaddedString (getSequenceNumber ());
   }

   public String getScriptStringFormalParameters () {
      return "()";
   }

   public String        getScriptStringName() {return getName()+getScriptStringFormalParameters();}
   public String getPartnerScriptStringName() {return getName()+getScriptStringFormalParameters();}

   public String getPackageClassScriptNameSequenceNumber() {
      String packageClassName = typeDeclaration.fullNameWithDots().replace ('.', '_');

      if (getSequenceNumber () >= 0)
         return   packageClassName  + "__" 
                + getName () + "_" 
                + getSequenceNumberString();
      else
         return   packageClassName  + "__" + getName ();
   }

   public String getNameSequenceNumber () {
      if (getSequenceNumber () >= 0)
           return getName () + "_" 
                + getSequenceNumberString();
      else return getName ();
   }

   public String getScripticVmPackagePrefix() {return "scriptic.vm.";}

   public String        getTemplateName () {return getNameSequenceNumber () + "_template";}
   public String getPartnerTemplateName () {return getNameSequenceNumber () + "_template";}
   public ArrayList<CommunicationPartnerDeclaration> getPartners () {return null;}
   public int getPartnerIndex () {return -1;}

   public String getCodeMethodName() {
		return getPackageClassScriptNameSequenceNumber()+"_code";
   }

   public void outLocalDataAccess (PreprocessorOutputStream stream,
                                   Variable variable,
                                   CompilerEnvironment env,
                                   boolean loadReferenceOnly) throws ByteCodingException, IOException {

      if (variable.isScriptParameter ()) {
         outParameterAccess (stream, (ScriptParameter)variable, env, loadReferenceOnly);
         return;
      }
      if (variable.isOldParameter ()) {
         outOldParameterAccess (stream, (OldParameter)variable, env);
         return;
      }
      if (variable.isScriptLocalVariable ()) {
         outLocalVariableAccess (stream, (ScriptLocalVariable)variable, env, loadReferenceOnly);
         return;
      }
      if (variable.isPrivateVariable ()) {
         outPrivateVariableAccess (stream, (ScriptPrivateVariable)variable, env, loadReferenceOnly);
         return;
      }
    //isMemberVariable, or the like
      outString (stream, variable.name);
/*********/
   }

   public void outParameterAccess (PreprocessorOutputStream stream,
                                   ScriptParameter target,
                                   CompilerEnvironment env,
                                   boolean loadReferenceOnly) throws ByteCodingException, IOException {
      BasicScriptDeclaration owner 
               = (BasicScriptDeclaration) target.source.owner;;
      int paramDataIndex = owner.getPartnerIndex ();
      String extraCastString = !loadReferenceOnly
                            && target.getDataType(env).holderClass(env)
                            == env.scripticVmObjectHolderType? 
                                  target.getDataType(env).getNameWithDots(): 
                                  null;
      outAccess (stream,
                 extraCastString,
                 target.getDataType(env).holderClass(env).getNameWithDots(),
                 "paramData",
                 true,
                 paramDataIndex,
                 target.slot,
                 "value",
                 false);
   }
   public void outOldParameterAccess (PreprocessorOutputStream stream,
                                      OldParameter target,
                                      CompilerEnvironment env) throws ByteCodingException, IOException {
      BasicScriptDeclaration owner 
               = (BasicScriptDeclaration) target.source.owner;;
      int oldParamDataIndex = owner.getPartnerIndex ();

      // Special treatment for BIDIRECTIONAL CHANNELS:
      // Must get "old" parameter from paramData(1)
      // instead of paramData(0)
      if( owner.isChannelDeclaration ()
      && ((ChannelDeclaration)owner).isBidirectionalChannel) {
         oldParamDataIndex = 1;
      }
      ClassType wrapperClass = target.getDataType(env).wrapperClass(env);
      String className = wrapperClass==env.javaLangObjectType? 
                                  target.getDataType(env).getNameWithDots(): 
                                  wrapperClass.getNameWithDots();
      outAccess (stream,
                 null,
                 className,
                 "oldParamData",
                 true,
                 oldParamDataIndex,
                 target.slot,
                 target.getDataType(env).accessNameForWrapperClass(),
                 true);
   }
   public void outLocalVariableAccess (PreprocessorOutputStream stream,
                                       ScriptLocalVariable target,
                                       CompilerEnvironment env,
                                       boolean loadReferenceOnly) throws ByteCodingException, IOException {
      String extraCastString = !loadReferenceOnly
                            && target.getDataType(env).holderClass(env)
                            == env.scripticVmObjectHolderType? 
                                  target.getDataType(env).getNameWithDots(): 
                                  null;
      if (target.slot >= 0) {
         outAccess (stream,
                    extraCastString,
                    target.getDataType(env).holderClass(env).getNameWithDots(),
                    "localData",
                    false,
                    -1,
                    target.slot,
                    "value",
                    false);
      } else {
         outString (stream, target.name);
      }
   }
   public void outPrivateVariableAccess (PreprocessorOutputStream stream,
                                         ScriptPrivateVariable target,
                                         CompilerEnvironment env,
                                         boolean loadReferenceOnly) throws ByteCodingException, IOException {
      String extraCastString = !loadReferenceOnly
                            && target.getDataType(env).holderClass(env)
                            == env.scripticVmObjectHolderType? 
                                  target.getDataType(env).getNameWithDots(): 
                                  null;
      if (target.slot >= 0) {
         outAccess (stream,
                    extraCastString,
                    target.getDataType(env).holderClass(env).getNameWithDots(),
                    "localData",
                    false,
                    -1,
                    target.slot,
                    "value",
                    false);
      } else {
         outString (stream, target.name);
      }
   }

   public void outAccess (PreprocessorOutputStream stream,
                          String  extraCastString,
                          String  classCastName,
                          String  accessConstructName,
                          boolean accessConstructIsFunction,
                          int     accessArgument1,
                          int     accessArgument2,
                          String  accessFieldName,
                          boolean accessFieldIsFunction) {

      // ((A__a_params)_n_.paramData()).p
      // ((A__a_params)_n_.paramData(n)).p
      // ((A__a_local_002)_n_.localData[2]).i

      // ((Integer)_n_.oldParamData()[0]).intValue();
      // ((scriptic.vm.IntHolder)_n_.paramData(n)[0]).value
      // ((scriptic.vm.IntHolder)_n_.localData[2]).value

      if (extraCastString != null) {
        outToken  (stream, ParenthesisOpenToken);
        outToken  (stream, ParenthesisOpenToken);
        outString (stream, extraCastString);
        outToken  (stream, ParenthesisCloseToken);  
      }
      outToken  (stream, ParenthesisOpenToken);
      outToken  (stream, ParenthesisOpenToken);
      outString (stream, classCastName);
      outToken  (stream, ParenthesisCloseToken);
      outString (stream, "_n_");
      outToken  (stream, PeriodToken);
      outString (stream, accessConstructName);
      if (accessConstructIsFunction)
      {
        outToken  (stream, ParenthesisOpenToken);
        if (accessArgument1 >= 0)
            outString (stream, String.valueOf (accessArgument1));
        outToken  (stream, ParenthesisCloseToken);
      }
      outToken  (stream, BracketOpenToken);
      outString (stream, String.valueOf (accessArgument2));
      outToken  (stream, BracketCloseToken);
      outToken  (stream, ParenthesisCloseToken);
      if (accessFieldName != null) {
        outToken  (stream, PeriodToken);
        outString (stream, accessFieldName);
        if (accessFieldIsFunction) {
          outToken  (stream, ParenthesisOpenToken);
          outToken  (stream, ParenthesisCloseToken);    
        }
      }
      if (extraCastString != null) {
        outToken  (stream, ParenthesisCloseToken);  
      }
   }

   public void outParameterDeclarations (PreprocessorOutputStream stream,
                                         boolean firstParameter) {

      if (hasFormalParameters ()) 
         parameterList.outDeclaration (stream, firstParameter);
   }

   public void outClassComponentName (PreprocessorOutputStream stream) {
      String className = typeDeclaration.getName ();
      outString (stream, className.replace ('.', '_'));
   }
}

