import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.io.*;

import org.json.*;

public class Main {

    // This method sends an httpRequest to the necessary DineOnCampus api url to retrieve the menu's JSON data, which is parsed later
    public static String httpRequest(String url) throws URISyntaxException, IOException, InterruptedException { 
        HttpResponse<String> response;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();
    
            response = client.send(request,
                BodyHandlers.ofString());
            return response.body();
        }
        catch (Exception e) {
            System.out.println("An error occurred: " + e); 
        }
        return "Catastrophic failure";
    }


    // This method parses the json and fills in the hashmap
    // Yes, i know, collision handling is done both here and in the main method
    // I had to because I have to parse multiple json files into hashmaps and merge them
    public static HashMap<String, ArrayList<MenuItem>> parseJSON(String jsonAsString, String diningHall, int period) {
        HashMap<String, ArrayList<MenuItem>> menuHashMap = new HashMap<>();

        JSONObject initialJSON = new JSONObject(jsonAsString);

        JSONObject menu = (JSONObject)initialJSON.get("menu");
        String date = (String)menu.get("date");

        JSONObject periods = (JSONObject)menu.get("periods");
        String meal = (String)periods.get("name");
        
        JSONArray categories = (JSONArray)periods.get("categories");

        for (int x = 0; x < categories.length(); x++) {
            JSONObject itemsBuffer = (JSONObject)categories.get(x);
            JSONArray items = (JSONArray)itemsBuffer.get("items");
            String station = (String)itemsBuffer.get("name");

            // hash map which contains all menu information (the proverbial Big One)
            // call an item's MenuItem by using that item's name as the key
            // utilizing closed addressing to handle item collisions (data structures & algo moment)
            // (ie an item is served twice or more in a semester)
            for (Object itemObject : items) {
                ArrayList<MenuItem> itemArrListTemp = new ArrayList<MenuItem>();
                JSONObject jsonItemObject = (JSONObject)itemObject;
                String itemName = (String)jsonItemObject.get("name");

                MenuItem dish = new MenuItem(itemName, date, station, meal, diningHall);
            
                // if item already in hashmap, add to that item's arraylist
                if (menuHashMap.containsKey(itemName)) {
                    menuHashMap.get(itemName).add(dish);
                }
                
                // if item not already in hashmap, add new hashmap entry with new MenuItem arraylist
                // using temp arraylist to work around clearing the arraylist later screwing the whole thang up
                else {
                    itemArrListTemp.add(dish);
                    menuHashMap.put(itemName, itemArrListTemp);
                }
            }
        }

        System.out.println("Meal period generated at period " + period);

        return menuHashMap;
    }

    // generates all URLs necessary to create a hashmap which contains all dishes served for breakfast, lunch, and dinner...
    public static ArrayList<String> generateURLs(int startingDay, int startingMonth, int days) {
        ArrayList<String> result = new ArrayList<String>();
        int day = startingDay;
        int month = startingMonth;
        String date = "";
        String breakfastKey = "659c0e22e45d43085e7d72cb";
        String lunchKey = "65981bd4e45d430889f3431a";
        String dinnerKey = "659834c2351d53068d32fa3f";
        HashMap<Integer, Integer> calendar = new HashMap<>();
        calendar.put(1, 31);
        calendar.put(2, 29); // 2024, tis a leap year
        calendar.put(3, 31);
        calendar.put(4, 30);
        calendar.put(5, 31);

        // ...until int parameter days
        for (int n = 0; n < days; n++) {
            if (day == calendar.get(month)) {
                month++;
                day = 1;
            }
            if ((month == 2 && day == 24)) {
                day = 26;
                n += 2;
            }
            if ((month == 3 && day == 21)) {
                day = 22;
                n++;
            }
            if ((month == 3 && day == 24)) {
                day = 25;
                n++;
            }
            if ((month == 3 && day == 30)) {
                day = 31;
                n++;
            }
            if ((month == 4 && day == 6)) {
                day = 8;
                n += 2;
            }
            date = "2024-" + String.valueOf(month) + "-" + String.valueOf(day);
            System.out.println(date);
            result.add("https://api.dineoncampus.com/v1/location/587124593191a200db4e68af/periods/" + breakfastKey + "?platform=0&date=" + date);
            result.add("https://api.dineoncampus.com/v1/location/587124593191a200db4e68af/periods/" + lunchKey + "?platform=0&date=" + date);
            result.add("https://api.dineoncampus.com/v1/location/587124593191a200db4e68af/periods/" + dinnerKey + "?platform=0&date=" + date);
            if (month == 3 && day == 31) {
                continue;
            }
            else {
                day++;
            }
            System.out.println(day);
        }

        return result;
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        ArrayList<String> urls = generateURLs(28, 1, 100); // ONLY HAS SUPPORT FOR THE CURRENT SEMESTER (JAN-MAY) DO NOT PUT 6 AS THE STARTING MONTH you GOOF
        // as of jan 19 2024 sovi menus are not posted for feb 24/25 or mar 24/30 or apr 6 for some reason if you start/go through there it will just skip over them (not counted as a "day")
        HashMap<String, ArrayList<MenuItem>> menuMap = new HashMap<>();
        HashMap<String, ArrayList<MenuItem>> menuMapTemp = new HashMap<>();

        // collision handling purgatory
        // using putAll() to merge temp map and master map did not work because it overwrites the value if there are
        // key collisions (which is not what I want)
        int n = 1;
        for (String url : urls) {
            String json = httpRequest(url);
            menuMapTemp = parseJSON(json, "Sovi", n);
            // absurdly scuffed collision handling
            for (String s : menuMapTemp.keySet()) {
                if (menuMap.containsKey(s)) {
                    // for loop needed because a couple items are served twice in one meal period
                    // at two different stations (The Diced Cantaloupe Problem)
                    for (MenuItem b : menuMapTemp.get(s)) {
                        menuMap.get(s).add(b);
                    }
                }
                else {
                    menuMap.put(s, menuMapTemp.get(s));
                }
            }
            n++;
        }
  
        BufferedWriter bf = null; 
  
        try { 
  
            bf = new BufferedWriter(new FileWriter("hashmap.txt")); 
  
            for (HashMap.Entry<String, ArrayList<MenuItem>> entry : 
                menuMap.entrySet()) { 
  
                bf.write(entry.getKey() + ":" + entry.getValue()); 
  
                bf.newLine(); 
            } 
  
            bf.flush(); 
        } 
        catch (IOException e) { 
            e.printStackTrace(); 
        } 
        finally { 
            try { 
                bf.close(); 
            } 
            catch (Exception e) { 
            } 
        } 

        try {
            boolean firstLoop = true;
            Scanner myObj = new Scanner(System.in);
            System.out.println("\nwhatchu want?");
            String request = myObj.nextLine();
        
            while (!request.equals("quit")) {
                if (firstLoop == false) {
                    System.out.println("\nwhat else you want?");
                    request = myObj.nextLine();
                }

                if (menuMap.containsKey(request)) {
                    System.out.println("\n" + menuMap.get(request).get(0).toString());
                    if (menuMap.get(request).size() > 1) {
                        System.out.println("\nOther times " + request + " will be served: \n");
                        for (int x = 1; x < menuMap.get(request).size(); x++) {
                            System.out.println(menuMap.get(request).get(x).toString());
                        }
                    }
                }
                else if (request.equals("quit")) {
                    break;
                }
                else {
                    System.out.println("Tough luck, " + request + " ain't being served anytime soon... it's ok champ you'll get em next time tiger");
                }

                firstLoop = false;
            }
            myObj.close();
        }
        catch (NoSuchElementException e) { }
    }
}