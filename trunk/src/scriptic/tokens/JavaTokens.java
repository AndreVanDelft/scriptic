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

public interface JavaTokens {

   /* Special Cases */
   public final static int EofToken                =   0;
   public final static int StringNotClosedToken    =   1;
   public final static int CommentNotClosedToken   =   2;
   public final static int ErrorToken              =   3;
   public final static int IdentifierToken         =   4;

   /* Literals */
   public final static int     FirstLiteralToken   =  10;
   public final static int   IntegerLiteralToken   =  FirstLiteralToken + 0;
   public final static int      LongLiteralToken   =  FirstLiteralToken + 1;
   public final static int     FloatLiteralToken   =  FirstLiteralToken + 2;
   public final static int    DoubleLiteralToken   =  FirstLiteralToken + 3;
   public final static int    StringLiteralToken   =  FirstLiteralToken + 4;
   public final static int CharacterLiteralToken   =  FirstLiteralToken + 5;
   public final static int   BooleanLiteralToken   =  FirstLiteralToken + 6;
   public final static int      ByteLiteralToken   =  FirstLiteralToken + 7;
   public final static int     ShortLiteralToken   =  FirstLiteralToken + 8;
   public final static int     LastLiteralToken    =  FirstLiteralToken + 8;

   public final static int FirstToken              =  20;


   /* Keywords from Java Spec 1.5 */
   public final static int FirstKeywordToken       =  20;
   public final static int AbstractToken           =  FirstKeywordToken +  0;
   public final static int DoToken                 =  FirstKeywordToken +  1;
   public final static int ImplementsToken         =  FirstKeywordToken +  2;
   public final static int PackageToken            =  FirstKeywordToken +  3;
   public final static int ThrowToken              =  FirstKeywordToken +  4;
   public final static int BooleanToken            =  FirstKeywordToken +  5;
   public final static int DoubleToken             =  FirstKeywordToken +  6;
   public final static int ImportToken             =  FirstKeywordToken +  7;
   public final static int PrivateToken            =  FirstKeywordToken +  8;
   public final static int ThrowsToken             =  FirstKeywordToken +  9;
   public final static int BreakToken              =  FirstKeywordToken + 10;
   public final static int ElseToken               =  FirstKeywordToken + 11;
   public final static int InnerToken              =  FirstKeywordToken + 12;
   public final static int ProtectedToken          =  FirstKeywordToken + 13;
   public final static int TransientToken          =  FirstKeywordToken + 14;
   public final static int ByteToken               =  FirstKeywordToken + 15;
   public final static int ExtendsToken            =  FirstKeywordToken + 16;
   public final static int InstanceofToken         =  FirstKeywordToken + 17;
   public final static int PublicToken             =  FirstKeywordToken + 18;
   public final static int TryToken                =  FirstKeywordToken + 19;
   public final static int CaseToken               =  FirstKeywordToken + 20;
   public final static int FinalToken              =  FirstKeywordToken + 21;
   public final static int IntToken                =  FirstKeywordToken + 22;
   public final static int RestToken               =  FirstKeywordToken + 23;
   public final static int VarToken                =  FirstKeywordToken + 24;
   public final static int CastToken               =  FirstKeywordToken + 25;
   public final static int FinallyToken            =  FirstKeywordToken + 26;
   public final static int InterfaceToken          =  FirstKeywordToken + 27;
   public final static int ReturnToken             =  FirstKeywordToken + 28;
   public final static int VoidToken               =  FirstKeywordToken + 29;
   public final static int CatchToken              =  FirstKeywordToken + 30;
   public final static int FloatToken              =  FirstKeywordToken + 31;
   public final static int LongToken               =  FirstKeywordToken + 32;
   public final static int ShortToken              =  FirstKeywordToken + 33;
   public final static int VolatileToken           =  FirstKeywordToken + 34;
   public final static int CharToken               =  FirstKeywordToken + 35;
   public final static int ForToken                =  FirstKeywordToken + 36;
   public final static int NativeToken             =  FirstKeywordToken + 37;
   public final static int StaticToken             =  FirstKeywordToken + 38;
   public final static int WhileToken              =  FirstKeywordToken + 39;
   public final static int ClassToken              =  FirstKeywordToken + 40;
   public final static int FutureToken             =  FirstKeywordToken + 41;
   public final static int NewToken                =  FirstKeywordToken + 42;
   public final static int SuperToken              =  FirstKeywordToken + 43;
   public final static int ConstToken              =  FirstKeywordToken + 44;
   public final static int NullToken               =  FirstKeywordToken + 46;
   public final static int SwitchToken             =  FirstKeywordToken + 47;
   public final static int ContinueToken           =  FirstKeywordToken + 48;
   public final static int GotoToken               =  FirstKeywordToken + 49;
 //public final static int OperatorToken           =  FirstKeywordToken + 50;
   public final static int SynchronizedToken       =  FirstKeywordToken + 51;
   public final static int DefaultToken            =  FirstKeywordToken + 52;
   public final static int IfToken                 =  FirstKeywordToken + 53;
   public final static int OuterToken              =  FirstKeywordToken + 54;
   public final static int ThisToken               =  FirstKeywordToken + 55;
   public final static int ByvalueToken            =  FirstKeywordToken + 56;
   public final static int DimensionToken          =  FirstKeywordToken + 57;
   public final static int LastKeywordToken        =  FirstKeywordToken + 59;

// NOTE: LastKeywordToken includes room for keywords defined
//       in ALL subclasses


