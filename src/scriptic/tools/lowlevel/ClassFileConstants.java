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

   /*------------------------ ClassFileConstants -----------------------*/

public interface ClassFileConstants {
   static final String lineSeparator = System.getProperty ("line.separator", "\r\n");

   static final int       PublicFlag = 0x0001;
   static final int      PrivateFlag = 0x0002;
   static final int    ProtectedFlag = 0x0004;
   static final int       StaticFlag = 0x0008;
   static final int        FinalFlag = 0x0010;
   static final int SynchronizedFlag = 0x0020;
   static final int        SuperFlag = 0x0020; // == SynchronizedFlag
   static final int     VolatileFlag = 0x0040;
   static final int    TransientFlag = 0x0080;
   static final int       NativeFlag = 0x0100;
   static final int    InterfaceFlag = 0x0200;
   static final int     AbstractFlag = 0x0400;

// static final int ConstantPoolAscizTag           = -1;
   static final int ConstantPoolUnicodeTag         =  1;
   static final int ConstantPoolIntegerTag         =  3;
   static final int ConstantPoolFloatTag           =  4;
   static final int ConstantPoolLongTag            =  5; 
   static final int ConstantPoolDoubleTag          =  6;
   static final int ConstantPoolClassTag           =  7;
   static final int ConstantPoolStringTag          =  8;
   static final int ConstantPoolFieldTag           =  9;
   static final int ConstantPoolMethodTag          = 10;
   static final int ConstantPoolInterfaceMethodTag = 11;
   static final int ConstantPoolNameAndTypeTag     = 12;

   static final int JAVA_MAGIC         = 0xCAFEBABE;
   static final int JAVA_MINOR_VERSION =  3;
   static final int JAVA_VERSION       = 45;

   // Instruction codes

   //Pushing Constants onto the Stack  

   static final short INSTRUCTION_bipush      =  16;
   static final short INSTRUCTION_sipush      =  17; 
   static final short INSTRUCTION_ldc         =  18;
   static final short INSTRUCTION_ldc_w       =  19;
   static final short INSTRUCTION_ldc_2w      =  20;
   static final short INSTRUCTION_aconst_null =   1;
   static final short INSTRUCTION_iconst_m1   =   2;
   static final short INSTRUCTION_iconst_0    =   3;
   static final short INSTRUCTION_iconst_1    =   4;
   static final short INSTRUCTION_iconst_2    =   5;
   static final short INSTRUCTION_iconst_3    =   6;
   static final short INSTRUCTION_iconst_4    =   7;
   static final short INSTRUCTION_iconst_5    =   8;

   static final short INSTRUCTION_lconst_0    =   9;
   static final short INSTRUCTION_lconst_1    =  10;

   static final short INSTRUCTION_fconst_0    =  11;
   static final short INSTRUCTION_fconst_1    =  12;
   static final short INSTRUCTION_fconst_2    =  13;

   static final short INSTRUCTION_dconst_0    =  14;
   static final short INSTRUCTION_dconst_1    =  15;

   // Loading Local Variables Onto the Stack  

   static final short INSTRUCTION_iload       =  21; 
   static final short INSTRUCTION_iload_0     =  26;  
   static final short INSTRUCTION_iload_1     =  27;  
   static final short INSTRUCTION_iload_2     =  28;  
   static final short INSTRUCTION_iload_3     =  29;  

   static final short INSTRUCTION_lload       =  22; 
   static final short INSTRUCTION_lload_0     =  30;  
   static final short INSTRUCTION_lload_1     =  31;  
   static final short INSTRUCTION_lload_2     =  32;  
   static final short INSTRUCTION_lload_3     =  33;  

   static final short INSTRUCTION_fload       =  23; 
   static final short INSTRUCTION_fload_0     =  34;  
   static final short INSTRUCTION_fload_1     =  35;  
   static final short INSTRUCTION_fload_2     =  36;  
   static final short INSTRUCTION_fload_3     =  37;  

   static final short INSTRUCTION_dload       =  24; 
   static final short INSTRUCTION_dload_0     =  38;  
   static final short INSTRUCTION_dload_1     =  39;  
   static final short INSTRUCTION_dload_2     =  40;  
   static final short INSTRUCTION_dload_3     =  41;  

   static final short INSTRUCTION_aload       =  25; 
   static final short INSTRUCTION_aload_0     =  42;  
   static final short INSTRUCTION_aload_1     =  43;  
   static final short INSTRUCTION_aload_2     =  44;  
   static final short INSTRUCTION_aload_3     =  45;  

   //Storing Stack Values into Local Variables

   static final short INSTRUCTION_istore      =  54; 
   static final short INSTRUCTION_istore_0    =  59;  
   static final short INSTRUCTION_istore_1    =  60;  
   static final short INSTRUCTION_istore_2    =  61;  
   static final short INSTRUCTION_istore_3    =  62;  

