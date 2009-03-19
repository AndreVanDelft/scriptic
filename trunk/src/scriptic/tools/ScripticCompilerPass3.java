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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import scriptic.tools.lowlevel.SourceFileAttribute;

/** 
 *  resolves data types used in variable declarations and method headers
 *  This pass is needed to finish the class's field signatures
 *  (dimenions of refinements may need those of the variables, so these are
 *   postponed a pass)
 *  Also adds default constructor if this is a class and there are no other constructors...
 */
public class ScripticCompilerPass3 extends ScripticParseTreeEnumeratorWithContext
               implements scriptic.tokens.ScripticTokens {

   public ScripticCompilerPass3 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }

   /* Main entry point */
   public boolean resolve (TopLevelTypeDeclaration t) {
      processTopLevelTypeDeclaration(t, null, null, null);
      return true;
   }
   
   // hack for design problem: anonymous types are not well taken into passes 3,4,5,6
   public boolean resolveAnonymousTypeDeclaration (AnonymousTypeDeclaration t) {
      pushContext (new LocalClassContext (env, this, t) );
      processLocalOrNestedTypeDeclaration(t, 0, null, null, null);
      popContext();
      return true;
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Type (= class or interface) Declaration            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processTopLevelTypeDeclaration (TopLevelTypeDeclaration t, ReturnValueHolder retValue, Object arg1, Object arg2) {
      initDeclarationTables ();
      super.processTopLevelTypeDeclaration (t, retValue, arg1, arg2);
   }

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      HashMap<String, HashMap<String, ScriptDeclaration>> s1 = scriptNamesSignatures;
      HashMap<String, HashMap<String, ChannelDeclaration>> s2 = channelNamesSignatures;
      HashMap<String, ArrayList<CommunicationDeclaration>> s3 = communicationNames;
      HashMap<String, HashMap<String, CommunicationPartnerDeclaration>> s4 = commPartnerNamesSignatures;
      HashMap<String, ChannelDeclaration> s5 = firstChannelSignatureOccurrences;
      HashMap<String, CommunicationPartnerDeclaration> s6 = firstCommPartnerSignatureOccurrences;
      initDeclarationTables ();
      super.processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
      scriptNamesSignatures                = s1;
      channelNamesSignatures               = s2;
      communicationNames                   = s3;
      commPartnerNamesSignatures           = s4;
      firstChannelSignatureOccurrences     = s5;
      firstCommPartnerSignatureOccurrences = s6;
   }

   /* pass 4 may add a default constructor
    * pass 6 sets the otherConstructorInvocation field
    */
   private void endOfProcessTypeDeclaration () {

      if (classType.getSuperclass(env) == null
      && !classType.nameWithDots.equals("java.lang.Object")) {
          classType.superclass1 = env.javaLangObjectType;
      }

      if (typeDeclaration.isClass()
      &&  classType.getMethodNamesSignatures(env).get("<init>")==null) { // add a default constructor
          ConstructorDeclaration c = new ConstructorDeclaration();
          c.sourceStartPosition = typeDeclaration.sourceStartPosition;
          c.sourceEndPosition   = typeDeclaration.sourceEndPosition;
          c.nameStartPosition   = typeDeclaration.nameStartPosition;
          c.nameEndPosition     = typeDeclaration.nameEndPosition;
          c.name                = "<init>";
          c.typeDeclaration     = typeDeclaration;
          c.statements          = new StatementBlock();
          c.parameterList       = new ParameterList();
          c.modifiers           = new Modifiers();
          if (typeDeclaration.modifiers.isPublic()) {
              c.modifiers.modifierFlags |= ModifierFlags.PublicFlag;
          }
          Method m              = c.makeTarget(env);
          m.returnType          = VoidType.theOne;
          m.signature           = "()V";
          if (classType.needsParentReference()) {
              Parameter p       = new Parameter();
              p.name            = "this$"+classType.parent().nestingLevel();
              p.dataType1       = classType.parent();
              m.parameters      = new Parameter[1];
              m.parameters[0]   = p;
              m.signature       = "(L"+classType.parent().nameWithSlashes+";)V".intern();
          }
          HashMap<String, Object> signatures  = new HashMap<String, Object>();
          signatures.put(m.signature,m);
          classType.getMethodNamesSignatures(env).put (m.name, signatures);
          classType.addMethod (m);
          classType.defaultConstructor = c;
      }
      else if (typeDeclaration instanceof AnonymousTypeDeclaration) {
              parserError (2, "Anonymous classes cannot have constructor declarations",
                            typeDeclaration.sourceStartPosition, 
                            typeDeclaration.  sourceEndPosition);
      }
      assignScriptSequenceNumbers ();

      if (typeDeclaration.compilationUnit != null) {
          String s = typeDeclaration.compilationUnit.getSourceFileName();
          if (s.length()!=0) {
            classType.addAttribute (new SourceFileAttribute(s));
          }
      }
   }
   void endOfProcessTopLevelTypeDeclaration     () {endOfProcessTypeDeclaration();}
   void endOfProcessLocalOrNestedTypeDeclaration() {endOfProcessTypeDeclaration();}

   /*-----------------------------------------------------------------*/

   // Tables to accumulate per-class method, variable, and
   // script declaration info
   protected HashMap<String, HashMap<String, ScriptDeclaration>>  scriptNamesSignatures;     
   protected HashMap<String, HashMap<String, ChannelDeclaration>> channelNamesSignatures; 
   protected HashMap<String, ArrayList<CommunicationDeclaration>> communicationNames;
   protected HashMap<String, HashMap<String, CommunicationPartnerDeclaration>> commPartnerNamesSignatures;
   protected HashMap<String, ChannelDeclaration> firstChannelSignatureOccurrences;
   protected HashMap<String, CommunicationPartnerDeclaration> firstCommPartnerSignatureOccurrences;


   protected void initDeclarationTables () {
       scriptNamesSignatures               = new HashMap<String, HashMap<String, ScriptDeclaration>>();
      channelNamesSignatures               = new HashMap<String, HashMap<String, ChannelDeclaration>>();
      communicationNames                   = new HashMap<String, ArrayList<CommunicationDeclaration>>();
      commPartnerNamesSignatures           = new HashMap<String, HashMap<String, CommunicationPartnerDeclaration>>();
      firstChannelSignatureOccurrences     = new HashMap<String, ChannelDeclaration>();
      firstCommPartnerSignatureOccurrences = new HashMap<String, CommunicationPartnerDeclaration>();
   }

   //-----------------------------------------------------------------

   // Assign sequence numbers to the given declarations. Assumes 
   // there's more than one declaration in the Enumeration.

   protected void assignScriptSequenceNumbers (
		   HashMap<String, ? extends BasicScriptDeclaration> declarations) {
	   
	   assignScriptSequenceNumbers(declarations.values());
   }
   protected void assignScriptSequenceNumbers (
		   Collection<? extends BasicScriptDeclaration> declarations) {
	   
	  int sequenceNumber = 0;
      for (BasicScriptDeclaration declaration: declarations) {
         declaration.sequenceNumber = sequenceNumber++;
      }
   }

   protected void assignAllScriptSequenceNumbers (
		   HashMap<String, HashMap<String, ? extends BasicScriptDeclaration>> declarationTables) {
      for (HashMap<String, ? extends BasicScriptDeclaration> declarations: declarationTables.values()) {
         if (declarations.size() > 1)
            assignScriptSequenceNumbers (declarations);
      }
   }

   protected void assignScriptSequenceNumbers () {
      for (HashMap<String, ? extends BasicScriptDeclaration> declarations: scriptNamesSignatures.values()) {
          if (declarations.size() > 1)
             assignScriptSequenceNumbers (declarations);
       }
      for (HashMap<String, ? extends BasicScriptDeclaration> declarations: channelNamesSignatures.values()) {
          if (declarations.size() > 1)
             assignScriptSequenceNumbers (declarations);
       }
      for (HashMap<String, ? extends BasicScriptDeclaration> declarations: commPartnerNamesSignatures.values()) {
          if (declarations.size() > 1)
             assignScriptSequenceNumbers (declarations);
       }

      // Bloody @!#$! Java library has no common ancestor class ("Collection")
      // for Vector and Hashtable
      for (ArrayList<CommunicationDeclaration> commDeclarations: communicationNames.values()) {
         if (commDeclarations.size() > 1)
            assignScriptSequenceNumbers (commDeclarations);
      }
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Superclasses and superinterfaces                   */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processSuperclassDeclaration (TypeDeclaration t, SuperclassDeclaration superclass, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (classType == env.javaLangObjectType) return; // error ???

      classType.superclass1 = (ClassType) mustResolveDataTypeDeclaration (superclass);
      if (!classType.superclass1.isResolved()) return;
      if ( classType.superclass1.isInterface()) {
            env.parserError (2, superclass.name
                           + " is an interface, so it cannot act as a superclass",
                               new ScannerPosition (scanner, superclass.nameStartPosition, 
                                                             superclass.nameEndPosition));
      } else
      if ( classType.superclass1.isFinal()) {
            env.parserError (2, t.name
                           + " cannot inherit from final class " + superclass.name,
                               new ScannerPosition (scanner, superclass.nameStartPosition, 
                                                             superclass.nameEndPosition));
      }
   }

   protected void processImplementsDeclaration (TypeDeclaration t, ImplementsDeclaration interfaceDeclaration, ReturnValueHolder retValue, Object arg1, Object arg2) {
      ClassType theInterface = (ClassType) mustResolveDataTypeDeclaration (interfaceDeclaration);
      if (!theInterface.isResolved()) return;
      classType.addInterface (theInterface);
      if ( theInterface.isClass()) {
            env.parserError (2, interfaceDeclaration.name
                           + " is a class, not an interface ",
                               new ScannerPosition (scanner, interfaceDeclaration.nameStartPosition, 
                                                             interfaceDeclaration.nameEndPosition));
      }
   }

   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processVariableDeclaration  (MultiVariableDeclaration multiVariableDeclaration,
                                               MemberVariableDeclaration variableDeclaration,
                                               int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      mustResolveDataTypeDeclaration  (variableDeclaration.dataTypeDeclaration);

      MemberVariable variable = variableDeclaration.makeTarget(env);

      if (classType.variablesByName.containsKey (variable.name)) {
               parserError (2, "Duplicate declaration of variable or unit \""
                              + variable.name + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition);
               return;
      }
      /* LangSpec $8.4: method names and variable names may overlap
       */

      classType.addMemberVariable (variable);
      classType.variablesByName.put (variable.name, variable);
   }

   /*-----------------------------------------------------------------*/

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      mustResolveDataTypeDeclaration (parameter.dataTypeDeclaration);
   }

   /*-----------------------------------------------------------------*/

      // we don't process the body, but we do process the localTypeDeclarations!!!
   protected void processLocalTypeDeclarationsInRefinement (RefinementDeclaration refinementDeclaration,
                                                            int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (LocalTypeDeclaration ltd: refinementDeclaration.localTypeDeclarations) {
          processLocalTypeDeclaration (refinementDeclaration,
        		  					   ltd,
                                       level+1, retValue, arg1, arg2);
      }
   }
   protected void processMethodDeclaration (MethodDeclaration methodDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      mustResolveDataTypeDeclaration (methodDeclaration.returnTypeDeclaration);
      makeSignatureAndTarget (methodDeclaration, level, retValue, arg1, arg2);
      if (methodDeclaration.throwsClause != null)
         processThrowsClause   (methodDeclaration, methodDeclaration.throwsClause, level + 1, retValue, arg1, arg2);

      pushContext (new MethodContext (env, this, methodDeclaration));
      processLocalTypeDeclarationsInRefinement (methodDeclaration, level, retValue, arg1, arg2);
      popContext();

      classType.addMethod (methodDeclaration.target);
   }

   protected void processInitializerBlock (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new InitializerBlockContext (env, this, initializerBlock));
      processLocalTypeDeclarationsInRefinement (initializerBlock, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void processScriptDeclaration (ScriptDeclaration scriptDeclaration, int level,
                                            ReturnValueHolder retValue, Object arg1, Object arg2) {
      makeSignatureAndTarget (scriptDeclaration, level, retValue, scriptNamesSignatures, arg2);
      pushContext (new ScriptContext (env, this, scriptDeclaration));
      processLocalTypeDeclarationsInRefinement (scriptDeclaration, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void processCommunicationDeclaration (CommunicationDeclaration comm, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      /* Collect communications with the same partner names.
         This is needed for assigning sequence numbers -- the signatures are
         not significant in the names of generated
         communication templates. */

      String commName       = comm.getPartnerNames();
      ArrayList<CommunicationDeclaration> communications = communicationNames.get (commName);
      if (communications == null) {
          communications = new ArrayList<CommunicationDeclaration>();
          communicationNames.put (commName, communications);
      }
      communications.add (comm);

      processCommunicationPartners(comm, comm.partners     , level + 1, retValue, arg1, new HashMap<String, CommunicationPartnerDeclaration>());

      pushContext (new ScriptContext (env, this, comm));
      processLocalTypeDeclarationsInRefinement (comm, level, retValue, arg1, arg2);
      popContext ();
   }
   protected void processChannelDeclaration (ChannelDeclaration channel, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      makeSignatureAndTarget (channel, level, retValue, channelNamesSignatures, arg2);
      pushContext (new ScriptContext (env, this, channel));
      processLocalTypeDeclarationsInRefinement (channel, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void makeSignatureAndTarget (RefinementDeclaration refinementDeclaration,
                                                int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      //if (refinementDeclaration.returnTypeDeclaration!=null) {
      //    mustResolveDataTypeDeclaration (refinementDeclaration.returnTypeDeclaration);
      //}
      if (refinementDeclaration.parameterList != null)
         processParameterList  (refinementDeclaration, refinementDeclaration.parameterList, level + 1, retValue, arg1, arg2);

      Method method = refinementDeclaration.makeTarget(env);

/*
if (method.returnType==null) {
System.out.println("ERROR; refinement.returnType==null in: "
                  +refinementDeclaration.getName()
                  + " class: "+refinementDeclaration.typeDeclaration.getName()
                  + "\nreturnTypeDeclaration: "+refinementDeclaration.returnTypeDeclaration.getName());
refinementSignature = "(LERROR;)LERROR;";
}
else */

      String signature = method.getSignature (env);

//System.out.println ("method.getSignature: "+signature);

      signaturesBlock: {
    	 HashMap<String, Object> signatures = classType.getMethodNamesSignatures (env).get (method.name);
         if (signatures == null) {
             signatures = new HashMap<String, Object>();
             classType.getMethodNamesSignatures (env).put (method.name, signatures);
         }
         else if (signatures.containsKey (signature)) {
                parserError (2, "Duplicate declaration of refinement (method or script) \""
                            + method.getSignaturePresentation() + "\"",
                              refinementDeclaration.nameStartPosition, 
                              refinementDeclaration.nameEndPosition);
                break signaturesBlock;
         }
         /* LangSpec $8.4: method names and variable names may overlap
          */
         signatures.put (signature, method);

         if (arg1 != null) { // then it is a kind of scriptNamesSignatures table...
                             // put the refinement in it,
                             // for subsequent sequence number assignment
        	 HashMap<String, HashMap<String, Object>> namesSignatures = (HashMap<String, HashMap<String, Object>>) arg1;
             signatures = namesSignatures.get(refinementDeclaration.name);
             if (signatures == null) {
                 signatures = new HashMap<String, Object>();
                 namesSignatures.put (refinementDeclaration.name, signatures);
             }
             signatures.put (signature, refinementDeclaration);
         }
      } // signaturesBlock
   }

   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      makeSignatureAndTarget (constructorDeclaration, level, retValue, arg1, arg2);
      if (constructorDeclaration.throwsClause != null)
         processThrowsClause   (constructorDeclaration,
                                constructorDeclaration.throwsClause, level + 1, retValue, arg1, arg2);

      pushContext (new MethodContext (env, this, constructorDeclaration));
      processLocalTypeDeclarationsInRefinement (constructorDeclaration, level, retValue, arg1, arg2);
      popContext();

      classType.addMethod (constructorDeclaration.target);
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                      Communication Partner                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processCommunicationPartner   (CommunicationDeclaration communicationDeclaration, CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      String partnerName                 = partner.getName ();
      String partnerSignature            = partner.getNameSignature ();
      HashMap<String, CommunicationPartnerDeclaration> localPartnerSignatures = 
    	  (HashMap<String, CommunicationPartnerDeclaration>) arg2;

      /* Is this the first occurrence of this communication partner?
         Only the first occurrence needs to generate template and method. */
      CommunicationPartnerDeclaration firstSignatureOccurrence
            = (CommunicationPartnerDeclaration) firstCommPartnerSignatureOccurrences.get (partnerSignature);
      if (firstSignatureOccurrence == null) {
    	  firstSignatureOccurrence = partner;
         firstCommPartnerSignatureOccurrences.put (partnerSignature, firstSignatureOccurrence);
      }
      else
      {
         partner.firstOccurrence = firstSignatureOccurrence;
      }
      firstSignatureOccurrence.communications.add(communicationDeclaration);
      
      checkSignatureOccurrence:
      { 
    	 HashMap<String, CommunicationPartnerDeclaration> partnerSignatures = commPartnerNamesSignatures.get (partnerName);
         if (partnerSignatures == null) {
            partnerSignatures = new HashMap<String, CommunicationPartnerDeclaration> ();
            commPartnerNamesSignatures.put (partnerName, partnerSignatures);
         } else {
            // It's not an error for a communication partner to occur
            // multiple times -- as long as they're in different comm declarations
         }

         if (localPartnerSignatures.get (partnerSignature) != null) {
               parserError (2, "Duplicate occurrence of communication partner \"" 
                              + partnerSignature 
                              + "\"",
                            partner.nameStartPosition, 
                            partner.nameEndPosition); 
               break checkSignatureOccurrence;
         } else {
            localPartnerSignatures.put (partnerSignature, partner);
         }

         if (firstSignatureOccurrence == null) {
            /* Only the first occurrence of a partner signature will generate
               template and thus needs to be stored */
            partnerSignatures.put (partnerSignature, partner);
         }

         HashMap<String, Object> signatures = classType.getMethodNamesSignatures (env).get (partnerName);
         if (signatures == null) {
            signatures = new HashMap<String, Object>();
            classType.getMethodNamesSignatures (env).put (partnerName, signatures);
         } else {
            if (firstSignatureOccurrence == null
            && signatures.containsKey (partnerSignature)) {
               parserError (2, "Illegal declaration; refinement (method or script) with signature \""
                              + partnerSignature + "\" already exists" ,
                            partner.nameStartPosition, 
                            partner.nameEndPosition); 
               break checkSignatureOccurrence;
            }
         }
         if (partner.parameterList != null)
            processParameterList  (partner, partner.parameterList, level + 1, retValue, arg1, arg2);
         Method method = partner.makeTarget(env);
         signatures.put (method.getSignature(env), method);
      } 
      super.processCommunicationPartner   (communicationDeclaration, partner, level, retValue, arg1, arg2);
   }

   protected void processThrowsClause (MethodDeclaration methodDeclaration, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      methodDeclaration.target.exceptionTypes = new ClassType[throwsClause.exceptionTypeDeclarations.size()];
      for (int i=0; i<methodDeclaration.target.exceptionTypes.length; i++) {
         DataTypeDeclaration exceptionType = throwsClause.exceptionTypeDeclarations.get(i);
         mustResolveDataTypeDeclaration(exceptionType);
         methodDeclaration.target.exceptionTypes[i] = (ClassType) exceptionType.dataType;
      }
   }

   /*-----------------------------------------------------------------*/

   protected void processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}

}


