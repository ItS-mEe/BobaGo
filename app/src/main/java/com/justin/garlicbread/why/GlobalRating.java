package com.justin.garlicbread.why;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * Created by mylesoleary on 4/7/19.
 */

public class GlobalRating {

    private HashMap<String, int[]> locations;
    private FirebaseDatabase db;
    private DatabaseReference myRef;

    //location / ratings stored as:
    //name location total n

    public GlobalRating(File file) {
        locations = new HashMap<String, int[]>();
        Scanner input;

            db = FirebaseDatabase.getInstance("https://bobadb-3e2b8.firebaseio.com/");
            myRef = db.getReference();

            myRef = myRef.child("GlobalRatings");
            final DatabaseReference _myref = myRef;
            ValueEventListener vel = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    for(DataSnapshot storeData : dataSnapshot.getChildren()) {
                        locations.put(storeData.getKey(), new int[]{-1, ((Long)storeData.child("rating").getValue()).intValue(), ((Long)storeData.child("n").getValue()).intValue()});
                    }
                    try {
                        Scanner input = new Scanner(file);
                        while (input.hasNextLine()) {
                            String s = input.nextLine();
                            String[] tokens = s.split(" ");
                            String key = "";
                            int i = 0;
                            for (; i < tokens.length - 2; i++) {
                                key += tokens[i] + " ";
                            }
                            key = key.substring(0, key.length() - 1);
                            if (!locations.containsKey(key)) {
                                locations.put(key, new int[]{0, Integer.parseInt(tokens[1 + i]), 1});
                                myRef.child(key).child("n").setValue(1);
                                myRef.child(key).child("rating").setValue(Integer.parseInt(tokens[1 + i]));
                            }
                            locations.get(key)[0] = Integer.parseInt(tokens[++i]);
                        }
                        input.close();
                    }catch(Exception e){

                    }

                    _myref.removeEventListener(this);

                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            };
            myRef.addValueEventListener(vel);
    }

    public void addRating(String location, int rating, File file) {
        if(!locations.containsKey(location)){
            locations.put(location, new int[]{rating, rating, 1});
            myRef.child(location).child("n").setValue(1);
            myRef.child(location).child("rating").setValue(rating);
            Log.d(TAG, "didn't contain key");
        } else {
            int delta = rating-locations.get(location)[0];
            if(locations.get(location)[0] == -1) {
                myRef.child(location).child("n").setValue(locations.get(location)[2]+1);
                locations.get(location)[1] += rating;
                myRef.child(location).child("rating").setValue(locations.get(location)[1] + rating);
                Log.d(TAG, "had key but location there was -1");
            }else {
                myRef.child(location).child("rating").setValue(locations.get(location)[1] + delta);
                locations.get(location)[1] += delta;
                locations.get(location)[0] = rating;
                Log.d(TAG, "had key and location there was not -1");
            }
        }
        PrintWriter writer;
        try {
            writer = new PrintWriter(file);
            Set<String> keys = locations.keySet();
            Iterator<String> iter = keys.iterator();
            while(iter.hasNext()) {
                String key = iter.next();
                writer.println(key + " " + locations.get(location)[0] + " " + locations.get(location)[1] + " ");
            }
            writer.close();
        }catch(IOException e) {}
    }

    public void setRating(String location, int oldR, int newR, File file) {
        locations.get(location)[0] = locations.get(location)[0] - oldR + newR;
        PrintWriter writer;
        try {
            writer = new PrintWriter(file);
            HashSet<String> keys = (HashSet<String>) locations.keySet();
            Iterator<String> iter = keys.iterator();
            while(iter.hasNext()) {
                String key = iter.next();
                writer.println(key + " " + locations.get(location)[0] + " " + locations.get(location)[1] + " ");
            }
            writer.close();
        }catch(IOException e) {}
    }

    public double getRating(String location) {
        return (double)locations.get(location)[1] / (double)locations.get(location)[2];
    }

    public HashMap<String, int[]> getRatings() {
        return locations;
    }

    public double numRatings(String location) {
        return locations.get(location)[2];
    }
}