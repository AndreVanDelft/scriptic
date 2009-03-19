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

/** a DebuggerInterface registers itself to FromJava using registerDebugger.
 *  A registration parameter denotes what callback functions will be called.
 *  These are available for the following specific events:
 *  the main loop: the main loop of a main script process starts a new pass 
 *  activate:      a new node is   activated in the run-time tree
 *  deactivate:    a     node is deactivated in the run-time tree
 *  has success:   a 'sure' code fragment is about to be executed,
 *                 or an unsure code fragment has just been executed successfully
 *  execute code:  some code is going to be executed; either
 *                 - a code fragment (sure, unsure, threaded, tiny, event handling)
 *                 - activation or deactivation code
 *                 - a test in an 'if', 'while', 'for'
 *                 - an 'init' or 'next' section in 'for'
 *  succeed        a node reports a success to its parent
 */
public interface DebuggerInterface {
    public void callbackMainloop   (NodeInterface node);
    public void callbackActivate   (NodeInterface node);
    public void callbackDeactivate (NodeInterface node);
    public void callbackHasSuccess (NodeInterface node);
    public void callbackExecuteCode(NodeInterface node);
    public void callbackSucceed    (NodeInterface node);
}
