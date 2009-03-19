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
import java.util.HashMap;

import scriptic.tools.lowlevel.ByteCodingException;

/**
 * Check expressions of final variables for constantness
 * potentialConstantMembers - vector of potential constant members,
 * i.e. members which may deserve an initializer in the class file
 * Also does the dimension signatures of the refinements
 *   (only the first time resolve is called)
 */
public class ScripticCompilerPass6 extends ExpressionChecker {

	ArrayList<MemberVariableDeclaration>          potentialConstantMembers;
	ArrayList<MemberVariableDeclaration> collectedPotentialConstantMembers;

   public ScripticCompilerPass6 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }
   boolean firstCallToResolve;

   /* Main entry point.
    * add potential constant members to collectedPotentialConstantMembers
    * that is:
    *   static final members with initializers that only contain elements
    *   that are either
    *   - known from constant expressions
    *   - if potentialConstantMembers==null: final members
    *   - if potentialConstantMembers!=null: potentialConstantMembers
    */
   public boolean resolve (TopLevelTypeDeclaration t,
                           ArrayList<MemberVariableDeclaration> potentialConstantMembers,
                           ArrayList<MemberVariableDeclaration> collectedPotentialConstantMembers) {
      this.         potentialConstantMembers =          potentialConstantMembers;
      this.collectedPotentialConstantMembers = collectedPotentialConstantMembers;
      firstCallToResolve = potentialConstantMembers==null;
      // System.out.println("resolve("+t.name+")");
      processTopLevelTypeDeclaration(t, null, null, null);
      return true;
   }

   /** check for inheritance cycles
    *  Ideally/theoretically this should NOT be done in pass 5, since pass 5 may iterate a few
    *  times to resolve constants; the extra cost of redundant checks will be neglectable, though.
    *  The checks need to be performed before pass 6;
    *  could be moved to the top of the main function of pass 6.
    */
   protected void checkCyclicInheritanceErrors (TypeDeclaration t) {

      try {
        if (t.target.getSuperclass(env) != null
        &&  t.target.superclass1.isSubtypeOf (env, t.target)) {
            env.parserError (2,  t.target.nameWithDots + " attempts cyclic class inheritance", t);
            return;
        }
      } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), t.superclass.nameStartPosition, 
                                                                    t.superclass.  nameEndPosition);
      }
      for (int i=0; i<t.target.getInterfaces(env).length; i++) {
          try {
            if (t.target.interfaces1[i].isSubtypeOf (env, t.target)) {
              env.parserError (2, t.target.nameWithDots + " attempts cyclic interface inheritance", t);
              return;
            }
          } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
          } catch (java.io.IOException e) {parserError (2, e.getMessage(), t.interfaces.get(i).nameStartPosition, 
                                                                           t.interfaces.get(i).  nameEndPosition);
          }
      }
      return;
   }

   void endOfProcessTopLevelTypeDeclaration     () {checkCyclicInheritanceErrors(typeDeclaration);}
   void endOfProcessLocalOrNestedTypeDeclaration() {checkCyclicInheritanceErrors(typeDeclaration);}

   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, MemberVariableDeclaration variable, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (multiVariableDeclaration.isFinal()
   // &&  multiVariableDeclaration.isStatic()  not correct
      ||  typeDeclaration.isInterface()) {
         if (variable.initializer != null // can't be otherwise, right?
         && !variable.hasError) {
            variable.getName ();
            int errorCount = parserErrorCount();

            pushContext (new FieldInitializerContext (env, this, variable));
            processJavaExpression (variable.owner, variable.initializer, 0, null, null, null);
            popContext();

            variable.hasError = errorCount != parserErrorCount(); // relevant for pass 6

            if ( variable.hasError                ) return;
            if (!variable.dataType().isPrimitive()) return;

            if (firstCallToResolve
            &&  variable.initializer.constantValue==null
            ||  variable.initializer.constantValue==ConstantMaybe.theOne) {
                variable.target.  setConstantValue (ConstantMaybe.theOne);
                collectedPotentialConstantMembers.add (variable);
                return;
            }
            if (!variable.initializer.isConstant()) {
                 variable.target.setConstantValue (null);
                 return;
            }
            try {
              if(!variable.initializer.canBeAssignedTo(env, variable.dataType())) {
                   parserError (2, "Incompatible types for initialization",
                                 variable.sourceStartPosition,
                                 variable.sourceEndPosition);
                   variable.hasError = true;
                   return;
              }
              variable.initializer = variable.initializer.convertForAssignmentTo (env, variable.dataType());
              variable.target.addAttribute     (variable.initializer.constantValue.makeAttribute(classType));
              variable.target.setConstantValue (variable.initializer.constantValue);

              // dimension checking

              // 0 may be assigned to any dimensioned number...
              if ( variable.initializer.dimensionSignature==null
              &&   variable.initializer.isZeroLiteralOrPrimitiveCastOfZero()) {
                 // just OK;
              }
              else if (!Dimension.equal (variable.target.dimensionSignature,
                                         variable.initializer.dimensionSignature)) {
                    // ...other clashes are prohibited
                    parserError (2, "Variable and initializer have different dimensions"
+": "+Dimension.getPresentation(variable.target.dimensionSignature)
+" and "+Dimension.getPresentation(variable.initializer.dimensionSignature),
                                 variable.initializer.sourceStartPosition,
                                 variable.initializer.sourceEndPosition);
              }

            } catch (ByteCodingException            e) {parserError (2, e.getMessage());
            } catch (java.io.IOException e) {parserError (2, e.getMessage(), variable.initializer.nameStartPosition, 
                                                                          variable.initializer.nameEndPosition);
            }
         }
      }
   }

   // we *do* process non-TopLevel NestedTypeDeclarations and LocalTypeDeclarations!!!
   // although these cannot contain *static* members, so there are no constants to check
   // cyclic inheritance needs to be checked
   // refinement declarations need their dimension signatures checked

   protected void processNestedTypeDeclaration(NestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
       if (t.isTopLevelType()||firstCallToResolve)
          super.processNestedTypeDeclaration (t, level, retValue, arg1, arg2);
       checkCyclicInheritanceErrors (t);
   }

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      // Vector paramsVector = (Vector) arg1;
      getDimensionSignatureOf (parameter.dataTypeDeclaration);
      parameter.target.dimensionSignature = parameter.dataTypeDeclaration.dimensionSignature;
      ((PartialRefinementContext)context).addParameter (parameter.target);
