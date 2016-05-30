import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class responsible for loading SPARQL queries from file and converting them to list of hashmaps
 */
public class QueryUtils {


    public Model model;

    public QueryUtils(Model model)
    {
        this.model = model;
    }

    public ResultSet getQueryResult(String queryPath)
    {
        //1. Load QueryUtils from file
        String queryString = FileUtils.readFile(queryPath, StandardCharsets.UTF_8);
        com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);

        //2. Execute the QueryUtils and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        return qe.execSelect();
    }

    public List<String> getResultVars(String queryPath)
    {
        ResultSet results =  getQueryResult(queryPath);
        return results.getResultVars();
    }

    public List<HashMap<String,String>> getQueryAsListHashMap(String queryPath)
    {
        ResultSet results =  getQueryResult(queryPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(baos, results);

        //save result to String from outputStream
        String queryResult = "";
        try {
            queryResult = baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // delete ontology URI from QueryUtils results since they're irrelevant in this context
        queryResult = queryResult.replace(FileUtils.ontologyIRI, "");

        // delete carriage return characters from QueryUtils results since they mess up printing results
        queryResult = queryResult.replaceAll("\r", "");

        // turn into lowercase
        queryResult = queryResult.toLowerCase();

        // split into lines
        List<String> queryLines = new LinkedList<>(Arrays.asList(queryResult.split("\n", -1)));
        queryLines.removeAll(Arrays.asList(null,"")); // delete empty strings

       /*
        QueryUtils results is stored in list of hashmaps, where:
        row - each one element of list
        column - hashmap key (QueryUtils variable)
        value - hashmap value
       */

        List<String> resultVars = results.getResultVars();
        int columnNumber = resultVars.size();
        int rowNumber = queryLines.size();  // we are ommiting first line since it only contains var names
        ArrayList<HashMap<String, String>> queryRows = new ArrayList(rowNumber);

        // split each line into array of seperate values and put them into hashmap
        for (int lineCounter = 1; lineCounter < rowNumber; lineCounter++) // we are ommiting first line since it only contains var names
        {
            HashMap queryRow = new HashMap(columnNumber);
            String[] explodedRow = queryLines.get(lineCounter).split(",", -1);
            for (int columnCounter = 0; columnCounter < columnNumber; columnCounter++) {

                queryRow.put(resultVars.get(columnCounter), explodedRow[columnCounter]);
            }
            queryRows.add(queryRow);
        }
        return queryRows;
    }

}
