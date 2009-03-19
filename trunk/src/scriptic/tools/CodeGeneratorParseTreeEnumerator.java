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




   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Code Generating Parse Tree Enumerator              */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class CodeGeneratorParseTreeEnumerator 
         extends ScripticParseTreeEnumerator
         implements scriptic.tokens.ScripticTokens, ModifierFlags {
   PreprocessorOutputStream  outputStream;

   /*-------------------------- Constructors -------------------------*/

   public CodeGeneratorParseTreeEnumerator (Scanner                  scanner,
                                            PreprocessorOutputStream outputStream,
                                            CompilerEnvironment env) {
      super (scanner, env);
      this.outputStream = outputStream;
   }


   /*------------------- Basic Code Generation routines -----------------*/

   protected void outToken (int token) {
      outputStream.print (Scanner.tokenRepresentation (token));
   }

   protected void outQuotedString (String s) {
      outputStream.print ("\""+s+"\"");
   }

   protected void outString (String s) {
      outputStream.print (s);
   }

   protected void outInteger (int i) {
      outputStream.print (i);
   }

   protected void outLong (long l) {
      outputStream.print (l);
   }

   protected void outSpace () {
      outputStream.print (' ');
   }

   protected void outSpaces (int length) {
      outputStream.writeSpaces (length);
   }

   protected void outLine (String s) {
      outputStream.println (s);
   }

   protected void outLine () {
      outputStream.println ();
   }


   public void indent () {
      outputStream.indent ();
   }

   public void indent (int amount) {
      outputStream.indent (amount);
   }

   public void setIndent () {
      outputStream.setIndent ();
   }

   public void setIndent (int level) {
      outputStream.setIndent (level);
   }

   public void unindent () {
      outputStream.unindent ();
   }

   public void startMultilineStringLiteral () {
      outputStream.startMultilineStringLiteral ();
   }

   public void stopMultilineStringLiteral () {
      outputStream.stopMultilineStringLiteral ();
   }


   /*---------------------- Useful utility routines ---------------------*/

   protected void outScriptHeaderComment (BasicScriptDeclaration refinement) {
      outScriptHeaderComment (refinement, new String ());
   }

   protected void outScriptHeaderComment (BasicScriptDeclaration refinement,
                                          String extraMessage) {
      outString ("/* ");

      refinement.outModifiers (outputStream);
      outString (refinement.getPresentation ());
    //outSpace  ();
    //outString (refinement.getHeaderSource ());
      outString (extraMessage);

      outString (" */");
      outLine   ();
   }

   protected void outHeaderSource (LanguageConstruct obj) {
      obj.setTargetStartPosition (outputStream.position);
      outString (obj.getHeaderSource(scanner));
      obj.setTargetEndPosition (outputStream.position);
   }

   protected void outSource (LanguageConstruct obj) {
      obj.setTargetStartPosition (outputStream.position);
      outString (obj.getSource(scanner));
      obj.setTargetEndPosition (outputStream.position);
   }

   protected void outStatement (LanguageConstruct obj) {
      outSource (obj);
      outToken  (SemicolonToken);
      outLine   ();
   }
}

