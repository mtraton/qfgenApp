import com.hp.hpl.jena.ontology.OntModel;

import java.util.*;

//TODO: podzielić na klasy

public class Qfgen {

    static OntModel ont;
    static QueryUtils query;
    public static void main(String[] args) {
        
        
        //1. Load model from file path
        ont = FileUtils.getOntologyModel(FileUtils.ontologyPath);
        query = new QueryUtils(ont);
        
       // 2. Execute queries and convert to dataset
        List queryRows = query.getQueryAsListHashMap(FileUtils.queryPath);

        //3. Save to file
        AttributeUtils attributeUtils = new AttributeUtils(ont);
        EntryUtils entryUtils = new EntryUtils(ont);

        String outputData = attributeUtils.createAttributes() + "\n" + entryUtils.createEntries((ArrayList) queryRows);
        FileUtils.saveFile(FileUtils.dataFilePath,  outputData );
    }
}





