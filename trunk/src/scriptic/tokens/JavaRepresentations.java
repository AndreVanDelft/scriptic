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

package scriptic.tokens;


public interface JavaRepresentations {

   /* Keywords from Java Spec 1.5 */
   public final static String AbstractRepresentation           =  "abstract";
   public final static String DoRepresentation                 =  "do";
   public final static String ImplementsRepresentation         =  "implements";
   public final static String PackageRepresentation            =  "package";
   public final static String ThrowRepresentation              =  "throw";
   public final static String BooleanRepresentation            =  "boolean";
   public final static String DoubleRepresentation             =  "double";
   public final static String ImportRepresentation             =  "import";
   public final static String PrivateRepresentation            =  "private";
   public final static String ThrowsRepresentation             =  "throws";
   public final static String BreakRepresentation              =  "break";
   public final static String ElseRepresentation               =  "else";
   public final static String InnerRepresentation              =  "inner";
   public final static String ProtectedRepresentation          =  "protected";
   public final static String TransientRepresentation          =  "transient";
   public final static String ByteRepresentation               =  "byte";
   public final static String ExtendsRepresentation            =  "extends";
   public final static String InstanceofRepresentation         =  "instanceof";
   public final static String PublicRepresentation             =  "public";
   public final static String TryRepresentation                =  "try";
   public final static String CaseRepresentation               =  "case";
   public final static String FinalRepresentation              =  "final";
   public final static String IntRepresentation                =  "int";
   public final static String RestRepresentation               =  "rest";
   public final static String VarRepresentation                =  "var";
   public final static String CastRepresentation               =  "cast";
   public final static String FinallyRepresentation            =  "finally";
   public final static String InterfaceRepresentation          =  "interface";
   public final static String ReturnRepresentation             =  "return";
   public final static String VoidRepresentation               =  "void";
   public final static String CatchRepresentation              =  "catch";
   public final static String FloatRepresentation              =  "float";
   public final static String LongRepresentation               =  "long";
   public final static String ShortRepresentation              =  "short";
   public final static String VolatileRepresentation           =  "volatile";
   public final static String CharRepresentation               =  "char";
   public final static String ForRepresentation                =  "for";
   public final static String NativeRepresentation             =  "native";
   public final static String StaticRepresentation             =  "static";
   public final static String WhileRepresentation              =  "while";
   public final static String ClassRepresentation              =  "class";
   public final static String FutureRepresentation             =  "future";
   public final static String NewRepresentation                =  "new";
   public final static String SuperRepresentation              =  "super";
   public final static String ConstRepresentation              =  "const";
   public final static String NullRepresentation               =  "null";
   public final static String SwitchRepresentation             =  "switch";
   public final static String ContinueRepresentation           =  "continue";
   public final static String GotoRepresentation               =  "goto";
 //public final static String OperatorRepresentation           =  "Roperator";
   public final static String SynchronizedRepresentation       =  "synchronized";
   public final static String DefaultRepresentation            =  "default";
   public final static String IfRepresentation                 =  "if";
   public final static String OuterRepresentation              =  "outer";
   public final static String ThisRepresentation               =  "this";
   public final static String ByvalueRepresentation            =  "byvalue";
   public final static String DimensionRepresentation          =  "dimension";


   /* Separators from Java Spec 1.8 */
   public final static String ParenthesisOpenRepresentation    = "(";
   public final static String ParenthesisCloseRepresentation   = ")";
   public final static String BraceOpenRepresentation          = "{";
   public final static String BraceCloseRepresentation         = "}";
   public final static String BracketOpenRepresentation        = "[";
   public final static String BracketCloseRepresentation       = "]";
   public final static String SemicolonRepresentation          = ";";
   public final static String CommaRepresentation              = ",";
   public final static String PeriodRepresentation             = ".";


   /* Operators from Java Spec 1.9 */
   // equality
   public final static String EqualsRepresentation             = "==";
   public final static String NotEqualRepresentation           = "!=";

   // relational
   public final static String GreaterThanRepresentation        = ">";
   public final static String LessThanRepresentation           = "<";
   public final static String LessOrEqualRepresentation        = "<=";
   public final static String GreaterOrEqualRepresentation     = ">=";

   // strictly unary
   public final static String ExclamationRepresentation        = "!";
   public final static String TildeRepresentation              = "~";
   public final static String IncrementRepresentation          = "++";
   public final static String DecrementRepresentation          = "--";

   // binary
   public final static String BooleanAndRepresentation         = "&&";
   public final static String BooleanOrRepresentation          = "||";
   public final static String PlusRepresentation               = "+";
   public final static String MinusRepresentation              = "-";
   public final static String AsteriskRepresentation           = "*";
   public final static String SlashRepresentation              = "/";
   public final static String AmpersandRepresentation          = "&";
   public final static String VerticalBarRepresentation        = "|";
   public final static String CaretRepresentation              = "^";
   public final static String PercentRepresentation            = "%";
   public final static String LeftShiftRepresentation          = "<<";
   public final static String RightShiftRepresentation         = ">>";
   public final static String UnsignedRightShiftRepresentation = ">>>";
   public final static String ExponentiationRepresentation     = "**";

   // assignment
   public final static String AssignRepresentation                   = "=";
   public final static String PlusAssignRepresentation               = "+=";
   public final static String MinusAssignRepresentation              = "-=";
   public final static String AsteriskAssignRepresentation           = "*=";
   public final static String SlashAssignRepresentation              = "/=";
   public final static String AmpersandAssignRepresentation          = "&=";
   public final static String VerticalBarAssignRepresentation        = "|=";
   public final static String CaretAssignRepresentation              = "^=";
   public final static String PercentAssignRepresentation            = "%=";
   public final static String LeftShiftAssignRepresentation          = "<<=";
   public final static String RightShiftAssignRepresentation         = ">>=";
   public final static String UnsignedRightShiftAssignRepresentation = ">>>=";

   // miscellaneous
   public final static String QuestionRepresentation           = "?";
   public final static String ColonRepresentation              = ":";


}

