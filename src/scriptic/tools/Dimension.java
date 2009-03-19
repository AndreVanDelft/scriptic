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

/* ***************

DimensionSignature                  = freeDimensionSignature
                                    | <n>(..dimensionReferenceSignature)
freeDimensionSignature              = 'V'<k>';'
dimensionReferenceSignature         = absoluteDimensionReferenceSignature
                                    | relativeDimensionReferenceSignature
absoluteDimensionReferenceSignature = 'D'<m>dimensionName';'
relativeDimensionReferenceSignature = 'W'<m>'V'<k>';'

 variables and parameters:     2D1physics/dimensions.distance;D-2physics/dimensions.time;
 method dimension signatures: (2D1physics/dimensions.distance;D-2physics/dimensions.time;0
                               2D1physics/dimensions.distance;D-1physics/dimensions.time;V)1W-1V0

                         for: int/dimension(d) f (int*accelleration a, AClass b,
                                                  double*speed c,      int*dimension d)

Source code lines for dimensions:

 270 JavaParser (of 3500, i.e. 7.7%)
 534 Dimension
  60 CompilerEnv
  75 ClassType
  25 JavaConstructs
  50 ClassFileInfo
  16 JavaParseTreeEnumerator
 130 ScripticParseTreeEnumeratorWithContext
 277 ExpressionChecker
 200 Pass 3
 115 Pass 5
  69 Pass 6
 100 Lowlevel
  10 Various
------------
1931 Total

*/
package scriptic.tools;
import java.util.HashMap;

abstract class Dimension extends Field {
  ClassType owner;
  DimensionDeclaration sourceDeclaration;
  static String errorSignature = "errorSignature";

   public ClassType ownerClass(CompilerEnvironment env) {return owner;}
   public LanguageConstruct source() {return sourceDeclaration;}
   public String getConstructName () {return "dimension";}

  /* eat a number; if it starts with '0' then only that digit */
  private static int eatNumber (String s, int pos[]) {
     int sign   = 1;
     int result = 0;
     int i      = 0;
     if (s.charAt(pos[0])=='-') {
       pos[0]++;
       sign = -1;
     }
     for (; pos[0]<s.length(); i++) {
       char c=s.charAt(pos[0]);
       if (! Character.isDigit(c)) {
         break;
       }
       result = 10*result + (c-'0');
       pos[0]++;
       if (i==0 && result==0) return 0;
     }
     if (i==0) throw new RuntimeException();
     return result*sign;
  }
  private static char eatChar (String s, int pos[]) {
     if (pos[0]>=s.length()) throw new RuntimeException();
     return s.charAt(pos[0]++);
  }
  private static String eatUntil (String s, int pos[], char endChar) {
     StringBuffer result = new StringBuffer();
     while (pos[0]<s.length()) {
       char c=s.charAt(pos[0]);
       if (c==endChar) {
         return result.toString();
       }
       result.append (c);
       pos[0]++;
     }
     throw new RuntimeException();
  }
  
  static String eatReference (String s, int pos[], char type[], int power[], int index[]) {
     char c = type[0] = eatChar (s, pos); // 'D' or 'W' ...
     if (c=='D') {
           power[0]      = eatNumber (s, pos);
           String result = eatUntil (s, pos, ';');
           pos[0]++; // eat ';'
           return result;
     }
     else if (c=='W') {
           power[0] = eatNumber (s, pos); 
           char d = eatChar (s, pos); // 'V'
           if (d != 'V') throw new RuntimeException ();
           index[0] = eatNumber (s, pos);
           pos[0]++; // eat ';'
           return null;
    }
    else {
          throw new RuntimeException ();
    }
  }
  /** From a method signature, eat the signature of a single parameter
   * or of the return type
   */
  static String eatParameterSignature (String s, int pos[]) {
     int first = pos[0];
     if (s.charAt(first)=='V') {
        pos[0]++;
        int n = eatNumber (s, pos);
        pos[0]++; // eat ';'
        return "V"+n+";";
     }
     int n = eatNumber (s, pos);
     if (n==0) return null;
     int last  = first;
     for (int i=0; i<n; i++) {
        last = s.indexOf (';', last) + 1;
     }
     pos[0] = last;
     return s.substring (first, last);
  }

