import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

/**
 * File paths and I/O utilities
 */
public class FileUtils {

    static String ontologyPath = "./res/rdfxml.owl"; // be sure to save and load ontology in propert format - we assume rdf/xml format
    static String queryPath = "./res/mainQuery";
    static String roomQueryPath = "./res/roomQuery";
    static String queryDescPath = "./res/descQuery";
    static String dataFilePath = "./output.arff";
    static String ontologyIRI = "http://www.semanticweb.org/qfgen#";

    // class FileUtils
    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    // class FileUtils
   public static String readFile(String path, Charset encoding) {
        String result = "";
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            result = new String(encoded, encoding);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

   public static void saveFile(String path, String fileContent)
    {
        try {
            Files.write(Paths.get(path), fileContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static OntModel getOntologyModel(String ontoFile) {
        OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        try {
            InputStream in = FileManager.get().open(ontoFile);
            try {
                ontoModel.read(in, "RDF/XML");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Ontology " + ontoFile + " loaded.");
        } catch (JenaException je) {
            System.err.println("ERROR" + je.getMessage());
            je.printStackTrace();
            System.exit(0);
        }
        return ontoModel;
    }
}
