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
    static String ontologyPath = "..\\qfgen\\rdfxml.owl";
    static String queryPath = "..\\mainQuery";

    //static String ontologyPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\rdfxml.owl";
    //static String queryPath = "C:\\Users\\Rael\\Dropbox\\Uczelnia\\Workshop\\mainQuery";

    static String ontologyIRI = "http://www.semanticweb.org/qfgen#"; //TODO: dodać automatyczne wykrywnaie URI ontologii

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

        /*
        for(Object hm : queryRows)
        {
          printMap((HashMap)hm) ;
        }

        System.out.println( createEntries(queryRows) );

        //QueryExecutionFactory close
        qe.close();
    */


        /*
        //1. Znajdź wszystkie możliwe obiekty, które należą do wszystkich pokojów w ontologi
        // TODO : wrzucić do jakiegoś pliku resource
        String attributeString = "";
        String attributeLineStart = "attribute(";
        String attributeLineEnd = ")\n";
        String nameSeparator = "_";
        String elementSeparator = ",";
        String booleanValues = "[true,false]";
        String valueStart = "[";
        String valueEnd = "]";
        String valueMarker = "$$$"; //todo: jaki znak wybrać?

        HashMap<String, String> objectPropertyValues = new HashMap<>();
        LinkedList<String> attributeList = new LinkedList<>();
        // iterate through list of hashmaps
        for (Object hm : queryRows) {
            //TODO: jak rozwiązać problem z odnoszeniem się do nazw kolumn

            // TODO: musimy znać wszystkie możliwe wartości property - lepiej chyba zrobić to tak, że tworzymy sobie jakaś klasę w której przechowyujemy to po wszystkich iteracjach scalamy i do stringa
            String booleanAttribute = "";
            String propertyAttribute = "";
            HashMap row = (HashMap) hm;
            String roomType = resultVars.get(0);
            String roomTypeName = (String) row.get(roomType); // todo: zmienić nazwę na mainObjectName

            String objectType = resultVars.get(1);
            String objectTypeName = (String) row.get(objectType); //

            String objectProperty = resultVars.get(3);
            String objectPropertyName = (String) row.get(objectProperty); //

            //create


            String propertyValue = resultVars.get(4);
            String propertyValueName = (String) row.get(propertyValue); //

            objectPropertyValues.put(objectPropertyName, propertyValueName);
            //attribute(classroom_whiteboard, [true, false]).
            //attribute(classroom_whiteboard_model, [blackboard, smartboard]).

            // pomysł - tworzę hashmapę wlasnosc-wartosc i na koniec zbieram wszystkie wartosci dla jednej wlasnosci
            booleanAttribute = attributeLineStart + roomTypeName + nameSeparator + objectTypeName + elementSeparator + booleanValues + attributeLineEnd;
            propertyAttribute = attributeLineStart + roomTypeName + nameSeparator + objectTypeName + nameSeparator + valueStart + valueMarker + valueEnd + attributeLineEnd;

            attributeList.add(booleanAttribute);
            attributeList.add(propertyAttribute);
        }

        for (String s: attributeList) {
            System.out.println(s);
        }
*/


        System.out.println( createEntries(queryRows) );


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

    public String createAttributeList() {return null;}



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
    /*
    createEntries

    ex:
    entry(data{classroom_color:yellow, classroom_color_intensity:unspecified, classroom_name:"Laboratorium 316", classroom_size:small, id:lab316, place:classroom}).
    entry(data{classroom_computer:true, classroom_computer_color:white, classroom_computer_model:unspecified, id:lab318}).
     */
    public static String createEntries(ArrayList queryList) {

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
            //printMap( (HashMap)queryList.get(i) );

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
                entry.append( dataID.trim() );
                entry.append("}).");
                entry.append("\n");
            }
//TODO: Z JAKIEGOS POWODU W DATAID JEST JAKIS SMIEC KTORY USUWA CALA LINIJKE...
//System.out.println("DATAID: "+dataID + dataID.length());

        }


        return entry.toString();
    }
}

