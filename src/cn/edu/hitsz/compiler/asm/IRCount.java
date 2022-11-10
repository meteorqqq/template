package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRVariable;

public class IRCount {
    public IRVariable irVariable;
    public int index;

    public IRCount(IRVariable irVariable, int index) {
        this.irVariable = irVariable;
        this.index = index;
    }
}
