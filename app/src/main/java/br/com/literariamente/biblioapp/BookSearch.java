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
    public static JSONArray search(String title,String author,String isbn)throws Exception{
        String query=isbn.trim().isEmpty()?"title="+enc(title)+"&author="+enc(author):"q="+enc("isbn:"+isbn.replaceAll("[^0-9Xx]",""));
        return json("https://openlibrary.org/search.json?"+query+"&limit=15&fields=key,title,author_name,edition_key,first_publish_year").getJSONArray("docs");
    }
    static String first(JSONObject o,String k){JSONArray a=o.optJSONArray(k);return a!=null&&a.length()>0?a.optString(0,""):"";}
    static String description(JSONObject o){Object v=o.opt("description");return v instanceof JSONObject?((JSONObject)v).optString("value",""):v instanceof String?(String)v:"";}
    /** Uses one selected edition so ISBN, pages and publisher are not mixed across editions. */
    public static JSONObject details(JSONObject doc)throws Exception{
        JSONObject out=new JSONObject();for(String f:Db.BOOK_FIELDS)out.put(f,"");
        out.put("title",doc.optString("title",""));JSONArray names=doc.optJSONArray("author_name");String authors="";
        if(names!=null)for(int i=0;i<names.length();i++)authors+=(i==0?"":", ")+names.optString(i);out.put("author",authors);
        String edition=first(doc,"edition_key");JSONObject e=new JSONObject();
        if(edition.matches("OL[0-9]+M"))e=json("https://openlibrary.org/books/"+edition+".json");
        out.put("title",e.optString("title",doc.optString("title","")));out.put("subtitle",e.optString("subtitle",""));out.put("publisher",first(e,"publishers"));
        String date=e.optString("publish_date","");java.util.regex.Matcher match=java.util.regex.Pattern.compile("\\b[12][0-9]{3}\\b").matcher(date);
        out.put("year",match.find()?match.group():doc.optString("first_publish_year",""));out.put("isbn",first(e,"isbn_13").isEmpty()?first(e,"isbn_10"):first(e,"isbn_13"));
        out.put("pages",e.optString("number_of_pages",""));out.put("description",description(e));
        JSONArray langs=e.optJSONArray("languages");if(langs!=null&&langs.length()>0)out.put("language",langs.getJSONObject(0).optString("key","").replace("/languages/",""));
        String work=doc.optString("key","");if(out.optString("description").isEmpty()&&work.matches("/works/OL[0-9]+W"))try{out.put("description",description(json("https://openlibrary.org"+work+".json")));}catch(Exception ignored){}
        JSONArray covers=e.optJSONArray("covers");if(covers!=null&&covers.length()>0&&covers.optLong(0)>0)try{
            out.put("cover",Base64.encodeToString(fetch("https://covers.openlibrary.org/b/id/"+covers.getLong(0)+"-M.jpg",1024*1024),Base64.NO_WRAP));
        }catch(Exception ignored){}return out;
    }
}
