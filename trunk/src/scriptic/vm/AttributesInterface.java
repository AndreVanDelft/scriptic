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
 * Interface for code fragments etc having attributes such as pass and priority
 */
public interface AttributesInterface {
	
	/** answer the priority */
	public int     getPriority ();
	/** answer the pass */
	public int     getPass     ();
	/** answer the success attribute */
	public Boolean getSuccess  ();
	/** set the success attribute */
	public void setSuccess  (Boolean v);
	/** set the success attribute to TRUE if v, else to null; answer whether it was TRUE*/
	public boolean setSuccessTrueOrNull(boolean v);
	/** answer the original duration */
	public double  oldDuration ();
	/** answer the duration that is still to go */
	public double  getDuration ();
	/** set the duration */
	public void    setDuration (double d);
	/** answer the anchor object for a native code fragment */
	public Object  getAnchor();
	/** set the anchor object for a native code fragment */
	public void setAnchor (Object obj);

    /** the lowest possible priority value */
    public final static    int MIN_PRIORITY = java.lang.Integer.MIN_VALUE;
    /** the highest possible priority value */
    public final static    int MAX_PRIORITY = java.lang.Integer.MAX_VALUE;
    /** pseudo value denoting no duration set at all */
    public final static double  NO_DURATION = java.lang.Double .MAX_VALUE;
}
