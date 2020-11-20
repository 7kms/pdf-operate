package com.km7;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDF {

    public final static  int startPageIndex = 14; // 正文页码开始的实际页数(除去空白和各种介绍篇幅所占的页数, 如前面一共有10页篇幅, 则startPageIndex=9)
    // 章节
    private final static  Pattern chapterPattern = Pattern.compile("^第(\\d+)章.*?(\\d+)$");
    // 普通标题
    private final static  Pattern titlePattern = Pattern.compile("^([\\d\\.]+)(.*?)(\\d+)$");
    // 其他标题
    private final static  Pattern otherPattern = Pattern.compile("(.*?)(\\d+)$");

    private static String removeDot(String originStr){
        return  originStr.replaceAll("(.*?)\\.+$", "$1");
    }

    private static HashMap<String, Object> parseLine(String lineStr){
        HashMap<String,Object> map = new HashMap<>();
        Pattern[] patterns = { chapterPattern, titlePattern, otherPattern};
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(lineStr);
            String chapter = null;
            String page = null;
            String content = null;
            if(m.matches()){
                if(pattern == chapterPattern){
                    //章节
                    chapter = m.group(1);
                    //内容
                    content = lineStr;
                    //页码
                    page = m.group(2);

                }else if(pattern == titlePattern){
                    //章节
                    chapter = m.group(1);
                    //内容
                    content = m.group(2);
                    //页码
                    page = m.group(3);

                }else if(pattern == otherPattern){
                    //内容
                    content = m.group(1);
                    //页码
                    page = m.group(2);
                }
                map.put("chapter", chapter);
                map.put("content", removeDot(content.trim()));
                map.put("page", page);
                break;
            }
        }
        return map;
    }

    /**
     * 读取目录文件, 将其转换到ArrayList当中
     * @return ArrayList 目录list
     * @throws IOException
     */
    public static ArrayList<Map<String, Object>> generateOutline() throws IOException{
        InputStream input = PDF.class.getResourceAsStream("/outlines.txt");
        StringBuilder sb = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(input))){
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.length() > 0) sb.append(line + "\r");
            }
        }
        String allText = sb.toString();
        String[] array = allText.split("\\r");
        ArrayList<Map<String, Object>> outlines = new ArrayList();

        for (String line: array) {
            Map<String,Object> lineMap = parseLine(line);
            String chapter = (String) lineMap.get("chapter");
            String content = (String) lineMap.get("content");
            String page = (String) lineMap.get("page");
            if(page != null){
                Map<String, Object> chapterMap = new HashMap<>();
                chapterMap.put("title", content);
                chapterMap.put("page", Integer.valueOf(page).intValue());
                outlines.add(chapterMap);
//                System.out.printf("章节: %s ; 标题: %s ;  页码: %s", lineMap.get("chapter"), chapterMap.get("title"), chapterMap.get("page"));
                System.out.println();
            }

        }

        return outlines;
    }


    public static void main(String[] args) throws IOException, DocumentException {

        // 1. 创建一个新的文档
        Document document = new Document();

        // 2. 创建一个pdfWriter将数据写入到目标路径中
        PdfCopy writer = new PdfCopy(document, new FileOutputStream("out.pdf"));
        writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);//设置打开pdf文件时显示书签
        document.open();


        // 3.逐页读入pdf文件并写入输出文件
        // 3.1 创建pdfReader
        PdfReader reader = new PdfReader(PDF.class.getResourceAsStream("/demo.pdf"));
        int n = reader.getNumberOfPages();
        for (int page = 1; page <= n; page++) {
            //3.2 从reader中逐页读入,并写入writer
            writer.addPage(writer.getImportedPage(reader, page));
        }
        // 释放reader
        writer.freeReader(reader);

        // 4. 添加书签
        PdfOutline root = writer.getRootOutline();
        // 4.1 获取书签配置
        // todo: 设计一个数据结构, 循环读取二级,三级等子级目录
        ArrayList<Map<String,Object>> outlines =  generateOutline();

        // 4.2 数据遍历设置pdfOutLine
        outlines.forEach(map -> {
            String sectionTitle = (String) map.get("title");
            int page = (int)map.get("page");
            if(page + startPageIndex > n){
                System.out.println("目录页码已经超过了pdf的总页数(将跳过后续设置), 请检查目录文件是否正确. ");
                return;
            }
            // 设置action 主要用于跳转
            PdfAction action = PdfAction.gotoLocalPage(page + startPageIndex, new PdfDestination(PdfDestination.FIT), writer);
            // 设置书签
            // todo: 此处的api是可以递归设置二级,三级等子级目录的
            new PdfOutline(root, action, "没有目录", false);
        });
        System.out.println("demo.pdf 生成完毕");
        document.close();
    }
}
