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

import java.util.HashMap;

public class Representations implements ScripticTokens, ScripticRepresentations, ScripticParseTreeCodes {

	public static HashMap<String, Integer> TokenCodes = new HashMap<String, Integer>();
	public static String [] TokenRepresentations = new String[LastToken + 1];
	public static String [] TokenNames           = new String[LastToken + 1];

	static void makeRepresentation (String s, int i) {
		      TokenRepresentations [i] = s;
		      TokenCodes.put (s,i);
	}
		static {
			TokenRepresentations[IdentifierToken] = "Identifier";
			TokenRepresentations[IntegerLiteralToken] = "Integer literal";
			TokenRepresentations[LongLiteralToken] = "Long integer literal";
			TokenRepresentations[FloatLiteralToken] = "Float literal";
			TokenRepresentations[DoubleLiteralToken] = "Double-precision literal";
			TokenRepresentations[StringLiteralToken] = "String literal";
			TokenRepresentations[CharacterLiteralToken] = "Character literal";
			TokenRepresentations[BooleanLiteralToken] = "Boolean literal";

			  /* Special case - boolean literals */
			  TokenCodes.put("true", BooleanLiteralToken);
			  TokenCodes.put("false", BooleanLiteralToken);

			  makeRepresentation (ParenthesisOpenRepresentation, ParenthesisOpenToken);
			  makeRepresentation (ParenthesisCloseRepresentation, ParenthesisCloseToken);
			  makeRepresentation (BraceOpenRepresentation, BraceOpenToken);
			  makeRepresentation (BraceCloseRepresentation, BraceCloseToken);
			  makeRepresentation (BracketOpenRepresentation, BracketOpenToken);
			  makeRepresentation (BracketCloseRepresentation, BracketCloseToken);
			  makeRepresentation (SemicolonRepresentation, SemicolonToken);
			  makeRepresentation (CommaRepresentation, CommaToken);
			  makeRepresentation (PeriodRepresentation, PeriodToken);

			  makeRepresentation (AssignRepresentation, AssignToken);
			  makeRepresentation (GreaterThanRepresentation, GreaterThanToken);
			  makeRepresentation (LessThanRepresentation, LessThanToken);
			  makeRepresentation (ExclamationRepresentation, ExclamationToken);
			  makeRepresentation (TildeRepresentation, TildeToken);
			  makeRepresentation (QuestionRepresentation, QuestionToken);
			  makeRepresentation (ColonRepresentation, ColonToken);
			  makeRepresentation (EqualsRepresentation, EqualsToken);
			  makeRepresentation (LessOrEqualRepresentation, LessOrEqualToken);
			  makeRepresentation (GreaterOrEqualRepresentation, GreaterOrEqualToken);
			  makeRepresentation (NotEqualRepresentation, NotEqualToken);

			  makeRepresentation (BooleanAndRepresentation, BooleanAndToken);
			  makeRepresentation (BooleanOrRepresentation, BooleanOrToken);
			  makeRepresentation (IncrementRepresentation, IncrementToken);
			  makeRepresentation (DecrementRepresentation, DecrementToken);
			  makeRepresentation (PlusRepresentation, PlusToken);
			  makeRepresentation (MinusRepresentation, MinusToken);
			  makeRepresentation (AsteriskRepresentation, AsteriskToken);
			  makeRepresentation (SlashRepresentation, SlashToken);
			  makeRepresentation (AmpersandRepresentation, AmpersandToken);
			  makeRepresentation (VerticalBarRepresentation, VerticalBarToken);
			  makeRepresentation (CaretRepresentation, CaretToken);
			  makeRepresentation (PercentRepresentation, PercentToken);
			  makeRepresentation (LeftShiftRepresentation, LeftShiftToken);
			  makeRepresentation (RightShiftRepresentation, RightShiftToken);
			  makeRepresentation (UnsignedRightShiftRepresentation, UnsignedRightShiftToken);

			  makeRepresentation (PlusAssignRepresentation, PlusAssignToken);
			  makeRepresentation (MinusAssignRepresentation, MinusAssignToken);
			  makeRepresentation (AsteriskAssignRepresentation, AsteriskAssignToken);
			  makeRepresentation (SlashAssignRepresentation, SlashAssignToken);
			  makeRepresentation (AmpersandAssignRepresentation, AmpersandAssignToken);
			  makeRepresentation (VerticalBarAssignRepresentation, VerticalBarAssignToken);
			  makeRepresentation (CaretAssignRepresentation, CaretAssignToken);
			  makeRepresentation (PercentAssignRepresentation, PercentAssignToken);
			  makeRepresentation (LeftShiftAssignRepresentation, LeftShiftAssignToken);
			  makeRepresentation (RightShiftAssignRepresentation, RightShiftAssignToken);
			  makeRepresentation (UnsignedRightShiftAssignRepresentation, UnsignedRightShiftAssignToken);

			  makeRepresentation (AbstractRepresentation, AbstractToken);
			  makeRepresentation (DoRepresentation, DoToken);
			  makeRepresentation (ImplementsRepresentation, ImplementsToken);
			  makeRepresentation (PackageRepresentation, PackageToken);
			  makeRepresentation (ThrowRepresentation, ThrowToken);
			  makeRepresentation (BooleanRepresentation, BooleanToken);
			  makeRepresentation (DoubleRepresentation, DoubleToken);
			  makeRepresentation (ImportRepresentation, ImportToken);
			  makeRepresentation (PrivateRepresentation, PrivateToken);
			  makeRepresentation (ThrowsRepresentation, ThrowsToken);
			  makeRepresentation (BreakRepresentation, BreakToken);
			  makeRepresentation (ElseRepresentation, ElseToken);
			  makeRepresentation (InnerRepresentation, InnerToken);
			  makeRepresentation (ProtectedRepresentation, ProtectedToken);
			  makeRepresentation (TransientRepresentation, TransientToken);
			  makeRepresentation (ByteRepresentation, ByteToken);
			  makeRepresentation (ExtendsRepresentation, ExtendsToken);
			  makeRepresentation (InstanceofRepresentation, InstanceofToken);
			  makeRepresentation (PublicRepresentation, PublicToken);
			  makeRepresentation (TryRepresentation, TryToken);
			  makeRepresentation (CaseRepresentation, CaseToken);
			  makeRepresentation (FinalRepresentation, FinalToken);
			  makeRepresentation (IntRepresentation, IntToken);
			  makeRepresentation (RestRepresentation, RestToken);
			  makeRepresentation (VarRepresentation, VarToken);
			  makeRepresentation (CastRepresentation, CastToken);
			  makeRepresentation (FinallyRepresentation, FinallyToken);
			  makeRepresentation (InterfaceRepresentation, InterfaceToken);
			  makeRepresentation (ReturnRepresentation, ReturnToken);
			  makeRepresentation (VoidRepresentation, VoidToken);
			  makeRepresentation (CatchRepresentation, CatchToken);
			  makeRepresentation (FloatRepresentation, FloatToken);
			  makeRepresentation (LongRepresentation, LongToken);
			  makeRepresentation (ShortRepresentation, ShortToken);
			  makeRepresentation (VolatileRepresentation, VolatileToken);
			  makeRepresentation (CharRepresentation, CharToken);
			  makeRepresentation (ForRepresentation, ForToken);
			  makeRepresentation (NativeRepresentation, NativeToken);
			  makeRepresentation (StaticRepresentation, StaticToken);
			  makeRepresentation (WhileRepresentation, WhileToken);
			  makeRepresentation (ClassRepresentation, ClassToken);
			  makeRepresentation (FutureRepresentation, FutureToken);
			  makeRepresentation (NewRepresentation, NewToken);
			  makeRepresentation (SuperRepresentation, SuperToken);
			  makeRepresentation (ConstRepresentation, ConstToken);
			  makeRepresentation (NullRepresentation, NullToken);
			  makeRepresentation (SwitchRepresentation, SwitchToken);
			  makeRepresentation (ContinueRepresentation, ContinueToken);
			  makeRepresentation (GotoRepresentation, GotoToken);
			  //   makeRepresentation  (OperatorRepresentation, OperatorToken);
			  makeRepresentation (SynchronizedRepresentation, SynchronizedToken);
			  makeRepresentation (DefaultRepresentation, DefaultToken);
			  makeRepresentation (IfRepresentation, IfToken);
			  makeRepresentation (OuterRepresentation, OuterToken);
			  makeRepresentation (ThisRepresentation, ThisToken);
			  makeRepresentation (ByvalueRepresentation, ByvalueToken);
			  makeRepresentation (DimensionRepresentation, DimensionToken);

		      makeRepresentation (BraceColonOpenRepresentation,           BraceColonOpenToken);
		      makeRepresentation (BraceColonCloseRepresentation,          BraceColonCloseToken);
		      makeRepresentation (BraceQuestionOpenRepresentation,        BraceQuestionOpenToken);
		      makeRepresentation (BraceQuestionCloseRepresentation,       BraceQuestionCloseToken);
		      makeRepresentation (BracePeriodOpenRepresentation,          BracePeriodOpenToken);
		      makeRepresentation (BracePeriodCloseRepresentation,         BracePeriodCloseToken);
		      makeRepresentation (BraceEllipsisOpenRepresentation,        BraceEllipsisOpenToken);
		      makeRepresentation (BraceEllipsisCloseRepresentation,       BraceEllipsisCloseToken);
		      makeRepresentation (BraceEllipsis3OpenRepresentation,       BraceEllipsis3OpenToken);
		      makeRepresentation (BraceEllipsis3CloseRepresentation,      BraceEllipsis3CloseToken);
		      makeRepresentation (BraceAsteriskOpenRepresentation,        BraceAsteriskOpenToken);
		      makeRepresentation (BraceAsteriskCloseRepresentation,       BraceAsteriskCloseToken);
		      makeRepresentation (EllipsisRepresentation,                 EllipsisToken);
		      makeRepresentation (Ellipsis3Representation,                 Ellipsis3Token);
		      makeRepresentation (DoubleQuestionRepresentation,           DoubleQuestionToken);
		      makeRepresentation (DoubleExclamationRepresentation,        DoubleExclamationToken);
		      makeRepresentation (AtSignRepresentation,                   AtSignToken);
		      makeRepresentation (HashRepresentation,                     HashToken);
		      makeRepresentation (ScriptRepresentation,              ScriptToken);
		      makeRepresentation (ScriptsRepresentation,             ScriptsToken);

				TokenNames[EofToken] = "EofToken";
				TokenNames[StringNotClosedToken] = "StringNotClosedToken";
				TokenNames[CommentNotClosedToken] = "CommentNotClosedToken";
				TokenNames[ErrorToken] = "ErrorToken";
				TokenNames[IdentifierToken] = "IdentifierToken";
				TokenNames[IntegerLiteralToken] = "IntegerLiteralToken";
				TokenNames[LongLiteralToken] = "LongLiteralToken";
				TokenNames[FloatLiteralToken] = "FloatLiteralToken";
				TokenNames[DoubleLiteralToken] = "DoubleLiteralToken";
				TokenNames[StringLiteralToken] = "StringLiteralToken";
				TokenNames[CharacterLiteralToken] = "CharacterLiteralToken";
				TokenNames[BooleanLiteralToken] = "BooleanLiteralToken";

				TokenNames[ParenthesisOpenToken] = "ParenthesisOpenToken";
				TokenNames[ParenthesisCloseToken] = "ParenthesisCloseToken";
				TokenNames[BraceOpenToken] = "BraceOpenToken";
				TokenNames[BraceCloseToken] = "BraceCloseToken";
				TokenNames[BracketOpenToken] = "BracketOpenToken";
				TokenNames[BracketCloseToken] = "BracketCloseToken";
				TokenNames[SemicolonToken] = "SemicolonToken";
				TokenNames[CommaToken] = "CommaToken";
				TokenNames[PeriodToken] = "PeriodToken";

				TokenNames[AssignToken] = "AssignToken";
				TokenNames[GreaterThanToken] = "GreaterThanToken";
				TokenNames[LessThanToken] = "LessThanToken";
				TokenNames[ExclamationToken] = "ExclamationToken";
				TokenNames[TildeToken] = "TildeToken";
				TokenNames[QuestionToken] = "QuestionToken";
				TokenNames[ColonToken] = "ColonToken";
				TokenNames[EqualsToken] = "EqualsToken";
				TokenNames[LessOrEqualToken] = "LessOrEqualToken";
				TokenNames[GreaterOrEqualToken] = "GreaterOrEqualToken";
				TokenNames[NotEqualToken] = "NotEqualToken";

				TokenNames[BooleanAndToken] = "BooleanAndToken";
				TokenNames[BooleanOrToken] = "BooleanOrToken";
				TokenNames[IncrementToken] = "IncrementToken";
				TokenNames[DecrementToken] = "DecrementToken";
				TokenNames[PlusToken] = "PlusToken";
				TokenNames[MinusToken] = "MinusToken";
				TokenNames[AsteriskToken] = "AsteriskToken";
				TokenNames[SlashToken] = "SlashToken";
				TokenNames[AmpersandToken] = "AmpersandToken";
				TokenNames[VerticalBarToken] = "VerticalBarToken";
				TokenNames[CaretToken] = "CaretToken";
				TokenNames[PercentToken] = "PercentToken";
				TokenNames[LeftShiftToken] = "LeftShiftToken";
				TokenNames[RightShiftToken] = "RightShiftToken";
				TokenNames[UnsignedRightShiftToken] = "UnsignedRightShiftToken";

				TokenNames[PlusAssignToken] = "PlusAssignToken";
				TokenNames[MinusAssignToken] = "MinusAssignToken";
				TokenNames[AsteriskAssignToken] = "AsteriskAssignToken";
				TokenNames[SlashAssignToken] = "SlashAssignToken";
				TokenNames[AmpersandAssignToken] = "AmpersandAssignToken";
				TokenNames[VerticalBarAssignToken] = "VerticalBarAssignToken";
				TokenNames[CaretAssignToken] = "CaretAssignToken";
				TokenNames[PercentAssignToken] = "PercentAssignToken";
				TokenNames[LeftShiftAssignToken] = "LeftShiftAssignToken";
				TokenNames[RightShiftAssignToken] = "RightShiftAssignToken";
				TokenNames[UnsignedRightShiftAssignToken] = "UnsignedRightShiftAssignToken";

				TokenNames[AbstractToken] = "AbstractToken";
				TokenNames[DoToken] = "DoToken";
				TokenNames[ImplementsToken] = "ImplementsToken";
				TokenNames[PackageToken] = "PackageToken";
				TokenNames[ThrowToken] = "ThrowToken";
				TokenNames[BooleanToken] = "BooleanToken";
				TokenNames[DoubleToken] = "DoubleToken";
				TokenNames[ImportToken] = "ImportToken";
				TokenNames[PrivateToken] = "PrivateToken";
				TokenNames[ThrowsToken] = "ThrowsToken";
				TokenNames[BreakToken] = "BreakToken";
				TokenNames[ElseToken] = "ElseToken";
				TokenNames[InnerToken] = "InnerToken";
				TokenNames[ProtectedToken] = "ProtectedToken";
				TokenNames[TransientToken] = "TransientToken";
				TokenNames[ByteToken] = "ByteToken";
				TokenNames[ExtendsToken] = "ExtendsToken";
				TokenNames[InstanceofToken] = "InstanceofToken";
				TokenNames[PublicToken] = "PublicToken";
				TokenNames[TryToken] = "TryToken";
				TokenNames[CaseToken] = "CaseToken";
				TokenNames[FinalToken] = "FinalToken";
				TokenNames[IntToken] = "IntToken";
				TokenNames[RestToken] = "RestToken";
				TokenNames[VarToken] = "VarToken";
				TokenNames[CastToken] = "CastToken";
				TokenNames[FinallyToken] = "FinallyToken";
				TokenNames[InterfaceToken] = "InterfaceToken";
				TokenNames[ReturnToken] = "ReturnToken";
				TokenNames[VoidToken] = "VoidToken";
				TokenNames[CatchToken] = "CatchToken";
				TokenNames[FloatToken] = "FloatToken";
				TokenNames[LongToken] = "LongToken";
				TokenNames[ShortToken] = "ShortToken";
				TokenNames[VolatileToken] = "VolatileToken";
				TokenNames[CharToken] = "CharToken";
				TokenNames[ForToken] = "ForToken";
				TokenNames[NativeToken] = "NativeToken";
				TokenNames[StaticToken] = "StaticToken";
				TokenNames[WhileToken] = "WhileToken";
				TokenNames[ClassToken] = "ClassToken";
				TokenNames[FutureToken] = "FutureToken";
				TokenNames[NewToken] = "NewToken";
				TokenNames[SuperToken] = "SuperToken";
				TokenNames[ConstToken] = "ConstToken";
				TokenNames[NullToken] = "NullToken";
				TokenNames[SwitchToken] = "SwitchToken";
				TokenNames[ContinueToken] = "ContinueToken";
				TokenNames[GotoToken] = "GotoToken";
				// TokenNames [ OperatorToken ] = "OperatorToken";
				TokenNames[SynchronizedToken] = "SynchronizedToken";
				TokenNames[DefaultToken] = "DefaultToken";
				TokenNames[IfToken] = "IfToken";
				TokenNames[OuterToken] = "OuterToken";
				TokenNames[ThisToken] = "ThisToken";
				TokenNames[ByvalueToken] = "ByvalueToken";
				TokenNames[DimensionToken] = "DimensionToken";
		      TokenNames [ EofToken                ] = "EofToken";
		      TokenNames [ BraceColonOpenToken     ] = "BraceColonOpenToken";
		      TokenNames [ BraceColonCloseToken    ] = "BraceColonCloseToken";
		      TokenNames [ BraceQuestionOpenToken  ] = "BraceQuestionOpenToken";
		      TokenNames [ BraceQuestionCloseToken ] = "BraceQuestionCloseToken";
		      TokenNames [ BracePeriodOpenToken    ] = "BracePeriodOpenToken";
		      TokenNames [ BracePeriodCloseToken   ] = "BracePeriodCloseToken";
		      TokenNames [ BraceEllipsisOpenToken  ] = "BraceEllipsisOpenToken";
		      TokenNames [ BraceEllipsisCloseToken ] = "BraceEllipsisCloseToken";
		      TokenNames [ BraceEllipsis3OpenToken ] = "BraceEllipsis3OpenToken";
		      TokenNames [ BraceEllipsis3CloseToken] = "BraceEllipsis3CloseToken";
		      TokenNames [ BraceAsteriskOpenToken     ] = "BraceAsteriskOpenToken";
		      TokenNames [ BraceAsteriskCloseToken    ] = "BraceAsteriskCloseToken";
		      TokenNames [ EllipsisToken           ] = "EllipsisToken";
		      TokenNames [ Ellipsis3Token          ] = "Ellipsis3Token";
		      TokenNames [ DoubleQuestionToken     ] = "DoubleQuestionToken";
		      TokenNames [ DoubleExclamationToken  ] = "DoubleExclamationToken";
		      TokenNames [ AtSignToken             ] = "AtSignToken";
		      TokenNames [ HashToken               ] = "HashToken";
		      TokenNames [ ScriptToken             ] = "ScriptToken";
		      TokenNames [ ScriptsToken            ] = "ScriptsToken";
		   }

