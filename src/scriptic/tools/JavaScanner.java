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

import scriptic.tokens.JavaRepresentations;
import static scriptic.tokens.Representations.*;

public class JavaScanner extends Scanner implements scriptic.tokens.JavaTokens,
		JavaRepresentations {

	public String quotedTokenRepresentation(int token) {
		if (token < FirstToken)
			return super.tokenRepresentation(token);
		else
			return super.quotedTokenRepresentation(token);
	}

	public boolean seenDeprecated;
	private boolean seeDeprecated;

	/* Public methods */
	public int next() {
		try {
			seenDeprecated = seeDeprecated;
			seeDeprecated = false;
			skipWhitespace();
			tokenStartPosition = charPosition;
			if (atEnd())
				return returnToken(EofToken);

			/*
			 * Remember that charBuffer is guaranteed to be large enough for
			 * charBuffer [charPosition + 1]
			 */

			char ch = charBuffer[charPosition];
			if (Scanner.isIdentifierStartChar(ch))
				return scanIdentifier();
			if (Scanner.isDigit(ch))
				return scanNumber();
			if (ch == '.' && Scanner.isDigit(charBuffer[charPosition + 1]))
				return scanFloatNumber();
			if (ch == '\'')
				return scanCharLiteral();
			if (ch == '\"')
				return scanStringLiteral();

			if (ch == '/') {
				char ch2 = charBuffer[charPosition + 1];
				if (ch2 == '*') {
					charPosition += 2;
					boolean isDocComment = charBuffer[charPosition] == '*';
					String commentString = skipTo("*/");
					seeDeprecated = isDocComment
							&& commentString.indexOf("@deprecated") >= 0;
					return next();
				} else if (ch2 == '/') {
					charPosition += 2;
					skipLine();
					return next();
				}
			}

			return scanSpecialSymbol();
		} finally {
			// System.out.println
			// ("scanner.next: "+TokenRepresentations[token]+" "+TokenNames[token]);
			// if (token==ScripticTokens.DoubleQuestionToken
			// || token==BraceCloseToken) new Exception().printStackTrace();
		}
	}

	protected int scanIdentifier() {
		charPosition++;
		while (charPosition < charBuffer.length
				&& Scanner.isIdentifierChar(charBuffer[charPosition]))
			charPosition++;

		tokenValue = new String(charBuffer, tokenStartPosition, charPosition
				- tokenStartPosition).intern();
		setTokenEndPosition(charPosition);

		Integer keywordCode = (Integer) TokenCodes.get(tokenValue);
		if (keywordCode != null)
			token = keywordCode.intValue();
		else
			token = IdentifierToken;
		return token;
	}

	protected int scanNumber() {

		/* When this is called, isDigit(firstCh) is known to be true */
		char firstCh = charBuffer[charPosition];
		char secondCh;
		char ch = ' ';
		/* Check for Hex Literal */
		if (firstCh == '0'
				&& charPosition + 2 < charBuffer.length
				&& ((secondCh = charBuffer[charPosition + 1]) == 'x' || (secondCh == 'X'))
				&& (Character.digit(charBuffer[charPosition + 2], 16) >= 0)) {

			/* Found Hex Literal */
			charPosition += 2; /* point at char beyond the 'x' */
			int hexDigits[] = new int[18];
			int noOfDigits = scanDigits(hexDigits, 16);

			/* Suffix 'L' denotes long, otherwise it's an int */
			boolean isLong = false;
			int allowedDigits = 8;
			if (charPosition < charBuffer.length
					&& ((ch = charBuffer[charPosition]) == 'l' || ch == 'L')) {
				isLong = true;
				allowedDigits = 16;
				charPosition++;
			}

			/*
			 * Check for too many significant digits; if so, return scanner
			 * error
			 */
			if (noOfDigits > allowedDigits) {
				/* throw new IllegalHexLiteral (...); */
				return returnToken(ErrorToken, "Hex literal too long");
			}

			return constructValue(hexDigits, noOfDigits, 4, isLong);
		}
		/* END OF Hex Literal processing */

		/*
		 * Check for Octal or Floating-point literal. If the first char is zero,
		 * it's one of these; otherwise it's a decimal literal. Note that the
		 * literal '0238' is a scanner error (octal containing '8') but that
		 * '0238.0' or '0238E+1' (but not '0238f') are all correct.
		 */

		int startPosition = charPosition;
		int digits[] = new int[24]; /*
									 * The longest allowed octal literal (017
									 * 7777 7777 7777 7777 7777L) has 22
									 * significant digits. Floating-point
									 * literals can be much longer but are
									 * treated separately.
									 */
		int noOfDigits = scanDigits(digits, 10);
		boolean isLong = false;

		/* Check suffixes */
		if (charPosition < charBuffer.length) {
			ch = charBuffer[charPosition];

			/* Floating-point */
			if (ch == '.' || ch == 'e' || ch == 'E') {
				charPosition = startPosition;
				return scanFloatNumber();
			}

			/* Integer type suffix 'L'/'l' denotes long */
			if (ch == 'l' || ch == 'L') {
				isLong = true;
				charPosition++;
			}
		}

		/* Check for Octal */
		if (firstCh == '0') {
			int allowedDigits = 11; /* Longest int octal */
			int leadingDigit = 3; /* is 0377 7777 7777 */

			if (isLong) {
				allowedDigits = 22; /* Longest long octal */
				leadingDigit = 1; /* is 017 7777 7777 7777 7777 7777L */
			}

			/*
			 * Check for too many significant digits; if so, return scanner
			 * error
			 */
			if (noOfDigits >= allowedDigits) {
				if ((noOfDigits > allowedDigits) || (digits[0] > leadingDigit)) {

					/* throw new IllegalOctLiteral (...); */
					return returnToken(ErrorToken, "Octal literal too long");
				}
			}

			/* Check for non-octal digits */
			for (int i = noOfDigits - 1; i >= 0; i--) {
				if (digits[i] > 7)
					return returnToken(ErrorToken, "Bad digit in octal literal");
			}

			return constructValue(digits, noOfDigits, 3, isLong);
		}
		/* END OF Octal Literal processing */

		/* Check for value too large based on individual digits */
		int allowedDigits = 10; /* Longest int decimal */
		int leadingDigit = 2; /* is 2 147 483 647 */
		if (isLong) {
			allowedDigits = 19; /* Longest long decimal */
			leadingDigit = 9; /* is 9 223 372 036 854 775 808L */
		}
		if (noOfDigits >= allowedDigits)
			if ((noOfDigits > allowedDigits) || (digits[0] > leadingDigit))
				return returnToken(ErrorToken, "Decimal literal too large");

		/* Construct Decimal literal value */
		if (isLong) {

			long result = 0;
			long multiplier = 1;
			long maximum = 0x7FFFFFFFFFFFFFFFL;
			long addend;
			for (int i = noOfDigits - 1; i >= 0; i--) {
				addend = (long) digits[i] * multiplier;
				if (result > maximum - addend)
					return returnToken(ErrorToken,
							"Long decimal literal too large");

				result += addend;
				multiplier *= 10;
			}
			return returnToken(LongLiteralToken, result);
		} else {

			int result = 0;
			int multiplier = 1;
			int maximum = 0x7FFFFFFF;
			int addend;
			for (int i = noOfDigits - 1; i >= 0; i--) {
				addend = digits[i] * multiplier;
				if (result > maximum - addend)
					return returnToken(ErrorToken, "Decimal literal too large");

				result += addend;
				multiplier *= 10;
			}
			return returnToken(IntegerLiteralToken, result);
		}
	}

	/*
	 * Scan digits with the given radix. Collect digit values in the array.
	 * Answer the number of significant digits collected (to the maximum of the
	 * length of the array). This method will return zero if all the digits are
	 * zero.
	 */
	protected int scanDigits(int digits[], int radix) {
		int digit;
		int noOfDigits = 0;
		boolean skippingZeroes = true;

		while (charPosition < charBuffer.length
				&& (digit = Character.digit(charBuffer[charPosition], radix)) >= 0) {
			charPosition++;
			if (digit > 0)
				skippingZeroes = false;
			if (!skippingZeroes) {
				if (noOfDigits < digits.length)
					digits[noOfDigits++] = digit;
			}
		}
		return noOfDigits;
	}

	/* Construct value from hex or octal digits. Answer scanner token. */
	protected int constructValue(int digits[], int noOfDigits, int shiftSize,
			boolean isLong) {
		if (isLong) {
			/* long */
			long result = 0;
			int shiftCount = 0;
			for (int i = noOfDigits - 1; i >= 0; i--) {
				result |= (long) digits[i] << shiftCount;
				shiftCount += shiftSize;
			}
			return returnToken(LongLiteralToken, result);
		} else {
			/* int */
			int result = 0;
			int shiftCount = 0;
			for (int i = noOfDigits - 1; i >= 0; i--) {
				result |= digits[i] << shiftCount;
				shiftCount += shiftSize;
			}
			return returnToken(IntegerLiteralToken, result);
		}
	}

	protected int scanFloatNumber() {

		/*
		 * When this is called, there's known to be either a '.' or an 'e'/'E';
		 * if the pattern starts with '.', it's known to be followed by a digit.
		 */

		char ch = ' ';

		/* Scan off leading digits (if any) */
		while (charPosition < charBuffer.length
				&& Scanner.isDigit(ch = charBuffer[charPosition]))
			charPosition++;

		/* Check for period */
		if (charPosition < charBuffer.length && ch == '.') {
			charPosition++;
			/* Scan off decimal digits (if any) */
			while (charPosition < charBuffer.length
					&& Scanner.isDigit(ch = charBuffer[charPosition]))
				charPosition++;
		}

		/* Check for exponent */
		if (charPosition < charBuffer.length && (ch == 'e' || ch == 'E')) {
			/*
			 * Java Spec says it's OK for the 'E' NOT to be followed by any
			 * exponent (the SignedInteger is 'opt')
			 */

			charPosition++;

			if (charPosition + 1 < charBuffer.length
					&& ((ch = charBuffer[charPosition]) == '+' || ch == '-')
					&& Scanner.isDigit(charBuffer[charPosition + 1]))
				charPosition++;

			/* Scan off exponent digits (if any...) */
			while (charPosition < charBuffer.length
					&& Scanner.isDigit(ch = charBuffer[charPosition]))
				charPosition++;
		}

		/* Check for suffix */
		boolean isDouble = true;
		if (charPosition < charBuffer.length) {
			ch = charBuffer[charPosition];
			if (ch == 'f' || ch == 'F') {
				isDouble = false;
				charPosition++;
			}
			if (ch == 'd' || ch == 'D') {
				charPosition++;
			}
		}

		String floatString = new String(charBuffer, tokenStartPosition,
				charPosition - tokenStartPosition).intern();

		if (isDouble) {
			Double result;
			try {
				result = Double.valueOf(floatString);
			} catch (NumberFormatException e) {
				return returnToken(ErrorToken, "Double literal format error");
			}
			/*
			 * THIS IS NOT ENTIRELY CORRECT. Experimentation has revealed that
			 * Double.valueOf() does not quite behave as mandated by the Java
			 * Spec in boundary cases. E.g. if the string passsed in is longer
			 * than about 64 characters, it quietly ignores the rest; it also
			 * quietly returns infinity for the literal '1e+500'.
			 */
			return returnToken(DoubleLiteralToken, result);
		} else {
			Float result;
			try {
				result = Float.valueOf(floatString);
			} catch (NumberFormatException e) {
				return returnToken(ErrorToken, "Float literal format error");
			}
			return returnToken(FloatLiteralToken, result);
		}
	}

	protected int scanCharLiteral() {
		char ch, result;

		charPosition++;
		if (charPosition >= charBuffer.length)
			return returnToken(ErrorToken, "Unfinished character literal");

		ch = charBuffer[charPosition++];
		if (charPosition >= charBuffer.length)
			return returnToken(ErrorToken, "Unfinished character literal");

		if (ch == '\r' || ch == '\n') {
			charPosition--;
			return returnToken(ErrorToken, "Unfinished character literal");
		}

		if (ch == '\'') {
			return returnToken(ErrorToken, "Bad character literal");
		}

		if (ch == '\\') {
			ch = charBuffer[charPosition++];
			if (charPosition >= charBuffer.length)
				return returnToken(ErrorToken,
						"Unfinished string/character literal");
			switch (ch) {
			case 'b': {
				result = '\b';
				break;
			} /* \u0008: backspace BS */
			case 't': {
				result = '\t';
				break;
			} /* : horizontal tab HT */
			case 'n': {
				result = '\n';
				break;
			} /* : linefeed LF */
			case 'f': {
				result = '\f';
				break;
			} /* : form feed FF */
			case 'r': {
				result = '\r';
				break;
			} /* : carriage return CR */
			case '\"': {
				result = '\"';
				break;
			} /* \u0022: double quote " */
			case '\'': {
				result = '\'';
				break;
			} /* \u0027: single quote ’ */
			case '\\': {
				result = '\\';
				break;
			} /* \u005c: backslash */
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7': {
				char ch1 = ch;
				/* Second digit */
				char ch2 = charBuffer[charPosition];
				if (ch2 >= '0' && ch2 <= '7') {
					charPosition++;
					if (charPosition >= charBuffer.length)
						return returnToken(ErrorToken,
								"Unfinished string/character literal");
					/* Third digit */
					char ch3 = charBuffer[charPosition];
					if (ch3 >= '0' && ch3 <= '7') {
						charPosition++;
						if (charPosition >= charBuffer.length)
							return returnToken(ErrorToken,
									"Unfinished string/character literal");
						if (ch1 > '3')
							return returnToken(ErrorToken,
									"Bad octal character escape");
						result = (char) (Character.digit(ch1, 10) * 64
								+ Character.digit(ch2, 10) * 8 + Character
								.digit(ch3, 10));
					} else {
						result = (char) (Character.digit(ch1, 10) * 8 + Character
								.digit(ch2, 10));
					}
				} else {
					result = (char) Character.digit(ch1, 10);
				}
				break;
			}
			default: {
				if (ch == '\r' || ch == '\n') {
					charPosition--;
					return returnToken(ErrorToken,
							"Unfinished string/character literal");
				}
				if (charBuffer[charPosition] == '\'')
					charPosition++;
				return returnToken(ErrorToken, "Bad character escape");
			}
			}

		} else
			result = ch;

		ch = charBuffer[charPosition++];
		if (ch == '\r' || ch == '\n')
			charPosition--;
		if (ch != '\'')
			return returnToken(ErrorToken, "Unfinished character literal");

		return returnToken(CharacterLiteralToken, result);
	}

	protected int scanStringLiteral() {
		StringBuffer result = new StringBuffer(128);
		char ch = ' ';

		charPosition++;
		int startPosition = charPosition;

		while (true) {
			while (charPosition < charBuffer.length
					&& (ch = charBuffer[charPosition]) != '\"' && ch != '\\'
					&& ch != '\r' && ch != '\n')
				charPosition++;

			if (ch == '\"') {
				result.append(charBuffer, startPosition, charPosition
						- startPosition);
				charPosition++;
				return returnToken(StringLiteralToken, result.toString());
			}

			if (charPosition >= charBuffer.length || ch == '\r' || ch == '\n')
				return returnToken(ErrorToken, "Unfinished String literal");

			/* Process character escape */
			result.append(charBuffer, startPosition, charPosition
					- startPosition);

			charPosition++;
			if (charPosition >= charBuffer.length)
				return returnToken(ErrorToken,
						"Unfinished string/character literal");
			ch = charBuffer[charPosition++];
			if (charPosition >= charBuffer.length)
				return returnToken(ErrorToken,
						"Unfinished string/character literal");

			char escapeCh;
			switch (ch) {
			case 'b': {
				escapeCh = '\b';
				break;
			} /* \u0008: backspace BS */
			case 't': {
				escapeCh = '\t';
				break;
			} /* : horizontal tab HT */
			case 'n': {
				escapeCh = '\n';
				break;
			} /* : linefeed LF */
			case 'f': {
				escapeCh = '\f';
				break;
			} /* : form feed FF */
			case 'r': {
				escapeCh = '\r';
				break;
			} /* : carriage return CR */
			case '\"': {
				escapeCh = '\"';
				break;
			} /* \u0022: double quote " */
			case '\'': {
				escapeCh = '\'';
				break;
			} /* \u0027: single quote ’ */
			case '\\': {
				escapeCh = '\\';
				break;
			} /* \u005c: backslash */
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7': {
				char ch1 = ch;
				/* Second digit */
				char ch2 = charBuffer[charPosition];
				if (ch2 >= '0' && ch2 <= '7') {
					charPosition++;
					if (charPosition >= charBuffer.length)
						return returnToken(ErrorToken,
								"Unfinished string/character literal");
					/* Third digit */
					char ch3 = charBuffer[charPosition];
					if (ch3 >= '0' && ch3 <= '7') {
						charPosition++;
						if (charPosition >= charBuffer.length)
							return returnToken(ErrorToken,
									"Unfinished string/character literal");
						if (ch1 > '3')
							return returnToken(ErrorToken,
									"Bad octal character escape");
						escapeCh = (char) (Character.digit(ch1, 10) * 64
								+ Character.digit(ch2, 10) * 8 + Character
								.digit(ch3, 10));
					} else {
						escapeCh = (char) (Character.digit(ch1, 10) * 8 + Character
								.digit(ch2, 10));
					}
				} else {
					escapeCh = (char) Character.digit(ch1, 10);
				}
				break;
			}
			default: {
				if (ch == '\r' || ch == '\n') {
					charPosition--;
					return returnToken(ErrorToken,
							"Unfinished string/character literal");
				}
				if (charBuffer[charPosition] == '\"')
					charPosition++;
				return returnToken(ErrorToken, "Bad character escape");
			}
			}

			result.append(escapeCh);
			startPosition = charPosition;
		}
	}

	protected int matchSpecialSymbol() {
		char ch = charBuffer[charPosition++];

		switch (ch) {
		case '(':
			return ParenthesisOpenToken;
		case ')':
			return ParenthesisCloseToken;
		case '{':
			return BraceOpenToken;
		case '}':
			return BraceCloseToken;
		case '[':
			return BracketOpenToken;
		case ']':
			return BracketCloseToken;
		case ';':
			return SemicolonToken;
		case ',':
			return CommaToken;
		case '.':
			return PeriodToken;
		case '~':
			return TildeToken;
		case '?':
			return QuestionToken;
		case ':':
			return ColonToken;

		case '=': { /* = == */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return EqualsToken;

			charPosition--;
			return AssignToken;
		}

		case '>': { /* > >= >> >>> >>= >>>= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return GreaterOrEqualToken;

			if (ch == '>') { /* >> >>> >>= >>>= */
				ch = charBuffer[charPosition++];
				if (ch == '=')
					return RightShiftAssignToken;

				if (ch == '>') { /* >>> >>>= */
					ch = charBuffer[charPosition++];
					if (ch == '=')
						return UnsignedRightShiftAssignToken;
					charPosition--;
					return UnsignedRightShiftToken;
				}
				charPosition--;
				return RightShiftToken;
			}
			charPosition--;
			return GreaterThanToken;
		}

		case '<': { /* < <= << <<= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return LessOrEqualToken;

			if (ch == '<') { /* << <<= */
				ch = charBuffer[charPosition++];
				if (ch == '=')
					return LeftShiftAssignToken;

				charPosition--;
				return LeftShiftToken;
			}
			charPosition--;
			return LessThanToken;
		}

		case '!': { /* ! != */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return NotEqualToken;

			charPosition--;
			return ExclamationToken;
		}

		case '&': { /* & && &= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return AmpersandAssignToken;
			if (ch == '&')
				return BooleanAndToken;

			charPosition--;
			return AmpersandToken;
		}

		case '|': { /* | || |= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return VerticalBarAssignToken;
			if (ch == '|')
				return BooleanOrToken;

			charPosition--;
			return VerticalBarToken;
		}

		case '+': { /* + += ++ */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return PlusAssignToken;

			if (ch == '+')
				return IncrementToken;

			charPosition--;
			return PlusToken;
		}

		case '-': { /* - -= -- */
			ch = charBuffer[charPosition++];

			if (ch == '=')
				return MinusAssignToken;

			if (ch == '-')
				return DecrementToken;

			charPosition--;
			return MinusToken;
		}

		case '*': { /*  *= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return AsteriskAssignToken;

			charPosition--;
			return AsteriskToken;
		}

		case '/': { /* / /= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return SlashAssignToken;

			charPosition--;
			return SlashToken;
		}

		case '%': { /* % %= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return PercentAssignToken;

			charPosition--;
			return PercentToken;
		}

		case '^': { /* ^ ^= */
			ch = charBuffer[charPosition++];
			if (ch == '=')
				return CaretAssignToken;

			charPosition--;
			return CaretToken;
		}

			/* Special case, for now */
		case '\0':
			return EofToken;
		}
		charPosition--;
		return -1;
	}

	protected int scanSpecialSymbol() {
		int tokenCode = matchSpecialSymbol();
		if (tokenCode < 0) {
			charPosition++;
			return returnToken(ErrorToken, "Bad character");
		} else {
			return returnToken(tokenCode);
		}
	}

	/*
	 * Skip to the next occurrence of the given sequence of characters. Assumes
	 * that the pattern does NOT contain any line terminator characters. Returns
	 * the String with the skipped characters
	 */

	protected String skipTo(String pattern) {
		int patternSize = pattern.length();
		if (patternSize == 0)
			return "";
		StringBuffer resultBuf = new StringBuffer();
		char startch = pattern.charAt(0);
		char ch = ' ';

		findInitialChar: while (true) {

			/* Match first char */
			while (charPosition < charBuffer.length
					&& (ch = charBuffer[charPosition]) != startch && ch != '\r'
					&& ch != '\n') {
				charPosition++;
				resultBuf.append(ch);
			}
			if (charPosition >= charBuffer.length)
				return resultBuf.toString();

			charPosition++;
			resultBuf.append(ch);

			if (ch != startch) {
				/* Process line terminator */
				if (ch == '\r' && charPosition < charBuffer.length
						&& charBuffer[charPosition] == '\n') {
					charPosition++;
					resultBuf.append(ch);
				}
				/* Track line numbers */
				maintainLineNumberPositionArray(charPosition);
				continue findInitialChar;
			}

			if (patternSize == 1)
				return resultBuf.toString();

			/* Match rest of pattern, if it fits in the buffer */
			if (charPosition + patternSize - 2 >= charBuffer.length) {
				charPosition = charBuffer.length;
				return resultBuf.toString();
			}

			for (int i = 1; i < patternSize; i++) {
				if (charBuffer[charPosition + i - 1] != pattern.charAt(i))
					continue findInitialChar;
			}

			/* Matched! Set position beyond the pattern and return */
			charPosition += patternSize - 1;
			return resultBuf.toString().substring(0, resultBuf.length() - 1);
		}
	}

	/* Skip to the beginning of the next line */

	protected void skipLine() {
		char ch = ' ';

		/* Find the end-of-line marker */
		while (charPosition < charBuffer.length
				&& (ch = charBuffer[charPosition]) != '\r' && ch != '\n')
			charPosition++;
		if (charPosition >= charBuffer.length)
			return;

		/* Skip the end-of-line marker */
		charPosition++;

		/* Check for '\r\n' */
		if (ch == '\r' && charPosition < charBuffer.length
				&& charBuffer[charPosition] == '\n')
			charPosition++;

		/* Track line numbers */
		maintainLineNumberPositionArray(charPosition);
	}

	/* Skip to the next non-whitespace character */

	protected void skipWhitespace() {
		char ch;

		while (charPosition < charBuffer.length
				&& (Scanner.isSpace(ch = charBuffer[charPosition]))) {

			charPosition++;

			if (ch == '\r' || ch == '\n') {

				/* Check for '\r\n' line terminator */
				if (ch == '\r' && charPosition < charBuffer.length
						&& charBuffer[charPosition] == '\n')
					charPosition++;

				/* Track line numbers */
				maintainLineNumberPositionArray(charPosition);
			}
		}
	}

	protected int returnToken(int theReturnToken) {
		return returnToken(theReturnToken, new String(), charPosition);
	}

	protected int returnToken(int theReturnToken, Object returnTokenValue) {
		return returnToken(theReturnToken, returnTokenValue, charPosition);
	}

	protected int returnToken(int theReturnToken, Object returnTokenValue,
			int returnTokenEndPosition) {
		token = theReturnToken;
		tokenValue = returnTokenValue;
		setTokenEndPosition(returnTokenEndPosition);
		return token;
	}

}
