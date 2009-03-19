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

   /*******************************************************************/
   /**                                                               **/
   /**                          STATEMENTS                           **/
   /**                                                               **/
   /*******************************************************************/

/**** first an annotated review of LangSpec $14.19 Unreachable Statements

The idea behind reachability analysis is that the programmer intends
each statement to be reachable.
As soon as a statement has been identified as unreachable for a compelling
reason, a error is raised and from then on the statement is regarded as reachable.
This prevents nnoying error message cascades.
Moreover, this frees us from the need of isReachable predicates.

Overview of $14.19:

OK(className   ) means: this has been implemented straightforwardly in the given class
OK(class.method) means: this has been implemented in the given method of the given class


Default behavior: A statement can complete normally iff reachable
-----------------------------------------------------------------
Local variable declaration statement
Empty statement    
Expression statement 
>> OK (JavaStatement)

The block that is the body of a constructor, method, or static initializer is reachable.
>> OK (ignored)

An   empty block that is not a switch block can complete normally iff it is reachable.
A nonempty block that is not a switch block can complete normally
   iff the last statement in it can complete normally.
>> OK (NestedStatement)

The first statement in a nonempty block that is not a switch block is reachable
iff the block is reachable.
>> OK (ignored)

Every other statement S in a nonempty block that is not a switch block is reachable
iff the statement preceding S can complete normally.
>> OK (ScripticCompilerPass8.processJavaStatements)
>> That is the only place where the statements are enumerated.
>> Error message for each statement immediately after a statement that does
>> not complete normally, except for case tags and default tags

A labeled statement can complete normally
iff The contained statement can complete normally. 
 || There is a reachable break statement that exits the labeled statement. 
The contained statement is reachable iff the labeled statement is reachable. 
>> OK (LabeledStatement, ScripticCompilerPass8.processJumpingStatement).
>>    see boolean isTargetOfBreak 

An if-then statement can complete normally iff it is reachable.
The then-statement is reachable iff the if-then statement is reachable. 
An if-then-else statement can complete normally
iff the then-statement can complete normally
 || the else-statement can complete normally.
The then-statement is reachable iff the if-then-else statement is reachable.
The else-statement is reachable iff the if-then-else statement is reachable.
>> OK (ifStatement)
>>
>> Seems that the definition could be improved:
>> E.g.,  while(true) if (false) break;
>> This now can complete normally; this is unwanted, and could be circumvented by:
>>
>>   A then part of an if-then statement is 'practically unreachable'
>>     if the condition is constantly false 
>>   A break statement is 'practically reachable' if it is reachable
>>     and it does not break out of a 'practically unreachable' if statement
>>   Rewriting rules stating 'unreachable' break statements using
>>                 'practically reachable' break statements
>>

A switch statement can complete normally
iff The last statement in the switch block can complete normally. 
 || The switch block is empty or contains only switch labels. 
 || There is at least one switch label after the last switch block statement group. 
 || There is a reachable break statement that exits the switch statement. 
>> Saying a CaseOrDefaultTag always completes normally does the job:
>> the switch block may be treated as any other nested statement
ERROR IN LANGUAGE SPECIFICATION ???????????? :
A switch statement can complete normally
  if it has no default tag...

A switch block is reachable iff its switch statement is reachable. 
A statement in a switch block is reachable
iff its switch statement is reachable
 &&    It bears a case or default label. 
    || There is a statement preceding it in the switch block
    && that preceding statement can complete normally
>> OK (ScripticCompilerPass8.processJavaStatements,
>>     ScripticCompilerPass8.processSwitchStatement
>>     CaseOrDefaultTag.canCompleteNormally() {return true;})

A while statement can complete normally
iff it is reachable and the condition expression is not a constantly true
 || there is a reachable break statement that exits the while statement
The contained statement is reachable
iff the while statement is reachable
 && the condition expression is not constantly false
>> OK (ScripticCompilerPass8.processWhileStatement, WhileStatement.canCompleteNormally)

A do statement can complete normally
iff    the contained statement can complete normally
    && the condition expression is not constantly true
 || there is a reachable break statement that exits the do statement. 
The contained statement is reachable iff the do statement is reachable. 
>> OK (DoStatement.canCompleteNormally)

A for statement can complete normally
iff it is reachable
    && there is a condition expression
    && the condition expression is not constantly true
 || there is a reachable break statement that exits the for statement. 
The contained statement is reachable
iff the for statement is reachable
 && (>>there is no condition expression || <<)the condition expression is not constantly false.
>> OK (ScripticCompilerPass8.processForStatement, ForStatement.canCompleteNormally)

A break, continue, return, or throw statement cannot complete normally. 
>> OK (JumpingStatement)

A synchronized statement can complete normally iff the contained statement can complete normally.
The contained statement is reachable iff the synchronized statement is reachable.
>> OK (NestedStatement)

A try statement can complete normally
iff    the  try  block can complete normally
    || any catch block can complete normally. 
 && if the try statement has a finally block, then the finally block can complete normally. 
The try block is reachable iff the try statement is reachable. 
A catch block C is reachable
iff Some expression or throw statement in the try block is reachable
      && it can throw an exception whose type is assignable to the parameter of the catch clause C.
         ( An expression is considered reachable
           iff the innermost statement containing it is reachable) 
 && There is no earlier catch block A in the try statement such that
          the type of C's parameter is the same as or a subclass of the type of A's parameter. 
If a finally block is present, it is reachable iff the try statement is reachable. 
>> OK (TryStatement.canCompleteNormally,
>>     ScripticCompilerPass7.processCatchBlock
>>     ScripticCompilerPass8.checkMethodCallThrows
>>     ScripticCompilerPass8.throwableIsThrown)
***************************************************************************************/

abstract class  JavaStatement extends LanguageConstruct {
   public boolean canCompleteNormally() {return  true;}
   public boolean hasTargetLabel     () {return false;} // possible for break or continue
   public String  getDescription     () {return getPresentation()
                                              + lineSeparator
                                              + (canCompleteNormally()? "completes normally"
                                                                      : "ABNORMAL completion");}
}


