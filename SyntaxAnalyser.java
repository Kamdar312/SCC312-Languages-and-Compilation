import java.io.IOException;

/**
 * @author Vishal Kamdar
 */
public class SyntaxAnalyser extends AbstractSyntaxAnalyser {
    private String filename;

    public SyntaxAnalyser(String filename) {
        this.filename = filename;
        try {
            lex = new LexicalAnalyser(filename);
        } catch (Exception e) {
            System.err.println("Lexical Analyser failed to load");
        }
    }

    @Override
    public void _statementPart_() throws IOException, CompilationException {
        myGenerate = new Generate(filename); // Pass filename to better understand which file has the errors
        myGenerate.commenceNonterminal("<statement part>");
        try {
            acceptTerminal(Token.beginSymbol); // Accept if begin
            // Enter into the statement list
            handleStatementList();
            acceptTerminal(Token.endSymbol); // Accept if end
        } catch (CompilationException e) {
            // Throw CompilationException with details about where the error occured
            throw new CompilationException("<statement part>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<statement part>");
    }

    @Override
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        if (nextToken.symbol == symbol) {
            myGenerate.insertTerminal(nextToken);
            nextToken = lex.getNextToken();
        } else {
            // Reports an error with information regarding the token that caused the error.
            // What line it was in and what it was expecting instead
            myGenerate.reportError(nextToken, "<" + Token.getName(symbol) + ">");
        }
    }

    private void handleStatementList() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<statement list>");
        try {
            handleStatement(); // Handles statement non-terminal
            while (nextToken.symbol == Token.semicolonSymbol) {
                acceptTerminal(Token.semicolonSymbol);
                handleStatementList(); // Handles statement recursively
            }
        } catch (CompilationException e) {
            throw new CompilationException("<statement list>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<statement list>");
    }

    private void handleStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<statement>");
        // Determines what type of statement the token is and handle it accordingly
        try {
            switch (nextToken.symbol) {
                case Token.identifier:
                    handleAssignmentStatement();
                    break;
                case Token.whileSymbol:
                    handleWhileStatement();
                    break;
                case Token.ifSymbol:
                    handleIfStatement();
                    break;
                case Token.callSymbol:
                    handleProcedureStatement();
                    break;
                case Token.untilSymbol:
                    handleUntilStatement();
                    break;
                case Token.forSymbol:
                    handleForStatement();
                    break;
                default:
                    // Report an error if none of the cases match
                    myGenerate.reportError(nextToken, "<identifier>, <while>, <if>, <call>, <until>, <for>");
            }
        } catch (CompilationException e) {
            throw new CompilationException("<statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<statement>");
    }

    private void handleAssignmentStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<assignment statement>");
        try {
            acceptTerminal(Token.identifier); // Accept the identifier (variable name)
            acceptTerminal(Token.becomesSymbol); // Accept the assignment operator (:=)
            if (nextToken.symbol == Token.stringConstant)
                acceptTerminal(Token.stringConstant);
            else
                handleExpression(); // Parse the expression that's being assigned
        } catch (CompilationException e) {
            throw new CompilationException("<assignment statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<assignment statement>");
    }

    private void handleExpression() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<expression>");
        try {
            handleTerm(); // Handle the first term
            while (nextToken.symbol == Token.plusSymbol || nextToken.symbol == Token.minusSymbol) {
                acceptTerminal(nextToken.symbol); // Accept the '+' or '-' symbol
                handleExpression(); // Handle the next Term recursively
            }
        } catch (CompilationException e) {
            throw new CompilationException("<expression>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<expression>");
    }

    private void handleTerm() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<term>");
        try {
            handleFactor(); // Handle the first factor
            // Assuming terms are just numbers or identifiers for simplicity
            if (nextToken.symbol == Token.divideSymbol || nextToken.symbol == Token.timesSymbol) {
                acceptTerminal(nextToken.symbol); // Accept the number or identifier
                handleTerm(); // Handle the next Factor recursively
            }
        } catch (CompilationException e) {
            throw new CompilationException("<term>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<term>");
    }

    private void handleFactor() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<factor>");
        try {
            switch (nextToken.symbol) {
                case Token.identifier:
                    acceptTerminal(Token.identifier); // Handle variable name
                    break;
                case Token.numberConstant:
                    acceptTerminal(Token.numberConstant); // Handle number constant
                    break;
                case Token.leftParenthesis:
                    acceptTerminal(Token.leftParenthesis); // Handle '('
                    handleExpression(); // Recursively handle expression within parentheses
                    acceptTerminal(Token.rightParenthesis); // Ensure closing ')'
                    break;
                default:
                    // If none of the expected tokens are found, throw an exception
                    myGenerate.reportError(nextToken,
                            "<identifier>, <numberConstant>, <leftParenthesis> or <rightParenthesis>");
            }
        } catch (CompilationException e) {
            throw new CompilationException("<factor>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<factor>");
    }

