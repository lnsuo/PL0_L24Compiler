/**
 * 类P-Code指令类型
 */
enum Fct {
	LIT, OPR, LOD, STO, CAL, INT, JMP, JPC, STOS, LODS, LITS
}

/**
 *　　这个类对应C语言版本中的 fct 枚举类型和 instruction 结构，代表虚拟机指令
 */
class Instruction {
	/**
	 * 虚拟机代码指令
	 */
	public Fct f;
	
	/**
	 * 引用层与声明层的层次差
	 */
	public int l;
	
	/**
	 * 指令参数
	 */
	public Data a;
}

/**
 *　　类P-Code代码解释器（含代码生成函数），这个类包含了C语言版中两个重要的全局变量 cx 和 code
 */
public class Interpreter {
	// 解释执行时使用的栈大小
	final int stacksize = 500;
	
	/**
	 * 虚拟机代码指针，取值范围[0, cxmax-1] 
	 */
	public int cx = 0;
	
	/**
	 * 存放虚拟机代码的数组
	 */
	public Instruction[] code = new Instruction[PL0.cxmax];
	
	/**
	 * 生成虚拟机代码
	 * @param x instruction.f
	 * @param y instruction.l
	 * @param z instruction.a
	 */
	public void gen(Fct x, int y, Data z) {
		if (cx >= PL0.cxmax) {
			throw new Error("Program too long");
		}
		
		code[cx] = new Instruction();
		code[cx].f = x;
		code[cx].l = y;
		code[cx].a = z.clone();
		cx ++;
	}

	/**
	 * 生成虚拟机代码
	 * @param x instruction.f
	 * @param y instruction.l
	 * @param z instruction.a
	 */
	public void gen(Fct x, int y, int z) {
		if (cx >= PL0.cxmax) {
			throw new Error("Program too long");
		}
		
		code[cx] = new Instruction();
		code[cx].f = x;
		code[cx].l = y;
		code[cx].a = new Data(z);
		cx ++;
	}

	/**
	 * 输出目标代码清单
	 * @param start 开始输出的位置
	 */
	public void listcode(int start) {
		if (PL0.listswitch) {
			for (int i=start; i<cx; i++) {
				String msg = i + " " + code[i].f + " " + code[i].l + " " + code[i].a.toString();
				System.out.println(msg);
				PL0.fa.println(msg);
			}
		}
	}
	
