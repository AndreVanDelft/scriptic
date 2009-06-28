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

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              ScriptTemplateArrayGenerator                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


   /**************
    * Creates a nested array with template information for this and subexpressions.
      Each top level array element is in turn another array
      with 1 up to 4 elements

   [0]: {token, codeBits, sl, sp, el, ep} "LanguageConstruct.generalDescriptor"
         token        - operator/operand type
         codeBits     - 0x000 often (no code)
                        0x001 if there is a code fragment
                        0x011 if there is an extra fragment
                        0x111 3 bits possible for "for(..;..;..)"
         sl,sp,el,ep  - start/end line/position
   [1]: {l, p, len} or {l1,p1,..., ...}
         l, p, len    - line+position+length of central name
         l1,p1, ...   - line+position of n-ary operators (lenght==1)
   [2]: {"name"}      - name of the called script or of the local variable
   [3]: {exclamParams, questParams} for script calls
         exclamParams - 64 bit mask for actual parameters with exclamation mark
         questParams  - 64 bit mask for actual parameters with question mark

    Example:

          AbpApplet__animateDA_template = new scriptic.vm.NodeTemplate
            ("pakkage", "clazz", "scriptName", "(signature)",
              modifiers, nIndexes, nParameters,
              {  {{         ScriptDeclarationCode,        0, sl, sp, el, ep}, {l, p, len}},
                 {{             ParOr2OperatorCode,        0, sl, sp, el, ep}, {l1,p1,l2,p2} }, // 3 operands...
                 {{ScriptLocalDataDeclarationCode, codeBits, sl, sp, el, ep}, {l,  p, len}, {"variableName"} },
                 {{            ActivationCodeCode, codeBits, sl, sp, el, ep}, {l,  p, len} },
                 {{        NativeCodeFragmentCode, codeBits, sl, sp, el, ep} },
                 {{        NativeCodeFragmentCode, codeBits, sl, sp, el, ep} },
                 {{      ScriptCallExpressionCode, codeBits, sl, sp, el, ep}, {l,  p, len}, {"calledScript"}, {exclamParams, questParams, adaptingParameterIndexes}},
                 {{               ChannelSendCode, codeBits, sl, sp, el, ep}, {l,  p, len}, {"channelName"} , {          0L,          0L}}
              }
            );
   }

   To be added:
     expressions such as "int i: a;b;c"
     are changed into "int i: (a;b;c;)
   Problem: don't change "(int i: a);b;c". OK; these are nestedScripts
   *******************************************/