		   public static String scripticParseTreeCodeRepresentations[];
		   static {
		     scripticParseTreeCodeRepresentations = new String [128];
		     scripticParseTreeCodeRepresentations [            ScriptDeclarationCode] = "ScriptDeclaration";
		     scripticParseTreeCodeRepresentations [     CommunicationDeclarationCode] = "CommunicationDeclaration";
		     scripticParseTreeCodeRepresentations [           ChannelDeclarationCode] = "ChannelDeclaration";
		     scripticParseTreeCodeRepresentations [           NativeCodeFragmentCode] = "NativeCodeFragment";
		     scripticParseTreeCodeRepresentations [    EventHandlingCodeFragmentCode] = "EventHandlingCodeFragment";
		     scripticParseTreeCodeRepresentations [               ActivationCodeCode] = "ActivationCode";
		     scripticParseTreeCodeRepresentations [             DeactivationCodeCode] = "DeactivationCode";
		     scripticParseTreeCodeRepresentations [  ConditionalScriptExpressionCode] = "ConditionalScriptExpression";
		     scripticParseTreeCodeRepresentations [           IfScriptExpressionCode] = "IfScriptExpression";
		     scripticParseTreeCodeRepresentations [        WhileScriptExpressionCode] = "WhileScriptExpression";
		     scripticParseTreeCodeRepresentations [          ForScriptExpressionCode] = "ForScriptExpression";
		     scripticParseTreeCodeRepresentations [       SwitchScriptExpressionCode] = "SwitchScriptExpression";
		     scripticParseTreeCodeRepresentations [      CaseTagScriptExpressionCode] = "CaseTagScriptExpression";
		     scripticParseTreeCodeRepresentations [         ScriptCallExpressionCode] = "ScriptCallExpression";
		     scripticParseTreeCodeRepresentations [   ScriptLocalDataDeclarationCode] = "ScriptLocalDataDeclaration";
		     scripticParseTreeCodeRepresentations [ PrivateScriptDataDeclarationCode] = "PrivateScriptDataDeclaration";
		     scripticParseTreeCodeRepresentations [       SendChannelDeclarationCode] = "SendChannelDeclaration";
		     scripticParseTreeCodeRepresentations [    ReceiveChannelDeclarationCode] = "ReceiveChannelDeclaration";
		     scripticParseTreeCodeRepresentations [                  ChannelSendCode] = "ChannelSend";
		     scripticParseTreeCodeRepresentations [               ChannelReceiveCode] = "ChannelReceive";
		     scripticParseTreeCodeRepresentations [             TinyCodeFragmentCode] = "TinyCodeFragment";
		     scripticParseTreeCodeRepresentations [           UnsureCodeFragmentCode] = "UnsureCodeFragment";
		     scripticParseTreeCodeRepresentations [          Unsure2CodeFragmentCode] = "Unsure2CodeFragment";
		     scripticParseTreeCodeRepresentations [         ThreadedCodeFragmentCode] = "ThreadedCodeFragment";
		     scripticParseTreeCodeRepresentations [   EventHandling0PlusCodeFragmentCode] = "EventHandling0PlusCodeFragment";
		     scripticParseTreeCodeRepresentations [   EventHandling1PlusCodeFragmentCode] = "EventHandling1PlusCodeFragment";
		     scripticParseTreeCodeRepresentations [   EventHandlingManyCodeFragmentCode] = "EventHandlingManyCodeFragment";
		     scripticParseTreeCodeRepresentations [               ZeroExpressionCode] = "ZeroExpression";
		     scripticParseTreeCodeRepresentations [                OneExpressionCode] = "OneExpression";
		     scripticParseTreeCodeRepresentations [              BreakExpressionCode] = "BreakExpression";
		     scripticParseTreeCodeRepresentations [              EllipsisOperandCode] = "EllipsisOperand";
		     scripticParseTreeCodeRepresentations [             Ellipsis3OperandCode] = "Ellipsis3Operand";
		     scripticParseTreeCodeRepresentations [           LaunchedExpressionCode] = "LaunchedExpression";
		     scripticParseTreeCodeRepresentations [             EllipsisOperatorCode] = "EllipsisOperator";
		     scripticParseTreeCodeRepresentations [               ChannelRequestCode] = "ChannelRequest";
		     scripticParseTreeCodeRepresentations [         CommunicationRequestCode] = "CommunicationRequest";
		     scripticParseTreeCodeRepresentations [                         RootCode] = "Root";
		     scripticParseTreeCodeRepresentations [                     CommRootCode] = "CommRoot";
		     scripticParseTreeCodeRepresentations [               RootScriptCallCode] = "RootScriptCall";
		     scripticParseTreeCodeRepresentations [             ParBreakOperatorCode] = "ParBreakOperator";
		     scripticParseTreeCodeRepresentations [              ParOrOperatorCode] = "ParOrOperator";
		     scripticParseTreeCodeRepresentations [                  ParAndOperatorCode] = "ParOperator";
		     scripticParseTreeCodeRepresentations [               ParAnd2OperatorCode] = "ParAnd2Operator";
		     scripticParseTreeCodeRepresentations [                ParOr2OperatorCode] = "ParOr2Operator";
		     scripticParseTreeCodeRepresentations [              SuspendOperatorCode] = "SuspendOperator";
		     scripticParseTreeCodeRepresentations [                   OrOperatorCode] = "OrOperator";
		     scripticParseTreeCodeRepresentations [                  SeqOperatorCode] = "SeqOperator";
		     scripticParseTreeCodeRepresentations [               NotSeqOperatorCode] = "NotSeqOperator";
		     scripticParseTreeCodeRepresentations [          ReactiveNotOperatorCode] = "ReactiveNotOperator";
		     scripticParseTreeCodeRepresentations [                  NotOperatorCode] = "NotOperator";
		   }

