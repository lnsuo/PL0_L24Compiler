/**
 *　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
public class Parser {
	/**
	 * 对词法分析器的引用
	 */
	private Scanner lex; 
	/**
	 * 对符号表的引用
	 */
	private Table table; 
	/**
	 * 对目标代码生成器的引用
	 */
	private Interpreter interp;
	
	private final int symnum = Symbol.values().length;
	
	// 实际上这就是声明、语句和因子的FIRST集合
	/**
	 * 表示声明开始的符号集合
	 */
	private SymSet declbegsys;
	/**
	 * 表示语句开始的符号集合
	 */
	private SymSet statbegsys;
	/**
	 * 表示因子开始的符号集合
	 */
	private SymSet facbegsys;
	
	/**
	 * 当前符号，由nextsym()读入
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * 当前作用域的堆栈帧大小，或者说数据大小（data size）
	 */
	private int dx = 0;
	
	/**
	 * 构造并初始化语法分析器，这里包含了C语言版本中init()函数的一部分代码
	 * @param l 编译器的词法分析器
	 * @param t 编译器的符号表
	 * @param i 编译器的目标代码生成器
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// 设置声明开始符号集
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);
		declbegsys.set(Symbol.strsym);

		// 设置语句开始符号集
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.scansym);
		statbegsys.set(Symbol.printsym);
		statbegsys.set(Symbol.lbrace);
		statbegsys.set(Symbol.callsym);

		// 设置因子开始符号集
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.strsym);
		facbegsys.set(Symbol.lparen);
	}
	
	/**
	 * 启动语法分析过程，此前必须先调用一次nextsym()
	 * @see #nextSym()
	 */
	public void start() {
		// <program> = "main" "{" <stmt_list> "}"

		nextSym();		// 前瞻分析需要预先读入一个符号

		checkNextSymbol(Symbol.mainsym, 101);
		checkNextSymbol(Symbol.lbrace, 102);

		SymSet nxtlev = new SymSet(symnum);
		nxtlev.or(declbegsys);
		nxtlev.or(statbegsys);
		nxtlev.set(Symbol.rbrace);

		parseStmtList(0, nxtlev, false);
		
		if (sym != Symbol.rbrace)
			Err.report(103);
	}
	
	/**
	 * 获得下一个语法符号，这里只是简单调用一下getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}
	
	/**
	 * 测试当前符号是否合法
	 * 
	 * @param s1 我们需要的符号
	 * @param s2 如果不是我们需要的，则需要一个补救用的集合
	 * @param errcode 错误号
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
		//（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
		// 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
		// 号），以及检测不通过时的错误号。
		if (!s1.get(sym)) {
			Err.report(errcode);
			// 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * 分析<分程序>
	 * 
	 * @param lev 当前分程序所在层
	 * @param fsys 当前模块后跟符号集
	 */
	public void parseStmtList(int lev, SymSet fsys, boolean haveBrace) {
		// <分程序> := [<变量说明部分>][<过程说明部分>]<语句>
		// <stmt_list> = {<变量声明> ";"}{<stmt> ";"}
		
		int dx0, tx0, cx0;				// 保留初始dx，tx和cx
		SymSet nxtlev = new SymSet(symnum);
		
		dx0 = dx;						// 记录本层之前的数据量（以便恢复）
		dx = 3;
		tx0 = table.tx;					// 记录本层名字的初始位置（以便恢复）
		table.get(table.tx).adr = interp.cx;
		
		interp.gen(Fct.JMP, 0, 0);
		
		if (lev > PL0.levmax)
			Err.report(111);
		
		// 分析<说明部分>
		do {
			// <变量说明部分>
			if (sym == Symbol.varsym) {
				nextSym();
				parseVarDeclaration(lev);
				while (sym == Symbol.comma) {
					nextSym();
					parseVarDeclaration(lev);
				}

				checkNextSymbol(Symbol.semicolon, 112);
			}

			if (sym == Symbol.strsym) {
				nextSym();
				parseStrDeclaration(lev);
				while (sym == Symbol.comma) {
					nextSym();
					parseStrDeclaration(lev);
				}

				checkNextSymbol(Symbol.semicolon, 113);
			}


			
			// <过程说明部分>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else { 
					Err.report(114);				// procedure后应为标识符
				}

				checkNextSymbol(Symbol.startsym, 115);
				
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.semicolon);
				parseStmtList(lev+1, nxtlev, true);
				
				if (sym == Symbol.semicolon) {
					nextSym();
					nxtlev = (SymSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					test(nxtlev, fsys, 116);
				} else { 
					Err.report(117);				// 漏掉了分号
				}
			}
			
			nxtlev = (SymSet) statbegsys.clone(); 
			nxtlev.set(Symbol.ident);

			if (sym == Symbol.rbrace) {
				break;
			}

			test(nxtlev, declbegsys, 118);
		} while (declbegsys.get(sym));		// 直到没有声明符号
		
		// 开始生成当前过程代码
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a.vn = interp.cx;
		item.adr = interp.cx;					// 当前过程代码地址
		item.size = dx;							// 声明部分中每增加一条声明都会给dx增加1，
												// 声明部分已经结束，dx就是当前过程的堆栈帧大小
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// 生成分配内存代码
		
		table.debugTable(tx0);
			
		// 分析<语句>
		nxtlev = (SymSet) fsys.clone();		// 每个后跟符号集和都包含上层后跟符号集和，以便补救
		nxtlev.set(Symbol.semicolon);		// 语句后跟符号为分号或'}'
		nxtlev.set(Symbol.rbrace);
		parseBraceStatement(nxtlev, lev, haveBrace);
		interp.gen(Fct.OPR, 0, 0);		// 每个过程出口都要使用的释放数据段指令
		
		nxtlev = new SymSet(symnum);	// 分程序没有补救集合
		test(fsys, nxtlev, 129);				// 检测后跟符号正确性
		
		interp.listcode(cx0);
		
		dx = dx0;							// 恢复堆栈帧计数器
		table.tx = tx0;						// 回复名字表位置
	}

	/**
	 * 分析<变量说明部分>
	 * @param lev 当前层次
	 */
	void parseVarDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			if (table.enter(Objekt.variable, lev, dx))
				dx ++;
			nextSym();
		} else {
			Err.report(121);					// var 后应是标识
		}
	}

	/**
	 * 分析<字符串说明部分>
	 * @param lev 当前层次
	 */
	void parseStrDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			table.enter(Objekt.string, lev, dx);
			dx ++;
			nextSym();
		} else {
			Err.report(131);					// var 后应是标识
		}
	}

	/**
	 * 分析<语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth 的 PL/0 编译器使用一系列的if...else...来处理
		// 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
		switch (sym) {
		case ident:
			parseAssignStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
			break;
		case whilesym:
			parseWhileStatement(fsys, lev);
			break;
		case scansym:
			parseScanStatement(fsys, lev);
			break;
		case printsym:
			parsePrintStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case lbrace:
			parseBraceStatement(fsys, lev, true);
			break;
		default:
			nxtlev = new SymSet(symnum);
			test(fsys, nxtlev, 141);
			break;
		}
	}

	/**
	 * 分析<当型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		
		cx1 = interp.cx;						// 保存判断条件操作的位置
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// 后跟符号为do

		checkNextSymbol(Symbol.lparen, 151);

		parseBoolExpr(nxtlev, lev);			// 分析<条件>

		checkNextSymbol(Symbol.rparen, 152);

		cx2 = interp.cx;						// 保存循环体的结束的下一个位置
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转，但跳出循环的地址未知

		parseStatement(fsys, lev);				// 分析<语句>

		interp.gen(Fct.JMP, 0, cx1);			// 回头重新判断条件
		interp.code[cx2].a.vn = interp.cx;			// 反填跳出循环的地址，与<条件语句>类似
	}

	/**
	 * 分析<复合语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBraceStatement(SymSet fsys, int lev, boolean haveBrace) {
		SymSet nxtlev;
		
		if (haveBrace) {
			nextSym();
		}
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.rbrace);
		parseStatement(nxtlev, lev);
		// 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			checkNextSymbol(Symbol.semicolon, 161);

			parseStatement(nxtlev, lev);
		}

		if (haveBrace) {
			checkNextSymbol(Symbol.rbrace, 162);
		}
	}
	/**
	 * 分析<条件语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;

		nextSym();

		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.thensym);				// 后跟符号为then或do ???
		nxtlev.set(Symbol.dosym);

		checkNextSymbol(Symbol.lparen, 171);

		parseBoolExpr(nxtlev, lev);			// 分析<条件>

		checkNextSymbol(Symbol.rparen, 172);

		checkNextSymbol(Symbol.thensym, 173);

		cx1 = interp.cx;						// 保存当前指令地址
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转指令，跳转地址未知，暂时写0

		parseStatement(fsys, lev);				// 处理then后的语句

		if (sym == Symbol.elsesym) {
			cx2 = interp.cx;
			interp.gen(Fct.JMP, 0, 0);		// 跳过else语句

			interp.code[cx1].a.vn = interp.cx;			// 经statement处理后，cx为then后语句执行
													// 完的位置，它正是前面未定的跳转地址
			nextSym();
			parseStatement(fsys, lev);

			interp.code[cx2].a.vn = interp.cx;
		} else {
			interp.code[cx1].a.vn = interp.cx;			// 经statement处理后，cx为then后语句执行
													// 完的位置，它正是前面未定的跳转地址
		}

		checkNextSymbol(Symbol.endsym, 174);
	}

	/**
	 * 分析<过程调用语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(181);					// 过程未找到
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(182);				// call后标识符应为过程
			}
			nextSym();
		} else {
			Err.report(183);						// call后应为标识符
		}
	}

	/**
	 * 分析<写语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parsePrintStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			int cnt = 0;
			do {
				if (cnt > 0) {
					interp.gen(Fct.OPR, 0, 17);
				}
				cnt++;

				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);

				if (sym == Symbol.strsym) {
					interp.gen(Fct.LITS, 0, new Data(lex.str));
					nextSym();
				} else {
					Table.Item item = table.get(table.position(lex.id));
					if (item.kind == Objekt.string) {
						parseStrExpression(nxtlev, lev);
					} else {
						parseExpression(nxtlev, lev);
					}

				}
				interp.gen(Fct.OPR, 0, 14);

			} while (sym == Symbol.comma);
			
			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(192);				// print()中应为完整表达式
		} else {
			Err.report(191);
		}
		interp.gen(Fct.OPR, 0, 15);
	}

	/**
	 * 分析<读语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseScanStatement(SymSet fsys, int lev) {
		int i;
		
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				if (sym == Symbol.ident)
					i = table.position(lex.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(201);			// read()中应是声明过的变量名
				} else {
					Table.Item item = table.get(i);
					if (item.kind == Objekt.variable) {
						interp.gen(Fct.OPR, 0, 16);
						interp.gen(Fct.STO, lev-item.level, item.adr);
					} else if (item.kind == Objekt.string) {
						interp.gen(Fct.OPR, 0, 20);
						interp.gen(Fct.STOS, lev-item.level, item.adr);
					} else {
						Err.report(202);		// read()中的标识符不是变量
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(203);					// 格式错误，应是左括号
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(204);					// 格式错误，应是右括号
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * 分析<赋值语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseAssignStatement(SymSet fsys, int lev) {
		int i;
		SymSet nxtlev;
		
		i = table.position(lex.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.variable) {
				nextSym();

				checkNextSymbol(Symbol.becomes, 211);

				nxtlev = (SymSet) fsys.clone();
				parseExpression(nxtlev, lev);
				// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else if (item.kind == Objekt.string) {
				nextSym();

				checkNextSymbol(Symbol.becomes, 212);
				
				nxtlev = (SymSet) fsys.clone();
				parseStrExpression(nxtlev, lev);
				// parseStrExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sts命令完成赋值
				interp.gen(Fct.STOS, lev - item.level, item.adr);
			} else {
				Err.report(213);						// 赋值语句格式错误
			}
		} else {
			Err.report(214);							// 变量未找到
		}
	}

	/**
	 * 分析<表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		// 分析[+|-]<项>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
		}
		
		// 分析{<加法运算符><项>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else
				interp.gen(Fct.OPR, 0, 3);
		}
	}

	/**
	 * 分析<字符串表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseStrExpression(SymSet fsys, int lev) {
		SymSet nxtlev;

		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.plus);
		parseStrTerm(nxtlev, lev);

		while (sym == Symbol.plus) {
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			parseStrTerm(nxtlev, lev);

			interp.gen(Fct.OPR, 0, 18);
		}
	}
	/**
	 * 分析<项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;

		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		parseFactor(nxtlev, lev);
		
		// 分析{<乘法运算符><因子>}
		while (sym == Symbol.times || sym == Symbol.slash) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else
				interp.gen(Fct.OPR, 0, 5);
		}
	}
	/**
	 * 分析<字符串项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseStrTerm(SymSet fsys, int lev) {
		SymSet nxtlev;

		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		parseStrFactor(nxtlev, lev);
		
		// 分析{ * num}
		boolean isFirst = true;
		while (sym == Symbol.times) {
			nextSym();
			parseFactor(nxtlev, lev);

			if (isFirst) {	// str * num
				isFirst = false;
				interp.gen(Fct.OPR, 0, 19);
			} else {		// str * (num * num)
				interp.gen(Fct.OPR, 0, 19);
			}
		}
	}

	/**
	 * 分析<因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		test(facbegsys, fsys, 221);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为常量或变量
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case constant:							// 名字为常量
						interp.gen(Fct.LIT, 0, item.val);
						break;
					case variable:							// 名字为变量
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case procedure:							// 名字为过程
						Err.report(222);				// 不能为过程
						break;
					case string:							// 名字为字符串
						interp.gen(Fct.LODS, lev - item.level, item.adr);
						break;
					}
				} else {
					Err.report(223);					// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {	// 因子为数 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(224);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(225);					// 缺少右括号
			} else {
				// 做补救措施
				test(fsys, facbegsys, 226);
			}
		}
	}

	/**
	 * 分析<因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseStrFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		test(facbegsys, fsys, 231);			// 检测因子的开始符号

		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为 var 或 str
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case variable:							// 名字为变量
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case string:							// 名字为字符串
						// System.out.println("Item: " + item);
						interp.gen(Fct.LODS, lev - item.level, item.adr);
						break;
					case procedure:							// 名字为过程
						Err.report(232);			// 不能为过程
						break;
					default:
						Err.report(233);
						break;
					}
				} else {
					Err.report(234);				// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {	// 因子为数 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(235);
					num = 0;
				}
				interp.gen(Fct.LITS, 0, new Data(Integer.toString(num)));
				nextSym();
			} else if (sym == Symbol.strsym) {
				String str = lex.str;
				interp.gen(Fct.LITS, 0, new Data(str));
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();

				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseStrExpression(nxtlev, lev);

				checkNextSymbol(Symbol.rparen, 236);
			} else {
				// 做补救措施
				test(fsys, facbegsys, 237);
			}
		}
	}

	/**
	 * 分析<条件>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBoolExpr(SymSet fsys, int lev) {
		Symbol relop;
		SymSet nxtlev;
		
		if (sym == Symbol.oddsym) {
			// 分析 ODD<表达式>
			nextSym();
			parseExpression(fsys, lev);
			interp.gen(Fct.OPR, 0, 6);
		} else {
			// 分析<表达式><关系运算符><表达式>
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			parseExpression(nxtlev, lev);
			if (sym == Symbol.eql || sym == Symbol.neq 
					|| sym == Symbol.lss || sym == Symbol.leq
					|| sym == Symbol.gtr || sym == Symbol.geq) {
				relop = sym;
				nextSym();
				parseExpression(fsys, lev);
				switch (relop) {
				case eql:
					interp.gen(Fct.OPR, 0, 8);
					break;
				case neq:
					interp.gen(Fct.OPR, 0, 9);
					break;
				case lss:
					interp.gen(Fct.OPR, 0, 10);
					break;
				case geq:
					interp.gen(Fct.OPR, 0, 11);
					break;
				case gtr:
					interp.gen(Fct.OPR, 0, 12);
					break;
				case leq:
					interp.gen(Fct.OPR, 0, 13);
					break;
				default:
					break;
				}
			} else {
				Err.report(241);
			}
		}
	}
	private void checkNextSymbol(Symbol s, int errcode) {
		if (sym == s) {
			nextSym();
		} else {
			Err.report(errcode);
		}
	}
}
