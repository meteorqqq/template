package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;


/**
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    private static final int regNum = 7;
    private final List<String> assembly = new ArrayList<>();
    private final Map<Integer, IRVariable> regIRMap = new HashMap<>();
    public int index = 0;
    private List<Instruction> originInstructions = new ArrayList<>();
    private List<IRCount> irVariableList = new ArrayList<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        int count = 0;
        for (var instruction : originInstructions) {
            switch (instruction.getKind()) {
                case ADD -> {
                    count++;
                    boolean isLHSImm = instruction.getLHS().isImmediate();
                    boolean isRHSImm = instruction.getRHS().isImmediate();
                    if (isLHSImm | isRHSImm) {
                        if (isLHSImm) {
                            irVariableList.add(new IRCount((IRVariable) instruction.getRHS(), count));
                            irVariableList.add(new IRCount(instruction.getResult(), count));
                        } else {
                            irVariableList.add(new IRCount((IRVariable) instruction.getLHS(), count));
                            irVariableList.add(new IRCount(instruction.getResult(), count));
                        }
                    } else {
                        irVariableList.add(new IRCount((IRVariable) instruction.getRHS(), count));
                        irVariableList.add(new IRCount((IRVariable) instruction.getLHS(), count));
                        irVariableList.add(new IRCount(instruction.getResult(), count));
                    }
                }
                case MOV -> {
                    count++;
                    boolean isImm = instruction.getFrom().isImmediate();
                    irVariableList.add(new IRCount(instruction.getResult(), count));
                    if (!isImm) {
                        irVariableList.add(new IRCount((IRVariable) instruction.getFrom(), count));
                    }
                }
                case MUL, SUB -> {
                    count++;
                    boolean isLHSImm = instruction.getLHS().isImmediate();
                    boolean isRHSImm = instruction.getRHS().isImmediate();
                    if (isLHSImm | isRHSImm) {
                        count++;
                        if (isLHSImm) {
                            irVariableList.add(new IRCount(IRVariable.temp(), count));
                            irVariableList.add(new IRCount((IRVariable) instruction.getRHS(), count));
                            irVariableList.add(new IRCount(instruction.getResult(), count));
                        } else {
                            irVariableList.add(new IRCount(IRVariable.temp(), count));
                            irVariableList.add(new IRCount((IRVariable) instruction.getLHS(), count));
                            irVariableList.add(new IRCount(instruction.getResult(), count));
                        }
                    } else {
                        irVariableList.add(new IRCount((IRVariable) instruction.getRHS(), count));
                        irVariableList.add(new IRCount((IRVariable) instruction.getLHS(), count));
                        irVariableList.add(new IRCount(instruction.getResult(), count));
                    }
                }
                case RET -> {
                    count++;
                    irVariableList.add(new IRCount((IRVariable) instruction.getReturnValue(), count));
                }
            }
        }
        this.originInstructions = originInstructions;
    }

    public void minIRList(List<IRCount> irVariableList) {
        List<IRVariable> irList = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();
        List<IRCount> minList = new ArrayList<>();
        Collections.reverse(irVariableList);
        for (var ir : irVariableList) {
            if (!irList.contains(ir.irVariable)) {
                irList.add(ir.irVariable);
                indexList.add(ir.index);
            }
        }
        for (int i = 0; i < irList.size(); i++) {
            minList.add(new IRCount(irList.get(i), indexList.get(i)));
        }
        this.irVariableList = minList;
    }

    /*
     *  寄存器查找算法，寻找IRVariable对应寄存器
     *  HashMap大小为2的幂次，用循环限制大小
     */
    public int getReg(IRVariable irVariable) throws RuntimeException {
        minIRList(irVariableList);
        if (regIRMap.containsValue(irVariable)) {
            for (int getKey = 0; getKey < regNum; getKey++) {
                if (regIRMap.get(getKey).equals(irVariable)) return getKey;
            }
        } else {
            for (int getKey = 0; getKey < regNum; getKey++) {
                if (!regIRMap.containsKey(getKey)) {
                    regIRMap.put(getKey, irVariable);
                    return getKey;
                }
            }
            for (var ir : irVariableList) {
                if (index > ir.index) {
                    for (int getKey = 0; getKey < regNum; getKey++) {
                        if (regIRMap.get(getKey).equals(ir.irVariable)) {
                            regIRMap.remove(getKey, ir.irVariable);
                            regIRMap.put(getKey, irVariable);
                            irVariableList.remove(ir);
                            return getKey;
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Reg Error");
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        assembly.add(".text");
        for (Instruction instruction : originInstructions) {
            int regRes, regL, regR, regTemp;
            switch (instruction.getKind()) {
                case RET -> {
                    index++;
                    regRes = getReg((IRVariable) instruction.getReturnValue());
                    assembly.add("    mv a0, t" + regRes);
                }
                case MOV -> {
                    index++;
                    boolean isImm = instruction.getFrom().isImmediate();
                    if (isImm) {
                        regRes = getReg(instruction.getResult());
                        assembly.add("    li t" + regRes + ", " + ((IRImmediate) instruction.getFrom()).getValue());
                    } else {
                        regL = getReg((IRVariable) instruction.getFrom());
                        regRes = getReg(instruction.getResult());
                        assembly.add("    mv t" + regRes + ", t" + regL);
                    }
                }
                case ADD -> {
                    index++;
                    boolean isLHSImm = instruction.getLHS().isImmediate();
                    boolean isRHSImm = instruction.getRHS().isImmediate();
                    if (isLHSImm & isRHSImm) {
                        regRes = getReg(instruction.getResult());
                        assembly.add("    li t" + regRes + ", " + (((IRImmediate) instruction.getLHS()).getValue() + ((IRImmediate) instruction.getRHS()).getValue()));
                    } else if (isLHSImm | isRHSImm) {
                        regRes = getReg(instruction.getResult());
                        if (isLHSImm) {
                            regL = getReg((IRVariable) instruction.getRHS());
                            assembly.add("    addi t" + regRes + ", t" + regL + ", " + ((IRImmediate) instruction.getLHS()).getValue());
                        } else {
                            regL = getReg((IRVariable) instruction.getLHS());
                            assembly.add("    addi t" + regRes + ", t" + regL + ", " + ((IRImmediate) instruction.getRHS()).getValue());
                        }
                    } else {
                        regRes = getReg(instruction.getResult());
                        regL = getReg((IRVariable) instruction.getLHS());
                        regR = getReg((IRVariable) instruction.getRHS());
                        assembly.add("    add t" + regRes + ", t" + regL + ", t" + regR);
                    }
                }
                case SUB -> {
                    index++;
                    boolean isLHSImm = instruction.getLHS().isImmediate();
                    boolean isRHSImm = instruction.getRHS().isImmediate();
                    if (isLHSImm & isRHSImm) {
                        regRes = getReg(instruction.getResult());
                        assembly.add("    li t" + regRes + ", " + (((IRImmediate) instruction.getLHS()).getValue() - ((IRImmediate) instruction.getRHS()).getValue()));
                    } else if (isLHSImm | isRHSImm) { //  新建缓存寄存器
                        index++;
                        regRes = getReg(instruction.getResult());
                        regTemp = getReg(IRVariable.temp());
                        if (isLHSImm) {
                            regR = getReg((IRVariable) instruction.getRHS());
                            assembly.add("    li t" + regTemp + ", " + ((IRImmediate) instruction.getLHS()).getValue());
                            assembly.add("    sub t" + regRes + ", t" + regTemp + ", t" + regR);
                        } else {
                            regL = getReg((IRVariable) instruction.getLHS());
                            assembly.add("    li t" + regTemp + ", " + ((IRImmediate) instruction.getRHS()).getValue());
                            assembly.add("    sub t" + regRes + ", t" + regL + ", t" + regTemp);
                        }
                    } else {
                        regRes = getReg(instruction.getResult());
                        regL = getReg((IRVariable) instruction.getLHS());
                        regR = getReg((IRVariable) instruction.getRHS());
                        assembly.add("    sub t" + regRes + ", t" + regL + ", t" + regR);
                    }
                }
                case MUL -> {
                    index++;
                    boolean isLHSImm = instruction.getLHS().isImmediate();
                    boolean isRHSImm = instruction.getRHS().isImmediate();
                    if (isLHSImm & isRHSImm) {
                        regRes = getReg(instruction.getResult());
                        assembly.add("    li t" + regRes + ", " + (((IRImmediate) instruction.getLHS()).getValue() * ((IRImmediate) instruction.getRHS()).getValue()));
                    } else if (isLHSImm | isRHSImm) { //  新建缓存寄存器
                        index++;
                        regRes = getReg(instruction.getResult());
                        regTemp = getReg(IRVariable.temp());
                        if (isLHSImm) {
                            regR = getReg((IRVariable) instruction.getRHS());
                            assembly.add("    li t" + regTemp + ", " + ((IRImmediate) instruction.getLHS()).getValue());
                            assembly.add("    mul t" + regRes + ", t" + regTemp + ", t" + regR);
                        } else {
                            regL = getReg((IRVariable) instruction.getLHS());
                            assembly.add("    li t" + regTemp + ", " + ((IRImmediate) instruction.getRHS()).getValue());
                            assembly.add("    mul t" + regRes + ", t" + regL + ", t" + regTemp);
                        }
                    } else {
                        regRes = getReg(instruction.getResult());
                        regL = getReg((IRVariable) instruction.getLHS());
                        regR = getReg((IRVariable) instruction.getRHS());
                        assembly.add("    mul t" + regRes + ", t" + regL + ", t" + regR);
                    }
                }
            }
        }
    }

    public List<String> getASM() {
        return assembly;
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path, getASM().stream().map(String::toString).toList());
    }
}

