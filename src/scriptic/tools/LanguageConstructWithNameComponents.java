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

class LanguageConstructWithNameComponents extends LanguageConstruct {

   public ArrayList<String>  nameComponents; // e.g. in "a.b.c"

   public LanguageConstructWithNameComponents () {super();}
   public LanguageConstructWithNameComponents (LanguageConstruct anotherConstruct) {
      super (anotherConstruct);
      if (name.indexOf ('.') >= 0) {
          setNameWithComponents (name);
      }
   }

   final void addNameComponent (String nameComponent) {
       if (nameComponents == null)
           nameComponents = new ArrayList<String>();
       nameComponents.add(nameComponent);
   }
   // eat away the last name component, and answer it.
   // there should be at least 1 name component
   final String popLastNameComponent() {
	   String result = nameComponents.remove(nameComponents.size()-1);
	  int index = name.lastIndexOf(".");
	  if (index<0)
	  {
		  name = "";
	  }
	  else
	  {
		  name = name.substring(0, index);
	  }
	  return result;
   }

   /**
    * set name from class file, e.g., "java/lang/Object"
    */
   void setNameWithComponents(String s) {
       name = s.replace('/','.');
       int i = -1, prev = -1;
       do {
          i = name.indexOf ('.', i+1);
          String comp;
          if (i<0) comp = s.substring (prev+1  );
          else     comp = s.substring (prev+1,i);
          //System.out.println("setNameWithComponentsmClassFile: "+s+" - "+comp);
          addNameComponent (comp);
          prev = i;
       } while (i>=0);
   }
}

