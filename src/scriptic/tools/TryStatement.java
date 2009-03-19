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
import scriptic.tools.lowlevel.LabelInstruction;

class TryStatement extends ConfiningJavaStatement {
   public TryBlock tryBlock       = new TryBlock();
   public ArrayList<JavaStatement>  catches         = new ArrayList<JavaStatement>();
   public ArrayList<JavaStatement>  finalStatements = new ArrayList<JavaStatement>();
// this is a bit tricky:
// the try part is a TryPartBlock,
// the finally statements could also be put in a FinallyStatementBlock
// this way inner breaks etc. can search the statement stack
// to see whether they are inside try or catch parts
// which is be needed for code generation that handles the finally part
// Also, confined variables need to know the end of their range

   public LabelInstruction             tryEndLabel; // to be created in scripticCompilerPass7?
   public LabelInstruction       finallyStartLabel; //
   public LabelInstruction  targetAfterTryAndCatch; //endLabel if there's no finally;
                                                    //else the jsr before the default catch handler

   public int languageConstructCode () {return TryStatementCode;}

   public    boolean canCompleteNormally() {
      //iff    the  try  block can complete normally
      // || any catch block can complete normally. 
      // && if the try statement has a finally block,
      //    then the finally block can complete normally.

      // so the finally part must complete normally, if present:
      if (!super.canCompleteNormally()) return false; 

      if (tryBlock.canCompleteNormally()) return true;
      for(int i=0; i<catches.size(); i++) {
         CatchBlock c = (CatchBlock) catches.get(i);
         if (c.canCompleteNormally()) return true;
      }
      return false;
   }
}

