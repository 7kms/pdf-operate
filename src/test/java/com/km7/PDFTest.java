package com.km7;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PDFTest {
    public static void  print (Object o){
        System.out.println(o);
    }

    @Test
    public void generateOutline() {

    }

    @Test
    public void testRegExpression(){
        String str = "xxx213";
        Pattern pattern = Pattern.compile("\\d+$");
        Matcher matcher = pattern.matcher(str);
        print(matcher.matches());
        while (matcher.find()){
            print(matcher.group());
        }
    }

}