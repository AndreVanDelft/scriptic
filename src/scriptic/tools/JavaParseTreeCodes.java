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

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        JavaParseTreeCodes                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

public interface JavaParseTreeCodes {

   /* TypeDeclaration */
   public final static int TopLevelTypeDeclarationCode     =   1;
   public final static int NestedTypeDeclarationCode       =   2;
   public final static int LocalTypeDeclarationCode        =   3;
   public final static int CommentTypeDeclarationCode      =   4;

   /* FieldDeclaration */
   public final static int MultiVariableDeclarationCode    =   5;
   public final static int MethodDeclarationCode           =   6;
   public final static int ConstructorDeclarationCode      =   7;
   public final static int InitializerBlockCode            =   8;
   public final static int CommentFieldDeclarationCode     =   9;

   public final static int     BaseDimensionDeclarationCode = 10;
   public final static int CompoundDimensionDeclarationCode = 11;

   /* JavaExpression */
   public final static int LayoutExpressionCode            =   0;
   public final static int BinaryExpressionCode            =   1;
   public final static int AssignmentExpressionCode        =   2;
   public final static int ConditionalExpressionCode       =   3;
   public final static int UnaryExpressionCode             =   4;
   public final static int PrimitiveCastExpressionCode     =   5;
   public final static int CastExpressionCode              =   6;
   public final static int TypeComparisonExpressionCode    =   7;
   public final static int PostfixExpressionCode           =   8;
   public final static int LiteralExpressionCode           =   9;
   public final static int NameExpressionCode              =  10;
   public final static int SpecialNameExpressionCode       =  11;
   public final static int NestedJavaExpressionCode        =  12;
   public final static int ArrayInitializerCode            =  13;
   public final static int FieldAccessExpressionCode       =  14;
   public final static int ArrayAccessExpressionCode       =  15;
   public final static int MethodCallExpressionCode        =  16;
   public final static int AllocationExpressionCode        =  17;
   public final static int ClassLiteralExpressionCode      =  18;
   public final static int QualifiedThisExpressionCode     =  19;
   public final static int QualifiedSuperExpressionCode    =  20;
   public final static int DimensionCastExpressionCode     =  21;


   /* JavaStatement */
   public final static int EmptyStatementCode                    =   0;
   public final static int CommentStatementCode                  =   1;
   public final static int NestedStatementCode                   =   2;
   public final static int LocalVariableDeclarationStatementCode =   3;
   public final static int LabeledStatementCode                  =   4;
   public final static int CaseTagCode                           =   5;
   public final static int DefaultCaseTagCode                    =   6;
   public final static int ExpressionStatementCode               =   7;
   public final static int IfStatementCode                       =   8;
   public final static int SwitchStatementCode                   =   9;
   public final static int WhileStatementCode                    =  10;
   public final static int DoStatementCode                       =  11;
   public final static int ForStatementCode                      =  12;
   public final static int BreakStatementCode                    =  13;
   public final static int ContinueStatementCode                 =  14;
   public final static int ReturnStatementCode                   =  15;
   public final static int ThrowStatementCode                    =  16;
   public final static int SynchronizedStatementCode             =  17;
   public final static int TryStatementCode                      =  18;
   public final static int TryBlockCode                          =  19;
   public final static int CatchBlockCode                        =  21;
   public final static int LocalTypeDeclarationStatementCode     =  22;
}

