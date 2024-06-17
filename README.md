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

## 完成状况

1. program      DONE
2. stmt_list    **TODO**
3. stmt         **TODO**
4. assign_stmt  DONE
5. if_stmt      **TODO** else还未实现
6. while_stmt   DONE 
7. scan_stmt    **TODO**
8. print_stmt   DONE
9. bool_expr    DONE
10. expr        DONE
11. term        DONE
12. factor      DONE

13. 声明        DONE
14. string      **TODO**