  // answer whether the signatures are equal, or whether one has an error
  static boolean equal (String s1, String s2) {
    if (s1==errorSignature) return true;
    if (s2==errorSignature) return true;
    if    (s1==null || s1.length()==0)
    return s2==null || s2.length()==0;
    return s1.equals  (s2);
  }

  // answer the dimension string of the n-th power of s
  static String power (String s, int p) {
    if (s==errorSignature) return errorSignature;
    if (p==1) return s;
    try {
     StringBuffer result = new StringBuffer();
     char type [] = new char[1];
     int  pos  [] = new  int[1];
     int  power[] = new  int[1];
     int  index[] = new  int[1];
     int n = eatNumber (s, pos); // <n>(..dimensionReference)
     result.append (n);
     for (int i=0; i<n; i++) {
        String name = eatReference (s, pos, type, power, index);
        char c = type[0]; // 'D' or 'W' ...
        result.append (c);
             if (c=='D') {result.append (p*power[0]).append (name).                  append (';');}
        else if (c=='W') {result.append (p*power[0]).append ('V' ).append (index[0]).append (';');}
        else             {throw new RuntimeException ();}
     }
     return result.toString();
    }
    catch (RuntimeException e) {
          throw new RuntimeException ("INTERNAL ERROR: illegal dimension signature: "+s);
    }
  }
  // answer the dimension string of the division of 1 by s
  static String invert (String s) {return power(s,-1);}

  // answer the dimension string of the division of s1 by s2
  static String division (String s1, String s2) {
     if (s2==null || s2.length()==0) return s1;
     return product (s1, invert(s2));
  }
  // answer the dimension string of the product of s1 and s2
  static String product (String s1, String s2) {
   try {
     if (s1==errorSignature
     ||  s2==errorSignature) return errorSignature;
     if (s1==null || s1.length()==0) return s2;
     if (s2==null || s2.length()==0) return s1;
     StringBuffer result = new StringBuffer();

     // arrays for reference passing values
     char type1 [] = new char[1];
     char type2 [] = new char[1];
     int  pos1  [] = new  int[1];
     int  pos2  [] = new  int[1];
     int  power1[] = new  int[1];
     int  power2[] = new  int[1];
     int  index1[] = new  int[1];
     int  index2[] = new  int[1];
     int n1        = eatNumber (s1, pos1); // <n>(..dimensionReference)
     int n2        = eatNumber (s2, pos2); // <n>(..dimensionReference)
     int n         = 0;

     String name1 = eatReference (s1, pos1, type1, power1, index1);
     String name2 = eatReference (s2, pos2, type2, power2, index2);

     boolean done1 = false;
     boolean done2 = false;
     for (;;) {
        // invariant: type1&type2, power1&power2 etc denote lexically ordered
        // components that still need to be reflected in the result buffer.
        //

        boolean eat1   = false;
        boolean eat2   = false;
        boolean spawn1 = false;
        boolean spawn2 = false;
        int power = 0;

        if (done1) {
           if (done2) break;
           eat2  = spawn2 = true;
           power = power2[0];
        }
        else if (done2) {
           eat1  = spawn1 = true;
           power = power1[0];
        }
        else if (type1[0]=='D') {
          if (type2[0]=='D') {
             int cmp=name1.compareTo(name2);
                  if (cmp<0) {eat1  = spawn1 = true; power = power1[0];}
             else if (cmp>0) {eat2  = spawn2 = true; power = power2[0];}
             else            {eat1  = eat2   = true; power = power1[0]+power2[0];
                              if (power!= 0) spawn1 = true;                      }
          }
          else { // type2[0]=='W'
             eat1  = spawn1 = true; power = power1[0];
          }
        } else { // type1[0]=='W'
             if (type2[0]=='D') {
               eat2  = spawn2 = true; power = power2[0];
             }
             else { // both 'W'
                    if (index1[0]<index2[0]) {eat1  = spawn1 = true; power = power1[0];}
               else if (index1[0]>index2[0]) {eat2  = spawn2 = true; power = power2[0];}
               else {                         eat1  = eat2   = true; power = power1[0]+power2[0];
                                              if (power!= 0) spawn1 = true;                     }
             }
        }
        if (spawn1) {
          n++;
          if (type1[0]=='D') result.append ('D').append (power).append (name1)                  .append(';');
          else               result.append ('W').append (power).append ('V')  .append(index1[0]).append (';');
        }
        else if (spawn2) {
          n++;
          if (type2[0]=='D') result.append ('D').append (power).append (name2)                   .append(';');
          else               result.append ('W').append (power).append ('V')  .append(index2[0]).append (';');
        }
        if (eat1) {if (--n1==0) done1=true; else name1=eatReference(s1,pos1,type1,power1,index1);}
        if (eat2) {if (--n2==0) done2=true; else name2=eatReference(s2,pos2,type2,power2,index2);}
     }
     if (n==0) return null;
     return n+result.toString();
  }
  catch (RuntimeException e) {
        throw new RuntimeException ("INTERNAL ERROR: illegal dimension signature: "+s1+" or "+s2);
  }
 }