    private void handleWhileStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<while statement>");
        try {
            acceptTerminal(Token.whileSymbol); // Consume 'while'
            handleCondition(); // Parse the condition of the while loop
            acceptTerminal(Token.loopSymbol); // Consume 'loop'
            handleStatementList(); // Parse the statements inside the loop
            acceptTerminal(Token.endSymbol); // Consume 'end'
            acceptTerminal(Token.loopSymbol); // Ensure 'loop' follows 'end' to properly close the loop
        } catch (CompilationException e) {
            throw new CompilationException("<while statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<while statement>");
    }

    private void handleForStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<for statement>");
        try {
            acceptTerminal(Token.forSymbol);
            acceptTerminal(Token.leftParenthesis);
            handleAssignmentStatement(); // Handle the initial assignment statement
            acceptTerminal(Token.semicolonSymbol); // Expect a semicolon after the initial assignment

            handleCondition(); // Handle the condition for continuing the loop
            acceptTerminal(Token.semicolonSymbol); // Expect a semicolon after the condition

            handleAssignmentStatement(); // Handle the subsequent assignment statement
            acceptTerminal(Token.rightParenthesis); // Expect a right parenthesis ')'

            acceptTerminal(Token.doSymbol); // Consume 'do'
            handleStatementList(); // Handle the list of statements that make up the body of the loop
        } catch (CompilationException e) {
            throw new CompilationException("<for statement>" + " at line " + nextToken.lineNumber, e);
        }
        acceptTerminal(Token.endSymbol); // Consume 'end'
        acceptTerminal(Token.loopSymbol); // Consume 'loop' to properly close the loop structure

        myGenerate.finishNonterminal("<for statement>");
    }

    private void handleIfStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<if statement>");
        try {
            acceptTerminal(Token.ifSymbol); // Consume 'if'
            handleCondition(); // Parse the condition of the if statement
            acceptTerminal(Token.thenSymbol); // Consume 'then'
            handleStatementList(); // Parse the statements in the then branch

            // Check for an optional else branch
            if (nextToken.symbol == Token.elseSymbol) {
                acceptTerminal(Token.elseSymbol); // Consume 'else'
                handleStatementList(); // Parse the statements in the else branch
            }
        } catch (CompilationException e) {
            throw new CompilationException("<if statement>" + " at line " + nextToken.lineNumber, e);
        }
        acceptTerminal(Token.endSymbol); // Consume 'end'
        acceptTerminal(Token.ifSymbol); // Ensure 'if' follows 'end' to properly close the if statement
        myGenerate.finishNonterminal("<if statement>");
    }

    private void handleProcedureStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<procedure statement>");
        try {
            acceptTerminal(Token.callSymbol); // Accept the 'call' keyword
            acceptTerminal(Token.identifier); // Accept the procedure name
            acceptTerminal(Token.leftParenthesis); // Accept the left parenthesis '('
            if (nextToken.symbol != Token.rightParenthesis) // Check if there are arguments
                handleArgumentList(); // Parse the argument list
            acceptTerminal(Token.rightParenthesis); // Accept the right parenthesis ')'
        } catch (CompilationException e) {
            throw new CompilationException("<procedure statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<procedure statement>");
    }

    private void handleArgumentList() throws IOException, CompilationException {
        // Assuming arguments are separated by commas and are identifiers
        myGenerate.commenceNonterminal("<argument list>");
        try {
            acceptTerminal(Token.identifier); // Accept the first argument
            while (nextToken.symbol == Token.commaSymbol) {
                acceptTerminal(Token.commaSymbol); // Accept the comma
                handleArgumentList();
            }
        } catch (CompilationException e) {
            throw new CompilationException("<argument list>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<argument list>");
    }

    private void handleUntilStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<until statement>");
        try {
            acceptTerminal(Token.doSymbol); // Accept the 'do' keyword
            handleStatementList(); // Parse the statements that are executed in the loop
            acceptTerminal(Token.untilSymbol); // Accept the 'until' keyword
            handleCondition(); // Parse the condition for the loop
        } catch (CompilationException e) {
            throw new CompilationException("<until statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<until statement>");
    }

    private void handleCondition() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<condition>");
        try {
            acceptTerminal(Token.identifier); // Accept the first identifier
            handleConditionalOperator(); // Handle the operator

            // Assume the condition compares two identifiers or an identifier and a number
            if (nextToken.symbol == Token.identifier || nextToken.symbol == Token.numberConstant) {
                acceptTerminal(nextToken.symbol); // Accept the second part of the condition
            } else {
                myGenerate.reportError(nextToken, "<condition>");
            }
        } catch (CompilationException e) {
            throw new CompilationException("<condition>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<condition>");
    }

    private void handleConditionalOperator() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<conditional operator>");
        try {
            if (nextToken.symbol == Token.equalSymbol || nextToken.symbol == Token.notEqualSymbol
                    || nextToken.symbol == Token.lessThanSymbol || nextToken.symbol == Token.greaterThanSymbol) {
                acceptTerminal(nextToken.symbol); // Accept the conditional operator
            } else {
                myGenerate.reportError(nextToken, "<conditional operator>" + " at line " + nextToken.lineNumber);
            }
        } catch (CompilationException e) {
            throw new CompilationException("<assignment statement>" + " at line " + nextToken.lineNumber, e);
        }
        myGenerate.finishNonterminal("<conditional operator>");
    }
}