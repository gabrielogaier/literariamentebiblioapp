package br.com.literariamente.biblioapp;
import org.json.*;
import java.net.*;
import java.io.*;

/** Optional HTTPS integration. No account or API key. */
public final class BookSearch {
    static byte[] fetch(String url,int limit)throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","LiterariamenteBiblioApp/1.0 (personal Android library)");
        try{
            if(c.getResponseCode()!=200)throw new IOException("Serviço indisponível ("+c.getResponseCode()+").");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1){
                    if(out.size()+n>limit)throw new IOException("Resposta muito grande.");
                    out.write(b,0,n);
                }
                return out.toByteArray();
            }
        }finally{c.disconnect();}
    }
    static JSONObject json(String url)throws Exception{return new JSONObject(new String(fetch(url,5*1024*1024),"UTF-8"));}
    static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    static JSONArray searchDocs(String query)throws Exception{
        String fields="key,title,author_name,first_publish_year,cover_i,editions,editions.title,editions.language,editions.publish_date";
        JSONObject result=json("https://openlibrary.org/search.json?"+query+"&lang=pt&limit=15&fields="+enc(fields));
        JSONArray docs=result.optJSONArray("docs");
        return docs==null?new JSONArray():docs;
    }
    public static JSONArray search(String title,String author)throws Exception{
        title=title==null?"":title.trim();
        author=author==null?"":author.trim();
        JSONArray docs;
        if(!title.isEmpty()&&!author.isEmpty()){
            docs=searchDocs("title="+enc(title)+"&author="+enc(author));
            if(docs.length()>0)return docs;
        }
        if(!title.isEmpty()){
            docs=searchDocs("title="+enc(title));
            if(docs.length()>0)return docs;
        }
        StringBuilder general=new StringBuilder();
        if(!title.isEmpty())general.append(title);
        if(!author.isEmpty()){
            if(general.length()>0)general.append(" ");
            general.append(author);
        }
        if(general.length()>0){
            docs=searchDocs("q="+enc(general.toString()));
            if(docs.length()>0)return docs;
        }
        if(!author.isEmpty()){
            docs=searchDocs("author="+enc(author));
            if(docs.length()>0)return docs;
        }
        return new JSONArray();
    }
    static String first(JSONObject o,String k){JSONArray a=o.optJSONArray(k);return a!=null&&a.length()>0?a.optString(0,""):"";}
    static JSONObject preferredEdition(JSONObject doc){
        JSONObject editions=doc.optJSONObject("editions");
        JSONArray docs=editions==null?null:editions.optJSONArray("docs");
        if(docs==null||docs.length()==0)return null;
        JSONObject fallback=docs.optJSONObject(0);
        for(int i=0;i<docs.length();i++){
            JSONObject e=docs.optJSONObject(i);if(e==null)continue;
            JSONArray languages=e.optJSONArray("language");
            if(languages!=null)for(int j=0;j<languages.length();j++){
                String lang=languages.optString(j,"");
                if(lang.equals("por")||lang.equals("pt"))return e;
            }
        }
        return fallback;
    }
    static String languageName(String code){
        if(code==null)return "";
        code=code.toLowerCase();
        if(code.equals("por")||code.equals("pt"))return "Português";
        if(code.equals("eng")||code.equals("en"))return "Inglês";
        if(code.equals("spa")||code.equals("es"))return "Espanhol";
        if(code.equals("fra")||code.equals("fre")||code.equals("fr"))return "Francês";
        return code;
    }
    public static String resultTitle(JSONObject doc){
        JSONObject e=preferredEdition(doc);
        return e!=null&&!e.optString("title","").isEmpty()?e.optString("title"):doc.optString("title","");
    }
    public static String resultLanguage(JSONObject doc){
        JSONObject e=preferredEdition(doc);if(e==null)return "";
        JSONArray langs=e.optJSONArray("language");
        return langs!=null&&langs.length()>0?languageName(langs.optString(0,"")):"";
    }
    public static String resultYear(JSONObject doc){
        JSONObject e=preferredEdition(doc);
        String date=e==null?"":e.optString("publish_date","");
        java.util.regex.Matcher match=java.util.regex.Pattern.compile("\\b[12][0-9]{3}\\b").matcher(date);
        return match.find()?match.group():doc.optString("first_publish_year","");
    }
    public static JSONObject details(JSONObject doc)throws Exception{
        JSONObject out=new JSONObject();
        out.put("title",resultTitle(doc));
        JSONArray names=doc.optJSONArray("author_name");String authors="";
        if(names!=null)for(int i=0;i<names.length();i++)authors+=(i==0?"":", ")+names.optString(i);
        out.put("author",authors);
        out.put("year",resultYear(doc));
        int coverId=doc.optInt("cover_i",0);
        if(coverId>0){
            try{
                byte[] image=fetch("https://covers.openlibrary.org/b/id/"+coverId+"-M.jpg",2*1024*1024);
                out.put("cover",android.util.Base64.encodeToString(image,android.util.Base64.NO_WRAP));
            }catch(Exception ignored){}
        }
        return out;
    }
}
