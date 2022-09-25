package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private final ArrayList<Token> tokens = new ArrayList<>();
    private String buffer;
    private int begin = 0;
    private int end = 0;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.buffer = null;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        //直接采用完整读入方法
        buffer = FileUtils.readFile(path);
    }

    /**
     * 判断是否分析结束
     */
    private boolean notEnd() {
        return end < buffer.length();
    }

    /**
     * 获取token首个字符
     */
    private char getChar() {
        char c;
        do {
            c = buffer.charAt(end);
            begin += 1;
            end += 1;
        } while (notEnd() && blankCharacter(c));
        begin--;
        return c;
    }

    /**
     * 获取当前字符的下一个字符
     */
    private char getNextChar() {
        return buffer.charAt(end);
    }

    private String getString(int begin, int end) {
        return buffer.substring(begin, end);
    }

    /**
     * 解析字符串，获得关键字/标识符
     */
    private Token getNextToken() {
        Token token = null;
        String string;
        char c = getChar();

        if (firstId(c) && notEnd()) {
            while (id(getNextChar()) && notEnd()) {
                end++;
            }
            string = getString(begin, end);
            if (TokenKind.isAllowed(string)) {
                token = Token.simple(string);
            } else {
                token = Token.normal("id", string);
                if (!symbolTable.has(string)) {
                    symbolTable.add(string);
                }
            }
        } else if (intConst(c) && notEnd()) {
            while (intConst(getNextChar()) && notEnd()) {
                end++;
            }
            string = getString(begin, end);
            token = Token.normal("IntConst", string);
        } else if (symbol(c)) {
            if (c == ';') {
                string = "Semicolon";
            } else {
                string = String.valueOf(c);
            }
            token = Token.simple(string);
        }

        begin = end;
        return token;
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        while (notEnd()) {
            tokens.add(getNextToken());
        }
        //添加终止符
        tokens.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        tokens.removeIf(Objects::isNull);
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(),false).map(Token::toString).toList()
        );
    }

    /**
     * 判断是否为空白字符
     */
    private boolean blankCharacter(char c) {
        return ((c == ' ') || (c == '\n') || (c == '\t') || (c == '\r'));
    }

    /**
     * 判断是否为数字
     */
    private boolean intConst(char c) {
        return ((c >= '0') && (c <= '9'));
    }

    /**
     * 判断是否为分隔符/运算符
     */
    private boolean symbol(char c) {
        return ((c == '=') || (c == ',') || (c == ';') || (c == '+') || (c == '-') || (c == '/') || (c == '(') || (c == ')') || (c == '*'));
    }

    /**
     * 判断是否为[a-zA-Z]
     */
    private boolean id(char c) {
        return ((c >= 'a') && (c <= 'z') || ((c >= 'A') && (c <= 'Z')));
    }

    /**
     * 判断是否为[a-zA-Z_]
     */
    private boolean firstId(char c) {
        return (id(c) || (c == '_'));
    }
}
