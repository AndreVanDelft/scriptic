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

class MethodCallParameterList extends LanguageConstruct {
   public ArrayList<JavaExpression> parameterExpressions = new ArrayList<JavaExpression>();
   public boolean hasError () {
      for (int i=0; i<parameterExpressions.size(); i++) {
         JavaExpression parameter
            = parameterExpressions.get(i);
         if (parameter.dataType==null
         || !parameter.dataType.isResolved()) {
            return true;
         }
      }
      return false;
   }
   public String getDescription() {
      StringBuffer result = new StringBuffer();
      result.append(super.getDescription());
      result.append("parameters: ").append(parameterExpressions.size());
      if (hasError()) result.append(" ERROR");
      return result.toString();
   }
}

