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

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

////////////////////////////////////////////////////////////////////////////
//
//                             OldParameter
//
////////////////////////////////////////////////////////////////////////////

class OldParameter extends NormalOrOldScriptParameter {

   public boolean isOldParameter() {return true;}

/***
            // A little "Cheat"... look at the code in BasicScriptDeclaration
            BasicScriptDeclaration owner 
                  = (BasicScriptDeclaration) p.owner.source;
            int partnerIndex = owner.getPartnerIndex ();

            // Special treatment for BIDIRECTIONAL CHANNELS:
            // Must get "old" parameter from paramData(1)
            // instead of paramData(0)
            if (   owner.isChannelDeclaration ()
                && ((ChannelDeclaration)owner).isBidirectionalChannel) {
               partnerIndex = 1;
            }
**/

   // needed in p!!, which translates to:
   // !_n_.isForced(p.slot) || p.parameterHolder.compare (old.p.wrapper);
   Instruction[] loadReferenceInstructions (FieldDeclaration fieldDeclaration, CompilerEnvironment env,
                                            ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {

       //_n_.oldParamData(partnerIndex)[slot] >>> Object|Integer|Character|...

       int partnerIndex = partnerIndex (fieldDeclaration);
       Instruction loadPartnerIndexInstruction = null;
       if (partnerIndex >= 0) {
           if (fieldDeclaration.isChannelDeclaration ()
           && ((ChannelDeclaration)fieldDeclaration).isBidirectionalChannel) {
               partnerIndex = 1;
           }
           loadPartnerIndexInstruction = Instruction.loadIntegerInstruction (constantPoolOwner, partnerIndex, instructionOwner);
       }
       Method paramMethod = env.mustResolveMethod (env.scripticVmNodeType,
                                                   "oldParamData",
                                                   "("+(partnerIndex>=0?"I":"")+")[Ljava/lang/Object;");
       int i=0;
       Instruction [] result = new Instruction [5+(loadPartnerIndexInstruction==null? 0: 1)];
       result[i++] = new Instruction (INSTRUCTION_aload, getSlotNumberForNodeParameter(), instructionOwner);
       if (loadPartnerIndexInstruction != null) {
         result[i++] = loadPartnerIndexInstruction;
       }
       result[i++] = paramMethod.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                                           constantPoolOwner, instructionOwner);
       result[i++] = Instruction.loadIntegerInstruction (constantPoolOwner, slot, instructionOwner);
       result[i++] = new Instruction (INSTRUCTION_aaload, instructionOwner);

       if (getDataType(env).wrapperClass(env)==env.javaLangObjectType) {
           result[i++] = new Instruction (INSTRUCTION_checkcast,
                                          constantPoolOwner.resolveClass ((ClassType) getDataType(env)),
                                          instructionOwner);
       }
       else {
           result[i++] = new Instruction (INSTRUCTION_checkcast,
                                       constantPoolOwner.resolveClass (getDataType(env).wrapperClass(env)),
                                       instructionOwner);
       }
       return result;
   }

   Instruction []     loadInstructions  (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
       // the wrapper object, if applicable, has already been loaded
       // fetch the 'value' field, if applicable
       if (getDataType(env).wrapperClass(env)!=env.javaLangObjectType) {
          Instruction [] result = new Instruction [1];
          Method f = env.mustResolveMethod (getDataType(env).wrapperClass(env),
                                            getDataType(env).accessNameForWrapperClass(),
                                            "()"+getDataType(env).getSignature());
          result[0] = f.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                           constantPoolOwner, instructionOwner);
          return result;
       }
       else {
         return new Instruction[0];
       }
   }
}

