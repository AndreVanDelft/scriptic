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

/** wrapper for boolean values */
public final class BooleanHolder extends ValueHolder {
    public BooleanHolder(){}
    public BooleanHolder(boolean b){value=b;}
    public boolean value;
    /** 
     * set the value to the other's value; 
     * the other ValueHolder must have the same type 
     */
    public void setValue (ValueHolder other) {value=((BooleanHolder)other).value;}
    /**
     * answer whether the other object equals the wrapped value
     */
    public boolean hasEqualValue (Object other) {
    	return other instanceof Boolean
        && value==((Boolean)other).booleanValue();
    }
    /**
     * answer whether the other object equals this
     */
    public boolean equals (Object other) {
       if (this==other)
       {
    	   return true;
       }
       if (!(other instanceof BooleanHolder))
       {
    	   return false;
       }
       BooleanHolder otherBooleanHolder = (BooleanHolder) other;
       return value==otherBooleanHolder.value;
    }
    public int hashCode() {return value? 1: 0;}
}

