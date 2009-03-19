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
import scriptic.tools.lowlevel.ByteCodingException;

/** 
 *  This tiny pass is needed to resolve dimension declarations
 */
public class ScripticCompilerPass4 extends ScripticParseTreeEnumerator
               implements scriptic.tokens.ScripticTokens {

   public ScripticCompilerPass4 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }

   ArrayList<DimensionDeclaration> oldReducingCompoundDimensions;
   ArrayList<DimensionDeclaration>    reducingCompoundDimensions;

   /* Main entry point.
    * add to-be-reduced compound dimension declarations to reducingCompoundDimensions
    * that is:
    *   declared dimensions defined in terms of dimensions that are either
    *   - base dimensions
    *   - known in terms of this (!!@$#%)
    *   - if oldReducingCompoundDimensions==null: dimension declarations
    *   - if oldReducingCompoundDimensions!=null: oldReducingCompoundDimensions
the first call: reducingCompoundDimensions becomes the set of (compound) dimension declarations
that did not immediately reduce to base dimensions
successive calls: reducingCompoundDimensions becomes the subset of
oldReducingCompoundDimensions that did not yet reduce to base dimensions
Reduction succeeds iff reducingCompoundDimensions shrinks during successive calls.
The first call oldReducingCompoundDimensions MUST BE null!
    */
   public boolean resolve (TopLevelTypeDeclaration t,
                           ArrayList<DimensionDeclaration> oldReducingCompoundDimensions2,
                           ArrayList<DimensionDeclaration> reducingCompoundDimensions2) {
      this.oldReducingCompoundDimensions = oldReducingCompoundDimensions2;
      this.   reducingCompoundDimensions =    reducingCompoundDimensions2;
      // System.out.println("resolve("+t.name+")");
      processTopLevelTypeDeclaration(t, null, null, null);
      return true;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Type (= class or interface) Declaration            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processTopLevelTypeDeclaration (TopLevelTypeDeclaration t, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processTopLevelTypeDeclaration (t, retValue, arg1, arg2);
   }

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
   }

   private void endOfProcessTypeDeclaration () {
   }
   void endOfProcessTopLevelTypeDeclaration     () {endOfProcessTypeDeclaration();}
   void endOfProcessLocalOrNestedTypeDeclaration() {endOfProcessTypeDeclaration();}

   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processBaseDimensionDeclaration (BaseDimensionDeclaration baseDimensionDeclaration,
                                                   int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (oldReducingCompoundDimensions!=null)
         return;

      // this only in the first time resolve is called:

      if (classType.resolveDimensionHere (baseDimensionDeclaration.name) != null) {
               parserError (2, "Duplicate declaration of dimension \""
                              + baseDimensionDeclaration.name + "\"",
                            baseDimensionDeclaration.nameStartPosition, 
                            baseDimensionDeclaration.nameEndPosition);
      }
      else try {
        baseDimensionDeclaration.makeTarget(classType);
        classType.addDimension    (baseDimensionDeclaration.target);
      } catch (ByteCodingException e) {parserError (2, e.getMessage());
      }
      if (classType.variablesByName.containsKey (baseDimensionDeclaration.unit.name)) {
               parserError (2, "Duplicate declaration of variable or unit \""
                              + baseDimensionDeclaration.unit.name + "\"",
                            baseDimensionDeclaration.unit.nameStartPosition, 
                            baseDimensionDeclaration.unit.nameEndPosition);
               return;
      }
      classType.addMemberVariable   (baseDimensionDeclaration.unit.target);
      classType.variablesByName.put (baseDimensionDeclaration.unit.target.name,
                                     baseDimensionDeclaration.unit.target);
   }
   protected void processCompoundDimensionDeclaration (CompoundDimensionDeclaration compoundDimensionDeclaration,
                                                       int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    if (compoundDimensionDeclaration.target == null) {
      if (classType.resolveDimensionHere (compoundDimensionDeclaration.name) != null) {
               parserError (2, "Duplicate declaration of dimension \""
                              + compoundDimensionDeclaration.name + "\"",
                            compoundDimensionDeclaration.nameStartPosition, 
                            compoundDimensionDeclaration.nameEndPosition);
               return;
      }
      compoundDimensionDeclaration.makeTarget (classType);
      classType.addDimension (compoundDimensionDeclaration.target);
    }
    if (compoundDimensionDeclaration.target.signature != null) {
        return; // already done
    }
    try {
      // check components, invertedComponents
      ArrayList<String> normalSignatures = new ArrayList<String>();
      ArrayList<String> invertSignatures = new ArrayList<String>();
      boolean allComponentsAreOK               = true;
      DimensionReference inaccessibleComponent = null;
      ArrayList<DimensionReference> allComponents = new ArrayList<DimensionReference>(compoundDimensionDeclaration.components);
      if (compoundDimensionDeclaration.invertedComponents != null)
      for (int i=0; i<compoundDimensionDeclaration.invertedComponents.size(); i++) {
         allComponents.add (compoundDimensionDeclaration.invertedComponents.get(i));
      }
      for (int i=0; i<allComponents.size(); i++) {
         DimensionReference dr = allComponents.get(i);

         Dimension d = findTargetDimension (dr);
         if (d==null) {
           allComponentsAreOK = false;
           if (oldReducingCompoundDimensions!=null) { // not the first time that resolve is called
               parserError (2, "Unknown dimension \"" + dr.name + "\"",
                            dr.nameStartPosition, 
                            dr.nameEndPosition);
           }
         }
         else if (d.signature==null) {
           allComponentsAreOK = false;
         }
         else if (allComponentsAreOK) {
           if (i<compoundDimensionDeclaration.components.size())
                normalSignatures.add (d.signature);
           else invertSignatures.add (d.signature);

           if (!d.isAccessibleFor (env, classType, false)) {
               inaccessibleComponent = dr;
           }
         }
      }
      if (allComponentsAreOK) {
        ((CompoundDimension) compoundDimensionDeclaration.target).signature
                             = CompoundDimension.getSignature(normalSignatures, invertSignatures);
        if (compoundDimensionDeclaration.target.signature         == null
        ||  compoundDimensionDeclaration.target.signature.length()==0) {
               parserError (2, "Dimension declaration reduces to zero dimension",
                            compoundDimensionDeclaration.sourceStartPosition, 
                            compoundDimensionDeclaration.sourceEndPosition);
        }
        if (inaccessibleComponent != null) {
               parserError (2, "Dimension is not accessible",
                               inaccessibleComponent.sourceStartPosition,
                               inaccessibleComponent.sourceEndPosition);
        }
      }
      else {
        reducingCompoundDimensions.add (compoundDimensionDeclaration);
      }
    } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {env.handleByteCodingException(e, typeDeclaration, compoundDimensionDeclaration);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), compoundDimensionDeclaration.sourceStartPosition, 
                                                                     compoundDimensionDeclaration.sourceEndPosition);
    }
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
      processLocalTypeDeclarationsInRefinement (methodDeclaration, level, retValue, arg1, arg2);
   }
   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLocalTypeDeclarationsInRefinement (constructorDeclaration, level, retValue, arg1, arg2);
   }

   protected void processMultiVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processInitializerBlock (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processScriptDeclaration (ScriptDeclaration scriptDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processCommunicationDeclaration (CommunicationDeclaration comm, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processChannelDeclaration (ChannelDeclaration channel, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
   protected void processCommunicationPartner   (CommunicationDeclaration communicationDeclaration, CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}
}


