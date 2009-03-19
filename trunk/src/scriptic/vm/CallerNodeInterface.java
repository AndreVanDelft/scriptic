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

/** interface for a node calling a script */
public interface CallerNodeInterface {

	/**
	 * Start a script, called from another script, in a given frame and without parameters
	 */
	void startScript (Object owner, NodeTemplate t);

	/**
	 * Start a script, called from another script, in a given frame and with given parameters 
	 */
	void startScript (Object owner, NodeTemplate t, Object params[]);

	/**
	 * Answer wether the actual parameter at the given 
	 * index is of forcing type ('!' or: '?!' and ... see language definition)
	 */
	boolean isActualParameterForcing(int index);
	/**
	 * Answer wether the actual parameter at the given 
	 * index is of out type ('?' or '?!' and ... see language definition)
	 */
	boolean isActualParameterOut(int index);
}