class ScriptTemplateArrayGenerator extends ScripticParseTreeEnumerator
                                implements scriptic.tokens.ScripticTokens,
                                           scriptic.tokens.ScripticParseTreeCodes,
                                           scriptic.vm.TemplateCodes {

   public ArrayList<Object[]> resultVector = new ArrayList<Object[]>();

   void addTemplateDescriptor (LanguageConstruct lc) {
        addTemplateDescriptor (lc, lc.languageConstructCode());
   }
   void addTemplateDescriptor (LanguageConstruct lc, int lcCode) {
	   addTemplateDescriptor (lc, lcCode, 0);
   }
   void addTemplateDescriptor (LanguageConstruct lc, int lcCode, int flags) {
	   addTemplateDescriptor (lc, lcCode, flags, true, null, null);
   }


   // add a descriptor for a single node, related to the given LanguageConstruct
   // this has allways a generalDescriptor with the given codeBits
   // if doNamePositionDescriptor, then also a namePositionDescriptor
   // also an extraDescriptor, if that is not null
   void addTemplateDescriptor (LanguageConstruct lc,
                                int     flags,
                                boolean doNamePositionDescriptor,
                                Object  extraDescriptor1,
                                Object  extraDescriptor2) {
        addTemplateDescriptor (lc, lc.languageConstructCode(), flags, doNamePositionDescriptor,
                               extraDescriptor1, extraDescriptor2);
   }
   // add a descriptor for a single node, related to the given LanguageConstruct
   // this has allways a generalDescriptor with the given codeBits
   // the languageConstructCode is passed as a parameter
   // if doNamePositionDescriptor, then also a namePositionDescriptor
   // also an extraDescriptor, if that is not null
   void addTemplateDescriptor (LanguageConstruct lc,
                               int     lcCode,
                               int     flags,
                               boolean doNamePositionDescriptor,
                               Object  extraDescriptor1,
                               Object  extraDescriptor2) {
      int size = 1;
      if (doNamePositionDescriptor ) size++;
      if ( null != extraDescriptor1) size++;
      if ( null != extraDescriptor2) size++;
      Object[] descriptor = new Object[size];
      int index = 0;
      descriptor[index++]       = generalDescriptor(lc, lcCode, flags);
      if (doNamePositionDescriptor) {
        descriptor[index++] = namePositionDescriptor(lc);
      }
      if (null != extraDescriptor1) descriptor[index++] = extraDescriptor1;
      if (null != extraDescriptor2) descriptor[index++] = extraDescriptor2;
      resultVector.add(descriptor);
   }

   int[] generalDescriptor (LanguageConstruct lc, int lcCode, int flags) {
      int[] result = new int[6];
      result[0] = lcCode;
      result[1] = flags;
      result[2] = scanner.lineAtPosition (           lc.sourceStartPosition);
      result[3] = scanner.positionOnLine (result[2], lc.sourceStartPosition);
      result[4] = scanner.lineAtPosition (           lc.  sourceEndPosition);
      result[5] = scanner.positionOnLine (result[4], lc.  sourceEndPosition);
      return result;
   }

   int[] namePositionDescriptor (LanguageConstruct lc) {
      int[] result = new int[3];
      result[0] = scanner.lineAtPosition (           lc.nameStartPosition);
      result[1] = scanner.positionOnLine (result[0], lc.nameStartPosition);
      result[2] = lc.  nameEndPosition
                - lc.nameStartPosition;
      return result;
   }

   int[] extraPositionsDescriptor (ScriptExpression expression) {
      int[] result = new int [expression.extraPositions().size()*2];
      for (int i=0; i<expression.extraPositions().size(); i++) {
        int p = expression.extraPositions().get(i);
        int line = scanner.lineAtPosition (      p);
        int lpos = scanner.positionOnLine (line, p);
        result[2*i  ] = line;
        result[2*i+1] = lpos;
      }
      return result;
   }

   public ScriptTemplateArrayGenerator (CompilerEnvironment env,
                                        Scanner             scanner,
                                        TypeDeclaration     typeDeclaration,
                                        ClassType           classType) {
      this.env             = env;
      this.scanner         = scanner;
      this.typeDeclaration = typeDeclaration;
      this.classType       = classType;
   }

   // the main entry point
   public Object[][] makeTemplateArrayFor (ScriptTemplateGetMethod m) {
	  int flags = 0;
      BasicScriptDeclaration scriptDeclaration = m.scriptDeclaration();
      if (scriptDeclaration.isSparseActivation)
      {
    	  flags |= SparseActivationFlag;
      }
      resultVector = new ArrayList<Object[]>();
      int constructCode = scriptDeclaration.languageConstructCode();
      if (m instanceof ScriptPartnerTemplateGetMethod) {
          if  (constructCode==ChannelDeclarationCode)
               constructCode =ChannelRequestCode;
          else constructCode = CommunicationRequestCode;
      }
      else if (constructCode==ChannelDeclarationCode) {
          ChannelDeclaration c = (ChannelDeclaration) scriptDeclaration;
               if (c.   isSendChannel) constructCode =    SendChannelDeclarationCode;
          else if (c.isReceiveChannel) constructCode = ReceiveChannelDeclarationCode;
      }
      addTemplateDescriptor(scriptDeclaration, constructCode, flags);

      if (scriptDeclaration.scriptExpression != null
      &&  !(m instanceof ScriptPartnerTemplateGetMethod)) {
          processScriptExpression (scriptDeclaration, scriptDeclaration.scriptExpression);
      }

      // convert result vector to array:
      Object result[][] = new Object[resultVector.size()][];
      for (int i=0; i<result.length; i++) {
         result[i] = resultVector.get(i);
      }
      return result;
   }

   // in case of "int i: a;b", process as if it reads: "int i: (a;b)"
   protected void processScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (expression.languageConstructCode() == InfixExpressionCode) {
          ArrayList<ScriptExpression> expressions = ((InfixExpression)expression).expressions;
          ScriptExpression e = expressions.get(0);
          if (e.languageConstructCode() == ScriptLocalDataDeclarationCode
          ||  e.languageConstructCode() == PrivateScriptDataDeclarationCode) {
          //  we have one. Now take it out, and return it with the rest as the scriptTerm
              ScriptLocalOrPrivateDataDeclaration lpd = (ScriptLocalOrPrivateDataDeclaration) e;
              expressions.set(0, lpd.scriptTerm);
              lpd.scriptTerm = expression;

              super.processScriptExpression (scriptDeclaration, lpd, level, retValue, arg1, arg2);

              lpd.scriptTerm = expressions.get(0);
              expressions.set(0, lpd);

              return;
          }
      }
      super.processScriptExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }
   protected void processInfixExpression (BasicScriptDeclaration scriptDeclaration, InfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int constructCode = -1;
      switch (expression.operator) {
      case BooleanAndToken : constructCode =    ParAnd2OperatorCode; break;
      case BooleanOrToken  : constructCode =     ParOr2OperatorCode; break;
      case HashToken       : constructCode =    SuspendOperatorCode; break;
      case PlusToken       : constructCode =        OrOperatorCode; break;
      case SlashToken      : constructCode =  ParBreakOperatorCode; break;
      case AmpersandToken  : constructCode =       ParAndOperatorCode; break;
      case VerticalBarToken: constructCode =   ParOrOperatorCode; break;
      case PercentToken    : constructCode =    NotSeqOperatorCode; break;
      case SemicolonToken  : constructCode =       SeqOperatorCode; break;
      case  EllipsisToken  : constructCode =  EllipsisOperatorCode; break;
      }
      addTemplateDescriptor(expression, constructCode, expression.operator==EllipsisToken?IterationFlag: 0,
                                                          false, extraPositionsDescriptor (expression), null);
      super.processInfixExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }

   protected void processUnaryScriptExpression       (BasicScriptDeclaration scriptDeclaration, UnaryScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int constructCode = -1;
      switch (expression.unaryToken) {
      case    MinusToken : constructCode = ReactiveNotOperatorCode; break;
      case    TildeToken : constructCode =         NotOperatorCode; break;
      }
      addTemplateDescriptor(expression, constructCode, 0, false, null, null);
      processScriptExpression (scriptDeclaration,
                               expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processSpecialNameScriptExpression (BasicScriptDeclaration scriptDeclaration, SpecialNameScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      int constructCode = -1;
      switch (expression.token) {
      case       DecrementToken :
      case ParenthesisOpenToken : constructCode =     ZeroExpressionCode; break;
      case           MinusToken : constructCode =      OneExpressionCode; break;
      case           BreakToken : constructCode =    BreakExpressionCode; break;
      case          PeriodToken : constructCode =   Ellipsis1OperandCode; break;
      case        EllipsisToken : constructCode =    EllipsisOperandCode; break;
      case       Ellipsis3Token : constructCode =   Ellipsis3OperandCode; break;
     }
      addTemplateDescriptor(expression, constructCode,
    		  (expression.token==EllipsisToken||expression.token==Ellipsis3Token)
    		  ?IterationFlag: 0, false, null, null);
   }

   protected void processNativeCodeFragment          (BasicScriptDeclaration scriptDeclaration, NativeCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      int flags = 0;

      if(!expression.isEmpty()) flags |= CodeFlag;
      if( expression.anchorExpression   != null) {
    	  flags |= AnchorFlag;
          if (expression.isPlainCodeFragment()) {
        	  flags |= AsyncFlag;
          }
      }
      if( expression.durationAssignment != null
      ||  expression.priorityAssignment != null) {

         flags |= InitCodeFlag;

         if (expression.durationAssignment != null) {
            flags |= DurationFlag;
         }
      }
      int constructCode = NativeCodeFragmentCode;
      switch (expression.startingDelimiter) {
      case BraceColonOpenToken    : constructCode =     TinyCodeFragmentCode; break;
      case BraceAsteriskOpenToken : constructCode = ThreadedCodeFragmentCode; break;
      case BraceQuestionOpenToken : constructCode =   UnsureCodeFragmentCode; break;
      case BraceQuestionCloseToken: constructCode =  Unsure2CodeFragmentCode; break;
      }
      addTemplateDescriptor(expression, constructCode, flags, false, null, null);
   }

   protected void processEventHandlingCodeFragment   (BasicScriptDeclaration scriptDeclaration, EventHandlingCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      int flags = 0;
      if(!expression.isEmpty()) flags |=   CodeFlag;
      /*because of anchor: */   flags |= AnchorFlag;

      int typeCode = 0;
      switch (expression.startingDelimiter)
      {
    	case BracePeriodOpenToken   : typeCode = EventHandlingCodeFragmentCode; break;
      	case BraceEllipsisOpenToken : typeCode = EventHandling0PlusCodeFragmentCode; break;
      	case BraceEllipsis3OpenToken: 
      		if (expression.endingDelimiter==BraceEllipsis3CloseToken) {
      			typeCode = EventHandling1PlusCodeFragmentCode; 
      		}
      		else
      		{
      			typeCode = EventHandlingManyCodeFragmentCode; 
      		}
      		break;
      }
      addTemplateDescriptor(expression, typeCode, flags, false, null, null);
   }

   protected void processNestedScriptExpression      (BasicScriptDeclaration scriptDeclaration, NestedScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (expression.startingDelimiter != ParenthesisOpenToken) {
          int code = -1;
          switch (expression.startingDelimiter) {
          case         ParenthesisOpenToken: code =   NestedScriptExpressionCode; break; // not used
          case                LessThanToken: code =       LaunchedExpressionCode; break; 
          }
          addTemplateDescriptor(expression, code, 0, true, null, null);
      }
      processScriptExpression (scriptDeclaration, expression.subExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processActivationCode          (BasicScriptDeclaration scriptDeclaration, ActivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = 0;
      if(!expression.activationCode.isEmpty()) flags |= CodeFlag;
      if( expression.activationCode.anchorExpression != null // still messy ... where does the anchor belong?
      ||  expression.anchorExpression != null) flags |= AnchorFlag;
      addTemplateDescriptor(expression, flags, true, null, null);
      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processDeactivationCode        (BasicScriptDeclaration scriptDeclaration, DeactivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = 0;
      if(!expression.deactivationCode.isEmpty()) flags |= CodeFlag;
      if( expression.deactivationCode.anchorExpression != null // still messy ... where does the anchor belong?
      ||  expression.anchorExpression != null  ) flags |= AnchorFlag;
      addTemplateDescriptor(expression, flags, true, null, null);
      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processConditionalScriptExpression (BasicScriptDeclaration scriptDeclaration, ConditionalScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      addTemplateDescriptor(expression, expression.failureTerm == null? 0: SecondTermFlag,
                            false, extraPositionsDescriptor (expression), null);
      super.processConditionalScriptExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }

   protected void processIfScriptExpression      (BasicScriptDeclaration scriptDeclaration, IfScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = 0;
           if (expression.elseTerm   !=  null) flags |= SecondTermFlag;
           if (expression.condition.isTrue ()) flags |=       TrueFlag;
      else if (expression.condition.isFalse()) flags |=      FalseFlag;
      else                                     flags |=       CodeFlag;
      if( expression.anchorExpression != null) flags |= AnchorFlag;
      addTemplateDescriptor(expression, flags,
                            false, extraPositionsDescriptor (expression), null);
      super.processIfScriptExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }

   protected void processWhileScriptExpression   (BasicScriptDeclaration scriptDeclaration, WhileScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = IterationFlag;
      flags |= expression.condition.isTrue ()? TrueFlag
             : expression.condition.isFalse()?FalseFlag
                                             : CodeFlag;
      if( expression.anchorExpression != null) flags |= AnchorFlag;
      addTemplateDescriptor(expression, flags, true, null, null);
   }

   protected void processForScriptExpression    (BasicScriptDeclaration scriptDeclaration, ForScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = IterationFlag;
      if (expression.condition      == null
      ||  expression.condition.isTrue()    ) flags |=     TrueFlag;
      else
      if (expression.condition.isFalse()   ) flags |=    FalseFlag;
      else                                   flags |=     CodeFlag;
      if (expression.initExpression != null) flags |= InitCodeFlag;
      if (expression.loopExpression != null) flags |= NextCodeFlag;
      if( expression.anchorExpression != null) flags |= AnchorFlag;
      addTemplateDescriptor(expression, flags, true, null, null);
   }

   protected void processSwitchScriptExpression  (BasicScriptDeclaration scriptDeclaration, SwitchScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int flags = CodeFlag;
      if( expression.anchorExpression != null) flags |= AnchorFlag;

      addTemplateDescriptor (expression, flags, false, extraPositionsDescriptor (expression), null);
      super.processSwitchScriptExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }

   protected void processCaseTagScriptExpression (BasicScriptDeclaration scriptDeclaration, CaseTagScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processScriptExpression (scriptDeclaration, expression.caseExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processScriptCallExpression    (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      int constructCode = ScriptCallExpressionCode;
      if (expression.isScriptChannelSend   ()) constructCode = ChannelSendCode;
      if (expression.isScriptChannelReceive()) constructCode = ChannelReceiveCode;

      ArrayList<JavaExpression> parameters = expression.parameterList.parameterExpressions;
      long parameterDescriptor[] = new long[2];
      for (int i=0; i<parameters.size(); i++) {
    	  ScriptCallParameter param = (ScriptCallParameter) parameters.get (i);
         if (param.isOutput  || param.isAdapting) parameterDescriptor[0] |= 1<<i;
         if (param.isForcing || param.isAdapting) parameterDescriptor[1] |= 1<<i;
         if (                   param.isAdapting) {
            long oldArray[] = parameterDescriptor;
            parameterDescriptor = new long [oldArray.length+1];
            System.arraycopy (oldArray, 0, parameterDescriptor, 0, oldArray.length);

if (param.target==null)
System.out.println ("param.target==null: "+param.getPresentation());

            parameterDescriptor [oldArray.length] = ((ScriptParameter) param.target).slot;
         }
      }
      String array[] = new String[1];
      array [0] = expression.name;
      int flags = CodeFlag;
      if( expression.anchorExpression != null) flags |= AnchorFlag;
      addTemplateDescriptor (expression, constructCode, flags, true, array, parameterDescriptor);
   }

   protected void processScriptLocalVariable  (BasicScriptDeclaration scriptDeclaration, LocalScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      String array[] = new String[1];
      array [0] = variableDeclaration.name;
      addTemplateDescriptor (variableDeclaration, ScriptLocalDataDeclarationCode, CodeFlag, true, array, null);
   }

   protected void processPrivateScriptVariable  (BasicScriptDeclaration scriptDeclaration, PrivateScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      String array[] = new String[1];
      array [0] = variableDeclaration.name;
      addTemplateDescriptor (variableDeclaration, PrivateScriptDataDeclarationCode, CodeFlag, true, array, null);
   }
}


