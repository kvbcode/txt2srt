/*
 * The MIT License
 *
 * Copyright 2020 Kirill Bereznyakov.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cyber.txt2srt;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Kirill Bereznyakov
 */
public class Main {    
    
    static int DEFAULT_SHOW_TIME_MS = 5000;
    static final String SRT_TIME_FORMAT = "%s --> %s";
    static final String SRT_MSG_FORMAT = "%s: %s";
    
    public static void main(String[] args) throws Exception{                
        
        if (args.length==0){
            showHelp();
            return;
        }
        
        int subDelayMs = DEFAULT_SHOW_TIME_MS;        
        if (args.length==3) subDelayMs = Integer.parseInt(args[2]);
        
        System.out.println("convert: " + args[0] + " to " + args[1]);                
        System.out.println(StandardCharsets.UTF_8 + " --> " + Charset.defaultCharset());
        System.out.println("sub delay: " + subDelayMs + " ms");
        
        String[] lines = Files.lines( Path.of(args[0]), StandardCharsets.UTF_8).toArray(String[]::new);
        
        try( PrintWriter out = new PrintWriter(args[1], Charset.defaultCharset()) ){
            for(int i=0; i<lines.length; i++){
                String[] srtData = proceedLine(subDelayMs, lines[i]);
                out.println( String.valueOf( i ) );
                out.println( srtData[0] );
                out.println( srtData[1] );
                out.println();
            }            
            out.flush();
        }
        
        System.out.println("Done.");
    }
    
    public static void showHelp() {
        System.out.println("java -jar txt2srt.jar <input> <output> [delay]");
        System.out.println("  <input> - input filename");
        System.out.println("  <output> - output filename");
        System.out.println("  [delay] - subtitle delay in milliseconds");        
    }
    
    public static String[] proceedLine( int showTimeMs, String line ){

        String[] srt = new String[2];
        
        String regexp = "^\\x5b([\\d\\:]{3,})\\x5d\\s(.+):\\s(.+)";
        Pattern pat = Pattern.compile(regexp, Pattern.UNICODE_CHARACTER_CLASS);        
        Matcher mat = pat.matcher(line);        
        mat.find();
        
        try{        
            String timeStr = mat.group(1);
            String user = mat.group(2);
            String msg = mat.group(3);

            srt[0] = getSrtTimeStr( timeStr, showTimeMs );
            srt[1] = String.format(SRT_MSG_FORMAT, user ,msg); 
        }catch(IllegalStateException ex){
            System.err.println("RegExp error on line: " + line);
            throw ex;
        }
        
        return srt;
    }
    
    public static Duration parseDuration(String timeStr){
        String[] timePart = timeStr.split(":");
        
        if (timePart.length==2){
            return Duration.ZERO
                .plusMinutes( Integer.valueOf(timePart[0]) )
                .plusSeconds( Integer.valueOf(timePart[1]) );
        }
        
        return Duration.ZERO
            .plusHours( Integer.valueOf(timePart[0]) )
            .plusMinutes( Integer.valueOf(timePart[1]) )
            .plusSeconds( Integer.valueOf(timePart[2]) );        
    }
    
    public static String formatDuration(Duration d){
        return String.format("%02d:%02d:%02d,%03d",
            d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart() );
    }
    
    public static String getSrtTimeStr(String startTimeStr, int showTimeMs){
    
        Duration startTime = parseDuration( startTimeStr );
        Duration endTime = startTime.plusMillis( showTimeMs );
        
        return String.format(SRT_TIME_FORMAT, formatDuration(startTime), formatDuration(endTime) );
    }        
        
}
