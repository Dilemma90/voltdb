package org.voltdb.sqlparser.syntax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.voltdb.sqlparser.syntax.grammar.SQLParserLexer;
import org.voltdb.sqlparser.syntax.grammar.SQLParserListener;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser;

public class SQLParserDriver {
    private ParseTree m_tree;
    /**
     * Construct an SQLParserDriver given a string.  Delegates to the InputStream
     * constructor.
     *
     * @param aDDL
     * @param aListener
     * @throws IOException
     */
    public SQLParserDriver(String aDDL, ANTLRErrorListener errorListener) throws IOException {
        this(new ByteArrayInputStream(aDDL.getBytes(StandardCharsets.UTF_8)), errorListener);
    }

    /**
     * Construct an SQLParserDriver from a generic InputStream
     * and a listener.  The input stream is parsed during the
     * construction.  After the constructor's return, the
     * input string will *not* have been consumed.  Call SQLParserDriver.parse()
     * to fill the listener with happy static semantics.
     *
     * @param aInput
     * @param aListener
     * @throws IOException
     */
    public SQLParserDriver(InputStream aInput, ANTLRErrorListener errorListener) throws IOException {
        ANTLRInputStream input = new ANTLRInputStream(aInput);

        // create a lexer that feeds off of input CharStream
        SQLParserLexer lexer = new SQLParserLexer(input);

        // create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // create a parser that feeds off the tokens buffer
        SQLParserParser parser = new SQLParserParser(tokens);

        if (errorListener != null) {
            parser.addErrorListener(errorListener);
        }
        m_tree = parser.ddl(); // begin parsing at init rule
    }

    /**
     * Parse the input stream.
     */
    public void walk(SQLParserListener aListener) {
        // Create a generic parse tree walker that can trigger callbacks
        ParseTreeWalker walker = new ParseTreeWalker();
        // Walk the tree created during the parse, trigger callbacks
        walker.walk(aListener, m_tree);
    }
}