		   static {
		      // static check for unicity of token codes
		      switch (0) {
		      case EofToken                :   
		      case StringNotClosedToken    :   
		      case CommentNotClosedToken   :   
		      case ErrorToken              :   
		      case IdentifierToken         :   
		      case IntegerLiteralToken   :  
		      case LongLiteralToken   :  
		      case FloatLiteralToken   :  
		      case DoubleLiteralToken   :  
		      case StringLiteralToken   :  
		      case CharacterLiteralToken   :  
		      case BooleanLiteralToken   :  
		      case ByteLiteralToken   :  
		      case ShortLiteralToken   :  
		      case AbstractToken           :  
		      case DoToken                 :  
		      case ImplementsToken         :  
		      case PackageToken            :  
		      case ThrowToken              :  
		      case BooleanToken            :  
		      case DoubleToken             :  
		      case ImportToken             :  
		      case PrivateToken            :  
		      case ThrowsToken             :  
		      case BreakToken              :  
		      case ElseToken               :  
		      case InnerToken              :  
		      case ProtectedToken          :  
		      case TransientToken          :  
		      case ByteToken               :  
		      case ExtendsToken            :  
		      case InstanceofToken         :  
		      case PublicToken             :  
		      case TryToken                :  
		      case CaseToken               :  
		      case FinalToken              :  
		      case IntToken                :  
		      case RestToken               :  
		      case VarToken                :  
		      case CastToken               :  
		      case FinallyToken            :  
		      case InterfaceToken          :  
		      case ReturnToken             :  
		      case VoidToken               :  
		      case CatchToken              :  
		      case FloatToken              :  
		      case LongToken               :  
		      case ShortToken              :  
		      case VolatileToken           :  
		      case CharToken               :  
		      case ForToken                :  
		      case NativeToken             :  
		      case StaticToken             :  
		      case WhileToken              :  
		      case ClassToken              :  
		      case FutureToken             :  
		      case NewToken                :  
		      case SuperToken              :  
		      case ConstToken              :  
		      case NullToken               :  
		      case SwitchToken             :  
		      case ContinueToken           :  
		      case GotoToken               :  
		      case SynchronizedToken       :  
		      case DefaultToken            :  
		      case IfToken                 :  
		      case OuterToken              :  
		      case ThisToken               :  
		      case ByvalueToken            :  
		      case DimensionToken          :  
		      case ParenthesisOpenToken    :  
		      case ParenthesisCloseToken   :  
		      case BraceOpenToken          :  
		      case BraceCloseToken         :  
		      case BracketOpenToken        :  
		      case BracketCloseToken       :  
		      case SemicolonToken          :  
		      case CommaToken              :  
		      case PeriodToken             :  
		      case EqualsToken             :  
		      case NotEqualToken           :  
		      case GreaterThanToken        :  
		      case LessThanToken           :  
		      case LessOrEqualToken        :  
		      case GreaterOrEqualToken     :  
		      case ExclamationToken        :  
		      case TildeToken              :  
		      case IncrementToken          :  
		      case DecrementToken          :  
		      case BooleanAndToken         :  
		      case BooleanOrToken          :  
		      case PlusToken               :  
		      case MinusToken              :  
		      case AsteriskToken           :  
		      case SlashToken              :  
		      case AmpersandToken          :  
		      case VerticalBarToken        :  
		      case CaretToken              :  
		      case PercentToken            :  
		      case LeftShiftToken          :  
		      case RightShiftToken         :  
		      case UnsignedRightShiftToken :  
		      case ExponentiationToken     :  
		      case AssignToken                   :  
		      case PlusAssignToken               :  
		      case MinusAssignToken              :  
		      case AsteriskAssignToken           :  
		      case SlashAssignToken              :  
		      case AmpersandAssignToken          :  
		      case VerticalBarAssignToken        :  
		      case CaretAssignToken              :  
		      case PercentAssignToken            :  
		      case LeftShiftAssignToken          :  
		      case RightShiftAssignToken         :  
		      case UnsignedRightShiftAssignToken :  
		      case QuestionToken                 :  
		      case ColonToken                    :  
		      case ScriptToken                   :  
		      case ScriptsToken                  :  
		      case BraceColonOpenToken           : 
		      case BraceColonCloseToken          : 
		      case BraceQuestionOpenToken        : 
		      case BraceQuestionCloseToken       : 
		      case BracePeriodOpenToken          : 
		      case BracePeriodCloseToken         : 
		      case BraceEllipsisOpenToken        : 
		      case BraceEllipsisCloseToken       : 
		      case BraceEllipsis3OpenToken        : 
		      case BraceEllipsis3CloseToken       : 
		      case BraceAsteriskOpenToken        : 
		      case BraceAsteriskCloseToken       : 
		      case EllipsisToken                 : 
		      case Ellipsis3Token                : 
		      case DoubleQuestionToken           : 
		      case DoubleExclamationToken        : 
		      case AtSignToken                   : 
		      case HashToken                     : 
		      }
		   }
}