//System.out.println (parameter.getDescription());
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
      if (!firstCallToResolve) return;
      //Vector paramsVector = new Vector();
      pushContext (new PartialRefinementContext (env, this, typeDeclaration));
      if (methodDeclaration.parameterList != null)
         processParameterList  ((RefinementDeclaration)methodDeclaration, methodDeclaration.parameterList, level + 1, retValue, arg1, arg2);
      getDimensionSignatureOf (methodDeclaration.returnTypeDeclaration);
      methodDeclaration.target.returnDimensionSignature = methodDeclaration.returnTypeDeclaration.dimensionSignature;
      methodDeclaration.target.setDimensionSignature();
      popContext();
      pushContext (new MethodContext (env, this, methodDeclaration));
      processLocalTypeDeclarationsInRefinement (methodDeclaration, level, retValue, arg1, arg2);
      popContext();
   }
   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      if (constructorDeclaration.parameterList != null) {
         pushContext (new PartialRefinementContext (env, this, typeDeclaration));
         processParameterList  ((RefinementDeclaration)constructorDeclaration, constructorDeclaration.parameterList, level + 1, retValue, arg1, arg2);
         constructorDeclaration.target.setDimensionSignature();
         popContext();
      }
      pushContext (new MethodContext (env, this, constructorDeclaration));
      processLocalTypeDeclarationsInRefinement (constructorDeclaration, level, retValue, arg1, arg2);
      popContext();
   }
   protected void processInitializerBlock (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      pushContext (new InitializerBlockContext (env, this, initializerBlock));
      processLocalTypeDeclarationsInRefinement (initializerBlock, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void processScriptDeclaration (ScriptDeclaration scriptDeclaration, int level,
                                            ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      if (scriptDeclaration.parameterList != null) {
         pushContext (new PartialRefinementContext (env, this, typeDeclaration));
         processParameterList  ((RefinementDeclaration)scriptDeclaration, scriptDeclaration.parameterList, level + 1, retValue, arg1, arg2);
         scriptDeclaration.target.setDimensionSignature();
         popContext();
      }
      pushContext (new ScriptContext (env, this, scriptDeclaration));
      processLocalTypeDeclarationsInRefinement (scriptDeclaration, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void processCommunicationDeclaration (CommunicationDeclaration comm, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      processCommunicationPartners(comm, comm.partners     , level + 1, retValue, arg1, new HashMap<String, CommunicationPartnerDeclaration>());
      pushContext (new ScriptContext (env, this, comm));
      processLocalTypeDeclarationsInRefinement (comm, level, retValue, arg1, arg2);
      popContext ();
   }
   protected void processChannelDeclaration (ChannelDeclaration channel, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      if (channel.parameterList != null) {
         pushContext (new PartialRefinementContext (env, this, typeDeclaration));
         processParameterList  ((RefinementDeclaration)channel, channel.parameterList, level + 1, retValue, arg1, arg2);
         channel.target.setDimensionSignature();
         popContext();
      }
      pushContext (new ScriptContext (env, this, channel));
      processLocalTypeDeclarationsInRefinement (channel, level, retValue, arg1, arg2);
      popContext ();
   }

   protected void processCommunicationPartner   (CommunicationDeclaration comm, CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!firstCallToResolve) return;
      if (partner.parameterList != null) {
         pushContext (new PartialRefinementContext (env, this, typeDeclaration));
         processParameterList  ((RefinementDeclaration)partner, partner.parameterList, level + 1, retValue, arg1, arg2);
         partner.target.setDimensionSignature();
         popContext();
      }
   }

   protected void processMethodCallExpression     (        FieldDeclaration decl, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processMethodCallParameterList  (FieldDeclaration fieldDeclaration,
                                                   MethodOrConstructorCallExpression methodCall,
                                                   MethodCallParameterList parameterList,
                                                   int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processAllocationExpression     (        FieldDeclaration decl, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processTypeComparisonExpression (        FieldDeclaration decl, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
}