 /** resolve a signature with possible references to already bound variable dimensions.
  * Example:
  *  signature = 2W2V1;W1V2;
  *  bindings  = {V1>>1D1T.Mass;, V2>>2D1T.Distance;D-1T.Time;}
  *  result    = 3D1T.Distance;D2T.Mass;D-1T.Time;
  */
 static String resolve (String s1, HashMap<String,String> bindings) {
     if (s1==null) return null;
     // arrays for reference passing values
     char type1 [] = new char[1];
   //char type2 [] = new char[1];
     int  pos1  [] = new  int[1];
   //int  pos2  [] = new  int[1];
     int  power1[] = new  int[1];
   //int  power2[] = new  int[1];
     int  index1[] = new  int[1];
   //int  index2[] = new  int[1];
     int n1        = eatNumber (s1, pos1); // <n>(..dimensionReference)

     String result = null;

     for (int i=0; i<n1; i++) {
       String name1  = eatReference (s1, pos1, type1, power1, index1);
       if (type1[0]=='D') {
          result = product (result, "1D"+power1[0]+name1+';');
       }
       else { // 'W'
         String key   = ("V"+index1[0])+';';
         String s2    = (String) bindings.get (key);
         //System.out.println ("resolve: W"+power1[0]+key+" > "+s2);
         result       = product (result, power(s2, power1[0]));
       }
     }
     //System.out.println ("resolve: "+s1+" >> "+result);
     return result;
 }
 static String getPresentation (String s) {
    if (s==errorSignature) return errorSignature;
    if (s==null)           return "none";
    try {
     StringBuffer result = new StringBuffer();
     char type [] = new char[1];
     int  pos  [] = new  int[1];
     int  power[] = new  int[1];
     int  index[] = new  int[1];
     int n = eatNumber (s, pos); // <n>(..dimensionReference)
     for (int i=0; i<n; i++) {
        String name = eatReference (s, pos, type, power, index);
        char c = type[0]; // 'D' or 'W' ...
             if (i > 0)  {result.append ('*');}
             if (c=='D') {result.append (name.replace('/', '.'));}
        else if (c=='W') {result.append ("variable").append ("index[0]");}
        else             {throw new RuntimeException ();}
        if (power[0]!=1) {result.append ('^').append(power[0]);}
     }
     return result.toString();
    }
    catch (RuntimeException e) {
          throw new RuntimeException ("INTERNAL ERROR: illegal dimension signature: "+s);
    }
  }
}

