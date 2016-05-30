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
import java.util.*;

//TODO: podzielić na klasy

public class qfgen {


    static int[] roomElementsNumbers =  {0, 1, 3, 4};
    static int [] roomElementsValueNumbers = {1,3};
    static int[] roomNumbers = {1,2};

    // string constants needed to create attributes
    static String roomTypeName = "pomieszczenie";
    static String attributeLineStart = "attribute(";
    static String attributeLineEnd = ").\n";
    static String nameSeparator = "_";
    static String elementSeparator = ",";
    static String booleanValues = "[true,false]";
    static String valueStart = "[";
    static String valueEnd = ", unspecified]";
    static String valueMarker = "$$$"; // string to mark place when we will put values of property

    static String ontologyPath = "./res/rdfxml.owl"; // be sure to save and load ontology in propert format - we assume rdf/xml format
    static String queryPath = "./res/mainQuery";
    static String roomQueryPath = "./res/roomQuery";
    static String queryDescPath = "./res/descQuery";
    static String dataFilePath = "./output.arff";

    static String ontologyIRI = "http://www.semanticweb.org/qfgen#";

    static OntModel testModel;

    public static void main(String[] args) {
        //1. Load model from file path
        testModel = getOntologyModel(ontologyPath);

       // 2. Execute queries and convert to dataset
        List queryRows = getQueryAsListHashMap(queryPath);

        //3. Save to file
        String outputData = createAttributes() + "\n" + createEntries((ArrayList) queryRows);
        saveFile(dataFilePath,  outputData );
    }


    // class utils
    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    // class queryHandler
    public static ResultSet getQueryResult(String queryPath)
    {
        //1. Load query from file
        String queryString = readFile(queryPath, StandardCharsets.UTF_8);
        Query query = QueryFactory.create(queryString);

        //2. Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, testModel);
        ResultSet results = qe.execSelect();
        return results;
    }

    // class queryHandler
    public static List<String> getResultVars(String queryPath)
    {
        ResultSet results =  getQueryResult(queryPath);
        List<String> resultVars = results.getResultVars();
        return resultVars;
    }

    // class queryHandler
   public static List<HashMap<String,String>> getQueryAsListHashMap(String queryPath)
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

       // delete ontology URI from query results since they're irrelevant in this context
       queryResult = queryResult.replace(ontologyIRI, "");

       // delete carriage return characters from query results since they mess up printing results
       queryResult = queryResult.replaceAll("\r", "");

       // turn into lowercase
       queryResult = queryResult.toLowerCase();

       // split into lines
       List<String> queryLines =  new LinkedList<String>(Arrays.asList(queryResult.split("\n", -1)));
       queryLines.removeAll(Arrays.asList(null,"")); // delete empty strings

