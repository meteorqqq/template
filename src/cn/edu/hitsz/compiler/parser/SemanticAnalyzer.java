package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

public class SemanticAnalyzer implements ActionObserver {
    private final Stack<SourceCodeType> sourceCodeTypeStack = new Stack<>();
    private final Stack<Token> tokenStack = new Stack<>();
    private SymbolTable symbolTable;

    @Override
    public void whenAccept(Status currentStatus) {
        sourceCodeTypeStack.clear();
        tokenStack.clear();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 4 -> { // S -> D id;
                var id = tokenStack.pop();
                if (symbolTable.has(id.getText())) {
                    var symbolTableEntry = symbolTable.get(id.getText());
                    symbolTableEntry.setType(sourceCodeTypeStack.pop());
                }
            }
            case 5 -> { // D -> int;
                sourceCodeTypeStack.add(SourceCodeType.Int);
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        tokenStack.add(currentToken);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }
}

