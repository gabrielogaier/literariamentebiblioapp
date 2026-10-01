package br.com.literariamente.biblioapp;
import org.json.*;
import java.net.*;
import java.io.*;
import android.util.Base64;
/** Optional HTTPS integration. No account or API key. */
public final class BookSearch {
    static byte[] fetch(String url,int limit)throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","LiterariamenteBiblioApp/1.0 (personal Android library)");
        try{
            if(c.getResponseCode()!=200)throw new IOException("Serviço indisponível ("+c.getResponseCode()+").");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Resposta muito grande.");out.write(b,0,n);}return out.toByteArray();
            }
        }finally{c.disconnect();}
    }
    static JSONObject json(String url)throws Exception{return new JSONObject(new String(fetch(url,5*1024*1024),"UTF-8"));}
    static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    static JSONArray searchDocs(String query)throws Exception{
        String fields="key,title,author_name,first_publish_year,edition_key,editions,editions.key,editions.title,editions.subtitle,editions.language,editions.publish_date,editions.publisher,editions.isbn,editions.number_of_pages,editions.cover_i";
        JSONObject result=json("https://openlibrary.org/search.json?"+query+"&lang=pt&limit=15&fields="+enc(fields));
        JSONArray docs=result.optJSONArray("docs");
        return docs==null?new JSONArray():docs;
    }
    public static JSONArray search(String title,String author,String isbn)throws Exception{
        title=title==null?"":title.trim();
        author=author==null?"":author.trim();
        isbn=isbn==null?"":isbn.replaceAll("[^0-9Xx]","").trim();
        JSONArray docs;
        if(!isbn.isEmpty()){
            docs=searchDocs("isbn="+enc(isbn));
            if(docs.length()>0)return docs;
        }
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
    static String description(JSONObject o){Object v=o.opt("description");return v instanceof JSONObject?((JSONObject)v).optString("value",""):v instanceof String?(String)v:"";}

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
        code=code.replace("/languages/","").toLowerCase();
        if(code.equals("por")||code.equals("pt"))return "Português";
        if(code.equals("eng")||code.equals("en"))return "Inglês";
        if(code.equals("spa")||code.equals("es"))return "Espanhol";
        if(code.equals("fra")||code.equals("fre")||code.equals("fr"))return "Francês";
        if(code.equals("ita")||code.equals("it"))return "Italiano";
        if(code.equals("deu")||code.equals("ger")||code.equals("de"))return "Alemão";
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

    /** Prefers the edition selected by Open Library for Portuguese users. */
    public static JSONObject details(JSONObject doc)throws Exception{
        JSONObject out=new JSONObject();for(String f:Db.BOOK_FIELDS)out.put(f,"");
        JSONArray names=doc.optJSONArray("author_name");String authors="";
        if(names!=null)for(int i=0;i<names.length();i++)authors+=(i==0?"":", ")+names.optString(i);
        out.put("author",authors);

        JSONObject preferred=preferredEdition(doc);
        String edition="";
        if(preferred!=null){
            String key=preferred.optString("key","");
            if(key.startsWith("/books/"))edition=key.substring("/books/".length());
        }
        if(edition.isEmpty())edition=first(doc,"edition_key");

        JSONObject e=new JSONObject();
        if(edition.matches("OL[0-9]+M"))e=json("https://openlibrary.org/books/"+edition+".json");

        String preferredTitle=preferred==null?"":preferred.optString("title","");
        out.put("title",e.optString("title",preferredTitle.isEmpty()?doc.optString("title",""):preferredTitle));
        out.put("subtitle",e.optString("subtitle",preferred==null?"":preferred.optString("subtitle","")));

        String publisher=first(e,"publishers");
        if(publisher.isEmpty()&&preferred!=null)publisher=first(preferred,"publisher");
        out.put("publisher",publisher);

        String date=e.optString("publish_date",preferred==null?"":preferred.optString("publish_date",""));
        java.util.regex.Matcher match=java.util.regex.Pattern.compile("\\b[12][0-9]{3}\\b").matcher(date);
        out.put("year",match.find()?match.group():doc.optString("first_publish_year",""));

        String foundIsbn=first(e,"isbn_13");
        if(foundIsbn.isEmpty())foundIsbn=first(e,"isbn_10");
        if(foundIsbn.isEmpty()&&preferred!=null)foundIsbn=first(preferred,"isbn");
        out.put("isbn",foundIsbn);

        String pages=e.optString("number_of_pages","");
        if(pages.isEmpty()&&preferred!=null)pages=preferred.optString("number_of_pages","");
        out.put("pages",pages);

        out.put("description",description(e));

        JSONArray langs=e.optJSONArray("languages");
        String lang="";
        if(langs!=null&&langs.length()>0){
            Object firstLang=langs.opt(0);
            if(firstLang instanceof JSONObject)lang=((JSONObject)firstLang).optString("key","");
            else lang=String.valueOf(firstLang);
        }
        if(lang.isEmpty()&&preferred!=null){
            JSONArray preferredLangs=preferred.optJSONArray("language");
            if(preferredLangs!=null&&preferredLangs.length()>0)lang=preferredLangs.optString(0,"");
        }
        out.put("language",languageName(lang));

        String work=doc.optString("key","");
        if(out.optString("description").isEmpty()&&work.matches("/works/OL[0-9]+W"))try{
            out.put("description",description(json("https://openlibrary.org"+work+".json")));
        }catch(Exception ignored){}

        JSONArray covers=e.optJSONArray("covers");
        long coverId=covers!=null&&covers.length()>0?covers.optLong(0):preferred==null?0:preferred.optLong("cover_i",0);
        if(coverId>0)try{
            out.put("cover",Base64.encodeToString(fetch("https://covers.openlibrary.org/b/id/"+coverId+"-M.jpg",1024*1024),Base64.NO_WRAP));
        }catch(Exception ignored){}
        return out;
    }
}
