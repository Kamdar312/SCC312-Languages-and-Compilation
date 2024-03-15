/**
 * 
 * @author Vishal Kamdar
 */
public class Generate extends AbstractGenerate {
    String filename;

    public Generate() {

    }

    public Generate(String filename) {
        this.filename = filename;
    }

    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {
        // Construct an error message including the erroneous token, its location, the
        // file name, and the expected syntax.
        String errorMessage = "Token: " + "\"" + token.text + "\"" + " at line " + token.lineNumber + "." + " In file "
                + filename
                + ". " + explanatoryMessage + " is expected";
        // Throw a new CompilationException with the constructed error message.
        throw new CompilationException(errorMessage);
    }

}