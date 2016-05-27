import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;


import java.io.InputStream;

/**
 * Created by Rael on 27.05.2016.
 */
public class testClass {

    /*
    Uwaga - trzeba zwracać uwagę przy wczytywaniu ontologii aby format zapisu się zgadzał

     */

    static String filePath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\rdfxml.owl";

    public static void main(String[] args) {
        System.out.println("Hello world");
        OntModel testModel = getOntologyModel(filePath);

        String queryString =
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

        Query query = QueryFactory.create(queryString);

        System.out.println("----------------------");

        System.out.println("Query Result Sheet");

        System.out.println("----------------------");

        System.out.println("Direct&Indirect Descendants (model1)");

        System.out.println("-------------------");


        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, testModel);
        ResultSet results =  qe.execSelect();

        // Output query results
        ResultSetFormatter.out(System.out, results, query);

        qe.close();

        System.out.println("----------------------");
        System.out.println("Only Direct Descendants");
        System.out.println("----------------------");

        // Execute the query and obtain results
        qe = QueryExecutionFactory.create(query, testModel);
        results =  qe.execSelect();

        // Output query results
        ResultSetFormatter.out(System.out, results, query);
        qe.close();

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
