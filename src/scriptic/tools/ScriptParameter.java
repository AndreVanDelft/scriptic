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

import scriptic.tools.lowlevel.FieldInstruction;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

class ScriptParameter extends NormalOrOldScriptParameter {
	public boolean isScriptParameter() {
		return true;
	}

	OldParameter oldVersion;

	// Special treatment for BIDIRECTIONAL CHANNELS:
	// Must get "old" parameter from paramData(1)
	// instead of paramData(0)

	public ClassType ownerClass(CompilerEnvironment env) throws ByteCodingException, IOException {
		return getDataType(env).holderClass(env);
	}

	Instruction dupReferenceInstruction(InstructionOwner instructionOwner)
			throws ByteCodingException {
		return new Instruction(INSTRUCTION_dup, instructionOwner);
	}

	String nameForFieldRef() {
		return "value";
	}

	DataType dataTypeForFieldRef(CompilerEnvironment env) throws ByteCodingException, IOException {
		DataType result = getDataType(env);
		if (!result.isPrimitive())
			result = env.javaLangObjectType;
		return result;
	}

	Instruction[] storeInstructions(CompilerEnvironment env,
			ClassType constantPoolOwner, InstructionOwner instructionOwner)
			throws ByteCodingException, IOException {
		Instruction[] result = new Instruction[1];
		result[0] = new FieldInstruction(INSTRUCTION_putfield, fieldRef(env,
				constantPoolOwner), getDataType(env).isBig(), instructionOwner);
		return result;
	}

	Instruction[] loadInstructions(CompilerEnvironment env,
			ClassType constantPoolOwner, InstructionOwner instructionOwner)
			throws ByteCodingException, IOException {
		Instruction checkCastInstruction = null;
		DataType dataType = getDataType(env);
		if (!dataType.isPrimitive() && dataType != env.javaLangObjectType) {
			checkCastInstruction = new Instruction(INSTRUCTION_checkcast,
					constantPoolOwner.resolveClass(dataType), instructionOwner);
		}
		Instruction[] result = new Instruction[checkCastInstruction == null ? 1
				: 2];
		result[0] = new FieldInstruction(INSTRUCTION_getfield, fieldRef(env,
				constantPoolOwner), dataType.isBig(), instructionOwner);
		if (checkCastInstruction != null) {
			result[1] = checkCastInstruction;
		}
		return result;
	}

	Instruction[] loadReferenceInstructions(FieldDeclaration fieldDeclaration,
			CompilerEnvironment env, ClassType constantPoolOwner,
			InstructionOwner instructionOwner) throws ByteCodingException, IOException {

		// _n_.paramData(partnerIndex)[slot]

		int partnerIndex = partnerIndex(fieldDeclaration);
		Instruction loadPartnerIndexInstruction = null;
		if (partnerIndex >= 0)
			loadPartnerIndexInstruction = Instruction.loadIntegerInstruction(
					constantPoolOwner, partnerIndex, instructionOwner);
		Method paramMethod = env.mustResolveMethod(env.scripticVmNodeType,
				"paramData", "(" + (partnerIndex >= 0 ? "I" : "")
						+ ")[Lscriptic/vm/ValueHolder;");
		int i = 0;
		Instruction[] result = new Instruction[5 + (loadPartnerIndexInstruction == null ? 0
				: 1)];
		result[i++] = new Instruction(INSTRUCTION_aload,
				getSlotNumberForNodeParameter(), instructionOwner);
		if (loadPartnerIndexInstruction != null) {
			result[i++] = loadPartnerIndexInstruction;
		}
		result[i++] = paramMethod.invokeInstruction(env,
				INSTRUCTION_invokevirtual, constantPoolOwner, instructionOwner);
		result[i++] = Instruction.loadIntegerInstruction(constantPoolOwner,
				slot, instructionOwner);
		result[i++] = new Instruction(INSTRUCTION_aaload, instructionOwner);
		result[i++] = new Instruction(INSTRUCTION_checkcast, constantPoolOwner
				.resolveClass(getDataType(env).holderClass(env)),
				instructionOwner);

		return result;
	}
}
