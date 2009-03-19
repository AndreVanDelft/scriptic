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
import java.io.*;

import scriptic.tools.lowlevel.ByteCodingException;

abstract class Context implements scriptic.tokens.JavaTokens {
  Context (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, TypeDeclaration typeDeclaration) {
      this.env                = env;
      this.typeDeclaration    = typeDeclaration;
      this.parserErrorHandler = parserErrorHandler;
  }
  public Context             parent;
  public CompilerEnvironment env;
  public ParserErrorHandler  parserErrorHandler;
  public  TypeDeclaration    typeDeclaration;
  public   ClassType         classType()      {return typeDeclaration.target;}
  public boolean isInitializerBlockContext () {return false;}
  public boolean isFieldInitializerContext () {return false;}
  public boolean isClassContext            () {return false;}
  public boolean isRefinementContext       () {return false;}
  public boolean isMethodContext           () {return false;}
  public boolean isScriptContext           () {return false;}
  public ClassType findNestedOrLocalClass (String name) {
    if (parent != null) return parent.findNestedOrLocalClass (name);
    return null;
  }
  public abstract boolean isStatic         ();
                  Variable resolveLocalName (NameExpression e, boolean fromLocalOrNestedClass) throws IOException, CompilerError {return null;}

  public final Variable resolveVariable (NameExpression e) throws IOException, ByteCodingException {
      return resolveVariable (e, this, false);
  }

  /* fromContext - the next lower level context from which this resolution attempt comes from
   * fromLocalOrNestedClass - whether there is a local or nested class context
   *                          between this and the original context
   */
  Variable resolveVariable (NameExpression e, Context fromContext, boolean fromLocalOrNestedClass)
          throws IOException, ByteCodingException { // Not final, because of FieldInitializerContext

      Variable result = resolveLocalName (e, fromLocalOrNestedClass);

      if (result == null)
      {
    	  if (parent != null) 
    	  {
    		  result = parent.resolveVariable  (e, this, fromLocalOrNestedClass);
    	  }
    	  else
    	  {
    		  //try static imports
    		  result = resolveStaticImportVariable(e); 
    	  }
      }
      else if (result.isMemberVariable()) {
         if (fromContext.isStatic()
         &&  !result    .isStatic()) {
                 parserError (2, "Attempt to use instance field "+result.name
                                                +" from static context",
                                                e.sourceStartPosition,
                                                e.sourceEndPosition);
         }
      }
      return result;
  }
  MemberVariable resolveStaticImportVariable (NameExpression e) throws IOException, CompilerError {
	  CompilationUnit compilationUnit = typeDeclaration.compilationUnit;
	  ImportStatement importstatement = compilationUnit.importedStaticMembers.get(e.name);
	  if (importstatement!=null)
	  {
		  ClassType classType = env.resolveClass(importstatement.packagePart.name, importstatement.classNameForStaticImport, true/*loadAsWell*/);
		  if (classType!=null)
		  {
			  MemberVariable mv = classType.resolveMemberVariable(env, e);
			  if (mv!=null && mv.isStatic())
			  {
				  return mv;
			  }
		  }
	  }
	  for (ImportStatement importstatementOD: compilationUnit.importStatements)
	  {
		  if (!importstatementOD.importStatic
	      ||  !importstatementOD.importOnDemand)
		  {
			  continue;
		  }
		  ClassType classType = env.resolveClass(importstatementOD.packagePart.name, importstatementOD.classNameForStaticImport, true/*loadAsWell*/);
		  if (classType!=null)
		  {
			  MemberVariable mv = classType.resolveMemberVariable(env, e);
			  if (mv!=null && mv.isStatic())
			  {
				  return mv;
			  }
		  }
	  }
	  return null;
  }
  
  public HashMap<String, Object> findApplicableStaticallyImportedMethods (CompilerEnvironment env,
		  																  ClassType callerClass,
		  																  RefinementCallExpression mcall)
		  																  throws ByteCodingException, IOException 
  {
	  CompilationUnit compilationUnit = typeDeclaration.compilationUnit;
	  ImportStatement importstatement = compilationUnit.importedStaticMembers.get(mcall.getName());
	  HashMap<String, Object> result = new HashMap<String, Object>();
	  if (importstatement!=null)
	  {
		  ClassType classType = env.resolveClass(importstatement.packagePart.name, importstatement.classNameForStaticImport, true/*loadAsWell*/);
		  if (classType!=null)
		  {
			  HashMap<String, Object> asims = classType.findApplicableMethods (env, callerClass, mcall);
			  for (String s: asims.keySet())
			  {
				  Method m = (Method) asims.get(s);
				  if (m.isStatic())
				  {
					  result.put(s,m);
				  }
			  }
			  if (!result.isEmpty())
			  {
				  return result;
			  }
		  }
	  }
	  for (ImportStatement importstatementOD: compilationUnit.importStatements)
	  {
		  if (!importstatementOD.importStatic
	      ||  !importstatementOD.importOnDemand)
		  {
			  continue;
		  }
		  ClassType classType = env.resolveClass(importstatementOD.packagePart.name, importstatementOD.classNameForStaticImport, true/*loadAsWell*/);
		  if (classType!=null)
		  {
			  HashMap<String, Object> asims = classType.findApplicableMethods (env, callerClass, mcall);
			  for (String s: asims.keySet())
			  {
				  Method m = (Method) asims.get(s);
				  if (m.isStatic())
				  {
					  result.put(s,m);
				  }
			  }
		  }
	  }
	  return result;
  }

  protected LocalVariableOrParameter getLocalName (String name) {return null;}
  protected void pushLocalName        (LocalVariableOrParameter declaration) {}
  protected void pushLocalDeclaration (LocalVariableOrParameter declaration) {}
  protected void  popLocalDeclaration (LocalVariableOrParameter declaration, LanguageConstruct endOfScope) {}
  protected void  popLocalDeclarations(ArrayList<LocalVariableOrParameter> declarations, LanguageConstruct endOfScope) {}

   /* ------------------------------------------------------------------ */

   protected boolean checkReservedIdentifier  (LanguageConstruct construct) {
      return false;
   }
   protected void parserError (int severity, String message, int startPosition, int endPosition) {
      parserErrorHandler.parserError (severity, message, 
                   new ScannerPosition (typeDeclaration.compilationUnit.scanner,
                                        startPosition, endPosition));
   }
}


