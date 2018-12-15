import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;

public class Parser {
  private Lexer scanner;
  private Symbol lookahead;

  /**
  * Constructor for the Parser.
  * Set up a scanner and assigns a value to each rules of
  * the grammar. It is either the number of the rule in the grammar or
  * it's name if verbose is active.
  * @param filePath path to the file to parse
  * @param v tell if the verbose option is active or not
  * @param t tell if the option for drawing the tree is active or not
  */

  public Parser(BufferedReader filePath, boolean v, boolean t) throws IOException {
    this.scanner = new Lexer(filePath);
    this.lookahead = scanner.yylex();
  }

  /*
  * Fetch the next token to parse.
  */
  private void nextToken() throws IOException {
    this.lookahead = scanner.yylex();
  }

  /**
  * Compare the expected token to the current token.
  * @param token the expected token.
  * @return if the tokens match, return a AbstractSyntaxTree (either null or with
  * the proper label if draw tree is active).
  * @throws IOException give an error if the tokens do not match.
  */
  private AbstractSyntaxTree compareToken(LexicalUnit token) throws IOException {
    if (!(lookahead.getType().equals(token))){
      throw new Error("\nError at line " + lookahead.getLine() + ": " +
      lookahead.getType() + " expected " + token);
    }
    //Symbol label = lookahead;
    nextToken();
    return new AbstractSyntaxTree();
  }

  /**
  * Start the parsing of the input file at the initial symbol of the grammar.
  * @return if the tree drawing is not active the parser return an empty AbstractSyntaxTree. If
  * the execution was without errors the proper rules numbers/names have been written
  * on the standard output. We don't need retrieve the tree in this case so all the nodes are null.
  * @return if the tree drawing option was active, the parser return a AbstractSyntaxTree
  * containing the nodes with their labels and children. The ParsTree can then be
  * retrieve by Main in order to write the tree on the specified file.
  */
  public AbstractSyntaxTree startParse() throws IOException {
    return program();
  }

  /**
  * We allowed the input program to have as many endlines has it want at some specific
  * points in the program. They are ignored by the "standard" parser and return a node
  * called "SkipLines" for the AbstractSyntaxTree. It allow to see in the tree where extra
  * endlines are.
  * @return a "SkipLines" node for the AbstractSyntaxTree.
  */
  private AbstractSyntaxTree skipEndline() throws IOException {
    while (lookahead.getType().equals(LexicalUnit.ENDLINE)) {
      nextToken();
    }
    return new AbstractSyntaxTree("SkipLines");
  }

  /**
  * The first step of the parsing. The function has two "modes". One where the option to
  * draw the tree is inactive and the other one where it is active. In the "inactive mode"
  * the parser only check that the input is correct. It returns a null node as the
  * AbstractSyntaxTree will be discarded at the end anyway. In the "active mode", a new AbstractSyntaxTree
  * following the rules defined in AbstractSyntaxTree.java is returned instead. In both case
  * the rule number/name is printed on the standard output.
  * The other functions will not be detailed but they all follow the same model.
  * @return a AbstractSyntaxTree either correct or null depending on the selected option.
  */
  //[01] <Program> -> BEGINPROG [ProgName] [EndLine] <Variables> <Code> ENDPROG
  private AbstractSyntaxTree program() throws IOException {
    List<AbstractSyntaxTree> treeList = Arrays.asList(
    skipEndline(),
    compareToken(LexicalUnit.BEGINPROG),
    compareToken(LexicalUnit.PROGNAME),
    compareToken(LexicalUnit.ENDLINE),
    skipEndline(),
    variables(),
    skipEndline(),
    code(),
    skipEndline(),
    compareToken(LexicalUnit.ENDPROG),
    skipEndline(),
    compareToken(LexicalUnit.EOS)
    );
    return new AbstractSyntaxTree("Program", treeList);
  }