   static final short INSTRUCTION_lstore      =  55; 
   static final short INSTRUCTION_lstore_0    =  63;  
   static final short INSTRUCTION_lstore_1    =  64;  
   static final short INSTRUCTION_lstore_2    =  65;  
   static final short INSTRUCTION_lstore_3    =  66;  

   static final short INSTRUCTION_fstore      =  56; 
   static final short INSTRUCTION_fstore_0    =  67;  
   static final short INSTRUCTION_fstore_1    =  68;  
   static final short INSTRUCTION_fstore_2    =  69;  
   static final short INSTRUCTION_fstore_3    =  70;  

   static final short INSTRUCTION_dstore      =  57; 
   static final short INSTRUCTION_dstore_0    =  71;  
   static final short INSTRUCTION_dstore_1    =  72;  
   static final short INSTRUCTION_dstore_2    =  73;  
   static final short INSTRUCTION_dstore_3    =  74;  

   static final short INSTRUCTION_astore      =  58; 
   static final short INSTRUCTION_astore_0    =  75;  
   static final short INSTRUCTION_astore_1    =  76;  
   static final short INSTRUCTION_astore_2    =  77;  
   static final short INSTRUCTION_astore_3    =  78;  

   static final short INSTRUCTION_iinc        = 132;  

   // Managing Arrays  

   static final short INSTRUCTION_newarray    = 188;
   static final short INSTRUCTION_anewarray   = 189;
   static final short INSTRUCTION_multianewarray = 197;
   static final short INSTRUCTION_arraylength = 190;
   static final short INSTRUCTION_iaload      =  46;
   static final short INSTRUCTION_laload      =  47;
   static final short INSTRUCTION_faload      =  48;
   static final short INSTRUCTION_daload      =  49;
   static final short INSTRUCTION_aaload      =  50;
   static final short INSTRUCTION_baload      =  51;
   static final short INSTRUCTION_caload      =  52;
   static final short INSTRUCTION_saload      =  53;
   static final short INSTRUCTION_iastore     =  79;
   static final short INSTRUCTION_lastore     =  80;
   static final short INSTRUCTION_fastore     =  81;
   static final short INSTRUCTION_dastore     =  82;
   static final short INSTRUCTION_aastore     =  83;
   static final short INSTRUCTION_bastore     =  84;
   static final short INSTRUCTION_castore     =  85;
   static final short INSTRUCTION_sastore     =  86;

   // Stack Instructions  

   static final short INSTRUCTION_nop         =   0;
   static final short INSTRUCTION_pop         =  87;
   static final short INSTRUCTION_pop2        =  88;
   static final short INSTRUCTION_dup         =  89;
   static final short INSTRUCTION_dup2        =  92;
   static final short INSTRUCTION_dup_x1      =  90;
   static final short INSTRUCTION_dup2_x1     =  93;
   static final short INSTRUCTION_dup_x2      =  91;
   static final short INSTRUCTION_dup2_x2     =  94;
   static final short INSTRUCTION_swap        =  95;

   // Arithmetic Instructions  

   static final short INSTRUCTION_iadd        =  96;
   static final short INSTRUCTION_ladd        =  97;  
   static final short INSTRUCTION_fadd        =  98;
   static final short INSTRUCTION_dadd        =  99;
   static final short INSTRUCTION_isub        = 100;
   static final short INSTRUCTION_lsub        = 101;
   static final short INSTRUCTION_fsub        = 102;
   static final short INSTRUCTION_dsub        = 103;
   static final short INSTRUCTION_imul        = 104;
   static final short INSTRUCTION_lmul        = 105;
   static final short INSTRUCTION_fmul        = 106;
   static final short INSTRUCTION_dmul        = 107;
   static final short INSTRUCTION_idiv        = 108;
   static final short INSTRUCTION_ldiv        = 109;
   static final short INSTRUCTION_fdiv        = 110;
   static final short INSTRUCTION_ddiv        = 111;
   static final short INSTRUCTION_irem        = 112;
   static final short INSTRUCTION_lrem        = 113;
   static final short INSTRUCTION_frem        = 114;
   static final short INSTRUCTION_drem        = 115;
   static final short INSTRUCTION_ineg        = 116;
   static final short INSTRUCTION_lneg        = 117;
   static final short INSTRUCTION_fneg        = 118;
   static final short INSTRUCTION_dneg        = 119;

   // Logical Instructions  

   static final short INSTRUCTION_ishl        = 120;
   static final short INSTRUCTION_lshl        = 121;
   static final short INSTRUCTION_ishr        = 122;
   static final short INSTRUCTION_lshr        = 123;
   static final short INSTRUCTION_iushr       = 124;
   static final short INSTRUCTION_lushr       = 125;
   static final short INSTRUCTION_iand        = 126;
   static final short INSTRUCTION_land        = 127;
   static final short INSTRUCTION_ior         = 128;
   static final short INSTRUCTION_lor         = 129;
   static final short INSTRUCTION_ixor        = 130;
   static final short INSTRUCTION_lxor        = 131;

  // Conversion Operations  

