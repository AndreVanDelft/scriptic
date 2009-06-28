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


import java.io.File;
import java.util.ArrayList;

public class ScripticParser extends JavaParser
   implements scriptic.tokens.ScripticTokens, scriptic.tokens.ScripticParseTreeCodes {

   public ScripticParser (Scanner scanner, File sourceFile, CompilerEnvironment env) {
      super (scanner, sourceFile, env);
   }
   public ScripticParser (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
   }


   protected TypeDeclaration  currentTypeDeclaration;

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

   /* This method overridden to ... */

   protected CompilationUnit parseCompilationUnit (String compilationUnitName) {
      return super.parseCompilationUnit (compilationUnitName);
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


   /*******************************************************************/
   /**                                                               **/
   /**                   SCRIPT-FIELD DECLARATIONS                   **/
   /**                                                               **/
   /*******************************************************************/

   protected boolean parseFieldDeclaration (TypeDeclaration typeDeclaration,
                                            ModifierList modifierList) {

      if (scanner.token == ScriptsToken) {
         typeDeclaration.hasScripts = true;
         scanner.next ();

         ScriptDeclaration s = new ScriptDeclaration();
         s.typeDeclaration   = typeDeclaration;
         if (!modifierList.checkModifiers (s, this)) 
            return false;
         if ((modifierList.modifiers & PrivateFlag) != 0) {
            parserError (2, "Private scripts are not supported");
         }
         if (!typeDeclaration.isPublic()) {
             parserError (2, "Scripts may only be declared in public classes");
         }
         return parseScriptsSection (typeDeclaration, modifierList);
      }

      if (scanner.token == ScriptToken) {
         typeDeclaration.hasScripts = true;

         ScriptDeclaration s = new ScriptDeclaration();
         s.typeDeclaration   = typeDeclaration;
         if (!modifierList.checkModifiers (s, this)) 
            return false;
         if ( (modifierList.modifiers & PrivateFlag) != 0) {
            parserError (2, "Private scripts are not supported");
         }
         if (!typeDeclaration.isPublic()) {
             parserError (2, "Scripts may only be declared in public classes");
         }

         if (scanner.next() == ScriptsToken) {
            /*   script scripts (...) { ... }   */
            if (!nextToken (ParenthesisOpenToken)) return false;
            scanner.next ();
            while (scanner.token == IdentifierToken
               || (scanner.token >= FirstLiteralToken
                && scanner.token <= LastLiteralToken)
               ||  scanner.token == PeriodToken
               ||  scanner.token == CommaToken
               ||  scanner.token == BooleanToken
               ||  scanner.token == CharToken
               ||  scanner.token == ByteToken
               ||  scanner.token == ShortToken
               ||  scanner.token == IntToken
               ||  scanner.token == LongToken
               ||  scanner.token == FloatToken
               ||  scanner.token == DoubleToken)
               scanner.next ();

            if (!expectToken (ParenthesisCloseToken)) return false;
            if (!nextToken (BraceOpenToken)) return false;
            scanner.next ();
            if (!parseScriptsSection (typeDeclaration, modifierList)) return false;
            if (!skipToken (BraceCloseToken)) return false;
            return true;
         } else {
            return parseScript (typeDeclaration, modifierList, true);
         }
      }

      return super.parseFieldDeclaration (typeDeclaration, modifierList);
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                         Script Declaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseScriptsSection (TypeDeclaration typeDeclaration,
                                          ModifierList modifierList) {
      /* scanner.token == start of 1st script */
      while (scanner.token == IdentifierToken) {
         if (!parseScript (typeDeclaration, modifierList, false)) return false;
      }
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseScript (TypeDeclaration typeDeclaration,
                                  ModifierList modifierList,
                                  boolean bracesAllowed) {

      if (scanner.token != IdentifierToken) {
         parserError (2, "Identifier (script name) expected");
         return false;
      }

      BasicScriptDeclaration scriptDeclaration 
            = parseScriptHeader (typeDeclaration, modifierList);
            
      if (scriptDeclaration == null) return false;
      scriptDeclaration.typeDeclaration = typeDeclaration;
      scriptDeclaration.isDeprecated   = seenDeprecated;
      typeDeclaration.fieldDeclarations.add (scriptDeclaration);

      if (typeDeclaration.isInterface ()
      ||  (scriptDeclaration.modifiers.modifierFlags & AbstractFlag) > 0) {
         if (!skipToken (SemicolonToken)) return false;
         return true;
      }

      boolean usingBraces = false;
      
	  int b [ ];
      if (scriptDeclaration.isChannelDeclaration()
      ||  scriptDeclaration.isCommunicationDeclaration())
      {
	      if (bracesAllowed) {
	         b = new int[]{EqualsToken, GreaterOrEqualToken, BraceOpenToken};
	         usingBraces = (scanner.token == BraceOpenToken);
	      } else {
	         b = new int[]{EqualsToken, GreaterOrEqualToken, BraceOpenToken};
	      }
      }
      else
      {
	      if (bracesAllowed) {
	         b = new int[]{AssignToken, BraceOpenToken};
	      } else {
		     b = new int[]{AssignToken};
	      }
      }
      if (!expectTokens (b)) return false;
      usingBraces = (scanner.token == BraceOpenToken);
      scriptDeclaration.isSparseActivation = scanner.token == GreaterOrEqualToken;
      scanner.next ();

      currentTypeDeclaration              = typeDeclaration;
      scriptDeclaration.bodyStartPosition = scanner.getPosition();

      ArrayList<LocalTypeDeclaration> savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement; // may be nested
      localTypeDeclarationsInRefinement = scriptDeclaration.localTypeDeclarations;

      ScriptExpression expression = parseScriptExpression (true);

      localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;

      if (expression == null) return false;

      if (keepFieldBodies
      || !scriptDeclaration.localTypeDeclarations.isEmpty() )
          scriptDeclaration.scriptExpression = expression;
      else System.gc();

      if (usingBraces) {
         if (!skipToken (BraceCloseToken)) return false;
      }

      return true;
   }

   /*-----------------------------------------------------------------*/

   /* Reparse script body  DEACTIVATED
***********************
   protected boolean reparseScriptBody (BasicScriptDeclaration scriptDeclaration) {

      if (scriptDeclaration.scriptExpression != null) {
          return true;
      }
      if (scriptDeclaration.bodyStartPosition == null) {
          new Exception("Internal error").printStackTrace();
          return true;
      }

      compilationUnit = scriptDeclaration.typeDeclaration.compilationUnit;

      scanner.setPosition (scriptDeclaration.bodyStartPosition);

      // Disable user-defined operators during script expression

      if (scanner instanceof OperatorScanner) {
         operatorScanner = (OperatorScanner)scanner;
         areUserDefinedOperatorsEnabled = operatorScanner.areUserDefinedOperatorsEnabled();
      }
      if (operatorScanner != null)
         operatorScanner.disableUserDefinedOperators ();

      Vector savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement; // may be nested
      localTypeDeclarationsInRefinement             = scriptDeclaration.localTypeDeclarations;

      long startTime              = env.timerLocalStart();
      currentTypeDeclaration      = scriptDeclaration.typeDeclaration;
      ScriptExpression expression = parseScriptExpression (true);
      env.timerLocalStop(env.ReparseMsg, scriptDeclaration.name, startTime);

      localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;

      if (operatorScanner != null)
         operatorScanner.disableUserDefinedOperators (!areUserDefinedOperatorsEnabled);

      if (expression == null) return false;

      scriptDeclaration.scriptExpression = expression;

      return true;
   }
****************************************/

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                          Script Header                          */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   public static final int MaxNoOfCommunicationPartners = 4;

   protected BasicScriptDeclaration parseScriptHeader 
                                       (TypeDeclaration typeDeclaration,
                                        ModifierList modifierList) {

      BasicScriptDeclaration scriptDeclaration = new ScriptDeclaration ();
      /* Will be turned into CommunicationDeclaration or ChannelDeclaration
         if that's what it turns out to be */

      scriptDeclaration.sourceStartPosition = scanner.tokenStartPosition;
      scriptDeclaration.sourceEndPosition   = scanner.tokenEndPosition;
      scriptDeclaration.nameStartPosition   = scanner.tokenStartPosition;
      scriptDeclaration.nameEndPosition     = scanner.tokenEndPosition;
      scriptDeclaration.name                = (String)scanner.tokenValue;
      scriptDeclaration.setModifiers (modifierList.modifiers);
      if (!skipToken (IdentifierToken)) return null;

      /* Parse the names and ranges of the formal indexes, if any. \
       * These are treated just like normal parameters, 
       * except that the count will be used in the script declaration */
      ParameterList parameterList = null;
      ScannerPosition bracketPosition = scanner.getPosition ();
      if (scanner.token == BracketOpenToken) {

         parameterList       = new ParameterList ();
         parameterList.sourceStartPosition = scanner.tokenStartPosition;
         parameterList.sourceEndPosition   = scanner.tokenEndPosition;
         parameterList.nameStartPosition   = scanner.tokenStartPosition;
         parameterList.nameEndPosition     = scanner.tokenEndPosition;
         parameterList.owner               = scriptDeclaration;
         parseParameterList ("script", parameterList, BracketCloseToken);
      }
      
      /* Test for channel indicators '<<' and '>>' */
      boolean isSendChannel           = scanner.token == LeftShiftToken;
      boolean isReceiveChannel        = scanner.token == RightShiftToken;
      boolean isBidirectionalChannel  = false;
      if (   isSendChannel
      ||  isReceiveChannel) {

         /* So it's a channel declaration */
         ChannelDeclaration channelDeclaration;
         channelDeclaration = new ChannelDeclaration (scriptDeclaration);
         channelDeclaration.sourceEndPosition = scanner.tokenEndPosition;
         scanner.next();
         if (isSendChannel && (scanner.token == RightShiftToken)) {
            /* Send and receive, i.e. bidirectional */
            isSendChannel          = false;
            isBidirectionalChannel = true;
            channelDeclaration.sourceEndPosition = scanner.tokenEndPosition;
            scanner.next ();
         }
         channelDeclaration.isSendChannel          = isSendChannel;
         channelDeclaration.isReceiveChannel       = isReceiveChannel;
         channelDeclaration.isBidirectionalChannel = isBidirectionalChannel;
         scriptDeclaration = channelDeclaration;
         if (parameterList!=null) {
        	 channelDeclaration.setFormalIndexCount(parameterList.parameters.size());
         }
      }
      else if (parameterList!=null)
      {
          parserError (2, "Script indexes are only allowed for channel declarations");
      }

      /* Formal parameters */
      if (scanner.token == ParenthesisOpenToken) {
         /* Parse formal parameter list */
    	 if (parameterList!=null) {
             parseParameterList ("script", parameterList, ParenthesisCloseToken);
    	 }
    	 else
    	 {
           parameterList = parseParameterList ("script", scriptDeclaration);
    	 }
         if (parameterList == null) return null;
      } 
      else if (parameterList==null) {
         /* There are no formal parameters. Create an empty parameter list. */
         parameterList                     = new ParameterList ();
         parameterList.sourceStartPosition = scriptDeclaration.sourceEndPosition;
         parameterList.sourceEndPosition   = scriptDeclaration.sourceEndPosition;
      }
      parameterList.nameStartPosition      = scriptDeclaration.nameStartPosition;
      parameterList.nameEndPosition        = scriptDeclaration.nameEndPosition;
      parameterList.name                   = scriptDeclaration.name;
      scriptDeclaration.headerEndPosition  = parameterList.sourceEndPosition;
      scriptDeclaration.sourceEndPosition  = parameterList.sourceEndPosition;
      scriptDeclaration.parameterList      = parameterList;

      if (scanner.token != CommaToken) {
         /* It's not a communication... if it's not even a channel */
         return scriptDeclaration;
      }

      /* It turns out that this is a communication declaration */
      if (scriptDeclaration.isChannelDeclaration ()) {
         parserError (2, "Cannot declare communication with a channel");
         return null;
      }

      CommunicationDeclaration commDeclaration;
      CommunicationPartnerDeclaration     partner;
      commDeclaration = new CommunicationDeclaration (scriptDeclaration);
      partner         = new CommunicationPartnerDeclaration     (scriptDeclaration);
      partner.typeDeclaration = typeDeclaration;
      partner.partnerIndex    = 0;
      commDeclaration.partners.add(partner);

      while (scanner.token == CommaToken) {
         if (commDeclaration.partners.size() >= MaxNoOfCommunicationPartners) {
            parserError (2, "Too many communication partners (maximum = "
                         + MaxNoOfCommunicationPartners + ")");
            return null;
         }

         if (scanner.next() != IdentifierToken) {
            if (commDeclaration.partners.size() == 1) {
               /* "script a, = ..." is OK;
                  it's a communication with one partner */
               break;
            }
            parserError (2, "Identifier (communication partner name) expected");
            return null;
         }

         partner = new CommunicationPartnerDeclaration ();
         partner.sourceStartPosition = scanner.tokenStartPosition;
         partner.sourceEndPosition   = scanner.tokenEndPosition;
         partner.nameStartPosition   = scanner.tokenStartPosition;
         partner.nameEndPosition     = scanner.tokenEndPosition;
         partner.name                = (String)scanner.tokenValue;
         partner.modifiers           = new Modifiers(modifierList.modifiers);
         partner.typeDeclaration     = typeDeclaration;
         scanner.next ();
         
         if (   scanner.token == LeftShiftToken
             || scanner.token == RightShiftToken) {
            parserError (2, "Cannot declare a channel in a communication partner");
            return null;
         }

         /* Partner's formal parameters */
         if (scanner.token == ParenthesisOpenToken) {
            parameterList = parseParameterList ("script", partner);
            if (parameterList == null) return null;
         } else {
            parameterList                     = new ParameterList ();
            parameterList.sourceStartPosition = partner.sourceEndPosition;
            parameterList.sourceEndPosition   = partner.sourceEndPosition;
         }
         parameterList.nameStartPosition   = partner.nameStartPosition;
         parameterList.nameEndPosition     = partner.nameEndPosition;
         parameterList.name                = partner.name;
         partner.headerEndPosition         = parameterList.sourceEndPosition;
         partner.sourceEndPosition         = parameterList.sourceEndPosition;
         partner.parameterList             = parameterList;
         partner.partnerIndex              = commDeclaration.partners.size ();
         commDeclaration.headerEndPosition = parameterList.sourceEndPosition;
         commDeclaration.sourceEndPosition = parameterList.sourceEndPosition;
         commDeclaration.partners.add (partner);
      }
      commDeclaration.name = commDeclaration.getPartnerNamesSeparated ('_');

      return commDeclaration;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                         Script Expression                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseScriptExpression (boolean topLevel) {

      return parseInfixExpression (topLevel, scripticOperators, 0);
   }

   /* Operators in order of precedence: */

   public final int scripticOperators [ ] = {
                                 	          PlusToken,          //   +
                                              BooleanOrToken,     //   ||
                                 	          BooleanAndToken,    //   &&
                                 	          VerticalBarToken,   //   |
                                 	          AmpersandToken,     //   &
                                 	          HashToken,     	  //   #
                                 	          SlashToken,         //   /
                                 	          PercentToken,       //   %
                                 	          SemicolonToken,     //   ;
                                 	          EllipsisToken,       //   ..
                                 	          Ellipsis3Token      //   ...
                                           };

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseInfixExpression (boolean topLevel,
                                                    int [ ] operators, 
                                                    int     operatorIndex) {

      if (operatorIndex >= operators.length)
         return parseScriptTerm (topLevel);

      ScriptExpression term = parseInfixExpression (topLevel,
                                                    operators, 
                                                    operatorIndex + 1);
      if (term == null) return null;

      int operatorToken  = operators [ operatorIndex ];
      if (scanner.token != operatorToken) return term;

      InfixExpression expression = constructInfixExpression (term);

      expression.extraPositions.clear(); // needed because this all may be called twice (reparse...)
      for (int i=0; scanner.token == operatorToken; i++) {
         expression.extraPositions.add (new Integer(scanner.tokenStartPosition));
         scanner.next ();
         term = parseInfixExpression (topLevel,
                                      operators, 
                                      operatorIndex + 1);
         if (term == null) return null;
         expression.sourceEndPosition = term.sourceEndPosition;
         expression.expressions.add (term);
      }

      return expression;
   }

   /*-----------------------------------------------------------------*/

   /* Construct an infix expression. Scanner is on the operator. */

   protected InfixExpression constructInfixExpression (ScriptExpression firstTerm) {
   
      InfixExpression expression     = new InfixExpression ();
      expression.sourceStartPosition = firstTerm.sourceStartPosition;
      expression.sourceEndPosition   = scanner.tokenEndPosition;
      expression.nameStartPosition   = scanner.tokenStartPosition;
      expression.nameEndPosition     = scanner.tokenEndPosition;
      expression.name                = Scanner.tokenRepresentation (scanner.token);

      expression.operator            = scanner.token;
      expression.expressions.add (firstTerm);
      return expression;
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parsePrimaryScriptTerm (boolean topLevel) {
      /* Parse a "script term" less deactivation and "? :" code */

      switch (scanner.token) {

         case PrivateToken:      
                  return parsePrivateDeclaration (topLevel);


         case BooleanToken:
         case CharToken:
         case ByteToken:
         case ShortToken:
         case IntToken:
         case LongToken:
         case FloatToken:
         case DoubleToken:

                  return parseLocalDataDeclaration (topLevel);


         case IdentifierToken:   

                  return parseLocalDataOrScriptCall (topLevel);


         case ThisToken:   
         case SuperToken:   

                  SpecialNameExpression specialScriptCall = new SpecialNameExpression (scanner.token);
                  specialScriptCall.sourceStartPosition = scanner.tokenStartPosition;
                  specialScriptCall.sourceEndPosition   = scanner.tokenEndPosition;
                  specialScriptCall.nameStartPosition   = scanner.tokenStartPosition;
                  specialScriptCall.nameEndPosition     = scanner.tokenEndPosition;
                  specialScriptCall.name                = Scanner.tokenRepresentation (scanner.token);

                  if (!nextToken (PeriodToken))
                     return null;

                  /* Period must be followed by one single identifier */
                  if (scanner.next() != IdentifierToken) {
                     parserError (2, "Identifier (script name) expected");
                     return null;
                  }

                  FieldAccessExpression specialScriptFieldAccess = new FieldAccessExpression ();
                  specialScriptFieldAccess.sourceStartPosition = specialScriptCall.sourceStartPosition;
                  specialScriptFieldAccess.sourceEndPosition   = scanner.tokenEndPosition;
                  specialScriptFieldAccess.nameStartPosition   = scanner.tokenStartPosition;
                  specialScriptFieldAccess.nameEndPosition     = scanner.tokenEndPosition;
                  specialScriptFieldAccess.name                = (String)scanner.tokenValue;

                  specialScriptFieldAccess.primaryExpression   = specialScriptCall;
                  scanner.next ();

                  if (scanner.token == PeriodToken) {
                     // Special-case error message
                     parserError (2, "Cannot use compound identifier as script name");
                     return null;
                  }
                  return parseScriptCall (topLevel, specialScriptFieldAccess);


         case BreakToken:
         case PeriodToken:
         case EllipsisToken:
         case Ellipsis3Token:

                  return parseSpecialOperand ();


         case DecrementToken:
         case MinusToken:

                  ScannerPosition scannerPosition = scanner.getPosition ();
                  int startPosition               = scanner.tokenStartPosition;

                  /* Kind of "optimization": we're only interested
                     in whether there's an even or odd number of minus
                     signs. Answer either an UnaryScriptExpression or a
                     SpecialNameScriptExpression, with either a MinusToken
                     (odd # of minus signs) or a DecrementToken 
                     (even # of minus signs) as the operator. */

                  boolean oddNumberOfMinuses = false;
                  while (   scanner.token == DecrementToken
                         || scanner.token == MinusToken) {

                     oddNumberOfMinuses ^= (scanner.token == MinusToken);
                     scannerPosition     = scanner.getPosition ();
                     scanner.next ();
                  }
                  int token = (oddNumberOfMinuses ? MinusToken : DecrementToken);
                  
                  if (   scanner.token == IdentifierToken
                      || scanner.token == BreakToken
                      || scanner.token == EllipsisToken
                      || scanner.token == Ellipsis3Token
                      || scanner.token == MinusToken          //superfluous
                      || scanner.token == TildeToken
                      || scanner.token == ParenthesisOpenToken
                      || scanner.token == DecrementToken      //superfluous
                      || scanner.token == LessThanToken
                      || scanner.token == BraceOpenToken
                      || scanner.token == BraceColonOpenToken
                      || scanner.token == BracePeriodOpenToken
                      || scanner.token == BraceEllipsisOpenToken
                      || scanner.token == BraceEllipsis3OpenToken
                      || scanner.token == BraceQuestionOpenToken
                      || scanner.token == BraceAsteriskOpenToken
                      || scanner.token == AtSignToken
                      || scanner.token == IfToken
                      || scanner.token == WhileToken
                      || scanner.token == ForToken
                      || scanner.token == SwitchToken) {

                     ScriptExpression term = parseScriptTerm (topLevel);
                     if (term == null) return null;

                     UnaryScriptExpression unaryExpression = new UnaryScriptExpression ();
                     unaryExpression.sourceStartPosition = startPosition;
                     unaryExpression.sourceEndPosition   = term.sourceEndPosition;
                     unaryExpression.nameStartPosition   = scannerPosition.tokenStartPosition;
                     unaryExpression.nameEndPosition     = scannerPosition.tokenEndPosition;
                     unaryExpression.name                = Scanner.tokenRepresentation (token);
                     unaryExpression.unaryToken          = token;
                     unaryExpression.primaryExpression   = term;

                     return unaryExpression;

                  } else {

                     SpecialNameScriptExpression specialNameExpression = new SpecialNameScriptExpression ();
                     specialNameExpression.sourceStartPosition = startPosition;
                     specialNameExpression.sourceEndPosition   = scannerPosition.tokenEndPosition;
                     specialNameExpression.nameStartPosition   = scannerPosition.tokenStartPosition;
                     specialNameExpression.nameEndPosition     = scannerPosition.tokenEndPosition;
                     specialNameExpression.name                = Scanner.tokenRepresentation (token);
                     specialNameExpression.token               = token;

                     return specialNameExpression;
                  }


         case TildeToken:

                     UnaryScriptExpression unaryExpression = new UnaryScriptExpression ();
                     unaryExpression.sourceStartPosition = scanner.tokenStartPosition;
                     unaryExpression.nameStartPosition   = scanner.tokenStartPosition;
                     unaryExpression.nameEndPosition     = scanner.tokenEndPosition;
                     unaryExpression.name                = Scanner.tokenRepresentation (scanner.token);
                     unaryExpression.unaryToken          = scanner.token;

                     scanner.next ();
                     ScriptExpression term = parseScriptTerm (topLevel);
                     if (term == null) return null;

                     unaryExpression.primaryExpression   = term;
                     unaryExpression.sourceEndPosition   = term.sourceEndPosition;
                     return unaryExpression;


         case ParenthesisOpenToken:

                  ScannerPosition parenthesisPosition = scanner.getPosition ();
                  scanner.next ();

                  if (scanner.token == ParenthesisCloseToken) {

                     /* The deadlock process, "()". For now we represent this
                        as a Special Name operand with name "()" and token
                        ParethesisOpenToken. */

                     SpecialNameScriptExpression specialNameExpression = new SpecialNameScriptExpression ();
                     specialNameExpression.sourceStartPosition = parenthesisPosition.tokenStartPosition;
                     specialNameExpression.sourceEndPosition   = scanner.tokenEndPosition;
                     specialNameExpression.nameStartPosition   = parenthesisPosition.tokenStartPosition;
                     specialNameExpression.nameEndPosition     = parenthesisPosition.tokenEndPosition;
                     specialNameExpression.name                = "()";
                     specialNameExpression.token               = ParenthesisOpenToken;

                     scanner.next ();
                     return specialNameExpression;
                  }

                  scanner.setPosition (parenthesisPosition);
                  return parseNestedScript (ParenthesisCloseToken);

         case LessThanToken:

                  return parseNestedScript (GreaterThanToken);

         case AtSignToken:
        	 	  scanner.next ();
        	      JavaExpression anchorExpression = parseJavaExpression();
        	      expectToken  (ColonToken);
        	      scanner.next ();
        	      expectTokens(new int[] {
        	         BraceOpenToken,
        	         BraceQuestionOpenToken,
        	         BraceColonOpenToken,
        	         BracePeriodOpenToken,
        	         BraceEllipsisOpenToken,
         	         BraceEllipsis3OpenToken,
       	         //case BraceAsteriskOpenToken,
        	         IdentifierToken,
        	         IfToken,
        	         WhileToken,
        	         ForToken,
        	         SwitchToken,
        	      });
        	      ScriptExpression scriptExpression = parsePrimaryScriptTerm (topLevel);
        	      if (scriptExpression!=null)
        	      {
        	    	  if (scriptExpression.languageConstructCode()==ActivationCodeCode)
        	    	  {
        	    		  ActivationCode ac = (ActivationCode) scriptExpression;
        	    		  ac.activationCode.setAnchorExpression(anchorExpression);
        	    	  }
        	    	  else
        	    	  {
        	    		  scriptExpression.setAnchorExpression(anchorExpression);
        	    	  }
        	      }
        	      return scriptExpression;
        	      
         case BraceOpenToken:

                  NativeCodeFragment codeFragment;
                  codeFragment  = parseCodeFragment (true, true, true);
                  if (codeFragment == null) return null;

                  // Get the lastStatement for later checking
                  JavaStatement lastStatement = codeFragment.lastStatement();

                  if (scanner.token == PeriodToken) {
                     // Script call code fragment "{ obj }.scriptCall"

                     // Period must be followed by an identifier
                     if (scanner.next() != IdentifierToken) {
                        parserError (2, "Identifier (script name) expected");
                        return null;
                     }

                     // No "duration" or "priority" tricks in script accesses!
                     if (codeFragment.durationAssignment != null) {
                        parserError (2, "Assignment to \"duration\" not allowed "
                                     + "in a script call code fragment",
                                     codeFragment.durationAssignment.sourceStartPosition,
                                     codeFragment.durationAssignment.sourceEndPosition);
                        codeFragment.durationAssignment = null;
                     }
                     if (codeFragment.priorityAssignment != null) {
                        parserError (2, "Assignment to \"priority\" not allowed "
                                     + "in a script call code fragment",
                                     codeFragment.priorityAssignment.sourceStartPosition,
                                     codeFragment.priorityAssignment.sourceEndPosition);
                        codeFragment.priorityAssignment = null;
                     }

         ///////////////////////////////
         // not done yet /////////////// {new A().b[0].c()}.a[1]<<(2) will be eliminated. Instead use
         ///////////////////////////////  new A().b[0].c() .a[1]<<(2) to be implemented later...

                     FieldAccessExpression fae = null;
                     if (lastStatement != null) {
                        if (lastStatement instanceof ExpressionStatement) {
                           ExpressionStatement es = (ExpressionStatement) lastStatement;
                           es.omitSemicolonTerminator = true;
                           fae = new FieldAccessExpression();
                           fae.primaryExpression   = es.expression;
                           fae.sourceStartPosition = es    .sourceStartPosition;
                           fae.sourceEndPosition   = scanner.tokenEndPosition;
                           fae.nameStartPosition   = scanner.tokenStartPosition;
                           fae.nameEndPosition     = scanner.tokenEndPosition;
                           fae.name                = (String)scanner.tokenValue;
                           if (codeFragment.statements.size()>1) {
                                  parserError (2, "Script call code fragment must be a reference expression",
                                         lastStatement.sourceStartPosition,
                                         lastStatement.sourceEndPosition);
                           }
                        } else {
                           parserError (2, "Script call code fragment must be a reference expression",
                                         lastStatement.sourceStartPosition,
                                         lastStatement.sourceEndPosition);
                           return null;
                        }
                     } else {
                        // "{ }.scriptCall"
                           parserError (2, "Script call code fragment cannot be empty",
                                         codeFragment.sourceStartPosition,
                                         codeFragment.sourceEndPosition);
                           return null;
                     }
                     scanner.next ();
                     return parseScriptCall (topLevel, fae);
                  }

                  // Not a script call code fragment. Complain if it contains
                  // a loose expression.
                  if (   lastStatement != null
                      && lastStatement instanceof ExpressionStatement) {
                        checkExpressionStatement ((ExpressionStatement)lastStatement);
                  }

                  if (scanner.token == LessThanToken) {
                     // Activation code "{ xxx }< ..."
                     ActivationCode activationCode      = new ActivationCode ();

                     // No "duration" tricks in activation code! ("priority" is OK)
                     if (codeFragment.durationAssignment != null) {
                        parserError (2, "Assignment to \"duration\" not allowed "
                                     + "in an activation code fragment",
                                     codeFragment.durationAssignment.sourceStartPosition,
                                     codeFragment.durationAssignment.sourceEndPosition);
                        codeFragment.durationAssignment = null;
                     }

                     // If there is a "priority" assignment, just lump it in
                     // with the rest of the statements
                     if (codeFragment.priorityAssignment != null) {
                        parserError (2, "Assignment to \"priority\" should not be "
                                     + "followed by \":\" in an activation code fragment",
                                     codeFragment.priorityAssignment.sourceStartPosition,
                                     codeFragment.priorityAssignment.sourceEndPosition);
                        codeFragment.statements.add 
                                          (0, codeFragment.priorityAssignment);
                        codeFragment.priorityAssignment = null;
                     }

                     activationCode.sourceStartPosition = codeFragment.sourceStartPosition;
                     activationCode.nameStartPosition   = scanner.tokenStartPosition;
                     activationCode.nameEndPosition     = scanner.tokenEndPosition;
                     activationCode.name                = Scanner.tokenRepresentation (scanner.token);
                     scanner.next ();

                     ScriptExpression activationTerm = parseScriptTerm (topLevel);
                     if (activationTerm == null) return null;
                     activationCode.activationCode      = codeFragment;
                     activationCode.scriptTerm          = activationTerm;
                     activationCode.sourceEndPosition   = activationTerm.sourceEndPosition;
                     return activationCode;
                  }
                  return codeFragment;


         case BraceQuestionOpenToken:

                  NativeCodeFragment unsureFragment;
                  unsureFragment  = parseCodeFragment (false, true, false);
                  if (unsureFragment == null) return null;
                  return unsureFragment;


         case BraceColonOpenToken:

                  NativeCodeFragment tinyFragment;
                  tinyFragment  = parseCodeFragment (false, false, false);
                  if (tinyFragment == null) return null;
                  return tinyFragment;


         case BraceAsteriskOpenToken:

                  ScriptExpression threadFragment;
                  threadFragment  = parseCodeFragment (false, true, true);
                  return threadFragment;


         case BracePeriodOpenToken:
         case BraceEllipsisOpenToken:
         case BraceEllipsis3OpenToken:

                  EventHandlingCodeFragment eventFragment
                                       = new EventHandlingCodeFragment ();
                  eventFragment.sourceStartPosition = scanner.tokenStartPosition;
                  eventFragment.sourceEndPosition   = scanner.tokenEndPosition;
                  eventFragment.nameStartPosition   = scanner.tokenStartPosition;
                  eventFragment.nameEndPosition     = scanner.tokenEndPosition;
                  eventFragment.name                = Scanner.tokenRepresentation (scanner.token);
                  eventFragment.startingDelimiter   = scanner.token;
                  scanner.next();
                  
                  eventFragment = (EventHandlingCodeFragment)
                                       parseRestOfCodeFragment 
                                             (eventFragment,
                                             false, false, false);
                  if (eventFragment == null) return null;
                  return eventFragment;


         case IfToken:

                  IfScriptExpression ifScript     = new IfScriptExpression ();
                  ifScript.sourceStartPosition    = scanner.tokenStartPosition;
                  ifScript.nameStartPosition      = scanner.tokenStartPosition;
                  ifScript.nameEndPosition        = scanner.tokenEndPosition;

                  if (!nextToken (ParenthesisOpenToken)) return null;
                  scanner.next();
                  JavaExpression ifCondition = parseJavaExpression ();
                  if (ifCondition == null) return null;
                  if (!skipToken (ParenthesisCloseToken)) return null;

                  ScriptExpression ifTerm = parseScriptTerm (topLevel);
                  if (ifTerm == null) return null;
                  ifScript.sourceEndPosition = ifTerm.sourceEndPosition;

                  ScriptExpression elseTerm = null;
                  if (scanner.token == ElseToken) {
                     scanner.next ();
                     elseTerm = parseScriptTerm (topLevel);
                     if (elseTerm == null) return null;
                     ifScript.sourceEndPosition = elseTerm.sourceEndPosition;
                  }

                  ifScript.condition = ifCondition;
                  ifScript.ifTerm    = ifTerm;
                  ifScript.elseTerm  = elseTerm;
                  return ifScript;


         case WhileToken:

                  WhileScriptExpression whileScript = new WhileScriptExpression ();
                  whileScript.sourceStartPosition   = scanner.tokenStartPosition;
                  whileScript.nameStartPosition     = scanner.tokenStartPosition;
                  whileScript.nameEndPosition       = scanner.tokenEndPosition;

                  if (!nextToken (ParenthesisOpenToken)) return null;
                  scanner.next();
                  JavaExpression whileCondition = parseJavaExpression ();
                  if (whileCondition == null) return null;
                  whileScript.sourceEndPosition = scanner.tokenEndPosition;
                  if (!skipToken (ParenthesisCloseToken)) return null;

                  whileScript.condition         = whileCondition;
                  return whileScript;


         case ForToken:

                  ForScriptExpression forScript = new ForScriptExpression ();
                  forScript.sourceStartPosition   = scanner.tokenStartPosition;
                  forScript.nameStartPosition     = scanner.tokenStartPosition;
                  forScript.nameEndPosition       = scanner.tokenEndPosition;

                  if (!nextToken (ParenthesisOpenToken)) return null;
                  scanner.next ();

                  JavaExpression initExpression = null;
                  if (scanner.token == SemicolonToken)
                     scanner.next();
                  else {
                     initExpression = parseJavaExpression ();
                     if (initExpression == null) return null;
                     if (!skipToken (SemicolonToken)) return null;
                  }

                  JavaExpression loopCondition = null;
                  if (scanner.token == SemicolonToken)
                     scanner.next();
                  else {
                     loopCondition = parseJavaExpression ();
                     if (loopCondition == null) return null;
                     if (!skipToken (SemicolonToken)) return null;
                  }

                  JavaExpression loopExpression = null;
                  if (scanner.token == ParenthesisCloseToken) {
                     forScript.sourceEndPosition = scanner.tokenEndPosition;
                     scanner.next();
                  } else {
                     loopExpression = parseJavaExpression ();
                     if (loopExpression == null) return null;
                     forScript.sourceEndPosition = scanner.tokenEndPosition;
                     if (!skipToken (ParenthesisCloseToken)) return null;
                  }

                  forScript.initExpression    = initExpression;
                  forScript.condition         = loopCondition;
                  forScript.loopExpression    = loopExpression;
                  return forScript;


         case SwitchToken:

                  SwitchScriptExpression switchScript = new SwitchScriptExpression ();
                  switchScript.sourceStartPosition = scanner.tokenStartPosition;
                  switchScript.nameStartPosition   = scanner.tokenStartPosition;
                  switchScript.nameEndPosition     = scanner.tokenEndPosition;

                  if (!nextToken (ParenthesisOpenToken)) return null;
                  scanner.next();
                  JavaExpression switchExpression = parseJavaExpression ();
                  if (switchExpression == null) return null;
                  if (!skipToken (ParenthesisCloseToken)) return null;

                  switchScript.switchExpression    = switchExpression;

                  if (!skipToken (ParenthesisOpenToken)) return null;
                  boolean seenDefaultCase = false;
                  do {

                     if (   scanner.token != CaseToken
                         && scanner.token != DefaultToken
                         && scanner.token != ParenthesisCloseToken) {

                        /* Don't "expect" the default tag if it's already 
                           been encountered */
                        if (seenDefaultCase) {
                           int ct [ ] = {CaseToken, ParenthesisCloseToken};
                           expectTokens (ct);
                           return null;
                        } else {
                           int cd [ ] = {CaseToken, DefaultToken, ParenthesisCloseToken};
                           expectTokens (cd);
                           return null;
                        }
                     }
                     if (scanner.token == ParenthesisCloseToken) break;

                     CaseTagScriptExpression caseTag  = new CaseTagScriptExpression ();
                     caseTag.sourceStartPosition = scanner.tokenStartPosition;
                     caseTag.nameStartPosition   = scanner.tokenStartPosition;
                     caseTag.nameEndPosition     = scanner.tokenEndPosition;

                     if (scanner.token == CaseToken) {

                        /* case tag: "case x, ..., x : scriptExpr" */
                        do {
                           if (scanner.next() == DefaultToken) {
                              if (seenDefaultCase) {
                                 parserError (2, "Duplicate \"default\" clause");
                                 return null;
                              }

                              SpecialNameExpression inlineDefaultTag = new SpecialNameExpression (scanner.token);
                              inlineDefaultTag.sourceStartPosition = scanner.tokenStartPosition;
                              inlineDefaultTag.sourceEndPosition   = scanner.tokenEndPosition;
                              inlineDefaultTag.nameStartPosition   = scanner.tokenStartPosition;
                              inlineDefaultTag.nameEndPosition     = scanner.tokenEndPosition;
                              inlineDefaultTag.name                = Scanner.tokenRepresentation (scanner.token);

                              caseTag.tags.add (inlineDefaultTag);
                              caseTag.hasDefaultTag = true;
                              seenDefaultCase      = true;
                              scanner.next();

                           } else {
                              JavaExpression tagExpression = parseJavaExpression ();
                              if (tagExpression == null) return null;
                              caseTag.tags.add (tagExpression);
                           }

                           int ce [ ] = {CommaToken, ColonToken};
                           if (!expectTokens (ce)) return null;
                           if (scanner.token == ColonToken) break;
                        } while (true);
                     } else {

                        /* "default : " case tag */
                        if (seenDefaultCase) {
                           parserError (2, "Duplicate \"default\" clause");
                           return null;
                        }

                        SpecialNameExpression defaultTagExpression = new SpecialNameExpression (scanner.token);
                        defaultTagExpression.sourceStartPosition = scanner.tokenStartPosition;
                        defaultTagExpression.sourceEndPosition   = scanner.tokenEndPosition;
                        defaultTagExpression.nameStartPosition   = scanner.tokenStartPosition;
                        defaultTagExpression.nameEndPosition     = scanner.tokenEndPosition;
                        defaultTagExpression.name                = Scanner.tokenRepresentation (scanner.token);

                        caseTag.tags.add (defaultTagExpression);
                        caseTag.hasDefaultTag = true;
                        seenDefaultCase       = true;
                        scanner.next();
                     }
                     /* Case tag finished */
                     if (!skipToken (ColonToken)) return null;

                     ScriptExpression caseExpression = parseScriptExpression (false);
                     if (caseExpression == null) return null;
                     caseTag.caseExpression    = caseExpression;
                     caseTag.sourceEndPosition = caseExpression.sourceEndPosition;

                     switchScript.caseTags.add(caseTag);
                  } while (true);

                  switchScript.sourceEndPosition = scanner.tokenEndPosition;
                  if (!skipToken (ParenthesisCloseToken)) return null;

                  /* Switch statement finished */
                  return switchScript;
      }

      parserError (2, "Script term expected");
      return null;
   }


   protected ScriptExpression parseScriptTerm (boolean topLevel) {

      ScriptExpression scriptTerm = parsePrimaryScriptTerm (topLevel);
      if (scriptTerm == null) return null;

      while (scanner.token == GreaterThanToken) {
         /* Deactivation code, if immediately followed by "{" or "@" */

         ScannerPosition position = scanner.getPosition ();
         int token = scanner.next ();
         if (token != BraceOpenToken
         &&  token != AtSignToken) {
            scanner.setPosition (position);
            break;
         }

         DeactivationCode deactivationCode  = new DeactivationCode ();

         deactivationCode.nameStartPosition   = position.tokenStartPosition;
         deactivationCode.nameEndPosition     = position.tokenEndPosition;
         deactivationCode.name                = Scanner.tokenRepresentation (position.token);

         JavaExpression anchorExpression = null;
         if (token == AtSignToken) {
	   	 	  scanner.next ();
	   	 	  anchorExpression = parseJavaExpression();
		      expectToken  (ColonToken);
		      scanner.next ();
		      expectToken  (BraceOpenToken);
         }
         NativeCodeFragment codeFragment = parseCodeFragment (false, false, false);
         if (codeFragment == null) return null;
         
         if (anchorExpression!=null)
         {
        	 codeFragment.setAnchorExpression(anchorExpression);
        	 deactivationCode.setAnchorExpression(anchorExpression); // seems to be necessary
        }
         deactivationCode.deactivationCode    = codeFragment;
         deactivationCode.scriptTerm          = scriptTerm;
         deactivationCode.sourceStartPosition = scriptTerm.sourceStartPosition;
         deactivationCode.sourceEndPosition   = codeFragment.sourceEndPosition;
         scriptTerm = deactivationCode;
      }

      while (scanner.token == QuestionToken) {
         /* "? :" operator */
         ConditionalScriptExpression expression = new ConditionalScriptExpression ();

         expression.sourceStartPosition = scriptTerm.sourceStartPosition;
         expression.nameStartPosition   = scanner.tokenStartPosition;
         expression.nameEndPosition     = scanner.tokenEndPosition;
         expression.name                = Scanner.tokenRepresentation (scanner.token);
         scanner.next ();

         ScriptExpression successTerm = parseScriptTerm (topLevel);
         if (successTerm == null) return null;
         expression.sourceEndPosition   = successTerm.sourceEndPosition;

         ScriptExpression failureTerm = null;
         if (scanner.token == ColonToken) {
            scanner.next ();
            failureTerm = parseScriptTerm (topLevel);
            if (failureTerm == null) return null;
            expression.sourceEndPosition = failureTerm.sourceEndPosition;
         }

         expression.condition    = scriptTerm;
         expression.successTerm  = successTerm;
         expression.failureTerm  = failureTerm;
         scriptTerm = expression;
      }
      return scriptTerm;
   }


   protected ScriptExpression parseNestedScript (int closingToken) { 

      NestedScriptExpression nestedScript = new NestedScriptExpression ();
      nestedScript.sourceStartPosition    = scanner.tokenStartPosition;
      nestedScript.nameStartPosition      = scanner.tokenStartPosition;
      nestedScript.nameEndPosition        = scanner.tokenEndPosition;
      nestedScript.name                   = Scanner.tokenRepresentation (scanner.token);
      nestedScript.startingDelimiter      = scanner.token;
      scanner.next ();

      ScriptExpression subExpression = parseScriptExpression (false);
      if (subExpression == null) return null;

      nestedScript.subExpression       = subExpression;
      nestedScript.endingDelimiter     = scanner.token;
      nestedScript.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipToken (closingToken)) return null;

      return nestedScript;
   }


   /*-----------------------------------------------------------------*/


   protected NativeCodeFragment parseCodeFragment (boolean permitLooseExpression,
                                                   boolean permitPriorityAssignment,
                                                   boolean permitDurationAssignment) { 

      NativeCodeFragment codeFragment = new NativeCodeFragment ();
      codeFragment.sourceStartPosition = scanner.tokenStartPosition;
      codeFragment.sourceEndPosition   = scanner.tokenEndPosition;
      codeFragment.nameStartPosition   = scanner.tokenStartPosition;
      codeFragment.nameEndPosition     = scanner.tokenEndPosition;
      codeFragment.name                = Scanner.tokenRepresentation (scanner.token);
      codeFragment.startingDelimiter   = scanner.token;

      scanner.next ();
      return parseRestOfCodeFragment (codeFragment, 
                                      permitLooseExpression,
                                      permitPriorityAssignment,
                                      permitDurationAssignment);
   }


   protected int currentClosingToken = 0;
   protected int currentClosingToken2 = 0;
   protected int currentStatementNestingLevel = 0;

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected NativeCodeFragment parseRestOfCodeFragment (NativeCodeFragment codeFragment,
                                                         boolean permitLooseExpression,
                                                         boolean permitPriorityAssignment,
                                                         boolean permitDurationAssignment) { 

      currentStatementNestingLevel = 0;
      switch (codeFragment.startingDelimiter)
      {
    	case          BraceOpenToken: currentClosingToken  = currentClosingToken2 = BraceCloseToken; break;
      	case  BraceQuestionOpenToken: currentClosingToken  = currentClosingToken2 = BraceQuestionCloseToken; break;
      	case     BraceColonOpenToken: currentClosingToken  = currentClosingToken2 = BraceColonCloseToken; break;
      	case  BraceAsteriskOpenToken: currentClosingToken  = currentClosingToken2 = BraceAsteriskCloseToken; break;
      	case    BracePeriodOpenToken: currentClosingToken  = currentClosingToken2 = BracePeriodCloseToken; break;
      	case  BraceEllipsisOpenToken: currentClosingToken  = currentClosingToken2 = BraceEllipsisCloseToken; break;
      	case BraceEllipsis3OpenToken: currentClosingToken  = BraceEllipsisCloseToken;
      								  currentClosingToken2 = BraceEllipsis3CloseToken; break;
      }

      // Get any attribute assignments out of the way

      boolean attributeMandatory = false;
      do { // loop for priority and/or duration

         // ...unless, of course, they're not wanted
         if (   !permitDurationAssignment
             && !permitPriorityAssignment)
            break;

         if (scanner.token != IdentifierToken) {
            if (attributeMandatory) {
               parserError (2, "Assignment to \"duration\" or \"priority\" expected");
               return null;
            } else {
               break;
            }
         }

         boolean hasDurationAssignment
                       = "duration".equals (scanner.tokenValue);
         boolean hasPriorityAssignment
                       = "priority".equals (scanner.tokenValue);
         if (    !hasDurationAssignment
             &&  !hasPriorityAssignment) {
            if (attributeMandatory) {
               parserError (2, "Assignment to \"duration\" or \"priority\" expected");
               return null;
            } else {
               break;
            }
         }

         if (hasDurationAssignment) {
            if (!permitDurationAssignment)
               parserError (2, "Assignment to \"duration\" not allowed in a \""
                            + Scanner.tokenRepresentation (codeFragment.startingDelimiter)
                            + "\" code fragment");
            else if (codeFragment.durationAssignment != null)
               parserError (2, "Only one assignment to \"duration\" allowed");

         }
         if (hasPriorityAssignment) {
            if (!permitPriorityAssignment)
               parserError (2, "Assignment to \"priority\" not allowed in a \""
                            + Scanner.tokenRepresentation (codeFragment.startingDelimiter)
                            + "\" code fragment");
            else if (codeFragment.priorityAssignment != null)
               parserError (2, "Only one assignment to \"priority\" allowed");
         }

         NameExpression nameExpression      = new NameExpression ();
         nameExpression.sourceStartPosition = scanner.tokenStartPosition;
         nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
         nameExpression.nameStartPosition   = scanner.tokenStartPosition;
         nameExpression.nameEndPosition     = scanner.tokenEndPosition;
         nameExpression.name                = (String)scanner.tokenValue;

         // NOTE: The user-defined operators are still disabled at this
         // point. For the time being, we'll keep it that way. To enable them,
         // not only would you need to move the temporary enable/disable
         // code bracketing the parseConditionalExpression() call below,
         // but you'd also have to copy the user-defined operator related
         // checking from JavaParser1.parsePostfixExpression()!

         scanner.next ();
         if (hasDurationAssignment) {
            // Only straight assignment "duration = xxx" allowed
            if (!expectToken (AssignToken))
               return null;
         }

         if (hasPriorityAssignment) {
            // All assignment operators and "++" / "--" allowed
            if (   scanner.token != IncrementToken
                && scanner.token != DecrementToken
                && (   scanner.token < FirstAssignmentToken
                    || scanner.token > LastAssignmentToken)) {
               parserError (2, "Assignment or \"++\" / \"--\" expected");
               return null;
            }
         }

         JavaExpression attributeExpression = null;
         if (   scanner.token == IncrementToken
             || scanner.token == DecrementToken) {
            // Construct postfix expression
            PostfixExpression expression   = new PostfixExpression ();
            expression.sourceStartPosition = nameExpression.sourceStartPosition;
            expression.sourceEndPosition   = scanner.tokenEndPosition;
            expression.nameStartPosition   = scanner.tokenStartPosition;
            expression.nameEndPosition     = scanner.tokenEndPosition;
            expression.name                = Scanner.tokenRepresentation (scanner.token);
            expression.unaryToken          = scanner.token;
            expression.primaryExpression   = nameExpression;
            scanner.next ();

            attributeExpression = expression;
         } else {
            // Construct assignment expression
            AssignmentExpression expression = new AssignmentExpression ();
            expression.sourceStartPosition = nameExpression.sourceStartPosition;
            expression.nameStartPosition   = scanner.tokenStartPosition;
            expression.nameEndPosition     = scanner.tokenEndPosition;
            expression.name                = Scanner.tokenRepresentation (scanner.token);
            expression.operatorToken       = scanner.token;
            scanner.next ();

            // Set the user-defined operator state correctly. The
            // parserJavaExpression method is overridden in ScripticParser
            // to do this automatically, but since we call into
            // parseConditionalExpression() here (in order to catch jokes like
            // "duration = priority = xxx"), we need to repeat that code here.

            JavaExpression rightExpression = parseConditionalExpression ();
            if (rightExpression == null) return null;

            expression.leftExpression      = nameExpression;
            expression.rightExpression     = rightExpression;
            expression.sourceEndPosition   = rightExpression.sourceEndPosition;
            attributeExpression = expression;
         }

         ExpressionStatement attributeStatement = new ExpressionStatement ();
         attributeStatement.sourceStartPosition = attributeExpression.sourceStartPosition;
         attributeStatement.sourceEndPosition   = attributeExpression.sourceEndPosition;
         attributeStatement.nameStartPosition   = attributeExpression.nameStartPosition;
         attributeStatement.nameEndPosition     = attributeExpression.nameEndPosition;
         attributeStatement.name                = attributeExpression.name;
         attributeStatement.expression          = attributeExpression;

         if (testStatementTerminator()) {
            // OK -- an attribute assignment in a "normal" statement. 
            // We'll allow this here, but only if this is the first such statement.
            // Later in ScripticParser2, this situation will cause an error anyway,
            // except in the case of a "priority" assignment in an 
            // activation code fragment (which is the whole reason for allowing
            // this thing at all).

            if (   codeFragment.durationAssignment == null
                && codeFragment.priorityAssignment == null) {

               codeFragment.statements.add (attributeStatement);
               skipStatementTerminator();
               break;
            }
         }

         if (hasDurationAssignment)
            codeFragment.durationAssignment = attributeStatement;
         if (hasPriorityAssignment)
            codeFragment.priorityAssignment = attributeStatement;

         if (scanner.token == ColonToken) {
            scanner.next ();
            break;
         }
         if (scanner.token != CommaToken) {
            if (   codeFragment.durationAssignment != null
                && codeFragment.priorityAssignment != null)
               parserError (2, "\":\" expected");
            else
               parserError (2, "\":\" or \",\" expected");
            return null;
         }

         scanner.next ();
         attributeMandatory = true;
      } while (true);

      // Clean up some errors
      if (!permitDurationAssignment)
         codeFragment.durationAssignment = null;
      if (!permitPriorityAssignment)
         codeFragment.priorityAssignment = null;


      // Loop processing the normal statements
      JavaStatement previousStatement = null;
      while (scanner.token != currentClosingToken
          && scanner.token != currentClosingToken2
          && scanner.token != EofToken) {

         JavaStatement statement = parseJavaStatement (!permitLooseExpression);
         if (statement == null) return null;
         codeFragment.statements.add (statement);

         if (permitLooseExpression) {
            // Only the FINAL statement is permitted to be a loose expression.
            // All preceding statements still need checking.
            if (   previousStatement != null
                && previousStatement instanceof ExpressionStatement) {
                  checkExpressionStatement ((ExpressionStatement)previousStatement);
            }
            previousStatement = statement;
         }
      }
      codeFragment.endingDelimiter   = scanner.token;
      codeFragment.sourceEndPosition = scanner.tokenEndPosition;

      if (currentClosingToken==currentClosingToken2)
      {
    	  expectToken(currentClosingToken);
      }
      else
      {
    	  expectTokens(new int[]{currentClosingToken, currentClosingToken2});
      }
      scanner.next ();
      return codeFragment;
   }


   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   /* Parse rest of expression statement. This method is overridden
      from the superclass in order to catch success testing expressions
      of the form "expr ??". */

   protected JavaStatement parseRestOfExpressionStatement 
                                             (ExpressionStatement expressionStatement,
                                              boolean doCheckExpressionStatement,
                                              boolean requireSemicolon) {

      if (!requireSemicolon) {
         // Called from inside of a "for" statement or other special situation.
         // Better not mess with it.
         return super.parseRestOfExpressionStatement 
                                             (expressionStatement,
                                              doCheckExpressionStatement,
                                              requireSemicolon);
      }


      if (scanner.token == QuestionToken
      ||  scanner.token == DoubleQuestionToken) {
         //Signal special case
         expressionStatement.specialCode = 
        	 scanner.token == QuestionToken? SpecialCode.singleSuccessTest: SpecialCode.doubleSuccessTest;
         expressionStatement.sourceEndPosition = scanner.tokenEndPosition;
         scanner.next ();
         if (!skipStatementTerminator ()) return null;
         return expressionStatement;
      }

      if (doCheckExpressionStatement)
         checkExpressionStatement (expressionStatement);

      if (scanner.token == SemicolonToken)
         expressionStatement.sourceEndPosition = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return expressionStatement;
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected boolean skipStatementTerminator () {
      /* Verify that the ";" is present following statements that 
         require it. In Scriptic, the ";" may be omitted right before
         the outer closing "}" (or "*}") of a code fragment. */

      if (currentStatementNestingLevel > 0) {
         return super.skipStatementTerminator ();
      } else {
         int [ ] et = { SemicolonToken, currentClosingToken, currentClosingToken2 };
         if (!expectTokens (et)) return false;
         while (scanner.token == SemicolonToken) // 'while' rather than 'if',
                     // since empty statements may cause trouble with 'canCompleteNormally
            scanner.next ();
         return true;
      }
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected boolean testStatementTerminator () {
      /* Check if the current token is a statement terminator.
         In Scriptic, the terminator ";" may be omitted right before
         the outer closing "}" (or "*}") of a code fragment. */

      if (currentStatementNestingLevel > 0) {
         return super.testStatementTerminator ();
      } else {
         return    scanner.token == SemicolonToken
                || scanner.token == currentClosingToken
                || scanner.token == currentClosingToken2
                || scanner.token == EofToken;
      }
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected void enterStatementLexicalLevel () {
      /* Enter a new lexical level for statements, i.e.,
         enter a "{...}" construct. In scriptic, the rule about
         omitting ";"s does not apply to nested "{...}" constructs. */
      currentStatementNestingLevel++;
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected void exitStatementLexicalLevel () {
      /* Exit from a lexical level for statements, i.e.,
         exit a "{...}" construct. In scriptic, the rule about
         omitting ";"s does not apply to nested "{...}" constructs. */
      currentStatementNestingLevel--;
   }


   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseLocalDataOrScriptCall (boolean topLevel) {

      if (scanner.token != IdentifierToken) {
         parserError (2, "Identifier (variable or script name) expected");
         return null;
      }

      ScannerPosition identifierPosition = scanner.getPosition ();

      /* Scan off compound identifier */
      String compoundIdentifier = (String) scanner.tokenValue;
      while (scanner.next () == PeriodToken) {
         if (!nextToken (IdentifierToken)) return null;
         compoundIdentifier +=  "." + (String) scanner.tokenValue;
      }

      /* Look at brackets */
      if (scanner.token == BracketOpenToken) {
         if (scanner.next() == BracketCloseToken) {

            /* Array type: "String [ ] s" */
            scanner.setPosition (identifierPosition);
            return parseLocalDataDeclaration (topLevel);
         }

         /* Formal index: "customer [10].live" */
         scanner.setPosition (identifierPosition);
         return parseScriptCall (topLevel);
      }

      if (scanner.token == IdentifierToken) {
         /* This is truly ambiguous. It could be a script call,
          * followed by the header of the next script:
          *
          *    scripts 
          *       live  = serveCustomer
          *       die   = {exit(0)}
          *
          * or a local data declaration:
          *
          *    scripts
          *       live  = Customer customer = new Customer (): ...
          *
          * Currently we employ a simple heuristic:
          * we will choose the local data declaration
          * if this occurs nested within a script expression
          * (which rules out the script-call-plus-next-script interpretation)
          * OR if the identifier is "easily" recognized as a type name.
          * The easily recognized, well-known type names are the common
          * types from java.lang, the types from import declarations,
          * and the name of the current class.
          *
          * One possibility to add some intelligence to this decision
          * could be to look for the colon that should follow a local
          * data declaration. The problem is that there could be
          * arbitrarily complex stuff involving commas, "=" signs,
          * brackets and expressions, all of which could be equally valid
          * as a script header and a variable declaration. And the behaviour
          * of the parser certainly shouldn't be too complex from the
          * point of view of the user (= the Scriptic programmer). */

         scanner.setPosition (identifierPosition);
         if (!topLevel
         ||  compoundIdentifier.equals (currentTypeDeclaration.name)
         ||  currentTypeDeclaration.isWellknownType (compoundIdentifier)) {

            return parseLocalDataDeclaration (topLevel);

         } else {

            return parseScriptCall (topLevel);
         }
      }

      /* Remaining possibilities are all script calls */
      scanner.setPosition (identifierPosition);
      return parseScriptCall (topLevel);
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseLocalDataDeclaration (boolean topLevel) {

      /* Parse type name and possible brackets */
      DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
      ScannerPosition typePosition = scanner.getPosition ();

      if (!parseDataType (dataTypeDeclaration, 
                          "Type (local variable declaration) expected",
                          true,
                          false,
                          true)) return null;

      if (dataTypeDeclaration.primitiveTypeToken==VoidToken) {
         parserError (2, "Cannot declare void variable", typePosition);
         return null;
      }

      /* Create declaration */
      MultiVariableDeclaration declaration = new MultiVariableDeclaration ();
      declaration.sourceStartPosition = dataTypeDeclaration.sourceStartPosition;
      declaration.nameStartPosition   = dataTypeDeclaration.nameStartPosition;
      declaration.nameEndPosition     = dataTypeDeclaration.nameEndPosition;
      declaration.dataTypeDeclaration = dataTypeDeclaration;

      currentTypeDeclaration.compilationUnit.addPossibleRelevantClass (dataTypeDeclaration);

      LocalScriptVariableDeclaration variable;
      do {
         /* Parse (next) variable name */
         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier (local variable name) expected");
            return null;
         }

         /* Create variable declaration */ 
         variable                     = new LocalScriptVariableDeclaration ();
         variable.sourceStartPosition = scanner.tokenStartPosition;
         variable.sourceEndPosition   = scanner.tokenEndPosition;
         variable.nameStartPosition   = scanner.tokenStartPosition;
         variable.nameEndPosition     = scanner.tokenEndPosition;
         variable.name                = (String)scanner.tokenValue;
         variable.dataTypeDeclaration = dataTypeDeclaration;
         scanner.next ();

         /* Check extra array dimensions ("int [] i [] ...") */
         while (scanner.token == BracketOpenToken) {
            if (!nextToken (BracketCloseToken)) return null;
            variable.extraArrayDimensions++;
            variable.sourceEndPosition = scanner.tokenEndPosition;
            scanner.next ();
         }

         /* Initializer */
         if (scanner.token == AssignToken) {
            scanner.next ();

            JavaExpression initializer;
            if (scanner.token == BraceOpenToken)
               initializer = parseArrayInitializer ();
            else
               initializer = parseJavaExpression ();

            if (initializer == null) return null;
            variable.initializer = initializer;
            variable.sourceEndPosition = initializer.sourceEndPosition;
         }
         declaration.variables.add (variable);

         /* Another declaration? */
         if (scanner.token != CommaToken) break;
         scanner.next ();
      } while (true);

      if (variable.initializer != null) {
         int vt [] = {ColonToken, CommaToken};
         if (!expectTokens (vt)) return null;
      } else {
         int vt [] = {ColonToken, AssignToken, CommaToken};
         if (!expectTokens (vt)) return null;
      }

      declaration.sourceEndPosition = variable.sourceEndPosition;
      skipToken (ColonToken);

      ScriptExpression term = parseScriptTerm (topLevel);
      if (term == null) return null;

      ScriptLocalDataDeclaration expression = new ScriptLocalDataDeclaration ();
      expression.sourceStartPosition = declaration.sourceStartPosition;
      expression.sourceEndPosition   = term.sourceEndPosition;
      expression.nameStartPosition   = declaration.nameStartPosition;
      expression.nameEndPosition     = declaration.nameEndPosition;

      expression.variables  = declaration;
      expression.scriptTerm = term;
      return expression;
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parsePrivateDeclaration (boolean topLevel) {
   

      PrivateScriptDataDeclaration expression = new PrivateScriptDataDeclaration ();
      expression.sourceStartPosition = scanner.tokenStartPosition;
      expression.sourceEndPosition   = scanner.tokenEndPosition;
      expression.nameStartPosition   = scanner.tokenStartPosition;
      expression.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (PrivateToken)) return null;

      PrivateScriptVariableDeclaration variable;
      do {
         /* Parse (next) variable name */
         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier (private variable name) expected");
            return null;
         }

         variable                     = new PrivateScriptVariableDeclaration ();
         variable.sourceStartPosition = scanner.tokenStartPosition;
         variable.sourceEndPosition   = scanner.tokenEndPosition;
         variable.nameStartPosition   = scanner.tokenStartPosition;
         variable.nameEndPosition     = scanner.tokenEndPosition;
         variable.name                = (String)scanner.tokenValue;

         expression.variables.add (variable);
         scanner.next ();
         if (scanner.token != CommaToken) break;
         scanner.next ();
      } while (true);

      int vt [] = {ColonToken, CommaToken};
      if (!expectTokens (vt)) return null;
      skipToken (ColonToken);

      ScriptExpression term = parseScriptTerm (topLevel);
      if (term == null) return null;

      expression.sourceEndPosition = term.sourceEndPosition;
      expression.scriptTerm        = term;
      return expression;
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseScriptCall (boolean topLevel) {
      if (scanner.token != IdentifierToken) {
         parserError (2, "Identifier (script name) expected");
         return null;
      }

      JavaExpressionWithTarget primaryExpression = new NameExpression ();
      primaryExpression.sourceStartPosition = scanner.tokenStartPosition;
      primaryExpression.sourceEndPosition   = scanner.tokenEndPosition;
      primaryExpression.nameStartPosition   = scanner.tokenStartPosition;
      primaryExpression.nameEndPosition     = scanner.tokenEndPosition;
      //primaryExpression.name              = scanner.tokenRepresentation (scanner.token);
      primaryExpression.name                = (String) scanner.tokenValue;

      //if (!parseCompoundName (nameExpression)) return null; obsolete...
      //nameExpression.sourceEndPosition = nameExpression.nameEndPosition;

      while (scanner.next() == PeriodToken) {

         scanner.next();
         if (!expectToken (IdentifierToken)) return null;

         FieldAccessExpression expression = new FieldAccessExpression ();
         expression.sourceStartPosition = primaryExpression.sourceStartPosition;
         expression.sourceEndPosition   = scanner.tokenEndPosition;
         expression.nameStartPosition   = scanner.tokenStartPosition;
         expression.nameEndPosition     = scanner.tokenEndPosition;
         expression.name                = (String)scanner.tokenValue;

         expression.primaryExpression   = primaryExpression;
         primaryExpression              = expression;
      }
      return parseScriptCall (topLevel, primaryExpression);
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseScriptCall (boolean topLevel, 
                                               JavaExpressionWithTarget fieldAccessExpression) {
   
      /* Parse rest of script call. The script name has already been parsed. */

      ScriptCallExpression scriptCall   = new ScriptCallExpression ();
      scriptCall.sourceStartPosition    = fieldAccessExpression.sourceStartPosition;
      scriptCall.sourceEndPosition      = fieldAccessExpression.sourceEndPosition;
      scriptCall.nameStartPosition      = fieldAccessExpression.nameStartPosition;
      scriptCall.nameEndPosition        = fieldAccessExpression.nameEndPosition;
      scriptCall.name                   = fieldAccessExpression.name;
      scriptCall.scriptAccessExpression = fieldAccessExpression;

      MethodCallParameterList parameterList = new MethodCallParameterList ();
      parameterList.nameStartPosition   = scriptCall.nameStartPosition;
      parameterList.nameEndPosition     = scriptCall.nameEndPosition;
      parameterList.name                = scriptCall.name;
      scriptCall.parameterList          = parameterList;

      int actualIndex = 0;

      /* Formal indexes */
      if (scanner.token == BracketOpenToken) {
    	 parameterList.sourceStartPosition = scanner.tokenStartPosition;
         do {
            scanner.next ();
            JavaExpression argument = parseJavaExpression ();
            if (argument == null) return null;

            ScriptCallParameter scriptArgument = new ScriptCallParameter ();
            scriptArgument.sourceStartPosition = argument.sourceStartPosition;
            scriptArgument.sourceEndPosition   = argument.sourceEndPosition;
            scriptArgument.nameStartPosition   = argument.nameStartPosition;
            scriptArgument.nameEndPosition     = argument.nameEndPosition;
            scriptArgument.name                = argument.name;
            scriptArgument.expression          = argument;
            scriptArgument.actualIndex         = actualIndex;

            parameterList.parameterExpressions.add (scriptArgument);
            actualIndex++;
            int ct [] = {CommaToken, BracketCloseToken};
            if (!expectTokens (ct)) return null;
            if (scanner.token == BracketCloseToken) {
             	parameterList.sourceEndPosition = scanner.tokenStartPosition;
                scanner.next();
            	break;
            }
            skipToken (CommaToken);
         } while (true);
      }
      parameterList.sourceStartPosition = scriptCall.sourceEndPosition;
      parameterList.sourceEndPosition   = scriptCall.sourceEndPosition;

      /* Channel send/receive */
      boolean isSend    = scanner.token == LeftShiftToken;
      boolean isReceive = scanner.token == RightShiftToken;
      if (isSend   
      ||  isReceive) {

         scriptCall.setScriptChannelSend   (isSend);
         scriptCall.setScriptChannelReceive(isReceive);

         parameterList.sourceStartPosition = scanner.tokenEndPosition;
         parameterList.sourceEndPosition   = scanner.tokenEndPosition;
         scanner.next ();
      }
      else if (parameterList.parameterExpressions.size()>0) {
          parserError (2, "Script call indexes are only allowed for send and receive calls over channels");
      }

      /* Parameters */
      if (scanner.token == ParenthesisOpenToken) {
    	 if (parameterList.parameterExpressions.size()==0) { 
             parameterList.sourceStartPosition = scanner.tokenStartPosition;
    	 }
         scanner.next ();
         if (scanner.token != ParenthesisCloseToken) {
            do {
               ScannerPosition argumentPosition = scanner.getPosition ();
               JavaExpression argument = parseJavaExpression ();
               if (argument == null) return null;

               ScriptCallParameter scriptArgument = new ScriptCallParameter ();
               scriptArgument.sourceStartPosition = argument.sourceStartPosition;
               scriptArgument.sourceEndPosition   = argument.sourceEndPosition;
               scriptArgument.nameStartPosition   = argument.nameStartPosition;
               scriptArgument.nameEndPosition     = argument.nameEndPosition;
               scriptArgument.name                = argument.name;
               scriptArgument.expression          = argument;
               scriptArgument.actualIndex         = actualIndex;

               parameterList.parameterExpressions.add (scriptArgument);
               actualIndex++;

/*
i!!      specialCode = 1
i!       specialCode = 2
i?!      specialCode = 3
i?       specialCode = 4
*/

               /* Parse "!", "?", "?!" */
               do { // bogus loop

                  SpecialCode specialCode = argument.getLastSpecialCodeInExpression();

                  if (scanner.token == ExclamationToken) {
                     scriptArgument.isForcing = true;
                     scriptArgument.sourceEndPosition = scanner.tokenEndPosition;
                     scanner.next ();
                     break;
                  }

                  if (scanner.token == QuestionToken) {

                     scriptArgument.sourceEndPosition = scanner.tokenEndPosition;
                     scanner.next ();

                     if (scanner.token == ExclamationToken) {
                        scriptArgument.isAdapting = true;
                        scriptArgument.sourceEndPosition = scanner.tokenEndPosition;
                        scanner.next ();

                        /* Check that adapting parameter is a plain name
                           (it must also be a formal parameter to this script;
                            this is checked in ScripticParser2) */
                        if (   !argument.isPlainName ()
                            || specialCode != SpecialCode.none) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid adapting parameter (must be a formal parameter name)", 
                                        argumentPosition);
                        }
                        break;

                     } else {
                        scriptArgument.isOutput = true;

                        /* Check that output parameter is assignable */
                        if (!argument.canBeAssigned()) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid output parameter (must be assignable)", 
                                        argumentPosition);
                           break;
                        }
/******************************
                        // For the time being, require that output parameter
                        // is a plain name, too (see JavaParser2) 
                        if (   !argument.isPlainName ()
                            || specialCode != 0) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid output parameter (for the time being, "
                                        + "it must be a formal parameter or "
                                        + "locally declared variable name)", 
                                        argumentPosition);
                           break;
                        }
*****************************/
                     }
                     break;
                  } /* scanner.token == QuestionToken */


                  // Neither ! nor ? was found here,
                  // but check whether ! or ? or ?! was parsed with the 
                  // parameter expression

                  if (specialCode == SpecialCode.isMatchingParameterOrAsMatchingParameter) {
                     // "x!"
                     // Take the ! to mean a forcing script call parameter,
                     // not a forcing parameter test
                     argument.setLastSpecialCodeInExpression (SpecialCode.none);
                     scriptArgument.isForcing = true;
                     break;
                  }

                  if (specialCode == SpecialCode.asAdaptingParameter) {
                     // "x?!"
                     // Adapting parameter
                     argument.setLastSpecialCodeInExpression (SpecialCode.none);
                     scriptArgument.isAdapting = true;
                     if (!argument.isPlainName ()) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid adapting parameter (must be a formal parameter name)", 
                                        argumentPosition);
                     }
                     break;
                  }

                  if (specialCode == SpecialCode.isOutParameterOrAsOutParameter) {
                     // "x?"
                     // Take the ? to mean an output script call parameter,
                     // not an output parameter test
                     argument.setLastSpecialCodeInExpression (SpecialCode.none);
                     scriptArgument.isOutput = true;
                        if (!argument.canBeAssigned()) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid output parameter (must be assignable)", 
                                        argumentPosition);
                           break;
                        }

                        /* For the time being, require that output parameter
                           is a plain name, too (see JavaParser2) */
                        if (!argument.isPlainName ()) {
                           argumentPosition.tokenEndPosition = argument.sourceEndPosition;
                           parserError (2, "Invalid output parameter (for the time being, "
                                        + "it must be a formal parameter or "
                                        + "locally declared variable name)", 
                                        argumentPosition);
                        }
                     break;
                  }
                  break;
               } while (false);

               int ct [] = {CommaToken, ParenthesisCloseToken};
               if (!expectTokens (ct)) return null;
               if (scanner.token == ParenthesisCloseToken) break;
               skipToken (CommaToken);
            } while (true);
         }
         parameterList.sourceEndPosition = scanner.tokenEndPosition;
         skipToken (ParenthesisCloseToken);
      }

      scriptCall.sourceEndPosition    = parameterList.sourceEndPosition;
      return scriptCall;
   }

   /*-----------------------------------------------------------------*/

   protected ScriptExpression parseSpecialOperand () {

      SpecialNameScriptExpression specialNameExpression = new SpecialNameScriptExpression ();
      specialNameExpression.sourceStartPosition = scanner.tokenStartPosition;
      specialNameExpression.sourceEndPosition   = scanner.tokenEndPosition;
      specialNameExpression.nameStartPosition   = scanner.tokenStartPosition;
      specialNameExpression.nameEndPosition     = scanner.tokenEndPosition;
      specialNameExpression.name                = Scanner.tokenRepresentation (scanner.token);
      specialNameExpression.token               = scanner.token;

      scanner.next ();
      return specialNameExpression;
   }

   /*-----------------------------------------------------------------*/

   /* The following method must be overridden from the JavaParser0
      in order to account for "output" and "adapting" script call parameters
      "Key>>(x?)" and "Key(x?!)". In order to avoid confusion with the
      ternary conditional operator "? :", we check whether the "?"
      is immediately followed by one of "!", ")" or ",". If so, we
      won't take it to be a ternary conditional operator. Note that this
      extra check should not affect the bona fide usage of the "?" as a
      conditional operator in any way. 
      
      Note that this solution isn't perfect. It won't always produce
      optimal error messages.

      ALSO, a better strategy would be to look for something that
      CONFIRMS the start of an expression, not something that rejects it.
      THIS IS TO BE IMPROVED */

   protected JavaExpression parseConditionalExpression () {

      /* Three lines copied from JavaParser0 */
      JavaExpression conditionExpression = parseConditionalOrExpression (null);
      if (conditionExpression == null) return null;
      if (scanner.token != QuestionToken) return conditionExpression;

      /* Remember position */
      ScannerPosition questionPosition = scanner.getPosition ();

      /* Quick peek after the question mark */
      int token = scanner.next ();
      scanner.setPosition (questionPosition);

      /* Now do the check */
      if (   token == ExclamationToken
          || token == CommaToken
          || token == ParenthesisCloseToken
          || token == BraceCloseToken
          || token == BraceColonCloseToken
          || token == BraceQuestionCloseToken
          || token == SemicolonToken)
          
         /* Leave the question mark alone */
         return conditionExpression;

      /* Otherwise, continue as usual in the JavaParser0 */
      return parseRestOfConditionalExpression (conditionExpression);
   }

   /*-----------------------------------------------------------------*/

   /* This method is overridden to parse the "script.this" construct */

   protected JavaExpression parsePrimaryExpression () {
      if (scanner.token != ScriptToken)
         return super.parsePrimaryExpression ();

      /* The whole "script.this" thing is represented by
         a SpecialNameExpression with token == ScriptToken */
      SpecialNameExpression specialNameExpression = new SpecialNameExpression (scanner.token);
      specialNameExpression.sourceStartPosition = scanner.tokenStartPosition;
      specialNameExpression.sourceEndPosition   = scanner.tokenEndPosition;
      specialNameExpression.nameStartPosition   = scanner.tokenStartPosition;
      specialNameExpression.nameEndPosition     = scanner.tokenEndPosition;
      specialNameExpression.name                = Scanner.tokenRepresentation (scanner.token);

      if (   scanner.next () != PeriodToken
          || scanner.next () != ThisToken) {
         parserError (2, "\"script.this\" expected");
         return null;
      }
      specialNameExpression.sourceEndPosition   = scanner.tokenEndPosition;
      specialNameExpression.name                = "script.this";
      scanner.next ();
      return specialNameExpression;
   }

   /*-----------------------------------------------------------------*/

   /* This method is overridden to parse the parameter type tests
      i!, i? and i!! */

   protected JavaExpression parse_NameFieldArrayMethod_Access (NameExpression nameExpression) {

      if (   scanner.token != ExclamationToken
          && scanner.token != QuestionToken
          && scanner.token != DoubleExclamationToken)
         // No need to worry...
         return parse_FieldArrayMethod_Access (nameExpression);

      if (scanner.token == DoubleExclamationToken) {
         // The most clear-cut case
         nameExpression.specialCode = SpecialCode.matchingTest;
         nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
         scanner.next ();
         return nameExpression;
      }

      if (scanner.token == ExclamationToken) {
         // The trouble here is that this could be either a 
         // forcing parameter test or a forcing script call parameter.
         // We will distinguish the latter when we parse
         // the script call parameters.
         nameExpression.specialCode = SpecialCode.isMatchingParameterOrAsMatchingParameter;
         nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
         scanner.next ();
         return nameExpression;
      }

      if (scanner.token == QuestionToken) {
         // This could be an output parameter test or an 
         // output script call parameter. Or it could be part of 
         // an adapting script call parameter. Worse yet, we could
         // be wrong altogether and the question could be part of
         // a conditional operator "? :" sequence.

         // Let's first eliminate the latter option. Like in the
         // conditional operator parsing code, we take the question mark
         // to be a conditional operator if it's NOT followed by
         // something that terminates an expression.
         // TO BE IMPROVED -- we should look for CONFIRMATION of the start
         // of an expression, not rejection.
         
         ScannerPosition questionPosition = scanner.getPosition ();
         scanner.next ();

         if (   scanner.token != ExclamationToken
             && scanner.token != SemicolonToken
             && scanner.token != BraceCloseToken
             && scanner.token != BraceAsteriskCloseToken
             && scanner.token != CommaToken
             && scanner.token != ParenthesisCloseToken) {
            // That settles that little matter...
            scanner.setPosition (questionPosition);
            return parse_FieldArrayMethod_Access (nameExpression);
            // or just return nameExpression;
         }

         if (scanner.token == ExclamationToken) {
            // Couldn't be simpler. An adapting parameter.

            nameExpression.specialCode = SpecialCode.asAdaptingParameter;  
            nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
            scanner.next ();
            return nameExpression;
         }

         // Distinguish the two meanings of "?" 
         // in the script call parameter parser
         nameExpression.specialCode       = SpecialCode.isOutParameterOrAsOutParameter;
         nameExpression.sourceEndPosition = questionPosition.tokenEndPosition;
         return nameExpression;
      }
      return parse_FieldArrayMethod_Access (nameExpression);
   }
/*
i!!      specialCode = 1
i!       specialCode = 2
i?!      specialCode = 3
i?       specialCode = 4
*/

}
