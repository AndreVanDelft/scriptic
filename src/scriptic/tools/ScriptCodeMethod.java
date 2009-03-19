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

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.LabelInstruction;
import scriptic.tools.lowlevel.TableswitchInstruction;

////////////////////////////////////////////////////////////////////////////
//
//                             ScriptCodeMethod
//
////////////////////////////////////////////////////////////////////////////

class ScriptCodeMethod extends Method {
    // "public void code (Node node, int index)"
    BasicScriptDeclaration scriptDeclaration;
    Parameter  nodeParameter;
    Parameter indexParameter;
    ArrayList<LabelInstruction> switchTargetLabels = new ArrayList<LabelInstruction>();
    LabelInstruction endLabel;
    Instruction            loadIndexInstruction;
    TableswitchInstruction    switchInstruction;

    ScriptCodeMethod (CompilerEnvironment env, BasicScriptDeclaration scriptDeclaration) {
      this.scriptDeclaration   = scriptDeclaration;
      owner                    = scriptDeclaration.typeDeclaration.target;
      name                     = scriptDeclaration.getCodeMethodName();
      signature                = "(Lscriptic/vm/Node;I)V";
      modifierFlags           |= PublicFlag;
      nextVariableSlot = 1;
      int nextParameterIndex = 0;
      parameters             = new Parameter[2];
      if (scriptDeclaration.modifiers.isStatic()) {
          modifierFlags       |= StaticFlag;
          nextVariableSlot = 0; 
          // hard to get that working in methods such as loadReferenceInstructions?
          // then insert dummy parameter instead:
          //parameters             = new Parameter[3];
          //parameters[0] = new Parameter();
          //parameters[0].name = "__dummy";
          //parameters[0].dataType1  = env.javaLangObjectType;
          //signature                = "(Ljava/lang/Object;Lscriptic/vm/Node;I)V";
          //nextParameterIndex++;
      }
      
      parameters[nextParameterIndex++] = nodeParameter = new Parameter();
      nodeParameter.name               = "__node";
      nodeParameter.dataType1          = env.scripticVmNodeType;
      parameters[nextParameterIndex++] = indexParameter = new Parameter();
      indexParameter.name              = "__index";
      indexParameter.dataType1         = IntType.theOne;
      
      nodeParameter.slot               = nextVariableSlot++;
      indexParameter.slot              = nextVariableSlot++;
      setSynthetic();
    }
}
