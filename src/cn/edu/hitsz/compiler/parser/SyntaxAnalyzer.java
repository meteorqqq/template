package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.LRTable;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private final List<Token> tokenList = new ArrayList<>();
    private LRTable lrTable;
    //

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // 使用list存储
        for (final var token:tokens){
            tokenList.add(token);
        }
    }

    public void loadLRTable(LRTable table) {
        this.lrTable = table;
    }

    public void run() {
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
        class StateToken{
            public final Status status;
            public final Term term;

            public StateToken(Status status,Term term){
                this.status = status;
                this.term = term;
            }
        }

        Stack<StateToken> stateTokens = new Stack<>();
        //初始化
        stateTokens.add(new StateToken(lrTable.getInit(), TokenKind.eof()));
        for (int i = 0;i < tokenList.size();){
            var token = tokenList.get(i);
            var action = lrTable.getAction(stateTokens.peek().status, token);
            switch (action.getKind()){
                case Accept -> {
                    callWhenInAccept(stateTokens.peek().status);
                    return;
                }
                case Shift -> {
                    callWhenInShift(action.getStatus(), token);
                    stateTokens.add(new StateToken(action.getStatus(), token.getKind()));
                    i++;
                }
                case Reduce -> {
                    var production = action.getProduction();
                    for (int j = 0; j < production.body().size(); j++){
                        stateTokens.pop();
                    }
                    callWhenInReduce(stateTokens.peek().status, production);
                    stateTokens.add(new StateToken(lrTable.getGoto(stateTokens.peek().status, production.head()), production.head()));
                }
                case Error -> {
                    throw new RuntimeException("Error");
                }
            }
        }
    }
}
