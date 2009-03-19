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


public interface TemplateCodes {

   public static final int       CodeFlag = 0x0001; // whether there is code
   public static final int   InitCodeFlag = 0x0002; // whether there is initialization code
   public static final int   NextCodeFlag = 0x0004; // whether there is 'next' code in 'for'
   public static final int   DurationFlag = 0x0008; // whether there is a duration
   public static final int SecondTermFlag = 0x0010; // whether there is an 'else' for 'if'
   public static final int       TrueFlag = 0x0020; // whether the expression is constantly  true
   public static final int      FalseFlag = 0x0040; // whether the expression is constantly false
   public static final int  IterationFlag = 0x0080; // whether the template is an iteration
   public static final int     AnchorFlag = 0x0100; // whether an anchor had been specified
   public static final int      AsyncFlag = 0x0200; // whether the specified anchor is an AsyncCodeInvoker
   public static final int SparseActivationFlag = 0x0400; // whether the activation of a communication must be sparse
}


