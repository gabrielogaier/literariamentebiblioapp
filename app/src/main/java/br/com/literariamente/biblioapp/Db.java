package br.com.literariamente.biblioapp;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

/** Single local database. Returned loans are never removed during normal use. */
public final class Db extends SQLiteOpenHelper {
    static final String[] LEGACY_BOOK_FIELDS={"title","author","subtitle","publisher","year","isbn","pages","description","language","cover"};
    public Db(Context c){this(c,"library.db");}
    Db(Context c,String filename){super(c,filename,null,2);}
    @Override public void onConfigure(SQLiteDatabase d){d.setForeignKeyConstraintsEnabled(true);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE books(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL CHECK(length(trim(title))>0),author TEXT NOT NULL CHECK(length(trim(author))>0),subtitle TEXT NOT NULL DEFAULT '',publisher TEXT NOT NULL DEFAULT '',year TEXT NOT NULL DEFAULT '',isbn TEXT NOT NULL DEFAULT '',pages TEXT NOT NULL DEFAULT '',description TEXT NOT NULL DEFAULT '',language TEXT NOT NULL DEFAULT '',cover TEXT NOT NULL DEFAULT '',rating INTEGER NOT NULL DEFAULT 0 CHECK(rating BETWEEN 0 AND 5))");
        d.execSQL("CREATE TABLE people(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL CHECK(length(trim(name))>0))");
        d.execSQL("CREATE TABLE loans(id INTEGER PRIMARY KEY AUTOINCREMENT,book_id INTEGER NOT NULL REFERENCES books(id),person_id INTEGER NOT NULL REFERENCES people(id),borrowed_at INTEGER NOT NULL CHECK(borrowed_at>0),returned_at INTEGER CHECK(returned_at IS NULL OR returned_at>=borrowed_at))");
        d.execSQL("CREATE UNIQUE INDEX one_active_loan ON loans(book_id) WHERE returned_at IS NULL");
        d.execSQL("CREATE INDEX person_loans ON loans(person_id,returned_at)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int old,int next){
        if(old<2)d.execSQL("ALTER TABLE books ADD COLUMN rating INTEGER NOT NULL DEFAULT 0 CHECK(rating BETWEEN 0 AND 5)");
    }
    public List<JSONObject> rows(String sql,String... args){
        List<JSONObject> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(sql,args)){
            while(c.moveToNext()){
                JSONObject o=new JSONObject();
                for(int i=0;i<c.getColumnCount();i++)try{
                    if(c.isNull(i))o.put(c.getColumnName(i),JSONObject.NULL);
                    else if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER)o.put(c.getColumnName(i),c.getLong(i));
                    else o.put(c.getColumnName(i),c.getString(i));
                }catch(JSONException e){throw new IllegalStateException(e);}
                result.add(o);
            }
        }return result;
    }
    public int count(String sql){try(Cursor c=getReadableDatabase().rawQuery(sql,null)){c.moveToFirst();return c.getInt(0);}}
    public long saveBook(long id,JSONObject book){
        ContentValues v=new ContentValues();
        v.put("title",book.optString("title","").trim());
        v.put("author",book.optString("author","").trim());
        v.put("year",book.optString("year","").trim());
        v.put("cover",book.optString("cover",""));
        int rating=book.optInt("rating",0);if(rating<0||rating>5)rating=0;v.put("rating",rating);
        if(v.getAsString("title").isEmpty()||v.getAsString("author").isEmpty())throw new IllegalArgumentException("Informe título e autor.");
        if(id==0)return getWritableDatabase().insertOrThrow("books",null,v);
        if(getWritableDatabase().update("books",v,"id=?",new String[]{""+id})!=1)throw new IllegalArgumentException("Livro não encontrado.");return id;
    }
    public long savePerson(String name){
        name=name.trim();if(name.isEmpty())throw new IllegalArgumentException("Informe o nome da pessoa.");
        ContentValues v=new ContentValues();v.put("name",name);return getWritableDatabase().insertOrThrow("people",null,v);
    }
    public void lend(long book,long person,String newName){lendMany(Collections.singletonList(book),person,newName);}
    public void lendMany(List<Long> books,long person,String newName){
        if(books==null||books.isEmpty())throw new IllegalArgumentException("Selecione ao menos um livro.");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            if(person==0){
                String name=newName==null?"":newName.trim();
                if(name.isEmpty())throw new IllegalArgumentException("Informe o nome da pessoa.");
                ContentValues pv=new ContentValues();pv.put("name",name);person=d.insertOrThrow("people",null,pv);
            }
            long now=System.currentTimeMillis();
            for(Long book:books){
                if(book==null||book<=0)throw new IllegalArgumentException("Livro inválido.");
                ContentValues v=new ContentValues();v.put("book_id",book);v.put("person_id",person);v.put("borrowed_at",now);d.insertOrThrow("loans",null,v);
            }
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    public void giveBack(long loan){
        ContentValues v=new ContentValues();v.put("returned_at",System.currentTimeMillis());
        if(getWritableDatabase().update("loans",v,"id=? AND returned_at IS NULL",new String[]{""+loan})!=1)throw new IllegalArgumentException("Este empréstimo já foi devolvido.");
    }
    public JSONObject backup() throws JSONException {
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
        JSONObject root=new JSONObject();root.put("format","literariamente-biblioapp");root.put("version",2);root.put("createdAt",System.currentTimeMillis());
        for(String t:new String[]{"books","people","loans"})root.put(t,new JSONArray(rows("SELECT * FROM "+t+" ORDER BY id")));d.setTransactionSuccessful();return root;
        }finally{d.endTransaction();}
    }
    /** Validate before deleting; the SQLite transaction rolls back every change on error. */
    public void restore(JSONObject root) throws JSONException {
        int backupVersion=root.optInt("version");
        if(!"literariamente-biblioapp".equals(root.optString("format"))||(backupVersion!=1&&backupVersion!=2))throw new IllegalArgumentException("Formato de backup não reconhecido.");
        JSONArray books=root.getJSONArray("books"),people=root.getJSONArray("people"),loans=root.getJSONArray("loans");
        if(books.length()>100000||people.length()>100000||loans.length()>500000)throw new IllegalArgumentException("Backup excede o limite de registros.");
        Set<Long> bids=new HashSet<>(),pids=new HashSet<>(),lids=new HashSet<>(),active=new HashSet<>();
        for(int i=0;i<books.length();i++){
            JSONObject o=books.getJSONObject(i);validId(o,bids);
            for(String f:LEGACY_BOOK_FIELDS)if(o.has(f)&&!(o.get(f) instanceof String))throw new IllegalArgumentException("Campo inválido: "+f);
            int rating=o.optInt("rating",0);if(rating<0||rating>5)throw new IllegalArgumentException("Avaliação inválida.");
            if(o.getString("title").trim().isEmpty()||o.getString("author").trim().isEmpty())throw new IllegalArgumentException("Livro sem título ou autor.");
        }
        for(int i=0;i<people.length();i++){JSONObject o=people.getJSONObject(i);validId(o,pids);if(!(o.get("name") instanceof String)||o.getString("name").trim().isEmpty())throw new IllegalArgumentException("Pessoa sem nome.");}
        for(int i=0;i<loans.length();i++){
            JSONObject o=loans.getJSONObject(i);validId(o,lids);long b=integer(o,"book_id"),p=integer(o,"person_id"),start=integer(o,"borrowed_at");
            if(!bids.contains(b)||!pids.contains(p)||start<=0)throw new IllegalArgumentException("Referência de empréstimo inválida.");
            if(!o.has("returned_at"))throw new IllegalArgumentException("Empréstimo incompleto.");
            if(o.isNull("returned_at")){if(!active.add(b))throw new IllegalArgumentException("Dois empréstimos ativos para o mesmo livro.");}
            else if(integer(o,"returned_at")<start)throw new IllegalArgumentException("Data de devolução inválida.");
        }
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            d.delete("loans",null,null);d.delete("people",null,null);d.delete("books",null,null);
            for(int i=0;i<books.length();i++){JSONObject o=books.getJSONObject(i);ContentValues v=new ContentValues();v.put("id",o.getLong("id"));for(String f:LEGACY_BOOK_FIELDS)if(o.has(f))v.put(f,o.optString(f,""));v.put("rating",o.optInt("rating",0));d.insertOrThrow("books",null,v);}
            for(int i=0;i<people.length();i++){JSONObject o=people.getJSONObject(i);ContentValues v=new ContentValues();v.put("id",o.getLong("id"));v.put("name",o.getString("name"));d.insertOrThrow("people",null,v);}
            for(int i=0;i<loans.length();i++){JSONObject o=loans.getJSONObject(i);ContentValues v=new ContentValues();for(String f:new String[]{"id","book_id","person_id","borrowed_at"})v.put(f,o.getLong(f));if(o.isNull("returned_at"))v.putNull("returned_at");else v.put("returned_at",o.getLong("returned_at"));d.insertOrThrow("loans",null,v);}
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    private static long integer(JSONObject o,String key)throws JSONException {Object n=o.get(key);if(!(n instanceof Integer)&&!(n instanceof Long))throw new IllegalArgumentException("Número inválido: "+key);return ((Number)n).longValue();}
    private static void validId(JSONObject o,Set<Long> ids)throws JSONException {long id=integer(o,"id");if(id<=0||!ids.add(id))throw new IllegalArgumentException("Identificador inválido ou repetido.");}
}