  //[02] <Variables> -> VARIABLES <VarList> [EndLine]
  //[03] <Variables> -> EPSILON
  private AbstractSyntaxTree variables() throws IOException {
    if (lookahead.getType().equals(LexicalUnit.VARIABLES)) {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.VARIABLES),
      varlist()
      );
      return new AbstractSyntaxTree("Variables", treeList);
    } else {
      List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
      return new AbstractSyntaxTree("Variables", treeList);
    }
  }

  //[04] <VarList> -> [VarName] <VarListEnd>
  private AbstractSyntaxTree varlist() throws IOException {
    List<AbstractSyntaxTree> treeList = Arrays.asList(
    compareToken(LexicalUnit.VARNAME),
    varlistend()
    );
    return new AbstractSyntaxTree("Varlist", treeList);
  }

  //[05] <VarListEnd> -> COMMA <VarList>
  //[06] <VarListEnd> -> EPSILON
  private AbstractSyntaxTree varlistend() throws IOException {
    if (lookahead.getType().equals(LexicalUnit.COMMA)) {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.COMMA),
      compareToken(LexicalUnit.VARNAME),
      varlistend()
      );
      return new AbstractSyntaxTree("Varlistend", treeList);
    } else {
      List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
      return new AbstractSyntaxTree("Varlistend", treeList);
    }
  }

  //[07] <Code> -> <Instruction> [EndLine] <Code>
  //[08] <Code> -> EPSILON
  private AbstractSyntaxTree code() throws IOException {
    switch(lookahead.getType()) {
      case VARNAME:
      case IF:
      case WHILE:
      case FOR:
      case PRINT:
      case READ:
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        instruction(),
        skipEndline(),
        code()
        );
        return new AbstractSyntaxTree("Code", treeList);
      default:
        return new AbstractSyntaxTree("End");
    }
  }

  //[09] <Instruction> -> <Assign>
  //[10] <Instruction> -> <If>
  //[11] <Instruction> -> <While>
  //[12] <Instruction> -> <For>
  //[13] <Instruction> -> <Print>
  //[14] <Instruction> -> <Read>
  private AbstractSyntaxTree instruction() throws IOException {
    switch(lookahead.getType()) {
      case VARNAME:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(assign()));
      case IF:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(parse_if()));
      case WHILE:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(parse_while()));
      case FOR:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(parse_for()));
      case PRINT:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(parse_print()));
      case READ:
        return new AbstractSyntaxTree("Instruction", Arrays.asList(parse_read()));
      default:
        return new AbstractSyntaxTree("End");
    }
  }

  //[15] <Assign> -> [VarName] ASSIGN <ExprArith>
  private AbstractSyntaxTree assign() throws IOException {
    List<AbstractSyntaxTree> treeList = Arrays.asList(
    compareToken(LexicalUnit.VARNAME),
    compareToken(LexicalUnit.ASSIGN),
    exprArith()
    );
    return new AbstractSyntaxTree("Assign", treeList);
  }

  //[16] <ExprArith> -> <HpProd> <LpExpr>
  private AbstractSyntaxTree exprArith() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      hpProd(),
      lpExpr()
      );
      return new AbstractSyntaxTree("ExprArith", treeList);
    }

    //[17] <HpProd> -> <SimpleExpr> <HpExpr>
    private AbstractSyntaxTree hpProd() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      simpleExpr(),
      hpExpr()
      );
      return new AbstractSyntaxTree("HpProd", treeList);
    }

    //[18] <HpExpr> -> <HpOp> <SimpleExpr> <HpExpr>
    //[19] <HpExpr> -> EPSILON
    private AbstractSyntaxTree hpExpr() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.TIMES) ||
      lookahead.getType().equals(LexicalUnit.DIVIDE)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        hpOp(),
        simpleExpr(),
        hpExpr()
        );
        return new AbstractSyntaxTree("HpExpr", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("HpExpr", treeList);
      }
    }

    //[20] <LpExpr> -> <LpOp> <HpProd> <LpExpr>
    //[21] <LpExpr> -> EPSILON
    private AbstractSyntaxTree lpExpr() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.PLUS) ||
      lookahead.getType().equals(LexicalUnit.MINUS)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        lpOp(),
        hpProd(),
        lpExpr()
        );
        return new AbstractSyntaxTree("LpExpr", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("LpExpr", treeList);
      }
    }

    //[22] <SimpleExpr> -> [VarName]
    //[23] <SimpleExpr> -> [Number]
    //[24] <SimpleExpr>	-> LPAREN <ExprArith> RPAREN
    //[25] <SimpleExpr>	-> MINUS <SimpleExpr>
    private AbstractSyntaxTree simpleExpr() throws IOException {
      switch(lookahead.getType()) {
        case VARNAME:
          List<AbstractSyntaxTree> treeList_VARNAME = Arrays.asList(
          compareToken(LexicalUnit.VARNAME)
          );
          return new AbstractSyntaxTree("SimpleExpr", treeList_VARNAME);
        case NUMBER:
          List<AbstractSyntaxTree> treeList_NUMBER = Arrays.asList(
          compareToken(LexicalUnit.NUMBER)
          );
          return new AbstractSyntaxTree("SimpleExpr", treeList_NUMBER);
        case LPAREN:
          List<AbstractSyntaxTree> treeList_LPAREN = Arrays.asList(
          compareToken(LexicalUnit.LPAREN),
          exprArith(),
          compareToken(LexicalUnit.RPAREN)
          );
          return new AbstractSyntaxTree("SimpleExpr", treeList_LPAREN);
        case MINUS:
          List<AbstractSyntaxTree> treeList_MINUS = Arrays.asList(
          compareToken(LexicalUnit.MINUS),
          exprArith()
          );
          return new AbstractSyntaxTree("SimpleExpr", treeList_MINUS);
        default:
          throw new Error("\nError at line " + lookahead.getLine() + ": " +
          lookahead.getType() + " expected a number, a variable or an arithmetic expression");
      }
    }

    //[26] <LpOp> -> PLUS
    //[27] <LpOp> -> MINUS
    private AbstractSyntaxTree lpOp() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.PLUS)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.PLUS)
        );
        return new AbstractSyntaxTree("LpOp", treeList);
      } else if (lookahead.getType().equals(LexicalUnit.MINUS)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.MINUS)
        );
        return new AbstractSyntaxTree("LpOp", treeList);
      } else {
        throw new Error("\nError at line " + lookahead.getLine() + ": " +
        lookahead.getType() + " expected addition or substraction operator");
      }
    }

    //[28] <HpOp> -> TIMES
    //[29] <HpOp> -> DIVIDE
    private AbstractSyntaxTree hpOp() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.TIMES)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.TIMES)
        );
        return new AbstractSyntaxTree("HpOp", treeList);
      } else if (lookahead.getType().equals(LexicalUnit.DIVIDE)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.DIVIDE)
        );
        return new AbstractSyntaxTree("HpOp", treeList);
      } else {
        throw new Error("\nError at line " + lookahead.getLine() + ": " +
        lookahead.getType() + " expected multiplication or division operator");
      }
    }

    //[30] <If> -> IF <Cond> THEN <Code> <IfElse> ENDIF
    private AbstractSyntaxTree parse_if() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.IF),
      compareToken(LexicalUnit.LPAREN),
      cond(),
      compareToken(LexicalUnit.RPAREN),
      compareToken(LexicalUnit.THEN),
      skipEndline(),
      code(),
      ifElse(),
      compareToken(LexicalUnit.ENDIF)
      );
      return new AbstractSyntaxTree("If", treeList);
    }

    //[31] <IfElse> -> ELSE [EndLine] <Code>
    //[32] <IfElse> -> EPSILON
    private AbstractSyntaxTree ifElse() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.ELSE)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.ELSE),
        compareToken(LexicalUnit.ENDLINE),
        code()
        );
        return new AbstractSyntaxTree("IfElse", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("IfElse", treeList);
      }
    }

    //[33] <Cond> -> <PCond> <LpCond>
    private AbstractSyntaxTree cond() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      pCond(),
      lpCond()
      );
      return new AbstractSyntaxTree("Cond", treeList);
    }

    //[34] <PCond> -> <SimpleCond> <HpCond>
    private AbstractSyntaxTree pCond() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      simpleCond(),
      hpCond()
      );
      return new AbstractSyntaxTree("PCond", treeList);
    }

    //[35] <HpCond> -> AND <SimpleCond> <HpCond>
    //[36] <HpCond> -> EPSILON
    private AbstractSyntaxTree hpCond() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.AND)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.AND),
        simpleCond(),
        hpCond()
        );
        return new AbstractSyntaxTree("HpCond", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("HpCond", treeList);
      }
    }

    //[37] <LpCond> -> OR <PCond> <LpCond>
    //[38] <LpCond> -> EPSILON
    private AbstractSyntaxTree lpCond() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.OR)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.OR),
        pCond(),
        lpCond()
        );
        return new AbstractSyntaxTree("LpCond", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("LpCond", treeList);
      }
    }

    //[39] <SimpleCond> -> NOT <SimpleCond>
    //[40] <SimpleCond> -> <ExprArith> <Comp> <ExprArith>
    private AbstractSyntaxTree simpleCond() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.NOT)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.NOT),
        simpleCond()
        );
        return new AbstractSyntaxTree("SimpleCond", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        exprArith(),
        comp(),
        exprArith()
        );
        return new AbstractSyntaxTree("SimpleCond", treeList);
      }
    }

    //[41] <Comp>	-> EQ
    //[42] <Comp> -> GEQ
    //[43] <Comp> -> GT
    //[44] <Comp> -> LEQ
    //[45] <Comp> -> LT
    //[46] <Comp> -> NEQ
    private AbstractSyntaxTree comp() throws IOException {
      switch(lookahead.getType()) {
        case EQ:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.EQ)));
        case GEQ:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.GEQ)));
        case GT:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.GT)));
        case LEQ:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.LEQ)));
        case LT:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.LT)));
        case NEQ:
        return new AbstractSyntaxTree("Comp", Arrays.asList(compareToken(LexicalUnit.NEQ)));
        default:
        throw new Error("\nError at line " + lookahead.getLine() + ": " +
        lookahead.getType() + " expected a comparison operator");
      }
    }

    //[47] <While> -> WHILE <Cond> DO <Code> ENDWHILE
    private AbstractSyntaxTree parse_while() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.WHILE),
      cond(),
      compareToken(LexicalUnit.DO),
      skipEndline(),
      code(),
      compareToken(LexicalUnit.ENDWHILE)
      );
      return new AbstractSyntaxTree("While", treeList);
    }

    //[48] <For> -> FOR [VarName] ASSIGN <ExprArith> TO <ExprArith> DO <Code> ENDFOR
    private AbstractSyntaxTree parse_for() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.FOR),
      compareToken(LexicalUnit.VARNAME),
      compareToken(LexicalUnit.ASSIGN),
      exprArith(),
      compareToken(LexicalUnit.TO),
      exprArith(),
      compareToken(LexicalUnit.DO),
      skipEndline(),
      code(),
      compareToken(LexicalUnit.ENDFOR)
      );
      return new AbstractSyntaxTree("For", treeList);
    }

    //[49] <Print> -> PRINT LPAREN <ExprList> RPAREN
    private AbstractSyntaxTree parse_print() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.PRINT),
      compareToken(LexicalUnit.LPAREN),
      exprList(),
      compareToken(LexicalUnit.RPAREN)
      );
      return new AbstractSyntaxTree("Print", treeList);
    }

    //[50] <Read> -> READ LPAREN <VarList> RPAREN
    private AbstractSyntaxTree parse_read() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      compareToken(LexicalUnit.READ),
      compareToken(LexicalUnit.LPAREN),
      varlist(),
      compareToken(LexicalUnit.RPAREN)
      );
      return new AbstractSyntaxTree("Read", treeList);
    }

    //[51] <ExpList> -> <ExprArith> <ExpListEnd>
    private AbstractSyntaxTree exprList() throws IOException {
      List<AbstractSyntaxTree> treeList = Arrays.asList(
      exprArith(),
      expListEnd()
      );
      return new AbstractSyntaxTree("ExprList", treeList);
    }

    //[52] <ExpListEnd> -> COMMA <ExpList>
    //[53] <ExpListEnd> -> EPSILON
    private AbstractSyntaxTree expListEnd() throws IOException {
      if (lookahead.getType().equals(LexicalUnit.COMMA)) {
        List<AbstractSyntaxTree> treeList = Arrays.asList(
        compareToken(LexicalUnit.COMMA),
        exprList()
        );
        return new AbstractSyntaxTree("ExpListEnd", treeList);
      } else {
        List<AbstractSyntaxTree> treeList = Arrays.asList(new AbstractSyntaxTree("EPSILON"));
        return new AbstractSyntaxTree("ExpListEnd", treeList);
      }
    }

  }
