package com.justin.garlicbread.why;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by mylesoleary on 4/6/19.
 */

public class Storage {

    //File stores prices in this format;
    //date price

    public static double read(File balance) {
        double total = 0;
        Scanner input;
        try {
            input = new Scanner(balance);
            while(input.hasNextLine()) {
                String[] s = input.nextLine().split(" ");
                total += Double.parseDouble(s[s.length-1]);
                Log.d("STORAGE ", "line: " + s[s.length-1]);
            }
            input.close();
        }catch(IOException e) {
            Log.d("STORAGE ", "exception thrown: " + e.getMessage());
        }
        return total;
    }

    public static double[] getHistory(File balance) {
        double[] ret = new double[31];
        double total = 0;
        Scanner input;
        try {
            input = new Scanner(balance);
            int i = 0;
            while(input.hasNextLine() && i<31) {
                String next = input.nextLine();
                String price = next.substring(next.lastIndexOf(" ") + 1);
                Date date = DateFormat.getInstance().parse(next.substring(0, next.lastIndexOf(" ")));
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                while(calendar.get(Calendar.DAY_OF_MONTH) - 2 > i){
                    i++;
                    ret[i] = total;
                }
                total += Double.parseDouble(price);
                ret[i] = total;
            }
            for(;i<31; i++){
                ret[i] = total;
            }
            for(double j : ret){
                Log.d("Storage", "" +j);
            }

            input.close();
        }catch(IOException e) {
            Log.d("STORAGE ", "exception thrown: " + e.getMessage());
        } catch (ParseException e) {
            Log.d("STORAGE ", "exception thrown: " + e.getMessage());
        }
        return ret;
    }

    public static void add(File balance, Date date, double amount) {
        PrintWriter pw;
        try {
            pw = new PrintWriter(new FileWriter(balance, true));
            pw.println(DateFormat.getInstance().format(date) + " " + amount);
            pw.close();
        }catch(IOException e) {}
    }

    public static void reset(File balance){
        PrintWriter pw;
        try {
            pw = new PrintWriter(new FileWriter(balance, false));
            pw.close();
        }catch(IOException e) {}
    }
}
