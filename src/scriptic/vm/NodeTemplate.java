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
/**
 * Node Operator Names:
 *  Zero, One, Neutral, OptionalExit, Exit
 *  CommBody, Channel, SendChannel, ReceiveChannel
 *  CommCall, Send, Receive
 *  CodeFrag, EmptyCodeFrag
 *  Pre, Post, Activation, Deactivation
 *  ScriptCall, Script, Communication
 *  Main, Launched, Communication
 *  Not, ReactiveNotNode, If
 *  Next, Select
 *  While
 *  For
 *  Or, Seq, NotSeq, Break
 *  Parallel
 *  Root, Par, ParOr, ParBreak, ParOr2, ParAnd2
 *
 * NodeTemplate flavors:
 *  Zero, One, Neutral, OptionalExit, Exit
 *  CommunicationBody, Channel, SendChannel, ReceiveChannel
 *  Request
 *      Partner [communication]
 *          CommunicationCall, Send, Receive
 *      CodeFragment
 *          EmptyCodeFragment
 *  Branch
 *      Pre, Post, ActivationCode, DeactivationCode
 *      ScriptCall, SendCall, ReceiveCall, Script, Communication
 *      Main, Launched,
 *      Not, ReactiveNot, Reactive, If
 *      Next, Select
 *      While, For
 *      Scripting [iterationPass]
 *          Or, Seq, NotSeq, Break
 *          Parallel
 *              Root, Par, ParOr, ParBreak, ParOr2, ParAnd2
 *      
 */


package scriptic.vm;

import java.lang.reflect.Method;
import java.util.ArrayList;

import scriptic.tokens.Representations;
import scriptic.tokens.ScripticParseTreeCodes;

/**
 * This class serves representing templates for nodes in the runtime tree.
 * Most features are implemented in subclasses.
 *
 * @version     1.0, 11/30/95
 * @author  Andre van Delft
 */

