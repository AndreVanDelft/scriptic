/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.vm;
import java.util.ArrayList;

interface NodeTemplateInterface extends TemplateCodes {

	/** whether it is clear that this template is part of an iteration */
	boolean isIteration();

	/** whether it is a code fragment with duration */
	boolean hasDuration();

	/** code index associated with this (template) */
	int codeIndex();

	/** name of package */
	String packageName();

	/** name of class */
	String className();

        /** signature */
        String signature();

        /** access modifiers*/
        int modifierFlags();

	/** name of script (partners)
         *  In case of a normal script: elementAt(0) is the name
         *  in case of a script call or send/receive over a channel:
         *  elementAt(0) is the name of the called script/channel
         */
        ArrayList<String> partnerNames();

	/** parent template */
	NodeTemplateInterface parent();

	/** child templates */
	NodeTemplateInterface[] childs();

	/** number of formal or actual parameters */
	short nrParameters();

	/** operator/operand type (see JavaTokens.java and ScripticTokens.java) */
        int typeCode();

        /** flags for code: 0x000 often (no code)
         *                  0x001 if there is a code fragment
         *                  0x011 if there is an extra fragment
         *                  0x111 3 bits possible for "for(..;..;..)"
         */
        int codeBits();

        /** start line */
        int startLine();

        /** end line */
        int endLine();

        /** start position */
        int startPos();

        /** end position */
        int endPos();

        /** extraPositionDescriptor: an array of the form
         *  {l, p, len} or {l1,p1,..., ...}
         *   l, p, len    - line+position+length of central name
         *   l1,p1, ...   - line+position of n-ary operators (lenght==1 or 2)
         */
        int[] extraPositionDescriptor();

	/** formal indexes of ?! parameters
         * if any, store this in array indexed by actual index
         * and with as values: the indexes of corresponding formal parameters.
         */
	short actualOutForcingParameterIndexes[] = null; // actualIndex >> formalIndex

	/** templates of partners, if any */
	NodeTemplateInterface[] getRelatedTemplates();

	boolean isActualParameterOut      (int i);
	boolean isActualParameterForcing  (int i);
	boolean isActualParameterAdapting (int i);

	String  getName();

        String scripticParseTreeCodeRepresentation (int i);

}