   static final short INSTRUCTION_i2l         = 133;
   static final short INSTRUCTION_i2f         = 134;
   static final short INSTRUCTION_i2d         = 135;
   static final short INSTRUCTION_l2i         = 136;
   static final short INSTRUCTION_l2f         = 137;
   static final short INSTRUCTION_l2d         = 138;
   static final short INSTRUCTION_f2i         = 139;
   static final short INSTRUCTION_f2l         = 140;
   static final short INSTRUCTION_f2d         = 141;
   static final short INSTRUCTION_d2i         = 142;
   static final short INSTRUCTION_d2l         = 143;
   static final short INSTRUCTION_d2f         = 144;
   static final short INSTRUCTION_int2byte    = 145;
   static final short INSTRUCTION_int2char    = 146;
   static final short INSTRUCTION_int2short   = 147;

  // Control Transfer Instructions  

   static final short INSTRUCTION_ifeq        = 153;
   static final short INSTRUCTION_iflt        = 155;
   static final short INSTRUCTION_ifle        = 158;
   static final short INSTRUCTION_ifne        = 154;
   static final short INSTRUCTION_ifgt        = 157;
   static final short INSTRUCTION_ifge        = 156;
   static final short INSTRUCTION_if_icmpeq   = 159;
   static final short INSTRUCTION_if_icmpne   = 160;
   static final short INSTRUCTION_if_icmplt   = 161;
   static final short INSTRUCTION_if_icmpgt   = 163;
   static final short INSTRUCTION_if_icmple   = 164;
   static final short INSTRUCTION_if_icmpge   = 162;
   static final short INSTRUCTION_lcmp        = 148; 
   static final short INSTRUCTION_fcmpl       = 149;
   static final short INSTRUCTION_fcmpg       = 150;
   static final short INSTRUCTION_dcmpl       = 151;
   static final short INSTRUCTION_dcmpg       = 152;
   static final short INSTRUCTION_if_acmpeq   = 165;
   static final short INSTRUCTION_if_acmpne   = 166;
   static final short INSTRUCTION_goto        = 167;
   static final short INSTRUCTION_jsr         = 168;
   static final short INSTRUCTION_ret         = 169;

  // Function Return  

   static final short INSTRUCTION_ireturn     = 172;
   static final short INSTRUCTION_lreturn     = 173;
   static final short INSTRUCTION_freturn     = 174;
   static final short INSTRUCTION_dreturn     = 175;
   static final short INSTRUCTION_areturn     = 176;
   static final short INSTRUCTION_return      = 177;

  // Table Jumping  

   static final short INSTRUCTION_tableswitch      = 170;
   static final short INSTRUCTION_lookupswitch     = 171;

  // Manipulating Object Fields  

   static final short INSTRUCTION_putfield         = 181;
   static final short INSTRUCTION_getfield         = 180;
   static final short INSTRUCTION_putstatic        = 179;
   static final short INSTRUCTION_getstatic        = 178;

  // Method Invocation  

   static final short INSTRUCTION_invokevirtual    = 182;
   static final short INSTRUCTION_invokespecial    = 183;
   static final short INSTRUCTION_invokestatic     = 184;
   static final short INSTRUCTION_invokeinterface  = 185;

  // Exception Handling  

   static final short INSTRUCTION_athrow           = 191;

  // Miscellaneous Object Operations
  
   static final short INSTRUCTION_new              = 187;
   static final short INSTRUCTION_checkcast        = 192;
   static final short INSTRUCTION_instanceof       = 193;

  // Monitors  

   static final short INSTRUCTION_monitorenter     = 194;
   static final short INSTRUCTION_monitorexit      = 195;

  // Debugging  

   static final short INSTRUCTION_breakpoint       = 202;

   // Other
   static final short INSTRUCTION_wide             = 196;
   static final short INSTRUCTION_ifnull           = 198;
   static final short INSTRUCTION_ifnonnull        = 199;
   static final short INSTRUCTION_goto_w           = 200;
   static final short INSTRUCTION_jsr_w            = 201;

   static final short INSTRUCTION_label            = 256; 
   static final short INSTRUCTION_deleted          = 257; 

   static final short INSTRUCTION_Length[] = {
    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //  0- 15
    2, 3, 2, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, // 16- 31
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 32- 47
    1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, // 48- 63
    //          //          //          //
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 64- 79
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 80- 95
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 96-111
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //112-127
    //          //          //          //
    1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //128-143
    1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, //144-159
    3, 3, 3, 3, 3, 3, 3, 3, 3, 2,99,99, 1, 1, 1, 1, //160-175   99: tableswitch, lookupswitch
    1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 1, 3, 2, 3, 1, 1, //176-191
    //          //          //          //
    3, 3, 1, 1, 0, 4, 3, 3, 5, 5, 0, 1, 1, 1, 1, 1, //192-207
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //208...
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,  
    0 /*needed to optimize "goto L2; L1: L2:..."*/}; // 256: label

}

