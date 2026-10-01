package br.com.literariamente.biblioapp;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private Db db;
    private LinearLayout body,root;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,EditText> fields=new LinkedHashMap<>();
    private RatingBar ratingBar;
    private String page="home";
    private long selected=0,editing=0;
    private JSONObject initial=new JSONObject();
    private int generation=0;
    private final int green=Color.rgb(35,100,91),ink=Color.rgb(34,48,44),paper=Color.rgb(245,242,235);
    private static final String[] BOOK_FIELDS={"title","author","year"};
    private static final String[] LABELS={"Título *","Autor *","Ano de lançamento"};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);db=new Db(this);
        if(state!=null){page=state.getString("page","home");selected=state.getLong("selected");editing=state.getLong("editing");}
        if(page.equals("edit")){
            JSONObject draft=null;try{if(state!=null&&state.getBoolean("hasDraft")){try(FileInputStream in=openFileInput("editor-draft.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);draft=new JSONObject(out.toString("UTF-8"));}}}catch(Exception ignored){}
            edit(editing,draft);
        }else if(page.equals("book"))book(selected);else if(page.equals("person"))person(selected);else if(page.equals("people"))people();else if(page.equals("history"))history();else if(page.equals("books"))books();else if(page.equals("lend"))lend(selected);else home();
    }
    @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);s.putString("page",page);s.putLong("selected",selected);s.putLong("editing",editing);if(page.equals("edit")){try{JSONObject d=draft();d.put("_initial",initial);try(FileOutputStream out=openFileOutput("editor-draft.json",MODE_PRIVATE)){out.write(d.toString().getBytes("UTF-8"));}s.putBoolean("hasDraft",true);}catch(Exception ignored){}}}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void layout(String title,String current){
        generation++;page=current;fields.clear();ratingBar=null;
        root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(paper);
        root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(root);root.requestApplyInsets();
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(16),dp(12),dp(16),dp(4));header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
        if(!current.equals("home")&&!current.equals("books")&&!current.equals("lend"))button(header,"‹",this::onBackPressed);
        TextView heading=text(header,title,24);heading.setTypeface(null,Typeface.BOLD);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(12),dp(20),dp(24));scroll.addView(body);
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(8),dp(6),dp(8),dp(6));root.addView(nav);
        navButton(nav,"Início",()->home());navButton(nav,"Livros",()->books());navButton(nav,"Emprestar",()->lend(0));
    }
    private void navButton(LinearLayout parent,String title,Runnable action){Button b=button(parent,title,()->leave(action));b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(54),1));}
    private TextView text(LinearLayout parent,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(5),0,dp(7));parent.addView(t);return t;}
    private Button button(LinearLayout parent,String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(green);b.setMinHeight(dp(50));parent.addView(b);b.setOnClickListener(v->action.run());return b;}
    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(16),dp(12),dp(16),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(14));c.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(10));parent.addView(c,lp);return c;}
    private EditText input(LinearLayout parent,String label,String value){text(parent,label,14);EditText e=new EditText(this);e.setText(value);e.setTextSize(18);e.setSingleLine(true);e.setMinHeight(dp(52));parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void confirm(String title,String message,Runnable action){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->action.run()).show();}
    private void safe(Runnable action){try{action.run();}catch(Exception e){toast("Não foi possível concluir. "+(e.getMessage()==null?"Tente novamente.":e.getMessage()));}}
    private boolean dirty(){return page.equals("edit")&&!draft().toString().equals(initial.toString());}
    private void leave(Runnable action){if(dirty())confirm("Descartar alterações?","As alterações deste cadastro ainda não foram salvas.",action);else action.run();}
    @Override public void onBackPressed(){leave(()->{if(page.equals("edit")&&editing>0)book(editing);else if(page.equals("book")||page.equals("edit"))books();else if(page.equals("person"))people();else if(!page.equals("home"))home();else super.onBackPressed();});}
    private String date(long time){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("pt","BR")).format(new Date(time));}
    private void home(){
        layout("Literariamente","home");text(body,"Sua biblioteca, sempre por perto.",16);
        int total=db.count("SELECT COUNT(*) FROM books"),out=db.count("SELECT COUNT(*) FROM loans WHERE returned_at IS NULL");
        LinearLayout stats=card(body);text(stats,"Livros: "+total+"\nEmprestados: "+out+"\nDisponíveis: "+(total-out),22);
        button(body,"Cadastrar livro",()->edit(0,null));text(body,"Com quem estão",21);
        List<JSONObject> people=db.rows("SELECT p.id,p.name,COUNT(*) AS total FROM people p JOIN loans l ON l.person_id=p.id WHERE l.returned_at IS NULL GROUP BY p.id ORDER BY p.name COLLATE NOCASE");
        if(people.isEmpty())text(body,"Nenhum livro emprestado no momento.",16);
        for(JSONObject p:people){LinearLayout c=card(body);text(c,p.optString("name"),20);text(c,p.optInt("total")+" livro(s)",16);button(c,"Ver livros",()->person(p.optLong("id")));}
        button(body,"Pessoas",this::people);button(body,"Histórico de empréstimos",this::history);
        LinearLayout backup=card(body);text(backup,"Proteja sua biblioteca",19);text(backup,"Salve uma cópia antes de trocar ou formatar o celular.",15);button(backup,"Exportar backup",this::exportBackup);button(backup,"Restaurar backup",this::importBackup);
    }
    private String stars(int rating){if(rating<=0)return "Sem avaliação";StringBuilder s=new StringBuilder();for(int i=1;i<=5;i++)s.append(i<=rating?"★":"☆");return s.toString();}
    private void books(){
        layout("Meus livros","books");button(body,"Cadastrar livro",()->edit(0,null));
        EditText q=input(body,"Pesquisar por título ou autor","");
        text(body,"Filtrar avaliação",14);
        Spinner ratingFilter=new Spinner(this);
        String[] filters={"Todos","Melhores avaliados","5 estrelas","4 estrelas ou mais","Não avaliados"};
        ratingFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,filters));
        body.addView(ratingFilter,new LinearLayout.LayoutParams(-1,dp(58)));
        LinearLayout list=new LinearLayout(this);list.setOrientation(1);body.addView(list);
        Runnable render=()->{
            list.removeAllViews();String term=q.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<JSONObject> data=db.rows("SELECT b.*,p.name AS holder FROM books b LEFT JOIN loans l ON l.book_id=b.id AND l.returned_at IS NULL LEFT JOIN people p ON p.id=l.person_id ORDER BY b.title COLLATE NOCASE");
            ArrayList<JSONObject> shown=new ArrayList<>();
            int mode=ratingFilter.getSelectedItemPosition();
            for(JSONObject b:data){
                String title=b.optString("title").toLowerCase(Locale.ROOT),author=b.optString("author").toLowerCase(Locale.ROOT);
                if(!term.isEmpty()&&!title.contains(term)&&!author.contains(term))continue;
                int rating=b.optInt("rating",0);
                if(mode==2&&rating!=5)continue;
                if(mode==3&&rating<4)continue;
                if(mode==4&&rating!=0)continue;
                shown.add(b);
            }
            if(mode==1)Collections.sort(shown,(a,b)->Integer.compare(b.optInt("rating",0),a.optInt("rating",0)));
            if(shown.isEmpty()){text(list,"Nenhum livro encontrado.",16);return;}
            for(JSONObject b:shown){
                LinearLayout c=card(list);text(c,b.optString("title"),20);text(c,b.optString("author"),16);
                String year=b.optString("year");if(!year.isEmpty())text(c,year,14);
                text(c,stars(b.optInt("rating",0)),18);
                text(c,b.isNull("holder")?"Disponível":"Com "+b.optString("holder"),15);
                button(c,"Abrir",()->book(b.optLong("id")));
            }
        };
        q.addTextChangedListener(watcher(render));
        ratingFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){render.run();}});
        render.run();
    }
    private TextWatcher watcher(Runnable action){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){action.run();}public void afterTextChanged(Editable e){}};}
    private void cover(LinearLayout parent,String encoded){if(encoded.isEmpty())return;try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);opt.inSampleSize=Math.max(1,Math.max(opt.outWidth,opt.outHeight)/600);opt.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(bitmap!=null){ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setContentDescription("Capa do livro");image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);parent.addView(image,new LinearLayout.LayoutParams(-1,dp(200)));}}catch(Exception ignored){}}
    private void book(long id){
        List<JSONObject> found=db.rows("SELECT * FROM books WHERE id=?",""+id);if(found.isEmpty()){books();return;}JSONObject b=found.get(0);selected=id;
        layout("Detalhes do livro","book");text(body,b.optString("title"),25);text(body,b.optString("author"),19);
        if(!b.optString("year").isEmpty())text(body,"Ano: "+b.optString("year"),16);
        text(body,stars(b.optInt("rating",0)),22);
        List<JSONObject> active=db.rows("SELECT l.id,p.name FROM loans l JOIN people p ON p.id=l.person_id WHERE l.book_id=? AND l.returned_at IS NULL",""+id);
        if(active.isEmpty()){text(body,"Disponível",18);button(body,"Emprestar este livro",()->lend(id));}
        else{JSONObject l=active.get(0);text(body,"Com "+l.optString("name"),19);button(body,"Devolver",()->confirm("Registrar devolução?",b.optString("title"),()->safe(()->{db.giveBack(l.optLong("id"));book(id);toast("Devolução registrada.");})));}
        button(body,"Editar livro e avaliação",()->edit(id,null));
        text(body,"Histórico deste livro",21);showHistory(body,db.rows("SELECT l.*,b.title,p.name FROM loans l JOIN books b ON b.id=l.book_id JOIN people p ON p.id=l.person_id WHERE b.id=? ORDER BY l.borrowed_at DESC",""+id));
    }
    private JSONObject draft(){
        JSONObject o=new JSONObject();
        try{
            for(String f:BOOK_FIELDS)o.put(f,fields.containsKey(f)?fields.get(f).getText().toString().trim():"");
            o.put("rating",ratingBar==null?0:Math.round(ratingBar.getRating()));
        }catch(Exception ignored){}
        return o;
    }
    private void edit(long id,JSONObject provided){
        JSONObject b=provided;if(b==null&&id>0){List<JSONObject> found=db.rows("SELECT * FROM books WHERE id=?",""+id);if(!found.isEmpty())b=found.get(0);}if(b==null)b=new JSONObject();
        editing=id;layout(id==0?"Cadastrar livro":"Editar livro","edit");
        text(body,"Título e autor são obrigatórios. A avaliação é sua e pode ser alterada depois.",16);
        for(int i=0;i<BOOK_FIELDS.length;i++){
            EditText e=input(body,LABELS[i],b.optString(BOOK_FIELDS[i],""));fields.put(BOOK_FIELDS[i],e);
            if(BOOK_FIELDS[i].equals("year"))e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        }
        text(body,"Sua avaliação",14);
        ratingBar=new RatingBar(this,null,android.R.attr.ratingBarStyle);
        ratingBar.setNumStars(5);ratingBar.setStepSize(1f);ratingBar.setRating(b.optInt("rating",0));ratingBar.setIsIndicator(false);
        body.addView(ratingBar,new LinearLayout.LayoutParams(-2,-2));
        button(body,"Limpar avaliação",()->ratingBar.setRating(0));
        initial=b.optJSONObject("_initial")==null?draft():b.optJSONObject("_initial");
        button(body,"Buscar informações online",this::search);
        button(body,"Salvar livro",()->{
            if(fields.get("title").getText().toString().trim().isEmpty()){fields.get("title").setError("Informe o título");return;}
            if(fields.get("author").getText().toString().trim().isEmpty()){fields.get("author").setError("Informe o autor");return;}
            safe(()->{long saved=db.saveBook(id,draft());book(saved);toast("Livro salvo.");});
        });
        button(body,"Cancelar",()->leave(()->{if(id>0)book(id);else books();}));
    }
    private void search(){
        JSONObject d=draft();String t=d.optString("title"),a=d.optString("author");
        if(t.isEmpty()&&a.isEmpty()){toast("Informe título ou autor para pesquisar.");return;}
        int token=generation;ProgressDialog progress=ProgressDialog.show(this,"Consultando Open Library","Você poderá salvar manualmente mesmo sem resultado.",true,true);
        worker.execute(()->{try{JSONArray results=BookSearch.search(t,a);runOnUiThread(()->{
            if(isFinishing()||isDestroyed())return;boolean canceled=!progress.isShowing();progress.dismiss();if(canceled||generation!=token)return;
            if(results.length()==0){toast("Nenhum resultado. Continue o cadastro manual.");return;}
            String[] labels=new String[results.length()];
            for(int i=0;i<labels.length;i++){
                JSONObject r=results.optJSONObject(i);
                String meta=BookSearch.first(r,"author_name");
                String year=BookSearch.resultYear(r),lang=BookSearch.resultLanguage(r);
                if(!year.isEmpty())meta+=(meta.isEmpty()?"":" · ")+year;
                if(!lang.isEmpty())meta+=(meta.isEmpty()?"":" · ")+lang;
                labels[i]=BookSearch.resultTitle(r)+"\n"+meta;
            }
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Escolha o livro").setItems(labels,(dialogInterface,index)->loadDetails(results.optJSONObject(index),token)).setNegativeButton("Cancelar",null).create();
            dialog.setOnShowListener(v->{ListView list=dialog.getListView();list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(220,220,220)));list.setDividerHeight(dp(1));});
            dialog.show();
        });}catch(Exception e){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;progress.dismiss();if(generation==token)toast("Não foi possível consultar. Você pode salvar normalmente com título e autor.");});}});
    }
    private void loadDetails(JSONObject result,int token){
        ProgressDialog progress=ProgressDialog.show(this,"Carregando informações","Buscando título, autor e ano...",true,true);
        worker.execute(()->{try{JSONObject info=BookSearch.details(result);runOnUiThread(()->{
            if(isFinishing()||isDestroyed())return;boolean canceled=!progress.isShowing();progress.dismiss();if(canceled||generation!=token)return;
            confirm("Usar estas informações?","Título, autor e ano encontrados serão preenchidos. Sua avaliação não será alterada.",()->{
                for(String f:BOOK_FIELDS)if(!info.optString(f).isEmpty())fields.get(f).setText(info.optString(f));
                toast("Informações preenchidas. Revise e toque em Salvar.");
            });
        });}catch(Exception e){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;progress.dismiss();if(generation==token)toast("Não foi possível carregar os detalhes. Continue manualmente.");});}});
    }
    private void lend(long preselected){
        selected=preselected;layout("Emprestar","lend");
        List<JSONObject> available=db.rows("SELECT b.id,b.title,b.author FROM books b WHERE NOT EXISTS(SELECT 1 FROM loans l WHERE l.book_id=b.id AND l.returned_at IS NULL) ORDER BY b.title COLLATE NOCASE");
        if(available.isEmpty()){text(body,"Não há livros disponíveis para emprestar.",18);button(body,"Cadastrar livro",()->edit(0,null));return;}

        EditText bookFilter=input(body,"Filtrar livros por título, autor ou ID","");
        TextView selectedCount=text(body,"Nenhum livro selecionado",16);
        ScrollView bookScroll=new ScrollView(this);
        LinearLayout.LayoutParams bookScrollParams=new LinearLayout.LayoutParams(-1,dp(300));
        bookScrollParams.setMargins(0,dp(4),0,dp(10));
        body.addView(bookScroll,bookScrollParams);
        LinearLayout bookList=new LinearLayout(this);bookList.setOrientation(1);bookScroll.addView(bookList,new ScrollView.LayoutParams(-1,-2));
        Set<Long> selectedBooks=new LinkedHashSet<>();
        if(preselected>0)selectedBooks.add(preselected);

        Runnable renderBooks=()->{
            bookList.removeAllViews();
            String term=bookFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            int shown=0;
            for(JSONObject b:available){
                String title=b.optString("title").toLowerCase(Locale.ROOT);
                String author=b.optString("author").toLowerCase(Locale.ROOT);
                String id=String.valueOf(b.optLong("id"));
                if(!term.isEmpty()&&!title.contains(term)&&!author.contains(term)&&!id.contains(term))continue;
                shown++;
                CheckBox check=new CheckBox(this);
                long bookId=b.optLong("id");
                check.setText(b.optString("title")+" · "+b.optString("author")+" (#"+bookId+")");
                check.setTextSize(16);check.setPadding(0,dp(4),0,dp(4));
                check.setChecked(selectedBooks.contains(bookId));
                check.setOnCheckedChangeListener((button,isChecked)->{
                    if(isChecked)selectedBooks.add(bookId);else selectedBooks.remove(bookId);
                    selectedCount.setText(selectedBooks.isEmpty()?"Nenhum livro selecionado":selectedBooks.size()+" livro(s) selecionado(s)");
                });
                bookList.addView(check,new LinearLayout.LayoutParams(-1,-2));
            }
            if(shown==0)text(bookList,"Nenhum livro encontrado.",15);
            selectedCount.setText(selectedBooks.isEmpty()?"Nenhum livro selecionado":selectedBooks.size()+" livro(s) selecionado(s)");
        };
        bookFilter.addTextChangedListener(watcher(renderBooks));renderBooks.run();

        List<JSONObject> ps=db.rows("SELECT * FROM people ORDER BY name COLLATE NOCASE");
        EditText personFilter=input(body,"Filtrar pessoa por nome ou ID","");
        text(body,"Quem ficará com os livros?",16);
        Spinner sp=new Spinner(this);
        ArrayList<JSONObject> filteredPeople=new ArrayList<>();
        ArrayList<String> personLabels=new ArrayList<>();
        personLabels.add("Cadastrar nova pessoa");
        for(JSONObject p:ps){filteredPeople.add(p);personLabels.add(p.optString("name")+" (#"+p.optLong("id")+")");}
        ArrayAdapter<String> personAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,personLabels);
        sp.setAdapter(personAdapter);body.addView(sp,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView personCount=text(body,ps.size()+" pessoa(s) cadastrada(s)",14);
        EditText name=input(body,"Nome da nova pessoa","");
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){name.setVisibility(position==0?View.VISIBLE:View.GONE);}});

        personFilter.addTextChangedListener(watcher(()->{
            String term=personFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            filteredPeople.clear();personLabels.clear();personLabels.add("Cadastrar nova pessoa");
            for(JSONObject p:ps){
                String personName=p.optString("name").toLowerCase(Locale.ROOT);
                String id=String.valueOf(p.optLong("id"));
                if(term.isEmpty()||personName.contains(term)||id.contains(term)){
                    filteredPeople.add(p);personLabels.add(p.optString("name")+" (#"+p.optLong("id")+")");
                }
            }
            personAdapter.notifyDataSetChanged();
            personCount.setText(filteredPeople.size()+" pessoa(s) encontrada(s)");
            sp.setSelection(0);
        }));

        button(body,"Confirmar empréstimo",()->{
            if(selectedBooks.isEmpty()){toast("Selecione ao menos um livro.");return;}
            int pos=sp.getSelectedItemPosition();String n=name.getText().toString().trim();
            if(pos==0&&n.isEmpty()){name.setError("Informe o nome");return;}
            JSONObject selectedPerson=pos==0?null:filteredPeople.get(pos-1);
            long person=selectedPerson==null?0:selectedPerson.optLong("id");
            String personName=selectedPerson==null?n:selectedPerson.optString("name");
            ArrayList<Long> booksToLend=new ArrayList<>(selectedBooks);
            String message=booksToLend.size()+" livro(s) para "+personName;
            Runnable commit=()->safe(()->{db.lendMany(booksToLend,person,n);home();toast(booksToLend.size()+" empréstimo(s) registrado(s).");});
            if(pos==0){
                List<JSONObject> matches=db.rows("SELECT * FROM people WHERE lower(name)=lower(?)",n);
                if(!matches.isEmpty()){
                    long existingPerson=matches.get(0).optLong("id");
                    new AlertDialog.Builder(this).setTitle("Já existe uma pessoa com esse nome").setMessage("Usar o cadastro existente?").setNegativeButton("Cancelar",null).setPositiveButton("Usar existente",(dlg,w)->confirm("Confirmar empréstimo?",booksToLend.size()+" livro(s) para "+n,()->safe(()->{db.lendMany(booksToLend,existingPerson,"");home();toast(booksToLend.size()+" empréstimo(s) registrado(s).");}))).show();
                    return;
                }
            }
            confirm("Confirmar empréstimo?",message,commit);
        });
    }

    private void people(){
        layout("Pessoas","people");List<JSONObject> ps=db.rows("SELECT p.id,p.name,COUNT(l.id) AS total FROM people p LEFT JOIN loans l ON l.person_id=p.id AND l.returned_at IS NULL GROUP BY p.id ORDER BY p.name COLLATE NOCASE");
        if(ps.isEmpty()){text(body,"As pessoas são cadastradas durante o empréstimo.",17);return;}
        EditText filter=input(body,"Buscar pessoa por nome ou ID","");
        TextView count=text(body,ps.size()+" pessoa(s)",14);
        LinearLayout list=new LinearLayout(this);list.setOrientation(1);body.addView(list);
        Runnable render=()->{
            list.removeAllViews();String term=filter.getText().toString().trim().toLowerCase(Locale.ROOT);int found=0;
            for(JSONObject p:ps){
                String personName=p.optString("name").toLowerCase(Locale.ROOT),id=String.valueOf(p.optLong("id"));
                if(!term.isEmpty()&&!personName.contains(term)&&!id.contains(term))continue;
                found++;LinearLayout c=card(list);text(c,p.optString("name"),21);text(c,p.optInt("total")+" livro(s) no momento",16);button(c,"Ver livros e histórico",()->person(p.optLong("id")));
            }
            count.setText(found==0?"Nenhuma pessoa encontrada":found+" pessoa(s) encontrada(s)");
        };
        filter.addTextChangedListener(watcher(render));render.run();
    }
    private void person(long id){
        List<JSONObject> found=db.rows("SELECT * FROM people WHERE id=?",""+id);if(found.isEmpty()){people();return;}selected=id;layout(found.get(0).optString("name"),"person");
        List<JSONObject> active=db.rows("SELECT l.id,b.title,b.author FROM loans l JOIN books b ON b.id=l.book_id WHERE l.person_id=? AND l.returned_at IS NULL ORDER BY b.title",""+id);
        text(body,active.size()+" livro(s) emprestado(s)",20);
        for(JSONObject l:active){LinearLayout c=card(body);text(c,l.optString("title"),21);text(c,l.optString("author"),16);button(c,"Devolver",()->confirm("Registrar devolução?",l.optString("title"),()->safe(()->{db.giveBack(l.optLong("id"));person(id);toast("Devolução registrada.");})));}
        text(body,"Histórico",21);showHistory(body,db.rows("SELECT l.*,b.title,p.name FROM loans l JOIN books b ON b.id=l.book_id JOIN people p ON p.id=l.person_id WHERE p.id=? ORDER BY l.borrowed_at DESC",""+id));
    }
    private void history(){
        layout("Histórico","history");
        List<JSONObject> loans=db.rows("SELECT l.*,b.title,p.name FROM loans l JOIN books b ON b.id=l.book_id JOIN people p ON p.id=l.person_id ORDER BY l.borrowed_at DESC");
        if(loans.isEmpty()){showHistory(body,loans);return;}
        EditText filter=input(body,"Buscar por livro ou pessoa","");
        TextView count=text(body,loans.size()+" registro(s)",14);
        LinearLayout list=new LinearLayout(this);list.setOrientation(1);body.addView(list);
        Runnable render=()->{
            list.removeAllViews();String term=filter.getText().toString().trim().toLowerCase(Locale.ROOT);ArrayList<JSONObject> filteredLoans=new ArrayList<>();
            for(JSONObject l:loans){
                String title=l.optString("title").toLowerCase(Locale.ROOT),person=l.optString("name").toLowerCase(Locale.ROOT);
                if(term.isEmpty()||title.contains(term)||person.contains(term))filteredLoans.add(l);
            }
            count.setText(filteredLoans.isEmpty()?"Nenhum registro encontrado":filteredLoans.size()+" registro(s) encontrado(s)");
            showHistory(list,filteredLoans);
        };
        filter.addTextChangedListener(watcher(render));render.run();
    }
    private void showHistory(LinearLayout parent,List<JSONObject> loans){if(loans.isEmpty())text(parent,"Nenhum empréstimo registrado.",16);for(JSONObject l:loans){LinearLayout c=card(parent);text(c,l.optString("title"),19);text(c,"Pessoa: "+l.optString("name")+"\nEmpréstimo: "+date(l.optLong("borrowed_at"))+"\n"+(l.isNull("returned_at")?"Em andamento":"Devolução: "+date(l.optLong("returned_at"))),15);}}
    private void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"literariamente-backup-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".json");safe(()->startActivityForResult(i,41));}
    private void importBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");safe(()->startActivityForResult(i,42));}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
        if(request==41)worker.execute(()->{try{byte[] bytes=db.backup().toString(2).getBytes("UTF-8");if(bytes.length>50*1024*1024)throw new IOException("Backup excede 50 MB.");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Arquivo indisponível");out.write(bytes);}runOnUiThread(()->{if(!isDestroyed())toast("Backup exportado. Guarde uma cópia fora do celular.");});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed())toast("Falha ao exportar o backup. Tente outro local.");});}});
        if(request==42)worker.execute(()->{try{JSONObject backup;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>50*1024*1024)throw new IOException("Backup maior que 50 MB.");out.write(b,0,n);}backup=new JSONObject(out.toString("UTF-8"));}
            runOnUiThread(()->{if(isDestroyed()||isFinishing())return;confirm("Substituir a biblioteca?","A restauração substitui livros, pessoas e empréstimos atuais. Exporte um backup antes de continuar.",()->{
                ProgressDialog progress=ProgressDialog.show(this,"Restaurando","Validando o backup...",true,false);
                worker.execute(()->{try{db.restore(backup);runOnUiThread(()->{if(isDestroyed())return;progress.dismiss();home();toast("Backup restaurado.");});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;progress.dismiss();toast("Backup inválido. Seus dados foram preservados.");});}});
            });});
        }catch(Exception e){runOnUiThread(()->{if(!isDestroyed())toast("Não foi possível ler esse backup. Seus dados foram preservados.");});}});
    }
}
