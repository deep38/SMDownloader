package com.ocean.smdownloader.YTMediaHandler;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTMediaRetrieverAlgorithms {
    public static final String getYtInitialResponse =
            "function yt(){ return JSON.stringify(ytInitialPlayerResponse.streamingData); } yt();";

    public static final String getBaseJsUrl =
            "var a = document.getElementsByTagName('script'); "+
                    "function getScript() { "+
                    "for (var i = 0; i < a.length; i++) { "+
                    "var script = a[i].innerText;"+
                    "if (script.includes('base.js')) {"+
                    "var links = script.match(/\\/s\\/player\\/(([0-9]|[A-Z]|[a-z]|[./_-])*)\\/base.js/);"+
                    "return links[0];"+
                    "}"+
                    "}"+
                    "}"+
                    "getScript();";

    public static final String getTitle =
            "function getTitle(){ "+
                    "return ytInitialData.contents.singleColumnWatchNextResults.results.results.contents[1].slimVideoMetadataSectionRenderer.contents[0].slimVideoInformationRenderer.title.runs[0].text;"+
                    "}"+
                    "getTitle();";


    public static String getScriptForDecipher(String result) throws Exception{
        String TAG = "Decipher";
        File file = new File(Environment.getExternalStorageDirectory() + "/Download/base.js");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(result);
        writer.close();

        Pattern pattern = Pattern.compile("([A-z])+=function\\(([A-z]+)\\)\\{([A-z])+=([A-z])+.split\\(\"\"\\);(.*)return [A-z].join\\(\"\"\\)\\};");
        Matcher matcher = pattern.matcher(result);
        String foo="";
        Log.d(TAG, matcher.groupCount() +"");
        while(matcher.find()){
            foo = matcher.group(0);
            Log.d(TAG, "Match found in "+ foo);

            String endOfFoo = ".join(\"\")};";
            assert foo != null;
            foo = foo.substring(0,foo.indexOf(endOfFoo)+endOfFoo.length());

            String subFuncVar = "", mainFuncName;
            mainFuncName = foo.substring(0,foo.indexOf("="));

            String[] functions = foo.split(";");
            if(functions.length>1)
                subFuncVar=functions[1].substring(0, functions[1].indexOf("."));

            result = result.substring(result.indexOf("var "+subFuncVar+"={"));
            result = result.substring(0, result.indexOf("}};")+3);
            foo = result+" var "+foo+ " "+mainFuncName;

            return foo;
        }
        throw new Exception("error while getting decipher functions: "+foo);
    }

    public static String getThumbnailLink(String link) {
        String key="";
        if (link.startsWith("https://youtu.be/")) {
            key = link.replace("https://youtu.be/", "");
        } else if (link.startsWith("https://m.youtube.com/")) {
            key = link.replace("https://m.youtube.com/watch?v=", "");
            key = key.substring(0, key.indexOf("&"));
        } else if (link.startsWith("https://youtube.com/shorts/")) {
            key = link.replace("https://youtube.com/shorts/", "");
            key = key.substring(0, key.indexOf("?"));
        }


        return "https://img.youtube.com/vi/" + key + "/default.jpg";
    }
}
