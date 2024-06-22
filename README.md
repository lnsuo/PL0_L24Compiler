# PL0_L24Compiler

## 编译命令

1. `javac -encoding UTF-8 *.java`
2. `java PL0`

## 语法

```txt
<program>           =   "main" "{" <declare_list> <procedure_list> <stmt_list> "}"

<declare_list>      =   [<vardeclare>] [<strdeclare>]
<procedure_list>    =   {<procedure> ";"}
<stmt_list>         =   {<stmt> ";"}

<vardeclare>        =   "var" <id> {"," <id>} ";"
<strdeclare>        =   "str" <id> {"," <id>} ";"

<procedure>         =   "procedure" <id> ":" <declare_list> "{" <stmt_list> "}"
<stmt>              =   <assign_stmt>| <if_stmt> |<while_stmt> |<scan_stmt> | <print_stmt>

<assign_stmt>       =   <ident> "=" <expr>
<if_stmt>           =   "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "end"
                    |   "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "else" "{"<stmt_list>"}" "end"
<while_stmt>        =   "while" "("<bool_expr>")" "{"<stmt_list>"}"
<scan_stmt>         =   "scan" "(" <ident> {"," <ident>} ")"
<print_stmt>        =   "print" "(" <expr> {"," <expr>} ")"

<bool_expr>         =   <expr> ("=="|"!="|"<"|"<="|">"|">=") <expr>

<expr>              = ["+"|"-"]<term>{("+"|"-")<term>}
<term>              = <factor>{("*"|"/")<factor>}>
<factor>            = <ident>|<number>|<string>|"("<expr>")"
```

## 字符串扩展

str 的基本运算符有"+"和“*"，支持print和scan。

1. 两个字符串之间的"+"运算得到它们之间的连接，
   字符串与整数之间的"+"运算定义为将整数转化为字符串后得到的串。
2. 仅支持字符串 str 与整数 n 之间的"*"，定义为 n 个 str 相连接。在语法中，只能按照"str \* n"，不能写成"n \* str"
3. 在print中，只能print(<str_ident>)，无法处理字符串运算

## 程序结构

1. PL0.java: 主程序入口
   先使用Scanner进行语法分析，语法分析通过后使用Interpreter解释生成的虚拟机代码
2. Scanner.java: 词法分析器
   词法分析器负责从源代码里面读取文法符号
3. Parser.java: 语法分析器
   在语法分析的过程中穿插着语法错误检查和目标代码生成。
4. Interpreter.java: 解释器
   负责将Parser生成的类P-Code代码进行解释
5. Table.java: 符号表
6. Symbol.java: 各种符号的编码
7. SymSet.java: 包装后的Bitset，便于编写代码
8. Data.java: 一个简单的类用来处理字符串和数字两种不同的数据类型
9. Err.java: 一个简单的出错处理类

## 出错表定义

1. Parser.start()
   101: 缺少main
   102: 缺少左括号
   103: 缺少右括号
2. Parser.parseStmtList()
   111: 超出最大允许嵌套的声明层
   112: 声明var时缺少分号
   113: 声明str时缺少分号
   114: 声明procedure后应为标识符
   115: 声明procedure时缺少冒号
   116: 声明procedure时，当前符号不在nxtlev中
   117: 声明procedure时，缺少分号
   118: 声明部分中，当前符号不在nxtlev中
   119: 后跟符号不在后跟符号集中
3. Parse.parseVarDeclaration()
   121: var声明后缺少标识符
4. Parse.parseStrDeclaration()
   131: str声明后缺少标识符
5. Parse.parseStatment()
   141: 当前语句符号不在后跟符号集中
6. Parse.parseWhileStatement()
   151: while语句缺少左括号
   152: while语句缺少右括号
7. Parse.parseBraceStatment()
   161: 语句后缺少分号
   162: 缺少必要的右大括号
8. Parse.parseIfStatment()
   171: if语句缺少左括号
   172: if语句缺少右括号
   173: if语句缺少then
   174: if语句缺少end
9. Parse.parseCallStatement()
    181: 未找到procedure
    182: call后标识符应为procedure
    183: call后应为标识符
10. Parse.parsePrintStatement()
    191: print后缺少左括号
    192: print后缺少右括号
11. Parse.parseScanStatement()
    201: scan中应为声明过的变量
    202: scan中的标识符不是变量
    203: scan缺少左括号
    204: scan缺少右括号
12. Parse.parseAssignStatement()
    211: 为var赋值应为单等号
    212: 为str赋值应为单等号
    213: 赋值语句格式错误
    214: 未找到赋值的变量
13. Parse.parseFactor()
    221: 开始符号不在表示因子开始的符号中
    222: 不能为过程
    223: 标识符未声明
    224: 数字超过最大位数限制
    225: 缺少右括号
    226: 当前符号不在后跟符号集中
14. Parse.parseStrFactor()
    231: 开始符号不在表示因子开始的符号中
    232: 不能为过程
    233: 标识符种类错误
    234: 标识符未声明
    235: 数字超过最大位数限制
    236: 缺少右括号
    237: 当前符号不在后跟符号集中
15. Parse.parseBoolExpr()
    241: 当前符号不属于逻辑运算符
16. Scanner.matchNumber()
    25: 数字超出最大位数限制

## 虚拟机

### 虚拟机指令格式

虚拟机指令被表示为Instruction类的一个实例，每个指令包括以下几个部分：

1. 指令类型（Fct）：这是一个枚举类型，定义了不同的指令操作。
2. 层次差（l）：这通常指的是指令参数与当前执行环境的层级差异，用于处理变量作用域。
3. 指令参数（a）：这是一个Data对象，可以是整数、字符串。

### 指令系统及解释

LIT, LITS:  将指令参数a的值取到栈顶。
OPR:        数学、逻辑运算以及输入输出
LOD, LODS:  取相对当前过程的数据基地址为a的内存的值到栈顶
STO, STOS:  将栈顶的值存到相对当前过程的数据基地址为a的内存中
CAL:        调用过程
INT:        分配内存
JMP:        直接跳转
JPC:        条件跳转（当栈顶为0时跳转）
