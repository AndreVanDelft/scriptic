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
/** wrapper for int values */
public final class IntHolder  extends ValueHolder {
    public IntHolder(){}
    public IntHolder(int v){value=v;}
    public int value;
    /** 
     * set the value to the other's value; 
     * the other ValueHolder must have the same type 
     */
    public void setValue (ValueHolder other) {value=((IntHolder)other).value;}

    /**
     * answer whether the other object equals the wrapped value
     */
    public boolean hasEqualValue (Object other) {
    	return other instanceof Integer
        && value==((Integer)other).intValue();
    }
    /**
     * answer whether the other object equals this
     */
    public boolean equals (Object other) {
       if (this==other)
       {
    	   return true;
       }
       if (!(other instanceof IntHolder))
       {
    	   return false;
       }
       IntHolder otherIntHolder = (IntHolder) other;
       return value==otherIntHolder.value;
    }    
    public int hashCode() {return value;}
}

