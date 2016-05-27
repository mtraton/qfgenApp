import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class testClass {

    /*
    Uwaga - trzeba zwracać uwagę przy wczytywaniu ontologii aby format zapisu się zgadzał

     */

    //TODO: ścieżka do pliku powinna być arguentem wejściowym
    static String ontologyPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\rdfxml.owl";
    static String queryPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\mainQuery";
    static String ontologyIRI = "http://www.semanticweb.org/qfgen#";
    public static void main(String[] args) {


        //1. Load model from file path
        OntModel testModel = getOntologyModel(ontologyPath);

        for(String uri : testModel.listImportedOntologyURIs())
        {
            System.out.println(uri);
        }

        // TODO: wczytuj zapytanie z pliku
        // 2. Create query
        String queryString = readFile(queryPath,  StandardCharsets.UTF_8);

        System.out.println("----------------------");
        System.out.println("Zapytanie : \n" + queryString);
        System.out.println("----------------------");
                /*=
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
        "PREFIX :  <http://www.semanticweb.org/qfgen#> " +
                "SELECT DISTINCT ?typPomieszczenia ?pomieszczenie " +
        "WHERE {" +
            "?pomieszczenie rdf:type ?typPomieszczenia ." +
            "?pomieszczenie rdf:type :Pomieszczenie ." +
                "}";

        */
        Query query = QueryFactory.create(queryString);


        System.out.println("----------------------");
        System.out.println("Wyniki zapytania");
        System.out.println("----------------------");

        //3. Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, testModel);
        ResultSet results =  qe.execSelect();



        //4.  Output query results
       // ResultSetFormatter.out(System.out, results, query);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(baos, results);
        String output = "";
        try {
             output = baos.toString("UTF-8" );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        output = output.replace(ontologyIRI, "");
        System.out.println(output);
        qe.close();

    /*
        //5. Nazwy zmiennych zapytania

        System.out.println("----------------------");
        System.out.println("Vars");
        System.out.println("----------------------");

        List<String> resultVars =  results.getResultVars();



        qe = QueryExecutionFactory.create(query, testModel);
        results =  qe.execSelect();

        // iterate through rows
        for ( ; results.hasNext() ; )
        {

            QuerySolution soln = results.next();
            for(String var : resultVars)
            {
                System.out.println(var);
                RDFNode n = soln.get(var) ; // "x" is a variable in the query
                // If you need to test the thing returned
                System.out.println(n);
                if ( n.isLiteral() )
                    ((Literal)n).getLexicalForm() ;
                if ( n.isResource() )
                {

                    Resource r = (Resource)n ;



                    if ( ! r.isAnon() )
                    {
                       System.out.println(r.getURI());
                    }
                    else
                    {
                        System.out.println(r.getLocalName() + " " + r.getId());

                    }
            }
            System.out.println("New column---------------------------------");
        }
        System.out.println("New row---------------------------------");

        }





    */
    }

    static String readFile(String path, Charset encoding)
    {
        String result = "";
        try{
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            result =  new String(encoded, encoding);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static OntModel getOntologyModel(String ontoFile)
    {
        OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        try
        {
            InputStream in = FileManager.get().open(ontoFile);
            try
            {
                ontoModel.read(in, "RDF/XML");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            System.out.println("Ontology " + ontoFile + " loaded.");
        }
        catch (JenaException je)
        {
            System.err.println("ERROR" + je.getMessage());
            je.printStackTrace();
            System.exit(0);
        }
        return ontoModel;
    }

}
