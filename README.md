# PL0_L24Compiler

## 编译命令

1. `javac -encoding UTF-8 *.java`
2. `java PL0`

## 已完成的语法

```txt
<program>           =   "main" "{" <vardeclare> <procedure_list> <stmt_list> "}"

<vardeclare>        =   "var" <id> {"," <id>} ";"

<procedure_list>    =   {<procedure> ";"}
<procedure>         =   "procedure" <id> ":" <vardeclare> "{" <stmt_list> "}"

<stmt_list>         =   {<stmt> ";"}
<stmt>              =   <assign_stmt>| <if_stmt> |<while_stmt> |<scan_stmt> | <print_stmt>

<assign_stmt>       =   <ident> "=" <expr>
<if_stmt>           =   "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "end"
                    |   "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "else" "{"<stmt_list>"}" "end"
<while_stmt>        =   "while" "("<bool_expr>")" "{"<stmt_list>"}"
<scan_stmt>         =   "scan" "(" <ident {"," <ident>} ")"
<print_stmt>        =   "print" "(" <expr> {"," <expr>} ")"

<bool_expr>         =   <expr> ("=="|"!="|"<"|"<="|">"|">=") <expr>

```

## 待完成

1. 直接print字符串
2. str的乘法
3. str 和 num 的加法
