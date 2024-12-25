package backend;

import java.util.ArrayList;

public enum Register {
    ZERO("$zero"),
    AT("$at"),
    V0("$v0"), V1("$v1"),
    A0("$a0"), A1("$a1"), A2("$a2"), A3("$a3"),
    T0("$t0"), T1("$t1"), T2("$t2"), T3("$t3"), T4("$t4"), T5("$t5"), T6("$t6"), T7("$t7"),
    S0("$s0"), S1("$s1"), S2("$s2"), S3("$s3"), S4("$s4"), S5("$s5"), S6("$s6"), S7("$s7"),
    T8("$t8"), T9("$t9"),
    K0("$k0"), K1("$k1"),
    GP("$gp"), SP("$sp"), FP("$fp"), RA("$ra");
    private String name;
    
    Register(String name) {
        this.name = name;
    }
    
    public static ArrayList<Register> getFreeRegisters() {
        /*
         * 该函数只会在初始化registerController和函数调用的时候被使用，用于初始化当前函数的freeRegisters
         * 图着色应该还会修改
         */
        ArrayList<Register> freeRegisters = new ArrayList<>();
        for (int i = T0.ordinal(); i <= T9.ordinal(); i++) {
            freeRegisters.add(values()[i]);
        }
        return freeRegisters;
    }
    
    public static ArrayList<Register> getFreeArgs() {
        ArrayList<Register> freeArgs = new ArrayList<>();
        for (int i = A1.ordinal(); i <= A3.ordinal(); i++) { //A0拿来系统调用
            freeArgs.add(values()[i]);
        }
        return freeArgs;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