	/**
	 * 解释程序
	 */
	public void interpret() {
		int p, b, t;						// 指令指针，指令基址，栈顶指针
		Instruction i;							// 存放当前指令
		Data[] s = new Data[stacksize];		// 栈
		for (int _i = 0; _i < stacksize; _i++) {
			s[_i] = new Data();
		}
		
		System.out.println("start pl0");
		t = b = p = 0;
		s[0] = new Data(0);
		s[1] = new Data(0);
		s[2] = new Data(0);
		do {
			i = code[p];					// 读当前指令
			p ++;
			switch (i.f) {
			case LIT:				// 将a的值取到栈顶
				s[t].change(i.a.vn);
				t++;
				break;
			case OPR:				// 数学、逻辑运算
				switch (i.a.vn)
				{
				case 0:
					t = b;
					p = s[t+2].vn;
					b = s[t+1].vn;
					break;
				case 1:
					s[t-1].change(-s[t-1].vn);
					break;
				case 2:		// +
					t--;
					s[t-1].change(s[t-1].vn + s[t].vn);
					break;
				case 3:		// -
					t--;
					s[t-1].change(s[t-1].vn - s[t].vn);
					break;
				case 4:		// *
					t--;
					s[t-1].change(s[t-1].vn * s[t].vn);
					break;
				case 5:		// /
					t--;
					s[t-1].change(s[t-1].vn / s[t].vn);
					break;
				case 6:		// % 2
					s[t-1].change(s[t-1].vn % 2);
					break;
				case 8:		// ==
					t--;
					s[t-1].change(s[t-1].vn == s[t].vn ? 1 : 0);
					break;
				case 9:		// !=
					t--;
					s[t-1].change(s[t-1].vn != s[t].vn ? 1 : 0);
					break;
				case 10:	// <
					t--;
					s[t-1].change(s[t-1].vn < s[t].vn ? 1 : 0);
					break;
				case 11:	// >=
					t--;
					s[t-1].change(s[t-1].vn >= s[t].vn ? 1 : 0);
					break;
				case 12:	// >
					t--;
					s[t-1].change(s[t-1].vn > s[t].vn ? 1 : 0);
					break;
				case 13:	// <=
					t--;
					s[t-1].change(s[t-1].vn <= s[t].vn ? 1 : 0);
					break;
				case 14:	// print
					if (s[t-1].isNum()) {
						System.out.print(s[t-1].vn);
						PL0.fa2.print(s[t-1].vn);
					} else {
						System.out.print(s[t-1].vs);
						PL0.fa2.print(s[t-1].vs);
					}
					t--;
					break;
				case 15:	// print "\n"
					System.out.println();
					PL0.fa2.println();
					break;
				case 16:	// scan num
					System.out.print("?");
					PL0.fa2.print("?");
					s[t].change(0);
					try {
						s[t].change(Integer.parseInt(PL0.stdin.readLine()));
					} catch (Exception e) {}
					PL0.fa2.println(s[t]);
					t++;
					break;
				case 17:	// print " "
					System.out.print(" ");
					PL0.fa2.print(" ");
					break;
				case 18:	// str + str
					t--;
					s[t-1].change(s[t-1].vs + s[t].vs);
					break;
				case 19:	// str * num
					t--;
					String tmp = s[t-1].vs;
					s[t-1].vs = "";
					// System.out.println("19: vn = " + s[t].vn);
					for (int _i = 0; _i < s[t].vn; _i++) {
						s[t-1].change(s[t-1].vs + tmp);
					}
				}
				break;
			case LOD:				// 取相对当前过程的数据基地址为a的内存的值到栈顶
				s[t].change(s[base(i.l,s,b)+i.a.vn].vn);
				t++;
				break;
			case STO:				// 栈顶的值存到相对当前过程的数据基地址为a的内存
				t--;
				s[base(i.l, s, b) + i.a.vn].change(s[t].vn);
				break;
			case CAL:				// 调用子过程
				s[t].change(base(i.l, s, b)); 	// 将静态作用域基地址入栈
				s[t+1].change(b);					// 将动态作用域基地址入栈
				s[t+2].change(p);					// 将当前指令指针入栈
				b = t;  					// 改变基地址指针值为新过程的基地址
				p = i.a.vn;   					// 跳转
				break;
			case INT:			// 分配内存
				t += i.a.vn;
				break;
			case JMP:				// 直接跳转
				p = i.a.vn;
				break;
			case JPC:				// 条件跳转（当栈顶为0的时候跳转）
				t--;
				if (s[t].vn == 0)
					p = i.a.vn;
				break;
			case STOS:				// 将栈顶的值存到相对当前过程的数据基地址为a的内存
				t--;
				s[base(i.l, s, b) + i.a.vn].change(s[t].vs);
				break;
			case LODS:				// 取相对当前过程的数据基地址为a的内存的字符串到栈顶
				// System.out.println("Before: " + s[base(i.l,s,b)+i.a.vn].vs);
				s[t].change(s[base(i.l,s,b)+i.a.vn].vs);
				// System.out.println("LODS: " + s[t].vs);
				t++;
				break;
			case LITS:			// 将a的字符串取到栈顶
				s[t].change(i.a.vs);
				// System.out.println("LITS: " + s[t].vs);
				t++;
				break;
			}
		} while (p != 0);
	}
	
	/**
	 * 通过给定的层次差来获得该层的堆栈帧基地址
	 * @param l 目标层次与当前层次的层次差
	 * @param s 运行栈
	 * @param b 当前层堆栈帧基地址
	 * @return 目标层次的堆栈帧基地址
	 */
	private int base(int l, Data[] s, int b) {
		int b1 = b;
		while (l > 0) {
			b1 = s[b1].vn;
			l --;
		}
		return b1;
	}
}
