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


////////////////////////////////////////////////////////////////////////////
//
//                             NormalOrOldScriptParameter
//
////////////////////////////////////////////////////////////////////////////

class NormalOrOldScriptParameter extends Parameter {
    // return the relative partner index relative to f
    // e.g., in a,b,c(int i) = {...}
    // f is the shared script, and 'i'.partnerIndex(f)==2
    int partnerIndex (FieldDeclaration f) {
       if (f instanceof ChannelDeclaration) {
    	   return 0;
       }
       BasicScriptDeclaration ownerScript = (BasicScriptDeclaration) source.owner;
       ArrayList<CommunicationPartnerDeclaration> partners = ((BasicScriptDeclaration)f).getPartners();
       if (partners==null) return -1;
       for (int i=0; i<partners.size(); i++) {
          if (partners.get(i)==ownerScript) {
              return i;
          }
       }
System.out.println("NormalOrOldScriptParameter: could not find appropriate partner");
       return -2;
    }
    int getSlotNumberForNodeParameter() {
        BasicScriptDeclaration ownerScript = (BasicScriptDeclaration) source.owner;
    	return ownerScript.isStatic()? 0: 1;
    }
}

