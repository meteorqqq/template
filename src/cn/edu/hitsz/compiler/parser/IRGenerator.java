package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static java.lang.Integer.parseInt;

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private final ArrayList<Instruction> instructions = new ArrayList<>();
    private final Stack<IRValue> irValueStack = new Stack<>();
    private SymbolTable symbolTable;

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        var text = currentToken.getText();
        if (currentToken.getKindId().equals("IntConst")) { // 立即数
            irValueStack.add(IRImmediate.of(parseInt(text)));
        } else if (currentToken.getKindId().equals("id")) { // 变量
            if (symbolTable.has(text)) {
                irValueStack.add(IRVariable.named(text));
            }
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 6 -> { // S -> id = E
                var op1 = irValueStack.pop();
                var op2 = irValueStack.pop();
                instructions.add(Instruction.createMov((IRVariable) op2, op1));
            }
            case 7 -> { // S -> return E
                var op1 = irValueStack.pop();
                instructions.add(Instruction.createRet(op1));
            }
            case 8 -> { // E -> E + A
                var temp = IRVariable.temp();
                var op1 = irValueStack.pop();
                var op2 = irValueStack.pop();
                instructions.add(Instruction.createAdd(temp, op2, op1));
                irValueStack.add(temp);
            }
            case 9 -> { // E -> E - A
                var temp = IRVariable.temp();
                var op1 = irValueStack.pop();
                var op2 = irValueStack.pop();
                instructions.add(Instruction.createSub(temp, op2, op1));
                irValueStack.add(temp);
            }
            case 11 -> { // A -> A * B
                var temp = IRVariable.temp();
                var op1 = irValueStack.pop();
                var op2 = irValueStack.pop();
                instructions.add(Instruction.createMul(temp, op2, op1));
                irValueStack.add(temp);
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        irValueStack.clear();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }

    public List<Instruction> getIR() {
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

