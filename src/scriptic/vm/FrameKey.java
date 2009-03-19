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

public class FrameKey {
  private Object owner;
  private NodeTemplate template;
  private ValueHolder[] indexValues;
  
  private int hash;
  
  FrameKey(Object owner, NodeTemplate template, ValueHolder[] indexValues)
  { 
	  this.owner = owner;
	  this.template = template;
	  this.indexValues = indexValues;
	  hash = computeHash(owner, template, indexValues);
  }
  public int hashCode() {return hash;}
  private static int computeHash(Object owner, NodeTemplate template, ValueHolder[] indexValues) {
     int hash1 = owner==null? 0: owner.hashCode();
     int hash2 = template.hashCode();
	 int result = hash1^hash2;
	 if (indexValues!=null)
	 {
		 for (ValueHolder vh: indexValues) {
			 result ^= vh.hashCode();
		 }
	 }
	 return result;
  }
  public boolean equals (Object other)
  {
	  if (!(other instanceof FrameKey))
	  {
		  return false;
	  }
	  FrameKey fk = (FrameKey) other;
	  
	  if (owner==null)
	  { 
		  if (fk.owner!=null)
		  {
			  return false;
		  }
	  }
	  else
	  {
		  if (!owner.equals(fk.owner))
		  {
			  return false;
		  }
	  }
	  if (!template.equals(fk.template))
	  {
		  return false;
	  }
	  if (indexValues!=fk.indexValues)
	  {
		  if (indexValues==null)
		  {
			  return false;
		  }
		  if (fk.indexValues==null)
		  {
			  return false;
		  }
		  if (indexValues.length!=fk.indexValues.length)
		  {
			  return false;
		  }
		  for (int i=0; i<indexValues.length; i++)
		  {
			  if (!indexValues[i].equals(fk.indexValues[i]))
			  {
				  return false;
			  }
		  }
	  }
	  return true;
  }
}
