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

/**
 * This exception is thrown when an internal scriptic rts error occurs
 */

class ScripticError extends Error {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -4939654060366004608L;
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	Throwable e;

    /**
     * Constructor
     */
    public ScripticError(String msg) {
	super(msg);
	this.e = this;
    }

    /**
     * Create an exception given another exception.
     */
    public ScripticError(Exception e) {
	super(e.getMessage());
	this.e = e;
    }
}
