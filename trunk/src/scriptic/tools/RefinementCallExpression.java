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
import java.io.IOException;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        JavaParseTreeCodes                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

public interface RefinementCallExpression extends scriptic.tools.lowlevel.InstructionOwner {

    static final int      superMode = 1;
    static final int    virtualMode = 2;
    static final int specialMode = 3;
    static final int  interfaceMode = 4;
    static final int     staticMode = 5;

   public boolean     isStaticMode();
   public boolean isSpecialMode();
   public boolean      isSuperMode();
   public boolean    isVirtualMode();
   public boolean  isInterfaceMode();

   public void        setSuperMode();
   public void      setVirtualMode();
   public void   setSpecialMode();
   public void    setInterfaceMode();
   public void       setStaticMode();

   public MethodCallParameterList   parameterList    ();
   public ArrayList<JavaExpression> getAllParameters (CompilerEnvironment env);
   public JavaExpressionWithTarget  accessExpression ();
   public Method                    target();
   public void                      setTarget(CompilerEnvironment env, TypeDeclaration typeDeclaration, Method t)
                                               throws IOException, CompilerError;
   public void                      setDimensionSignature(String s);
   public String                    getName();
   public int                       sourceStartPosition();
   public int                       sourceEndPosition  ();
   public boolean                   isScriptChannelSend   ();
   public boolean                   isScriptChannelReceive();
}