final public class NodeTemplate implements ScripticParseTreeCodes,
                                           NodeTemplateInterface {

    void traceOutput(String       s) {Node.traceOutput(s);}
    void traceOutput(StringBuffer s) {Node.traceOutput(s);}
    void debugOutput(String       s) {Node.debugOutput(s);}
    void trace()               {traceOutput(toString());}

    static final int lengthCol0 = 50;
    StringBuffer infoNested () {return infoNested(0);}
    StringBuffer info (int nestingLevel) {
        int posToken = nestingLevel*2;
        if (posToken >= lengthCol0) posToken = lengthCol0;
            StringBuffer result = new StringBuffer();
            String str = getName();
        int i;
        for (i=0; i<lengthCol0; i++) {
            if      (i <posToken)      result.append(i%4==2? ' ': ' ');
            else if (i==posToken)      result.append(str);
            else if (i > posToken
                           + str.length()) result.append(' ');
        }
        result.append(codeIndex)
                  .append(isIteration()? "..": "")
                  .append("["+indexAsChild+"]")
                  .append('\n');
            return result;
    }
    StringBuffer infoNested (int nestingLevel)
    {
            StringBuffer result = info(nestingLevel);
        if (childs != null) for (int i=0; i<childs.length; i++)
              result.append(childs[i].infoNested(nestingLevel+1));
            return result;
    }   


    static NodeTemplate         launchTemplate;
    static NodeTemplate           rootTemplate;
    static NodeTemplate       commRootTemplate;
    static NodeTemplate rootScriptCallTemplate;

    static boolean initialized;
    static void initialize() {
           if (initialized) return;
           initialized = true;

                  launchTemplate = new NodeTemplate(LaunchedExpressionCode);
                rootTemplate = new NodeTemplate(              RootCode);
            commRootTemplate = new NodeTemplate(          CommRootCode);
      rootScriptCallTemplate = new NodeTemplate(    RootScriptCallCode);
    }
        static {initialize();}

    static NodeTemplate getRootTemplate() {return rootTemplate;}

    /** sequential index as parent's child
         * 960513 Bug found in activation/success mechanism:
         * each template should know its index as child in the parent,
         * so that when a related node succeeds, the parent, say ;,
         * can activate the next node, which as the index+1.
         * This went wrong when activating child templates
         * having operators such as int:, if and switch
         * On activation, these do something, and then immediately
         * activate one of *their* childs, which they return.
         * So a node becomes child with this *grandchild* template.
         * The solution is that the indexAsChild in such templates is
         * inherited downwards...
         */
    short  indexAsChild;

    /** whether it is clear that this template is part of an iteration */
    public boolean isIteration() {return (codeBits&IterationFlag) != 0;}

    /** whether it is a code fragment with duration */
    public boolean hasDuration() {return (codeBits&DurationFlag) != 0;}

    /** related templates: communication declarations for partners vv */
    NodeTemplate[] relatedTemplates;
    
    /** code index associated with this (template) */
    int codeIndex = -1;

    /** callback method for java code in scripts */
    Method codeMethod;
    
    /** type code: sequence, script, while etc */
    int  typeCode;

    /** name of source file */
    String sourceFileName = "";

    /** name of package */
    String packageName = "";

    /** name of class */
    String className = "";

        String signature;
        int modifierFlags;

    /** name of script (partners) */
    ArrayList<String> partnerNames = null;

    /** parent template */
    NodeTemplate parent;

    /** child templates */
    NodeTemplate childs[];

    /** number of formal indexes */
    short nrIndexes = 0;

    /** number of formal or actual indexes and parameters */
    short nrIndexesAndParameters = 0;

        int       generalDescriptor[];
        int extraPositionDescriptor[];
        int codeBits;

    /** set denoting actual '?' parameters */
    long outParametersSet;

    /** set denoting actual '!' parameters */
    long forcingParametersSet;

    /** formal indexes of ?! parameters
         * if any, store this in array indexed by actual index
         * and with as values: the indexes of corresponding formal parameters.
         */
    short actualOutForcingParameterIndexes[] = null; // actualIndex >> formalIndex
    void setActualParameterAdapting (int actualIndex, int formalIndex) {
        // just make a new array; anyway; there is garbage collection
        short oldArray[] = actualOutForcingParameterIndexes;
        actualOutForcingParameterIndexes = new short[actualIndex+1];
            if (oldArray != null)
                System.arraycopy (oldArray, 0, actualOutForcingParameterIndexes, 0, oldArray.length);
        actualOutForcingParameterIndexes[actualIndex] = (short) formalIndex;
        }

        /** answer at[0] the partner index and at[1] the partner's index of this' parameter
         *  at the given index
         */
        int[] partnerIndexOfParameterAt(CommRequestNode partners[], int index) {
        for (int i=0; i<partners.length; i++) {
            int nrParameters = partners[i].template.nrIndexesAndParameters;
            if (index < nrParameters)
                    return new int[]{i,index};
        index = (short) (index - nrParameters);
        //index -= nrParameters; gives compiler error
        }
            return null;
        }
    void setActualParameterOut    () {    outParametersSet |= 1L<<nrIndexesAndParameters;}
    void setActualParameterForcing() {forcingParametersSet |= 1L<<nrIndexesAndParameters;}
    public boolean isActualParameterOut      (int i) {long mask=1L<<i; return (outParametersSet&mask) != 0 && (forcingParametersSet&mask) == 0;}
    public boolean isActualParameterForcing  (int i) {long mask=1L<<i; return (outParametersSet&mask) == 0 && (forcingParametersSet&mask) != 0;}
    public boolean isActualParameterAdapting (int i) {long mask=1L<<i; return (outParametersSet&mask) != 0 && (forcingParametersSet&mask) != 0;}

	public NodeTemplate[] getRelatedTemplates() {return relatedTemplates;}
	public void setRelatedTemplates(NodeTemplate[] relatedTemplates) {this.relatedTemplates = relatedTemplates;}
    
        ValueHolder[] makeParameterHolders (Object oldParameters[]) {
            int nrOfOldParameters = oldParameters==null? 0: oldParameters.length;
            ValueHolder result[] = new ValueHolder[nrOfOldParameters];
            if (signature.equals("([Ljava/lang/String;)")) { // main script...
                ObjectHolder h = new ObjectHolder();
                h.value        = oldParameters[0];
                result[0]      = h;
                return result;
            }
            int offset = "(Lscriptic/vm/Node;".length(); // offset in signature; skip some characters...

            for (int i=0; i<nrOfOldParameters; i++) {

               int noOfArrayBrackets = 0;
               while (offset < signature.length() - 1
               &&  signature.charAt(offset) == '[') {
                   offset++;
                   noOfArrayBrackets++;

                   while (offset < signature.length() - 1
                   &&  Character.isDigit(signature.charAt(offset))) {
                       offset++;
                   }
               }
               if (offset >= signature.length())
                  throw new RuntimeException("offset ERROR in makeParameterHolders(\""+signature
                         +"\") value="+offset);

               char signatureChar = signature.charAt(offset);
               ValueHolder holder = null;
               offset++;

               switch (signatureChar) {
               case 'B' : {   ByteHolder h = new    ByteHolder(); holder=h; h.value = ((     Byte)oldParameters[i]).   byteValue();} break;
               case 'C' : {   CharHolder h = new    CharHolder(); holder=h; h.value = ((Character)oldParameters[i]).   charValue();} break;
               case 'D' : { DoubleHolder h = new  DoubleHolder(); holder=h; h.value = ((   Double)oldParameters[i]). doubleValue();} break;
               case 'F' : {  FloatHolder h = new   FloatHolder(); holder=h; h.value = ((    Float)oldParameters[i]).  floatValue();} break;
               case 'I' : {    IntHolder h = new     IntHolder(); holder=h; h.value = ((  Integer)oldParameters[i]).    intValue();} break;
               case 'J' : {   LongHolder h = new    LongHolder(); holder=h; h.value = ((     Long)oldParameters[i]).   longValue();} break;
               case 'S' : {  ShortHolder h = new   ShortHolder(); holder=h; h.value = ((    Short)oldParameters[i]).  shortValue();} break;
               case 'Z' : {BooleanHolder h = new BooleanHolder(); holder=h; h.value = ((  Boolean)oldParameters[i]).booleanValue();} break;
               case 'L' : { ObjectHolder h = new  ObjectHolder(); holder=h; h.value =           oldParameters[i];
                          //int startPos = offset;
                          while (offset < signature.length()
                          &&  signature.charAt(offset) != ';')
                              offset++;
                          // String signatureName = signature.substring (startPos, offset);
                          offset++;
                          }
                          break;
               default:
                   throw new RuntimeException("Unknown signature character: "+signatureChar
                                             +" in "+signature
                                             +"["+(offset-1)+"]");
               }
               result [i] = holder;
           }
           return result;
        }

    void setPackageName (String name) {packageName = name;}
    void setClassName   (String name) {  className = name;}
    void setName        (String name) {
      partnerNames = new ArrayList<String>();
      partnerNames.add(name);
    }

    void addName (String name) {
      if (partnerNames == null) {
          setName(name);
          return;
      }
      partnerNames.add(name);
    }

    public String  getName() {

      String result;
      if (typeCode<0 || typeCode >= Representations.scripticParseTreeCodeRepresentations.length) 
               result = "Error: typeCode = " + typeCode;
      else result = Representations.scripticParseTreeCodeRepresentations[typeCode];

      if (result=="") result="ERROR: NO OPERATOR NAME";

          result += ' ';
          if (packageName.length()>0) result += packageName+'.';
          if (  className.length()>0) result +=   className+'.';

      if (partnerNames == null) {
      } else {
        for (int i=0; i < partnerNames.size(); i++)
        {
          result += (i==0?"":",") + partnerNames.get(i);
        }
      }
      return result;
    }

    
    /** 
     * constructors: 
     *
     */ 
    NodeTemplate() {if (!initialized) initialize();}
    NodeTemplate(int typeCode) {this(); this.typeCode = typeCode;}

    public static NodeTemplate makeNodeTemplate(
    		String packageName,
            String className,
            String name,
            String signature,
            String codeMethodName,
            int    modifierFlags,
            short  nrIndexes,
            short  nrParameters,
            Object templateArray[][]) {
    	
    	String fullClassName = 
    		packageName.length()==0? className: packageName+"."+className;
		try {
	    	Method codeMethod = null;
	    	if (codeMethodName!=null)
	    	{
	    		Class<?> ownerClass = Class.forName(fullClassName);
	    		codeMethod = ownerClass.getDeclaredMethod(codeMethodName, new Class[]{scriptic.vm.Node.class, Integer.TYPE});
	    	}
			return new NodeTemplate (
					packageName,
					className,
					name,
					signature,
					codeMethod,
					modifierFlags,
					nrIndexes,
					nrParameters,
					templateArray,
					new int[2]);
		} catch (ClassNotFoundException e) {
			// will probably not happen
			e.printStackTrace();
		} catch (SecurityException e) {
			// will probably not happen
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// will probably not happen
			e.printStackTrace();
		}
		return null;
	}

    public NodeTemplate (String packageName,
                             String className,
                             String name,
                             String signature,
                             Method codeMethod,
                             int    modifierFlags,
                             short  nrIndexes,
                             short  nrParameters,
                             Object templateArray[][]) {
            this (packageName,
                  className,
                  name,
                  signature,
                  codeMethod,
                  modifierFlags,
                  nrIndexes,
                  nrParameters,
                  templateArray,
                  new int[2]);
        }
        int getAnchorCodeIndex()
        {
          if ((codeBits &   AnchorFlag)==0) return -1;
          if ((codeBits & InitCodeFlag)==0) return codeIndex-1;
          return codeIndex-2;
        }
        int getNextCodeIndex()
        {
          if ((codeBits & NextCodeFlag)==0) return -1;
          if ((codeBits &     CodeFlag)==0) return codeIndex;
          return codeIndex+1;
        }
        
         /** constructor:
         * 'Read' a sequence of elements from the descriptor array,
         * which contain data on this and childs.
         * Start at the offset specified as element 0 of the given array;
         * the code index is specified as element 1
         * these number are to be incremented whenever applicable
         * They have been packed into a single array to emulate reference parameters
         */
        private NodeTemplate (String packageName,
                              String className,
                              String name,
                              String signature,
                              Method codeMethod,
                              int    modifierFlags,
                              short  nrIndexes,
                              short  nrParameters,
                              Object templateArray[][],
                              int    offsetAndCodeIndex[]) {
            this.packageName   = packageName;
            this.className     = className;
            this.signature     = signature;
            this.modifierFlags = modifierFlags;
            this.nrIndexes     = nrIndexes;
            this.nrIndexesAndParameters = (short) (nrIndexes + nrParameters);
            setName (name);
            int offset = offsetAndCodeIndex[0]++;
            Object thisDescriptor[] = templateArray[offset];
            
            this.codeMethod    = codeMethod;

            int descriptorIndex     = 0;
                generalDescriptor   = (int[]) thisDescriptor[descriptorIndex++];
            typeCode                = generalDescriptor[0];
            codeBits                = generalDescriptor[1];

            if (typeCode!=DeactivationCodeCode) { // else the code comes AFTER the operands code so treat specially
              // if present, init code has index equal to the codeIndex-1
              if ((codeBits &   AnchorFlag)>0) offsetAndCodeIndex[1]++;
              if ((codeBits & InitCodeFlag)>0) offsetAndCodeIndex[1]++;
              codeIndex                      = offsetAndCodeIndex[1];
              if ((codeBits &     CodeFlag)>0) offsetAndCodeIndex[1]++;
              if ((codeBits & NextCodeFlag)>0) offsetAndCodeIndex[1]++;
            }
            int nChilds = 0;

            switch (typeCode) {
            case             ParBreakOperatorCode:
            case            SuspendOperatorCode:
            case              ParOrOperatorCode:
            case                  ParAndOperatorCode:
            case               ParAnd2OperatorCode:
            case                ParOr2OperatorCode:
            case                   OrOperatorCode:
            case                  SeqOperatorCode:
            case               NotSeqOperatorCode:
            case             EllipsisOperatorCode:

                                extraPositionDescriptor = (int[]) thisDescriptor[descriptorIndex++];
                                nChilds = extraPositionDescriptor.length/2 + 1;
                                break;

            case  ConditionalScriptExpressionCode:
            case           IfScriptExpressionCode:

                                extraPositionDescriptor = (int[]) thisDescriptor[descriptorIndex++];
                                nChilds = 1
                                        + ((codeBits & SecondTermFlag) !=0 ? 1: 0)
                                        + (typeCode==IfScriptExpressionCode? 0: 1);
                                break;

            case            ScriptDeclarationCode:
            case     CommunicationDeclarationCode:
            case           ChannelDeclarationCode:
            case       SendChannelDeclarationCode:
            case    ReceiveChannelDeclarationCode:
            case               ActivationCodeCode:
            case             DeactivationCodeCode:
            case       SwitchScriptExpressionCode:
            case      CaseTagScriptExpressionCode:
            case   ScriptLocalDataDeclarationCode:
            case PrivateScriptDataDeclarationCode:
            case           LaunchedExpressionCode:
            case          ReactiveNotOperatorCode:
            case                  NotOperatorCode: nChilds = 1;
            }

            switch (typeCode) {

            case            ScriptDeclarationCode:
            case     CommunicationDeclarationCode:
            case           ChannelDeclarationCode:
            case       SendChannelDeclarationCode:
            case    ReceiveChannelDeclarationCode:
            case               ActivationCodeCode:
            case             DeactivationCodeCode:
            case        WhileScriptExpressionCode:
            case          ForScriptExpressionCode:
            case       SwitchScriptExpressionCode:
            case      CaseTagScriptExpressionCode:
            case                  ChannelSendCode:
            case               ChannelReceiveCode:
            case         ScriptCallExpressionCode:
            case   ScriptLocalDataDeclarationCode:
            case PrivateScriptDataDeclarationCode: 
                                extraPositionDescriptor = (int[]) thisDescriptor[descriptorIndex++];
            }

            switch (typeCode) {

            case                  ChannelSendCode:
            case               ChannelReceiveCode:
            case         ScriptCallExpressionCode:
            case   ScriptLocalDataDeclarationCode:
            case PrivateScriptDataDeclarationCode: setName((String) ((Object[]) thisDescriptor[descriptorIndex++])[0]);
            }

            switch (typeCode) {
            case                  ChannelSendCode:
            case               ChannelReceiveCode:
            case         ScriptCallExpressionCode:
                  long parametersDescriptor[] = (long[]) thisDescriptor[descriptorIndex++];
                      outParametersSet = parametersDescriptor [0];
                  forcingParametersSet = parametersDescriptor [1];
                  for (int i=0; i<64; i++) {
                     if (((outParametersSet>>i) & 1) != 0) {
                         offsetAndCodeIndex[1]++;  // a code entry for each out parameter...
                         this.nrIndexesAndParameters = (short) (i+1);
                     }
                  }
                  int actualIndex = -1;
                  for (int i=2; i<parametersDescriptor.length; i++) {
                     // gather formal indexes of adapting parameters
                     while (++actualIndex < 64) {
                        if (isActualParameterAdapting (actualIndex)) {
                           break;
                        }
                     }
                     setActualParameterAdapting (actualIndex, (int) parametersDescriptor[i]);
                  }
            }
//if (Node.doTrace) System.out.println ("NodeTemplate "+info(0));

            if (nChilds > 0) {
                 childs = new NodeTemplate[nChilds];
                 for (short i=0; i<nChilds; i++) {
                   addChild (i, new NodeTemplate (packageName,
                                                  className,
                                                  name,
                                                  signature,
                                                  codeMethod,
                                                  modifierFlags,
                                                  nrIndexes,
                                                  nrParameters,
                                                  templateArray,
                                                  offsetAndCodeIndex));
                 }
            }
            if (typeCode==DeactivationCodeCode) { // the code comes AFTER the operands code so treat specially
             if ((codeBits & AnchorFlag)>0) offsetAndCodeIndex[1]++;
             codeIndex = offsetAndCodeIndex[1]++;
            }

        }
        /** add a child to the end of the childs array (increment that) */
    void addChild (NodeTemplate child) {
            if (childs == null) {
                childs = new NodeTemplate[1];
            }
        else {
                NodeTemplate oldArray[] = childs;
                childs = new NodeTemplate[oldArray.length+1];
        System.arraycopy(oldArray,0,childs,0,oldArray.length);
        }
            addChild ((short) (childs.length-1), child);
    }

        /** add a child at the given position of the childs array (should be large enough) */
    void addChild (short index, NodeTemplate child) {
        child.parent       = this;
        if (child.propagatesIteration()) codeBits |= IterationFlag;
        child.indexAsChild  = index;
        childs[child.indexAsChild] =  child;

            switch (typeCode) {
          case         ScriptDeclarationCode:
              case  CommunicationDeclarationCode:
          case        ChannelDeclarationCode:
          case    SendChannelDeclarationCode:
          case ReceiveChannelDeclarationCode: propagateIndexAsChild(); break;
            }
    }

        /* overrule this indexAsChild in case of several kinds of typeCode
         * see the above comments on indexAsChild
         */
        void propagateIndexAsChild() {
        if (childs != null) for (int i=0; i<childs.length; i++) {
              switch (typeCode) {
                case             EllipsisOperatorCode:
                case           IfScriptExpressionCode:
                case       SwitchScriptExpressionCode:
                case   ScriptLocalDataDeclarationCode:
                case PrivateScriptDataDeclarationCode:
                case               ActivationCodeCode: childs[i].indexAsChild = indexAsChild; break;
              }
              childs[i].propagateIndexAsChild();
        }
        }
                                
    boolean propagatesIteration() {
        if (!isIteration()) return false;
        switch (typeCode) {
            case ParBreakOperatorCode:
            case  SuspendOperatorCode:
            case    ParOrOperatorCode:
            case   ParAndOperatorCode:
            case  ParAnd2OperatorCode:
            case   ParOr2OperatorCode:
            case       OrOperatorCode:
            case      SeqOperatorCode:
            case   NotSeqOperatorCode: return false;
        }
        return true;
        }


    final int activateFrom (Node parentNode, Object owner, Object params[]) {
      Node   node = null;
      switch (typeCode) {
      case            ChannelRequestCode: if (parentNode.template.typeCode==ChannelReceiveCode) //RootScriptCall also possible!
                                          node = new ReceiveRequestNode(this, parentNode, owner, params);
                                     else node = new    SendRequestNode(this, parentNode, owner, params); break;
      case         ScriptDeclarationCode: node = new         ScriptNode(this, parentNode, owner, params); break;
      case      CommunicationRequestCode: node = new    CommRequestNode(this, parentNode, owner, params); break;

default: Node.debugOutput("activateFrom(n,f,p) " + parentNode.getName() + " : " + getName()
                     + "   ERROR: UNEXPECTED operator type: "+typeCode);
(new Exception()).printStackTrace();
          }

          if (node.operatorAncestor!=null
          &&  isIteration())
              node.operatorAncestor.isIteration = true;

      int result = node.activate();

          if (node.firstChild == null) {
            switch (typeCode) {
            case         ScriptDeclarationCode:
        case  CommunicationDeclarationCode:         
        case        ChannelDeclarationCode:       
        case    SendChannelDeclarationCode:   
        case ReceiveChannelDeclarationCode: node.markForDeactivation();
            }
      }
      return result;
    } 
    final int activateFrom (Node parentNode) {
      int result = 0;
      Node node = null;

      switch (typeCode) {
      case  ZeroExpressionCode:    break;
      case   OneExpressionCode:    result = Node.SuccessBit; break;
      case BreakExpressionCode:    result = Node.   ExitBit; break;
      case Ellipsis1OperandCode:
          case EllipsisOperandCode:    result = Node.   SomeBit; // NO break
      /* case OP_NEUTRAL:*/        if (!parentNode.isWithOrLikeOptr()) result |= Node.SuccessBit; break;     
          case EllipsisOperatorCode:{int i = childs.length; if (i >  parentNode.pass)
                                                   i = (int) parentNode.pass;
                          return childs[i].activateFrom (parentNode);}
 
      case Ellipsis3OperandCode:
          parentNode.success = Boolean.TRUE;
          if (!parentNode.isWithOrLikeOptr()) result = Node.SuccessBit; break;
          
      case   ForScriptExpressionCode:
    	  						
                              if (parentNode.pass==0) 
                                   {if ((codeBits&InitCodeFlag) != 0) {
                                	   parentNode.doAnchorCodeIfPresent();
                                	   parentNode.doCode(codeIndex-1);}
                                   }
                              else {if ((codeBits&NextCodeFlag) != 0) {
                            	       parentNode.doAnchorCodeIfPresent();
                            	       parentNode.doCode(getNextCodeIndex());
                                    }
                                   }
                              //NO break!
                              
      case WhileScriptExpressionCode:
    	                      
                              if ((codeBits&CodeFlag) != 0) {
                            	  parentNode.doAnchorCodeIfPresent();
                            	  parentNode.doCode(codeIndex);
                              }
                              else parentNode.success = (codeBits&TrueFlag) != 0? Boolean.TRUE: Boolean.FALSE;
                          if (parentNode.success!=Boolean.TRUE) result = Node.ExitBit;
                          else
                              if (!parentNode.isWithOrLikeOptr()) result = Node.SuccessBit; break;
    //case DoToken:       if (!parentNode.isWithOrLikeOptr()) result = Node.SuccessBit; break;

      case IfScriptExpressionCode:
                               {Boolean oldSuccess = parentNode.success;
    	                        if ((codeBits&CodeFlag) != 0) {
                                	parentNode.doAnchorCodeIfPresent();
                                	parentNode.doCode(codeIndex);
                                }
                                else parentNode.success = (codeBits&TrueFlag) != 0? Boolean.TRUE: Boolean.FALSE;
                                if  (parentNode.success==Boolean.TRUE) {
                                     parentNode.success=oldSuccess;
                                     return childs[0].activateFrom(parentNode);
                                 } 
                                 parentNode.success=oldSuccess;
                            }
                                if (childs.length > 1) 
                                  return childs[1].activateFrom(parentNode);
                                if (!parentNode.isWithOrLikeOptr()  ) result = Node.SuccessBit;
                            break;
      case SwitchScriptExpressionCode: {int p = parentNode.pass;
                             parentNode.pass = 0;
                        	 parentNode.doAnchorCodeIfPresent();
                             parentNode.doCode(codeIndex);
                             int i = parentNode.pass;
                             parentNode.pass = p;
                             if (i>0) return childs[i-1].activateFrom(parentNode);
                                 if (!parentNode.isWithOrLikeOptr()  ) result = Node.SuccessBit; 
                             break;}
      case TinyCodeFragmentCode:
                            if ((codeBits&CodeFlag) != 0) {
                          	    parentNode.doAnchorCodeIfPresent();
                            	parentNode.doCode(codeIndex);
                            }
                            if (parentNode.success==Boolean.TRUE
                            && !parentNode.isWithOrLikeOptr()) result = Node.SuccessBit; 
                            break;
      case  ConditionalScriptExpressionCode: node = new  BiTernaryTestNode(this, parentNode); break;

      case   ScriptLocalDataDeclarationCode:
      case PrivateScriptDataDeclarationCode:
                                    parentNode.doCode(codeIndex);
                                        return childs[0].activateFrom(parentNode);
      case       ActivationCodeCode: node = new     ActivationNode(this, parentNode); break;
      case     DeactivationCodeCode: node = new   DeactivationNode(this, parentNode); break;
      case     ParBreakOperatorCode: node = new       ParBreakNode(this, parentNode); break;
      case        ParOrOperatorCode: node = new        ParOrNode(this, parentNode); break;
      case      ParAnd2OperatorCode: node = new         ParAnd2Node(this, parentNode); break;
      case       ParAndOperatorCode: node = new              ParAndNode(this, parentNode); break;
      case      SuspendOperatorCode: node = new       SuspendResumeNode(this, parentNode); break;
      case       ParOr2OperatorCode: node = new            ParOr2Node(this, parentNode); break; 
      case           OrOperatorCode: node = new               OrNode(this, parentNode); break;
      case          SeqOperatorCode: node = new              SeqNode(this, parentNode); break;
      case       NotSeqOperatorCode: node = new           NotSeqNode(this, parentNode); break;

      case ScriptCallExpressionCode: node = new       ScriptCallNode(this, parentNode); break;
      case       ChannelReceiveCode: node = new      ReceiveCallNode(this, parentNode); break;
      case          ChannelSendCode: node = new         SendCallNode(this, parentNode); break;

      case  ReactiveNotOperatorCode: node = new     ReactiveNotNode(this, parentNode); break;
      case          NotOperatorCode: node = new             NotNode(this, parentNode); break;
//    case     ThreadExpressionCode: node = new          ThreadNode(this, parentNode); break;    
      case  EventHandlingCodeFragmentCode:
      case EventHandling0PlusCodeFragmentCode:
      case EventHandling1PlusCodeFragmentCode:
      case EventHandlingManyCodeFragmentCode:
                                     node = new EventHandlingCodeFragmentNode(this, parentNode,typeCode);
                                     node.doAnchorCodeIfPresent();
                                     if ((codeBits&InitCodeFlag) != 0) node.doCode(codeIndex-1);
                                     if (typeCode==EventHandling0PlusCodeFragmentCode) 
                                         if (!parentNode.isWithOrLikeOptr()  ) result = Node.SuccessBit;
                                     break;
      case  UnsureCodeFragmentCode:   
      case Unsure2CodeFragmentCode: node = new UnsureCodeFragmentNode(this, parentNode,typeCode==Unsure2CodeFragmentCode); break;

      case  NativeCodeFragmentCode: node = hasDuration()
                                             ? new PlainCodeFragmentWithDurationNode(this, parentNode)
                                             : new PlainCodeFragmentNode            (this, parentNode);
                                        node.doAnchorCodeIfPresent();
                                        if ((codeBits&InitCodeFlag) != 0) node.doCode(codeIndex-1);
                                        break;
      case ThreadedCodeFragmentCode: node = hasDuration()
                                             ? new ThreadedCodeFragmentWithDurationNode(this, parentNode)
                                             : new ThreadedCodeFragmentNode            (this, parentNode);
                                        node.doAnchorCodeIfPresent();
                                        if ((codeBits&InitCodeFlag) != 0) node.doCode(codeIndex-1);
                                        break;
      case LaunchedExpressionCode:         
      //case LaunchedThreadExpressionCode: 
                                node = //(typeCode==LaunchedExpressionCode)
                                //? (Node) 
                                 new       LaunchedNode(this, parentNode);
                   // : (Node) new LaunchedThreadNode(this, parentNode);
                   {int activateResult = node.activate();
                            if ((activateResult&Node.CodeBit)==0) result  = Node.CodeBit; 
                            if (!parentNode.isWithOrLikeOptr()  ) result |= Node.SuccessBit; 
                                return result;
                       }     

default: debugOutput("activateFrom " + parentNode.getName() + " : " + getName()
                + "   ERROR: UNEXPECTED operator type: "+typeCode);
          }
      if (node != null) {

        result = node.activate();

        switch (typeCode)
        {
	        case EventHandling0PlusCodeFragmentCode:    
	        case EventHandling1PlusCodeFragmentCode:    
	        case EventHandlingManyCodeFragmentCode:    
            case  EventHandlingCodeFragmentCode:    
            case         NativeCodeFragmentCode:    
            case         UnsureCodeFragmentCode:   
            case        Unsure2CodeFragmentCode:   
            case       ThreadedCodeFragmentCode: break; 
            default: if (node.firstChild == null) {

if (partnerNames != null
&&  (partnerNames.get(0)).equals ("waitTime"))
new Exception ("Marking for deactivation: waitTime").printStackTrace();

                                                          node.markForDeactivation(); } break;
        }
      }
      else {
if(Node.doTrace) {
  StringBuffer sb = new StringBuffer();
  sb.append("A> ")
    //.append(ScripticScanner.TokenRepresentations[typeCode])
    .append(" parent: ")    .append(parent.getName())
    .append((codeIndex>=0? "["+codeIndex+"]": ""))
    .append(": ")    .append(((result &    Node.ExitBit) != 0)? "exit"   : "no exit")
    .append("; ")    .append(((result & Node.SuccessBit) != 0)? "success": "no success")
    .append("  ");
  Node.traceOutput(sb.toString());
}
      }
      return result;
    } 

   public String scripticParseTreeCodeRepresentation (int i) {
          return Representations.scripticParseTreeCodeRepresentations[i];    }

   ////////////////// NodeTemplateInterface stuff

    /** code index associated with this (template) */
    public int codeIndex() {return codeIndex;}

    /** operator/operand type (see JavaTokens.java and ScripticTokens.java) */
    public int  typeCode() {return typeCode;}

    /** name of package */
    public String packageName() {return packageName;}

    /** name of class */
    public String className() {return className;}

        /** signature */
        public String signature() {return signature;}

        /** access modifiers*/
        public int modifierFlags() {return modifierFlags;}

    /** name of script (partners) */
    public ArrayList<String> partnerNames() {return partnerNames;}

    /** parent template */
    public NodeTemplateInterface parent() {return parent;}

    /** child templates */
    public NodeTemplateInterface[] childs() {return childs;}

    /** number of formal or actual parameters */
    public short nrParameters() {return nrIndexesAndParameters;}

        /** flags for code: 0x000 often (no code)
         *                  0x001 if there is a code fragment
         *                  0x011 if there is an extra fragment
         *                  0x111 3 bits possible for "for(..;..;..)"
         */
        public int codeBits() {return generalDescriptor[1];}

        /** start line */
        public int startLine() {return generalDescriptor[2];}

        /** start position */
        public int startPos() {return generalDescriptor[3];}

        /** end line */
        public int endLine() {return generalDescriptor[4];}

        /** end position */
        public int endPos() {return generalDescriptor[5];}

        public int[] extraPositionDescriptor() {return extraPositionDescriptor;}
}