   /* Separators from Java Spec 1.8 */
   public final static int FirstSeparatorToken     =  80;
   public final static int ParenthesisOpenToken    =  FirstSeparatorToken +  0;
   public final static int ParenthesisCloseToken   =  FirstSeparatorToken +  1;
   public final static int BraceOpenToken          =  FirstSeparatorToken +  2;
   public final static int BraceCloseToken         =  FirstSeparatorToken +  3;
   public final static int BracketOpenToken        =  FirstSeparatorToken +  4;
   public final static int BracketCloseToken       =  FirstSeparatorToken +  5;
   public final static int SemicolonToken          =  FirstSeparatorToken +  6;
   public final static int CommaToken              =  FirstSeparatorToken +  7;
   public final static int PeriodToken             =  FirstSeparatorToken +  8;


// NOTE: First available token code after keywords and separators,
//       including those defined in subclasses, is 120. All operators
//       reserve the two least significant bits for the user defined
//       operators.


   /* Operators from Java Spec 1.9 */
   public final static int FirstOperatorToken      =  90;

   // equality
   public final static int EqualsToken             =  FirstOperatorToken + 0;
   public final static int NotEqualToken           =  FirstOperatorToken + 1;

   // relational
   public final static int GreaterThanToken        =  FirstOperatorToken + 2;
   public final static int LessThanToken           =  FirstOperatorToken + 3;
   public final static int LessOrEqualToken        =  FirstOperatorToken + 4;
   public final static int GreaterOrEqualToken     =  FirstOperatorToken + 5;

   // strictly unary
   public final static int ExclamationToken        =  FirstOperatorToken + 6;
   public final static int TildeToken              =  FirstOperatorToken + 7;
   public final static int IncrementToken          =  FirstOperatorToken + 8;
   public final static int DecrementToken          =  FirstOperatorToken + 9;

   // binary
   public final static int BooleanAndToken         =  FirstOperatorToken + 10;
   public final static int BooleanOrToken          =  FirstOperatorToken + 11;
   public final static int PlusToken               =  FirstOperatorToken + 12;
   public final static int MinusToken              =  FirstOperatorToken + 13;
   public final static int AsteriskToken           =  FirstOperatorToken + 14;
   public final static int SlashToken              =  FirstOperatorToken + 15;
   public final static int AmpersandToken          =  FirstOperatorToken + 16;
   public final static int VerticalBarToken        =  FirstOperatorToken + 17;
   public final static int CaretToken              =  FirstOperatorToken + 18;
   public final static int PercentToken            =  FirstOperatorToken + 19;
   public final static int LeftShiftToken          =  FirstOperatorToken + 20;
   public final static int RightShiftToken         =  FirstOperatorToken + 21;
   public final static int UnsignedRightShiftToken =  FirstOperatorToken + 22;
   public final static int ExponentiationToken     =  FirstOperatorToken + 23;

   public final static int LastOperatorToken             =  FirstOperatorToken + 23;
   
   public final static int FirstAssignmentToken          =  120;

   // assignment
   public final static int AssignToken                   =  FirstAssignmentToken + 0;
   public final static int PlusAssignToken               =  FirstAssignmentToken + 1;
   public final static int MinusAssignToken              =  FirstAssignmentToken + 2;
   public final static int AsteriskAssignToken           =  FirstAssignmentToken + 3;
   public final static int SlashAssignToken              =  FirstAssignmentToken + 4;
   public final static int AmpersandAssignToken          =  FirstAssignmentToken + 5;
   public final static int VerticalBarAssignToken        =  FirstAssignmentToken + 6;
   public final static int CaretAssignToken              =  FirstAssignmentToken + 7;
   public final static int PercentAssignToken            =  FirstAssignmentToken + 8;
   public final static int LeftShiftAssignToken          =  FirstAssignmentToken + 9;
   public final static int RightShiftAssignToken         =  FirstAssignmentToken + 10;
   public final static int UnsignedRightShiftAssignToken =  FirstAssignmentToken + 11;

   public final static int LastAssignmentToken           =  FirstAssignmentToken + 11;



   // miscellaneous
   public final static int FirstMiscToken          =  140;

   public final static int QuestionToken           =  FirstMiscToken + 0;
   public final static int ColonToken              =  FirstMiscToken + 1;

   public final static int LastToken               =  ScripticTokens.AtSignToken; // messy

}
