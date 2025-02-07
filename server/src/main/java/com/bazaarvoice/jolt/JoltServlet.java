package com.bazaarvoice.jolt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bazaarvoice.jolt.exception.JoltException;

/**
 * With the move to the Java8 App Engine, I had a hell of a time
 *  trying to get JAX-RS stuff to work.   So, nevermind.
 *
 * Just do the simplest, stupidest, BOG standard Servlet.
 */
public class JoltServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");
        response.getWriter().println("Pong");
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {

        resp.setContentType("text/plain");

        String inputString, specString;
        Boolean sort;

        try {
            inputString = req.getParameter( "input" );

            specString = req.getParameter( "spec" );

            //Filter the actual Jolt spec
            ArrayList joltSpec = getArrayList(specString);
            specString = JsonUtils.toJsonString(joltSpec);

            sort = Boolean.valueOf( req.getParameter( "sort" ) );
        }
        catch ( Exception e ) {
            resp.getWriter().println( "Could not url-decode the inputs.\n");
            return;
        }

        Object input, spec;

        try {
            input = JsonUtils.jsonToObject( inputString );
        }
        catch ( Exception e ) {
            resp.getWriter().println( "Could not parse the 'input' JSON.\n" );
            return;
        }

        try {
            spec = JsonUtils.jsonToObject( specString );
        }
        catch ( Exception e ) {
            resp.getWriter().println( "Could not parse the 'spec' JSON.\n" );
            return;
        }

        String result = doTransform( input, spec, sort );
        resp.getWriter().println( result );
    }


    private String doTransform( Object input, Object spec, boolean doSort ) throws IOException {

        try {
            Chainr chainr = Chainr.fromSpec( spec );

            Object output = chainr.transform( input );

            if ( doSort ) {
                output = Sortr.sortJson( output );
            }

            return JsonUtils.toPrettyJsonString( output );
        }
        catch ( Exception e ) {

            StringBuilder sb = new StringBuilder();
            sb.append( "Error running the Transform.\n\n" );

            // Walk up the stackTrace printing the message for any JoltExceptions.
            Throwable exception = e;
            do {
                if ( exception instanceof JoltException ) {
                    sb.append( exception.getMessage() );
                    sb.append( "\n\n");
                }

                exception = exception.getCause();
            }
            while( exception != null );

            return sb.toString();
        }
    }

    /*
    * Extracting the Jolt spec array
    * */
    private ArrayList<Object> getArrayList(String spec) {
        ArrayList<Object> specJSON = (ArrayList) JsonUtils.jsonToObject(spec);
        LinkedHashMap<String, Object> linkedHashMap3 = (LinkedHashMap) (specJSON.get(0));
        ArrayList<Object> configs = (ArrayList) linkedHashMap3.get("configs");
        LinkedHashMap<String, Object> linkedHashMap2 = (LinkedHashMap) configs.get(0);
        LinkedHashMap<String, Object> linkedHashMap1 = (LinkedHashMap) linkedHashMap2.get("query");
        ArrayList<Object> al = (ArrayList) linkedHashMap1.get("select");
        return al;
    }
}