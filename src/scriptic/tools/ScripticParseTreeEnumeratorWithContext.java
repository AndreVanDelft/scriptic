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


/**
 * Check expressions - determine type and constantness
 */
public class ScripticParseTreeEnumeratorWithContext extends ScripticParseTreeEnumerator
               implements scriptic.tokens.ScripticTokens {

   /* contexts are needed:
    * in pass   5,6 - to resolve member names (pass 5 only top level members)
    * in pass 4,5,6 - to resolve local type names
    *                 variable initializer contexts are not needed in pass 4,5,
    *                 so those contexts are left for pass 6;
    *                 the others (type declarations, refinements) are set below
    */

   Context context;
   ScriptContext scriptContext;

   /*-----------------------------------------------------------------*/

   /**
    * get the dimension signature of the DimensionReference.
    * If the DimensionReference is a RelativeDimensionReference:
    *         if its nameExpression is found in  params:
    *             if the parameter has a free dimension: "1W"+the dimensionSignature of the parameter
    *             else the dimensionSignature of the parameter
    *    else if its nameExpression is found in context: the dimensionSignature of the variable
    *    else error
    * Else use findTargetDimension to lookup the dimension name in the class hierarchy
    */
   String getDimensionSignatureOf (DimensionReference dr) throws ByteCodingException, IOException {

//System.out.println ("getDimensionSignatureOf: "+dr.getPresentation());
//if (params!=null) new Exception ("params: "+params.size()).printStackTrace();
      if (dr instanceof RelativeDimensionReference) {
         RelativeDimensionReference rdr = (RelativeDimensionReference) dr;
         processJavaExpression (null, rdr.javaExpression, 0, null, null, null);

//System.out.println ("getDimensionSignatureOf RelativeDimensionReference: "+rdr.javaExpression.getDescription());
//System.out.println ("getDimensionSignatureOf RelativeDimensionReference: "+dr.getPresentation()+" >> "+rdr.javaExpression.dimensionSignature);

         return rdr.javaExpression.dimensionSignature;
/******
         // check in params
         if (params!=null) {
            for (int i=0; i<params.size(); i++) {
               FormalIndexOrParameter p = (FormalIndexOrParameter) params.elementAt(i);
               if (p.name.equals (rdr.nameExpression.name)) {
                 if (p.dimensionSignature.charAt(0)=='V') {
                    return "1W1"+p.dimensionSignature;
                 }
                 return p.dimensionSignature;
               }
            }
         }
         Variable v = context.resolveVariable (rdr.nameExpression);
         if (v==null) {
              parserError (2, "Unknown identifier",
                    rdr.nameExpression.sourceStartPosition,
                    rdr.nameExpression.sourceEndPosition);
              return Dimension.errorSignature;
         }
         else {
            return v.dimensionSignature;
         }
*******/
      }
      Dimension d = findTargetDimension (dr);
      if (d==null) {
              parserError (2, "Unknown dimension: "+dr.name,
                    dr.sourceStartPosition,
                    dr.sourceEndPosition);
              return Dimension.errorSignature;
      }
      if (!d.isAccessibleFor (env, classType, false)) {
               parserError (2, "Dimension is not accessible",
                               dr.sourceStartPosition,
                               dr.sourceEndPosition);
      }
      return d.signature;
   }

   /*
    * get the dimension signature of the dataTypeDeclaration.
    * If its dimensionDeclaration==null: null
    * If its dimensionDeclaration may not be a FreeDimensionDeclaration
    * If its dimensionDeclaration is a CompoundDimensionDeclaration:
    *   the combined signature of the components and inverted components
    ****************************************************************************************8
   void getDimensionSignatureOf (DataTypeDeclaration dataTypeDeclaration) {
        getDimensionSignatureOf (dataTypeDeclaration, null);
   }
***********************************************/


   static int freeDimensionSequenceNumber;

   /**
    * get the dimension signature of the dataTypeDeclaration.
    * If its dimensionDeclaration==null: null
    * If its dimensionDeclaration is a FreeDimensionDeclaration:
    *                             "V"+(freeDimensionSequenceNumber++)+";"
    * If its dimensionDeclaration is a CompoundDimensionDeclaration:
    *   the combined signature of the components and inverted components
    */
   void getDimensionSignatureOf (DataTypeDeclaration dataTypeDeclaration) {

    if (dataTypeDeclaration.dimensionDeclaration==null) return;
    if (dataTypeDeclaration.dimensionDeclaration instanceof FreeDimensionDeclaration) {
        dataTypeDeclaration.dimensionSignature = "V"+(freeDimensionSequenceNumber++)+";";
        return;
    }
    CompoundDimensionDeclaration cd = (CompoundDimensionDeclaration) dataTypeDeclaration.dimensionDeclaration;
    dataTypeDeclaration.dimensionSignature = getDimensionSignatureOf (cd);
//System.out.println ("getDimensionSignatureOf: "+dataTypeDeclaration.dimensionSignature);
   }

   /**
    * answer the dimension signature of the CompoundDimensionDeclaration.
    * If its dimensionDeclaration==null: null
    * If its dimensionDeclaration is a FreeDimensionDeclaration:
    *                             "V"+(freeDimensionSequenceNumber++)+";"
    * If its dimensionDeclaration is a CompoundDimensionDeclaration:
    *   the combined signature of the components and inverted components
    */
   String getDimensionSignatureOf (CompoundDimensionDeclaration cd) {

    // the following code is largely copied from ScripticCompilerPass4...

    try {
      // check components, invertedComponents
      ArrayList<String> normalSignatures = new ArrayList<String>();
      ArrayList<String> invertSignatures = new ArrayList<String>();
      ArrayList<DimensionReference> allComponents = new ArrayList<DimensionReference>(cd.components);
      if (cd.invertedComponents != null) {
        for (int i=0; i<cd.invertedComponents.size(); i++) {
           allComponents.add (cd.invertedComponents.get(i));
        }
      }
      for (int i=0; i<allComponents.size(); i++) {
         DimensionReference dr = allComponents.get(i);

         String sig = getDimensionSignatureOf (dr);
         if (sig!=null) {
           if (i<cd.components.size())
                normalSignatures.add(sig);
           else invertSignatures.add(sig);
         }
      }
      return CompoundDimension.getSignature (normalSignatures, invertSignatures);
    } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException       e) {env.handleByteCodingException(e, typeDeclaration, cd);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), cd.sourceStartPosition, 
                                                                     cd.sourceEndPosition);
    }
    return Dimension.errorSignature;
   }

   
   void pushContext (Context context) {
       context.parent = this.context;
       this.context   = context;
       scriptContext  = context.isScriptContext()
                      ? (ScriptContext) context : null;
   }
   void popContext () {
       this.context   = context.parent;
       scriptContext  = context!=null && context.isScriptContext()
                      ? (ScriptContext)  context : null;
   }
   void pushScriptContext(BasicScriptDeclaration decl) {pushContext (new ScriptContext (env, this, decl));}
   void  popScriptContext() { popContext ();}

   public ScripticParseTreeEnumeratorWithContext (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
   }

   protected void processTopLevelTypeDeclaration (TopLevelTypeDeclaration t, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new ClassContext (env, this, t) );
      super.processTopLevelTypeDeclaration (t, retValue, arg1, arg2);
      popContext();
   }

   protected void processNestedTypeDeclaration     (NestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new NestedClassContext (env, this, t) );
      super.processNestedTypeDeclaration (t, level, retValue, arg1, arg2);
      popContext();
   }

   protected void processLocalTypeDeclaration     (RefinementDeclaration refinement, LocalTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new LocalClassContext (env, this, t) );
      super.processLocalTypeDeclaration (refinement, t, level, retValue, arg1, arg2);
      popContext();
   }
   protected void processMethodDeclaration (MethodDeclaration method, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new MethodContext (env, this, method));
      super.processMethodDeclaration (method, level, retValue, arg1, arg2);
      popContext();
   }
   protected void processConstructorDeclaration (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new MethodContext (env, this, constructorDeclaration));
      super.processConstructorDeclaration (constructorDeclaration, level, retValue, arg1, arg2);
      popContext ();
   }
   protected void processInitializerBlock      (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushContext (new InitializerBlockContext (env, this, initializerBlock));
      super.processInitializerBlock (initializerBlock, level, retValue, arg1, arg2);
      popContext ();
   }
   /*******************************************************************/
   /**                                                               **/
   /**                      SCRIPT DECLARATIONS                      **/
   /**                                                               **/
   /*******************************************************************/

   protected void processScriptDeclaration (ScriptDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushScriptContext(decl);
      super.processScriptDeclaration (decl, level, retValue, arg1, arg2);
      popScriptContext();
   }
   protected void processCommunicationDeclaration (CommunicationDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushScriptContext(decl);
      super.processCommunicationDeclaration (decl, level, retValue, arg1, arg2);
      popScriptContext();
   }
   protected void processChannelDeclaration (ChannelDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushScriptContext(decl);
      super.processChannelDeclaration (decl, level, retValue, arg1, arg2);
      popScriptContext();
   }

   /** resolve the given datatype declaration.
    *  Must succeed, else error ...
    */
   public DataType mustResolveDataTypeDeclaration (DataTypeDeclaration dataTypeDeclaration) {

       // search in the context chain upwards
       //   - at a LocalOrNestedTypeDeclaration:
       //        check that name, and of nested childs
       //   - at a LocalTypeDeclaration:
       //        check the localTypesContext

       if (dataTypeDeclaration.dataType!=null) return dataTypeDeclaration.dataType;

       String name = dataTypeDeclaration.nameComponents!=null
                   ? dataTypeDeclaration.nameComponents.get(0)
                   : dataTypeDeclaration.name;
       ClassType c = context.findNestedOrLocalClass (name);
       if (c != null) {
              // the start is there. Now the rest should also be there...
              if (dataTypeDeclaration.nameComponents != null) {
                 for (int i=1; i<dataTypeDeclaration.nameComponents.size(); i++) {
                     name = dataTypeDeclaration.nameComponents.get(i);
                     ClassType nestedClass = (ClassType) c.nestedClassesByName.get (name);
                     if (nestedClass==null) {
                        parserError (2, "Class or interface "+c.nameWithDots
                                   + " does not contain type "+name,
                                     dataTypeDeclaration.nameStartPosition,
                                     dataTypeDeclaration.  nameEndPosition);
                        return null;
                     }
                     c = nestedClass;
                 }
              }
              dataTypeDeclaration.dataType = c;
       }
       if (dataTypeDeclaration.dataType==null) {
          env.resolveDataTypeDeclaration (typeDeclaration.compilationUnit, dataTypeDeclaration);
       }
       if (dataTypeDeclaration.dataType==null) {
             parserError (2, "Data type cannot be resolved", dataTypeDeclaration.nameStartPosition,
                                                          dataTypeDeclaration.  nameEndPosition);
             dataTypeDeclaration.dataType = new UnresolvedClassOrInterfaceType();
       }
       else if (dataTypeDeclaration.dataType.baseType().isClassOrInterface()) {
           ClassType cc = (ClassType) dataTypeDeclaration.dataType.baseType();
           if (!cc.isPublic()
           &&  !cc.packageNameWithSlashes.equals(typeDeclaration.compilationUnit.getPackageNameWithSlashes())) {
                                                 //System.out.println("cc.isPublic(): "+cc.isPublic());
                cc.getSuperclass(env);            //System.out.println("cc.isPublic(): "+cc.isPublic());
                cc.getInterfaces(env);            //System.out.println("cc.isPublic(): "+cc.isPublic());
                cc.getMethodNamesSignatures(env); //System.out.println("cc.isPublic(): "+cc.isPublic()); 
                
                // extra test: javax.swing.JTable etc seem to go wrong...
//                if (!cc.isPublic()
//                && !cc.packageNameWithSlashes.startsWith("javax.swing") )
//                {
                     parserError (2, "Illegal attempt to access non-public class " + cc.nameWithDots
                                           + " in different package",
                                            dataTypeDeclaration.nameStartPosition,
                                            dataTypeDeclaration.  nameEndPosition);
//                }
           }
       }
       return dataTypeDeclaration.dataType;
   }
}
