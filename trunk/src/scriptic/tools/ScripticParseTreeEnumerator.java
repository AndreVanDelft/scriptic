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



import java.util.ArrayList;



public class ScripticParseTreeEnumerator
         extends JavaParseTreeEnumerator
         implements scriptic.tokens.ScripticParseTreeCodes {

   /* Please read the comments in JavaParseTreeEnumerator 
      about scanner and errorHandler */


   /* -------------------------- Constructors ---------------------------*/

   public ScripticParseTreeEnumerator (Scanner scanner) {
      super (scanner);
   }
   
   public ScripticParseTreeEnumerator (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
   }

   /* Constructor without arguments; can be used when parserError()
      isn't called */
   public ScripticParseTreeEnumerator () {
      super (null, null);
   }



   /* -------------- Additional default main entry points -------------- */

   public Object processScriptExpression (ScriptExpression expression) {
      return processScriptExpression (null, expression);
   }

   public Object processScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression) {
      ReturnValueHolder retValue = new ReturnValueHolder ();
      processTopLevelScriptExpression (scriptDeclaration, expression, 0, retValue, null, null);
      return retValue.value;
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        Field Declaration                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


   protected void processFieldDeclaration (FieldDeclaration fieldDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (fieldDeclaration.languageConstructCode ()) {
         case MultiVariableDeclarationCode :
                  processMultiVariableDeclaration ((MultiVariableDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case MethodDeclarationCode :
                  processMethodDeclaration        ((MethodDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case ConstructorDeclarationCode :
                  processConstructorDeclaration   ((ConstructorDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case InitializerBlockCode :
                  processInitializerBlock        ((InitializerBlock)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case CommentFieldDeclarationCode :
                  processCommentFieldDeclaration  ((CommentFieldDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case BaseDimensionDeclarationCode :
                  processBaseDimensionDeclaration  ((BaseDimensionDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case CompoundDimensionDeclarationCode :
                  processCompoundDimensionDeclaration  ((CompoundDimensionDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;

         case ScriptDeclarationCode :
                  processScriptDeclaration        ((ScriptDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case CommunicationDeclarationCode :
                  processCommunicationDeclaration ((CommunicationDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case ChannelDeclarationCode :
                  processChannelDeclaration       ((ChannelDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case NestedTypeDeclarationCode :
                  processNestedTypeDeclaration    ((NestedTypeDeclaration) fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         default:
                  throw new RuntimeException ("Unknown kind of FieldDeclaration");
      }
   }

   protected void processBasicScriptDeclaration   (BasicScriptDeclaration scriptDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (scriptDeclaration, level, retValue, arg1, arg2);

      if (scriptDeclaration.parameterList != null)
         processParameterList    ((RefinementDeclaration)scriptDeclaration, scriptDeclaration.parameterList, level + 1, retValue, arg1, arg2);
      if (scriptDeclaration.scriptExpression != null)
         processTopLevelScriptExpression (scriptDeclaration, scriptDeclaration.scriptExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processScriptDeclaration        (ScriptDeclaration scriptDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processBasicScriptDeclaration ((BasicScriptDeclaration) scriptDeclaration, level, retValue, arg1, arg2);
   }

   protected void processCommunicationDeclaration (CommunicationDeclaration communicationDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (communicationDeclaration, level, retValue, arg1, arg2);

      processCommunicationPartners (communicationDeclaration, communicationDeclaration.partners, level + 1, retValue, arg1, arg2);
      if (communicationDeclaration.scriptExpression != null)
         processTopLevelScriptExpression (communicationDeclaration, communicationDeclaration.scriptExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processChannelDeclaration       (ChannelDeclaration channelDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processBasicScriptDeclaration ((BasicScriptDeclaration) channelDeclaration, level, retValue, arg1, arg2);
   }

   protected void processCommunicationPartners (CommunicationDeclaration communicationDeclaration, 
		   ArrayList<CommunicationPartnerDeclaration> partners, int level, ReturnValueHolder retValue, 
		   Object arg1, Object arg2) {
      for (BasicScriptDeclaration bsd: partners) {
    	 CommunicationPartnerDeclaration cpd = (CommunicationPartnerDeclaration) bsd;
         processCommunicationPartner (communicationDeclaration, cpd, level, retValue, arg1, arg2);
      }
   }

   protected void processCommunicationPartner   (CommunicationDeclaration communicationDeclaration, CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (partner, level, retValue, arg1, arg2);

      if (partner.parameterList != null)
         processParameterList    ((RefinementDeclaration)partner, partner.parameterList, level + 1, retValue, arg1, arg2);
   }




   /*******************************************************************/
   /**                                                               **/
   /**                      SCRIPT EXPRESSIONS                       **/
   /**                                                               **/
   /*******************************************************************/

   protected void processTopLevelScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processScriptExpression (scriptDeclaration, expression, level, retValue, arg1, arg2);
   }

   protected void processScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (expression.languageConstructCode ()) {
         case LayoutScriptExpressionCode :
                  processLayoutScriptExpression (scriptDeclaration, (LayoutScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case InfixExpressionCode :
                  processInfixExpression        (scriptDeclaration, (InfixExpression) expression, level, retValue, arg1, arg2);
                  break;
         case UnaryScriptExpressionCode :
                  processUnaryScriptExpression  (scriptDeclaration, (UnaryScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case SpecialNameScriptExpressionCode :
                  processSpecialNameScriptExpression (scriptDeclaration, (SpecialNameScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case NestedScriptExpressionCode :
                  processNestedScriptExpression (scriptDeclaration, (NestedScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case NativeCodeFragmentCode :
                  processNativeCodeFragment     (scriptDeclaration, (NativeCodeFragment) expression, level, retValue, arg1, arg2);
                  break;
         case EventHandlingCodeFragmentCode :
                  processEventHandlingCodeFragment (scriptDeclaration, (EventHandlingCodeFragment) expression, level, retValue, arg1, arg2);
                  break;
         case ActivationCodeCode :
                  processActivationCode         (scriptDeclaration, (ActivationCode) expression, level, retValue, arg1, arg2);
                  break;
         case DeactivationCodeCode :
                  processDeactivationCode       (scriptDeclaration, (DeactivationCode) expression, level, retValue, arg1, arg2);
                  break;
         case ConditionalScriptExpressionCode :
                  processConditionalScriptExpression (scriptDeclaration, (ConditionalScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case IfScriptExpressionCode :
                  processIfScriptExpression     (scriptDeclaration, (IfScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case WhileScriptExpressionCode :
                  processWhileScriptExpression  (scriptDeclaration, (WhileScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case ForScriptExpressionCode :
                  processForScriptExpression    (scriptDeclaration, (ForScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case SwitchScriptExpressionCode :
                  processSwitchScriptExpression (scriptDeclaration, (SwitchScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case CaseTagScriptExpressionCode :
                  processCaseTagScriptExpression (scriptDeclaration, (CaseTagScriptExpression) expression, level, retValue, arg1, arg2);
                  break;
         case ScriptCallExpressionCode :
                  processScriptCallExpression   (scriptDeclaration, (ScriptCallExpression) expression, level, retValue, arg1, arg2);
                  break;
         case ScriptLocalDataDeclarationCode :
                  processScriptLocalDataDeclaration   (scriptDeclaration, (ScriptLocalDataDeclaration) expression, level, retValue, arg1, arg2);
                  break;
         case PrivateScriptDataDeclarationCode :
                  processPrivateScriptDataDeclaration (scriptDeclaration, (PrivateScriptDataDeclaration) expression, level, retValue, arg1, arg2);
                  break;
         default:
              throw new RuntimeException ("Unknown kind of ScriptExpression; code: "+expression.languageConstructCode ());
      }
   }

   protected void processScriptExpressions (BasicScriptDeclaration scriptDeclaration, ArrayList<ScriptExpression> scriptExpressions, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (ScriptExpression se: scriptExpressions) {
         processScriptExpression (scriptDeclaration, se, level, retValue, arg1, arg2);
      }
   }

   protected void processLayoutScriptExpression      (BasicScriptDeclaration scriptDeclaration, LayoutScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Default is to completely skip/ignore the LayoutScriptExpression.
         To do anything with it, this method must be explicitly overridden. */
      processScriptExpression (scriptDeclaration, expression.realExpression, level, retValue, arg1, arg2);
   }

   protected void processInfixExpression             (BasicScriptDeclaration scriptDeclaration, InfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processScriptExpressions (scriptDeclaration, expression.expressions, level + 1, retValue, arg1, arg2);
   }

   protected void processUnaryScriptExpression       (BasicScriptDeclaration scriptDeclaration, UnaryScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processScriptExpression (scriptDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processSpecialNameScriptExpression (BasicScriptDeclaration scriptDeclaration, SpecialNameScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
   }

   protected void processNestedScriptExpression      (BasicScriptDeclaration scriptDeclaration, NestedScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processScriptExpression (scriptDeclaration, expression.subExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processNativeCodeFragment          (BasicScriptDeclaration scriptDeclaration, NativeCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      if (expression.anchorExpression != null)
          processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
      if (expression.durationAssignment != null)
         processJavaStatement  (scriptDeclaration, expression.durationAssignment, level + 1, retValue, arg1, arg2);
      if (expression.priorityAssignment != null)
         processJavaStatement  (scriptDeclaration, expression.priorityAssignment, level + 1, retValue, arg1, arg2);

      processJavaStatements    (scriptDeclaration, expression.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processEventHandlingCodeFragment   (BasicScriptDeclaration scriptDeclaration, EventHandlingCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      if (expression.anchorExpression != null)
         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
      processJavaStatements    (scriptDeclaration, expression.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processActivationCode          (BasicScriptDeclaration scriptDeclaration, ActivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processScriptExpression  (scriptDeclaration, expression.activationCode, level + 1, retValue, arg1, arg2);
      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processDeactivationCode            (BasicScriptDeclaration scriptDeclaration, DeactivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
      processScriptExpression  (scriptDeclaration, expression.deactivationCode, level + 1, retValue, arg1, arg2);
   }

   protected void processConditionalScriptExpression (BasicScriptDeclaration scriptDeclaration, ConditionalScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      processScriptExpression (scriptDeclaration, expression.condition, level + 1, retValue, arg1, arg2);
      processScriptExpression (scriptDeclaration, expression.successTerm, level + 1, retValue, arg1, arg2);
      if (expression.failureTerm != null)
         processScriptExpression (scriptDeclaration, expression.failureTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processIfScriptExpression      (BasicScriptDeclaration scriptDeclaration, IfScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      processJavaExpression   (scriptDeclaration, expression.condition, level + 1, retValue, arg1, arg2);
      processScriptExpression (scriptDeclaration, expression.ifTerm, level + 1, retValue, arg1, arg2);
      if (expression.elseTerm != null)
         processScriptExpression (scriptDeclaration, expression.elseTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processWhileScriptExpression   (BasicScriptDeclaration scriptDeclaration, WhileScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression    (scriptDeclaration, expression.condition, level + 1, retValue, arg1, arg2);
   }

   protected void processForScriptExpression    (BasicScriptDeclaration scriptDeclaration, ForScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      if (expression.initExpression != null)
         processJavaExpression (scriptDeclaration, expression.initExpression, level + 1, retValue, arg1, arg2);
      if (expression.condition != null)
         processJavaExpression (scriptDeclaration, expression.condition, level + 1, retValue, arg1, arg2);
      if (expression.loopExpression != null)
         processJavaExpression (scriptDeclaration, expression.loopExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processSwitchScriptExpression  (BasicScriptDeclaration scriptDeclaration, SwitchScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      processJavaExpression (scriptDeclaration, expression.switchExpression, level + 1, retValue, arg1, arg2);

      for (CaseTagScriptExpression caseTag: expression.caseTags) {
         processCaseTagScriptExpression (scriptDeclaration, caseTag, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processCaseTagScriptExpression (BasicScriptDeclaration scriptDeclaration, CaseTagScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      for (JavaExpression tag: expression.tags) {
         processJavaExpression (scriptDeclaration, tag, level + 1, retValue, arg1, arg2);
      }
      processScriptExpression (scriptDeclaration, expression.caseExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processScriptCallExpression    (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (scriptDeclaration, expression.scriptAccessExpression, level + 1, retValue, arg1, arg2);
      if (expression.parameterList != null)
         processScriptCallParameterList (scriptDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
   }

   protected void processScriptCallParameterList  (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, MethodCallParameterList parameterList, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (parameterList.parameterExpressions.isEmpty()) return;
      processLanguageConstruct     (parameterList, level, retValue, arg1, arg2);

      for (JavaExpression obj: parameterList.parameterExpressions) {
         ScriptCallParameter scp = (ScriptCallParameter) obj;
         processScriptCallParameter (scriptDeclaration, scriptCall, scp, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processScriptCallFormalIndex    (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, JavaExpression formalIndex, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression       (scriptDeclaration, formalIndex, level, retValue, arg1, arg2);
   }

   protected void processScriptCallParameter      (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, ScriptCallParameter parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct    (parameter, level, retValue, arg1, arg2);
      processJavaExpression       (scriptDeclaration, parameter.expression, level + 1, retValue, arg1, arg2);
   }


   protected void processScriptLocalDataDeclaration  (BasicScriptDeclaration scriptDeclaration, ScriptLocalDataDeclaration expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct        (expression, level, retValue, arg1, arg2);

      for (Object obj: expression.variables.variables) {
    	  LocalScriptVariableDeclaration lsv = (LocalScriptVariableDeclaration) obj;
    	  processScriptLocalVariable (scriptDeclaration, lsv, level + 1, retValue, arg1, arg2);
      }

      processScriptExpression (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processScriptLocalVariable  (BasicScriptDeclaration scriptDeclaration, LocalScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (variableDeclaration, level, retValue, arg1, arg2);
      if (variableDeclaration.initializer != null)
         processJavaExpression (scriptDeclaration, variableDeclaration.initializer, level + 1, retValue, arg1, arg2);
   }


   protected void processPrivateScriptDataDeclaration (BasicScriptDeclaration scriptDeclaration, PrivateScriptDataDeclaration expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);

      for (Object obj: expression.variables) {
    	  PrivateScriptVariableDeclaration psv = (PrivateScriptVariableDeclaration) obj;
         processPrivateScriptVariable (scriptDeclaration, psv, level + 1, retValue, arg1, arg2);
      }

      processScriptExpression (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processPrivateScriptVariable  (BasicScriptDeclaration scriptDeclaration, PrivateScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (variableDeclaration, level, retValue, arg1, arg2);
   }
}


