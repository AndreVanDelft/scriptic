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

package scriptic.tools.lowlevel;

public interface ClassEnvironment {

  /**
   * resolve ConstantPoolItems; these are "uniquefied".
   * Ie, if you add a ConstantPoolItem whose
   * contents already exist in the ConstantPool, only one entry is finally
   * written out when the ConstantPoolItem is written.
   */

  ConstantPoolClass                    resolveClass                   (String s);
  ConstantPoolFieldReference           resolveFieldReference          (String clazz, String name, String sig);
  ConstantPoolMethodReference          resolveMethodReference         (String clazz, String name, String sig);
  ConstantPoolInterfaceMethodReference resolveInterfaceMethodReference(String clazz, String name, String sig);
  ConstantPoolNameAndType              resolveNameAndType             (              String name, String sig);
  ConstantPoolString                   resolveString                  (String name);
  ConstantPoolInteger                  resolveInteger                 (int    value);
  ConstantPoolFloat                    resolveFloat                   (float  value);
  ConstantPoolLong                     resolveLong                    (long   value);
  ConstantPoolDouble                   resolveDouble                  (double value);
  ConstantPoolUnicode                  resolveUnicode                 (String value);
  ConstantPoolItem                     getConstantPoolItem            (int        i) throws ByteCodingException, java.io.IOException;
  int                                  line                           (int position);
  int                                  positionOnLine                 (int line, int position);
}