       /*
        Query results is stored in list of hashmaps, where:
        row - each one element of list
        column - hashmap key (query variable)
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

    // class utils
    static String readFile(String path, Charset encoding) {
        String result = "";
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            result = new String(encoded, encoding);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    static void saveFile(String path, String fileContent)
    {
        try {
            Files.write(Paths.get(path), fileContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //class ontologyHandler
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


    //class dataAttributes

    public static String createBooleanAttributeString(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {0, 1};  //variable values numbers from query which are needed to create given attribute

        String booleanAttribute = attributeLineStart + rowValues.get(numbers[0]) + nameSeparator + rowValues.get(numbers[1]) + elementSeparator + booleanValues + attributeLineEnd;

        return booleanAttribute;
    }

    public static String createPropertyAttributeString(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {0,1,3}; //variable values numbers from query which are needed to create given attribute

        String  propertyAttribute = attributeLineStart + rowValues.get(numbers[0]) + nameSeparator + rowValues.get(numbers[1]) + nameSeparator + rowValues.get(numbers[2]) + elementSeparator + valueStart + valueMarker + valueEnd + attributeLineEnd;
        return propertyAttribute;
    }

    public static String createRoomAttribute(HashMap<Integer, String> rowValues)
    {
        int [] numbers =   {1};
        String roomProperty = rowValues.get(numbers[0]);
        String roomAttribute =
                attributeLineStart +
                        "pomieszczenie" +
                        nameSeparator +
                        roomProperty + //variable values numbers from query which are needed to create given attribute
                        elementSeparator +
                        valueStart +
                        valueMarker +
                        valueEnd +
                        attributeLineEnd;

        return roomAttribute;
    }


    public static String  createAttributes()
    {
        // output - list of attributes
        LinkedList<String> attributeList = new LinkedList<>();

        // common variables
        List<HashMap<String, String>> queryRows = null;
        List<String> resultVars = null;
        List<String> roomAttributes = new LinkedList<>();
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();

        //1. Create room element attributes
        queryRows =  getQueryAsListHashMap(queryPath);
        resultVars = getResultVars(queryPath);
        List roomElementAttributes  = iterateThroughRows("RoomElementAttributes",queryRows, resultVars, roomElementsNumbers, roomElementsValueNumbers);

        //2. Create room attributes
        queryRows =  getQueryAsListHashMap(roomQueryPath);
        resultVars = getResultVars(roomQueryPath);
        roomAttributes  = iterateThroughRows("RoomAttributes",queryRows, resultVars, roomNumbers, roomNumbers);
        List allRoomAttributes = new ArrayList<String>(roomElementAttributes);
        allRoomAttributes.addAll(roomAttributes);

        //3. Create attributes with subattributes
        queryRows =  getQueryAsListHashMap(queryDescPath);
        resultVars = getResultVars(queryDescPath);
        List<String> allAttributes = new LinkedList<>();

        HashMap<String, HashSet<String>> subAtributeVals = new HashMap<>();

        for (HashMap<String, String> row: queryRows)
        {
            String subAttribute = row.get(resultVars.get(0));
            String attribute = row.get(resultVars.get(1));
            String subAttributeVal =  row.get(resultVars.get(2));
            for(String attr : (List<String>) allRoomAttributes)
            {
                if(attr.contains(attribute))
                {
                    String newAttribute = attr.replace(attribute, attribute + nameSeparator + subAttribute);

                    // delete everything after [ character
                    newAttribute = newAttribute.substring(0, newAttribute.indexOf("[") +1);

                    HashSet<String> set = null;
                    if(subAtributeVals.containsKey(subAttribute))
                    {
                        set = subAtributeVals.get(subAttribute);
                    }
                    else
                    {
                          set = new HashSet<>();
                    }
                    set.add(subAttributeVal);
                    subAtributeVals.put(subAttribute,set);
                    allAttributes.add(newAttribute);
                }
            }
        }
        // add values of subattribute
        for(String key : subAtributeVals.keySet())
        {
            for (String attribute: allAttributes
                 ) {
                if (attribute.contains(key))
                {
                    String vals = subAtributeVals.get(key).toString();
                    vals = vals.substring(1, vals.length() -1); // delete [] from string
                    vals = vals + valueEnd + attributeLineEnd;
                    String newAttribute = attribute.concat(vals);
                    allAttributes.set(allAttributes.indexOf(attribute), newAttribute);
                }
            }
        }
        allAttributes.addAll(allRoomAttributes);
        String output =  allAttributes.toString();
        output = output.replace("\n,", "\n"); // delete unnecessary commas created when using toString();
        output = output.substring(1, output.length() - 1);
        return output;
    }

    public static String createRoomKey(HashMap<Integer, String> rowValues)
    {
        String key = roomTypeName  + nameSeparator + rowValues.get(1) ; ;
        return key;

    }
    public static String createRoomElementKey(HashMap<Integer, String> rowValues, int[] numbers)
    {
        String key = "";
        for (int number: numbers
                ) {
            key = key + rowValues.get(number) + nameSeparator;
        }
        key = key.substring(0, key.length()-1);
        return key;
    }

    public static String createSubAttributeKey(HashMap<Integer, String> rowValues)
    {
        String key = rowValues.get(0); // key is subattribute
        return key;
    }

    // iterate through query rows and produce things
    public static List iterateThroughRows(String type, List queryRows, List resultVars, int[] variableNumbers, int[] numbers)
    {

        List attributeList = new LinkedList<>();
        HashMap<Integer, String> rowValues = new HashMap<>();
        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();
        for (Object hm : queryRows) {
            HashMap row = (HashMap) hm;
            for (int number: variableNumbers) {
                String key = (String) resultVars.get(number);
                String value = (String) row.get(key);
                rowValues.put(number, value);
            }

            String key = "";

            if(type.equals("RoomElementAttributes"))
            {
                key = createRoomElementKey(rowValues, numbers);
            }
            else if(type.equals("RoomAttributes"))
            {
                key = createRoomKey(rowValues);
            }

            HashMap<String, HashSet<String>> subAtributeVals = new HashMap<>();

            // add property value to hashmap (we need to gather all possible values from every row)
            String value = rowValues.get(variableNumbers.length); // assumption - last variable represents value of subattribute
            HashSet<String> set = null;

            if(objectPropertyValues.containsKey(key))
            {
               set = objectPropertyValues.get(key);
            }
            else
            {
               set = new HashSet<>();
            }
            set.add(value);
            objectPropertyValues.put(key , set);

            if(type.equals("RoomElementAttributes"))
            {
                attributeList.add(createBooleanAttributeString(rowValues));
                attributeList.add(createPropertyAttributeString(rowValues));
            }
            else if(type.equals("RoomAttributes"))
            {
                attributeList.add(createRoomAttribute(rowValues));
            }

        }

        List<String>  attributesWithoutDuplicates =  new ArrayList<>(new LinkedHashSet(attributeList));

        // add values of properties
        for(String propertyName : objectPropertyValues.keySet())
        {
            String searchedProperty = propertyName;
            String values = objectPropertyValues.get(propertyName).toString();
            values =  values.substring(1, values.length() - 1);  // delete [ and ] from values string

            // iterate through list of attributes and put proper values of property
            for (String attr: attributesWithoutDuplicates) {
                if(attr.contains(searchedProperty))
                {
                    int elementIndex = attributesWithoutDuplicates.indexOf(attr);
                    String attributeWithValues = attr.replace(valueMarker,values );
                    attributesWithoutDuplicates.set(elementIndex, attributeWithValues);
                }
            }
        }
        return attributesWithoutDuplicates;
    }

    /*
    createEntries - creates entries based on query (about rooms)

    ex:
    entry(data{classroom_color:yellow, classroom_color_intensity:unspecified, classroom_name:"Laboratorium 316", classroom_size:small, id:lab316, place:classroom}).
    entry(data{classroom_computer:true, classroom_computer_color:white, classroom_computer_model:unspecified, id:lab318}).
     */
    public static String createEntries(ArrayList queryList) {

        HashMap describesQuery = describes();


        //todo: hard-code keys (?)
        String type = "typ";
        String property = "wlasnosc";
        String value = "wartosc";
        String roomID = "pomieszczenie";
        String roomType = "typPomieszczenia";
        String obj = "obj";



        StringBuilder entry = new StringBuilder();

        boolean newEntry = true;

        //Loop for every line of query
        for(int i=0; i<queryList.size(); i++) {

            HashMap h = (HashMap) queryList.get(i);

            String dataRoomType = (String)h.get(roomType);
            String dataType = (String)h.get(type);
            String dataProperty = (String)h.get(property);
            String dataValue = (String)h.get(value);
            String dataID = (String)h.get(roomID);

            //APPEND:
            if(newEntry) {
                entry.append( "entry(data{" );
                entry.append( dataRoomType );
                entry.append("_");
                entry.append( dataType );
                entry.append(":true");
                entry.append(", ");
            }

            entry.append( dataRoomType );
            entry.append("_");
            entry.append( dataType );
            entry.append("_");

            if(describesQuery.containsKey(dataProperty)) {
                String valDescribed = (String)describesQuery.get(dataProperty);

                entry.append( valDescribed.trim() );// delete carriage returns
                entry.append("_");
            }

            entry.append( dataProperty );
            entry.append(":");
            entry.append( dataValue );
            entry.append(", ");


            if(i<queryList.size()-1) {
                HashMap h1 = (HashMap) queryList.get(i+1);

                String dataObj = (String)h.get(obj);
                String dataNEXTObj = (String)h1.get(obj);

                if( dataObj.equals( dataNEXTObj ) ) {
                    newEntry = false;
                } else {
                    newEntry = true;
                }
            }


            if(newEntry) {
                entry.append("id:");
                entry.append( dataID.trim() );  //REMEMBER trim() !!
                entry.append("}).");
                entry.append("\n");
            }
//TODO: Z JAKIEGOS POWODU W DATAID JEST JAKIS SMIEC KTORY USUWA CALA LINIJKE...
//System.out.println("DATAID: "+dataID + dataID.length());

        }


        return entry.toString();
    }


