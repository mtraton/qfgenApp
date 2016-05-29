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


public class testClass {

    /*
    Uwaga - trzeba zwracać uwagę przy wczytywaniu ontologii aby format zapisu się zgadzał
     */

    //TODO: ścieżka do pliku powinna być arguentem wejściowym
    //static String ontologyPath = "..\\qfgen\\rdfxml.owl";
    //static String queryPath = "..\\mainQuery";

     static String ontologyPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\rdfxml.owl";
     static String queryPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\mainQuery";

    static String ontologyIRI = "http://www.semanticweb.org/qfgen#"; //TODO: dodać automatyczne wykrywnaie URI ontologii

    public static void main(String[] args) {


        //1. Load model from file path
        OntModel testModel = getOntologyModel(ontologyPath);

        for (String uri : testModel.listImportedOntologyURIs()) {
            System.out.println(uri);
        }

        // TODO: wczytuj zapytanie z pliku
        // 2. Create query
        String queryString = readFile(queryPath, StandardCharsets.UTF_8);

        System.out.println("----------------------");
        System.out.println("Zapytanie : \n" + queryString);
        System.out.println("----------------------");

        Query query = QueryFactory.create(queryString);


        System.out.println("----------------------");
        System.out.println("Wyniki zapytania");
        System.out.println("----------------------");

        //3. Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, testModel);
        ResultSet results = qe.execSelect();

        List<String> resultVars = results.getResultVars();


        //4.  Output query results
        //ResultSetFormatter.out(System.out, results, query);

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


        // Query results is stored in list of hashmaps, where:
        // row - each one element of list
        // column - hashmap key (query variable)
        // value - hashmap value

        //TODO: wrzucić do osobnej funkcji dla czytelności

        int columnNumber = resultVars.size();
        int rowNumber = queryLines.length - 1;  // we are ommiting first line since it only contains var names
        ArrayList queryRows = new ArrayList(rowNumber);

        // split each line into array of seperate values and put them into hashmap
        for (int lineCounter = 1; lineCounter < rowNumber; lineCounter++) // we are ommiting first line since it only contains var names
        {
            HashMap queryRow = new HashMap(columnNumber);
            String[] explodedRow = queryLines[lineCounter].split(",", -1);
            for (int columnCounter = 0; columnCounter < columnNumber; columnCounter++) {

                System.out.println(resultVars.size() + ", " + explodedRow.length);
                //System.out.println(resultVars.get(columnCounter));
                System.out.println(explodedRow[columnCounter]);
                queryRow.put(resultVars.get(columnCounter), explodedRow[columnCounter]);
            }
            queryRows.add(queryRow);

        }
        String test = createAttributeList(queryRows, resultVars);
        /*
      for(Object hm : queryRows)
      {
          printMap((HashMap)hm) ;
      }
        qe.close();
    */



        //1. Znajdź wszystkie możliwe obiekty, które należą do wszystkich pokojów w ontologi
        // TODO : wrzucić do jakiegoś pliku resource




        //todo:
        //1) znajdź wszystkie atrybuty z jokerem'
        //2)) znajdź nazwę property w tym atrybucie
        //3) zamień joker na wartości z hashmapy
        //4. usunąć zdubplikowane linijki
    }

    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }




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

    public static String createAttributeList(List queryRows, List<String> resultVars) {

        // todo: podzielić na partie
        // 1) cechy samego pomieszczenia
        // 2) cechy elementów pomieszczenie - to mamy
        // 3) subatrybuty

        // constants definitions
        String attributeLineStart = "attribute(";
        String attributeLineEnd = ")\n";
        String nameSeparator = "_";
        String elementSeparator = ",";
        String booleanValues = "[true,false]";
        String valueStart = "[";
        String valueEnd = ", unspecified]";
        String valueMarker = "$$$"; // string to mark place when we will put values of property

        HashMap<String, HashSet<String>> objectPropertyValues = new HashMap<>();


        LinkedList<String> attributeList = new LinkedList<>();
        // iterate through list of hashmaps
        for (Object hm : queryRows) {
            //TODO: jak rozwiązać problem z odnoszeniem się do nazw kolumn

            String booleanAttribute = "";
            String propertyAttribute = "";
            HashMap row = (HashMap) hm;


            String roomType = resultVars.get(0);
            String roomTypeName = (String) row.get(roomType); // todo: zmienić nazwę na mainObjectName

            String objectType = resultVars.get(1);
            String objectTypeName = (String) row.get(objectType); //

            String objectProperty = resultVars.get(3);
            String objectPropertyName = (String) row.get(objectProperty); //

            String propertyValue = resultVars.get(4);
            String propertyValueName = (String) row.get(propertyValue); //

            // add property value to hashmap (we need to gather all possible values from every row)
            String key = objectTypeName + nameSeparator + objectPropertyName;

            if(objectPropertyValues.containsKey(key))
            {
                HashSet<String> set = objectPropertyValues.get(key);
                set.add(propertyValueName);
                objectPropertyValues.put(key,set);
            }
            else
            {
                HashSet<String> set = new HashSet<>();
                set.add(propertyValueName);
                objectPropertyValues.put(key , set);
            }

              /*
            Attribute example from ai.wiki
            attribute(classroom_whiteboard, [true, false]).
            attribute(classroom_whiteboard_model, [blackboard, smartboard]).
             */

            //todo: uwzględnić wartość unspecified

            //attribute(classroom_name, ["Laboratorium 316", unspecified]).\ - do tego potrzebujemy innego zapytania
            //attribute(classroom_desk_color_intensity, [high, unspecified]).
            //attribute(classroom_desk_model, [regular, unspecified]).
            //attribute(classroom_door_color, [gray]).

            booleanAttribute = attributeLineStart + roomTypeName + nameSeparator + objectTypeName + elementSeparator + booleanValues + attributeLineEnd;
            propertyAttribute = attributeLineStart + roomTypeName + nameSeparator + objectTypeName + nameSeparator + objectPropertyName + elementSeparator + valueStart + valueMarker + valueEnd + attributeLineEnd;

            attributeList.add(booleanAttribute);
            attributeList.add(propertyAttribute);
        }

        // delete duplicate lines
        List<String> attributeListWithoutDuplicates =  new ArrayList<>(new LinkedHashSet(attributeList));

        // add values of properties
        for(String propertyName : objectPropertyValues.keySet())
        {
            String searchedProperty = propertyName;
            String values = objectPropertyValues.get(propertyName).toString();
            values =  values.substring(1, values.length() - 1);  // delete [ and ] from values string

            // iterate through list of attributes and put proper values of property
            for (String attribute: attributeListWithoutDuplicates) {
                if(attribute.contains(searchedProperty))
                {
                    int elementIndex = attributeListWithoutDuplicates.indexOf(attribute);
                    String attributeWithValues = attribute.replace(valueMarker,values );
                    attributeListWithoutDuplicates.set(elementIndex, attributeWithValues);
                }
            }
        }
        // deal with sub attributes?

        for (String s: attributeListWithoutDuplicates
                ) {
            System.out.print(s);
        }

        return null;


    }



    /*
        //5. Nazwy zmiennych zapytania

        System.out.println("----------------------");
        System.out.println("Vars");
        System.out.println("----------------------");





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

