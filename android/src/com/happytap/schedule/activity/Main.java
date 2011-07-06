package com.happytap.schedule.activity;

import java.io.*;
public class Main
{
    public static void main(String[] args) throws Exception
    {
    	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        while ((s = in.readLine()) != null && s.length() != 0) {
        	String[] line = s.split(" ");
        	for(String string : line) {
        		System.out.println(string);
        	}
        }
          
      }
}