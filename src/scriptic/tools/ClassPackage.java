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

import java.util.HashMap;

class ClassPackage extends LanguageConstruct {
   public HashMap<String, ClassType> classFiles = new HashMap<String, ClassType>();
   public boolean containsAmbiguousClass = false;

   ClassPackage (String name) {
      this.name = name;
   }

   public String getName () {
      String realName = super.getName ();
      if (realName.length() == 0)
           return "<default>";
      else return realName;
   }
}