    /*
    Function to create HashMap for what describes what(target)
    example:
    room_tv_intensity:dark
    vs
    room_tv_color_intensity:dark
     */
    public static HashMap describes() {

        //String queryDescPath = "..\\descQuery";
        String queryDescPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\descQuery";

        String queryString = readFile(queryDescPath, StandardCharsets.UTF_8);

        Query query = QueryFactory.create(queryString);

        QueryExecution qe = QueryExecutionFactory.create(query, testModel);
        ResultSet results = qe.execSelect();

        List<String> resultVars = results.getResultVars();

        // Output query results
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(baos, results);
        String queryResult = "";
        //save result to String from outputStream
        try {
            queryResult = baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // delete ontology URI from query results since they're irrelevant in this context
        queryResult = queryResult.replace(ontologyIRI, "");
        // turn into lowercase
        queryResult = queryResult.toLowerCase();
        // split into lines
        String[] queryLines = queryResult.split("\n", -1);


        // TO HashMap
        int rowNumber = queryLines.length - 1;  // we are ommiting first line since it only contains var names
        HashMap describesQuery = new HashMap(rowNumber);

        // split each line into array of seperate values and put them into hashmap
        for (int lineCounter = 1; lineCounter < rowNumber; lineCounter++) // we are ommiting first line since it only contains var names
        {
            String[] explodedRow = queryLines[lineCounter].split(",", -1);

            describesQuery.put(explodedRow[0].trim(),explodedRow[1].trim());

            //System.out.println("key:"+explodedRow[0].trim() +", value:"+explodedRow[1].trim()+",");
            //System.out.println("key:"+describesQuery.containsKey("intensywność") +", value:"+describesQuery.get("intensywność")+",");
        }


        return describesQuery;
    }


}

