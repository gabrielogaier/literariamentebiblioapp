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
    private String pendingCover="";
    private String page="home";
    private long selected=0,editing=0;
    private JSONObject initial=new JSONObject();
    private int generation=0;
    private final int wine=Color.rgb(142,47,60),wineDark=Color.rgb(102,35,45),gold=Color.rgb(211,151,45),ink=Color.rgb(66,45,43),paper=Color.rgb(250,244,235),cardPaper=Color.rgb(255,251,246),blush=Color.rgb(248,229,224),line=Color.rgb(229,208,199),green=Color.rgb(73,126,84);
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
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(14),dp(8),dp(18),dp(6));header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header,new LinearLayout.LayoutParams(-1,dp(current.equals("home")?64:58)));
        if(!current.equals("home")&&!current.equals("books")&&!current.equals("lend")){
            TextView back=new TextView(this);back.setText("‹");back.setTextSize(36);back.setTextColor(wineDark);back.setGravity(Gravity.CENTER);back.setPadding(0,0,dp(10),0);
            header.addView(back,new LinearLayout.LayoutParams(dp(46),-1));back.setOnClickListener(v->onBackPressed());
        }
        TextView heading=text(header,title,current.equals("home")?30:26);heading.setTextColor(wineDark);heading.setTypeface(Typeface.SERIF,Typeface.BOLD);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(12),dp(20),dp(24));scroll.addView(body);
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(8),dp(6),dp(8),dp(8));nav.setBackgroundColor(cardPaper);root.addView(nav);
        navButton(nav,"⌂\nInício",()->home());navButton(nav,"▤\nLivros",()->books());navButton(nav,"⇄\nEmprestar",()->lend(0));
    }
    private void navButton(LinearLayout parent,String title,Runnable action){Button b=button(parent,title,()->leave(action));b.setTextSize(13);b.setBackgroundColor(Color.TRANSPARENT);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(58),1));}
    private TextView text(LinearLayout parent,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(5),0,dp(7));parent.addView(t);return t;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
    private Button button(LinearLayout parent,String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(wineDark);GradientDrawable bg=rounded(blush,14);bg.setStroke(dp(1),line);b.setBackground(bg);b.setMinHeight(dp(52));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(7));parent.addView(b,lp);b.setOnClickListener(v->action.run());return b;}
    private Button primaryButton(LinearLayout parent,String value,Runnable action){Button b=button(parent,value,action);b.setTextColor(Color.WHITE);b.setTypeface(null,Typeface.BOLD);b.setBackground(rounded(wine,14));return b;}
    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(16),dp(14),dp(16),dp(14));GradientDrawable bg=rounded(cardPaper,16);bg.setStroke(dp(1),line);c.setBackground(bg);c.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(10));parent.addView(c,lp);return c;}
    private EditText input(LinearLayout parent,String label,String value){TextView l=text(parent,label,14);l.setTypeface(null,Typeface.BOLD);EditText e=new EditText(this);e.setText(value);e.setTextSize(17);e.setSingleLine(true);e.setTextColor(ink);e.setHintTextColor(Color.rgb(158,137,131));e.setPadding(dp(14),0,dp(14),0);GradientDrawable bg=rounded(cardPaper,13);bg.setStroke(dp(1),line);e.setBackground(bg);e.setMinHeight(dp(54));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,0,0,dp(10));parent.addView(e,lp);return e;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void confirm(String title,String message,Runnable action){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->action.run()).show();}
    private void safe(Runnable action){try{action.run();}catch(Exception e){toast("Não foi possível concluir. "+(e.getMessage()==null?"Tente novamente.":e.getMessage()));}}
    private boolean dirty(){return page.equals("edit")&&!draft().toString().equals(initial.toString());}
    private void leave(Runnable action){if(dirty())confirm("Descartar alterações?","As alterações deste cadastro ainda não foram salvas.",action);else action.run();}
    @Override public void onBackPressed(){leave(()->{if(page.equals("edit")&&editing>0)book(editing);else if(page.equals("book")||page.equals("edit"))books();else if(page.equals("person"))people();else if(!page.equals("home"))home();else super.onBackPressed();});}
    private String date(long time){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("pt","BR")).format(new Date(time));}
    private void home(){
        layout("Literariamente","home");
        TextView sub=text(body,"Sua biblioteca pessoal",16);sub.setTextColor(wine);sub.setTypeface(Typeface.SERIF,Typeface.ITALIC);
        text(body,"“Boas histórias também vivem aqui.”",17).setTypeface(Typeface.SERIF,Typeface.ITALIC);
        int total=db.count("SELECT COUNT(*) FROM books"),out=db.count("SELECT COUNT(*) FROM loans WHERE returned_at IS NULL");
        int holders=db.count("SELECT COUNT(DISTINCT person_id) FROM loans WHERE returned_at IS NULL");
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);body.addView(stats,new LinearLayout.LayoutParams(-1,-2));
        stat(stats,"▤",String.valueOf(total),"livros");stat(stats,"⇄",String.valueOf(out),"emprestados");stat(stats,"●",String.valueOf(holders),"pessoas com livros");
        text(body,"Sua estante",21).setTypeface(Typeface.SERIF,Typeface.BOLD);
        primaryButton(body,"＋ Adicionar livro",()->edit(0,null));
        button(body,"▤  Meus livros",this::books);
        button(body,"⇄  Empréstimos",()->lend(0));
        button(body,"●  Pessoas",this::people);
        button(body,"◷  Histórico",this::history);
        List<JSONObject> people=db.rows("SELECT p.id,p.name,COUNT(*) AS total FROM people p JOIN loans l ON l.person_id=p.id WHERE l.returned_at IS NULL GROUP BY p.id ORDER BY p.name COLLATE NOCASE");
        if(!people.isEmpty()){
            text(body,"Com quem estão",21).setTypeface(Typeface.SERIF,Typeface.BOLD);
            for(JSONObject p:people){LinearLayout c=card(body);TextView n=text(c,p.optString("name"),19);n.setTypeface(Typeface.SERIF,Typeface.BOLD);text(c,p.optInt("total")+" livro(s)",15);button(c,"Ver livros",()->person(p.optLong("id")));}
        }
        LinearLayout backup=card(body);text(backup,"Proteja sua biblioteca",18).setTypeface(Typeface.SERIF,Typeface.BOLD);text(backup,"Salve uma cópia antes de trocar ou formatar o celular.",14);button(backup,"Exportar backup",this::exportBackup);button(backup,"Restaurar backup",this::importBackup);
    }
    private void stat(LinearLayout parent,String icon,String value,String label){
        LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setGravity(Gravity.CENTER);c.setPadding(dp(6),dp(10),dp(6),dp(10));GradientDrawable bg=rounded(cardPaper,15);bg.setStroke(dp(1),line);c.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(104),1);lp.setMargins(dp(3),dp(4),dp(3),dp(10));parent.addView(c,lp);
        TextView i=text(c,icon,20);i.setTextColor(wine);i.setGravity(Gravity.CENTER);
        TextView v=text(c,value,21);v.setTypeface(Typeface.SERIF,Typeface.BOLD);v.setGravity(Gravity.CENTER);
        TextView l=text(c,label,11);l.setGravity(Gravity.CENTER);
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
                LinearLayout c=card(list);c.setOrientation(LinearLayout.HORIZONTAL);
                coverThumb(c,b.optString("cover",""));
                LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);c.addView(info,new LinearLayout.LayoutParams(0,-2,1));
                TextView title=text(info,b.optString("title"),19);title.setTypeface(Typeface.SERIF,Typeface.BOLD);
                text(info,b.optString("author"),15);
                String year=b.optString("year");if(!year.isEmpty())text(info,year,13);
                TextView starText=text(info,stars(b.optInt("rating",0)),18);starText.setTextColor(gold);
                TextView status=text(info,b.isNull("holder")?"Disponível":"Com "+b.optString("holder"),14);status.setTextColor(b.isNull("holder")?green:wine);
                button(info,"Abrir",()->book(b.optLong("id")));
            }
        };
        q.addTextChangedListener(watcher(render));
        ratingFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){render.run();}});
        render.run();
    }
    private TextWatcher watcher(Runnable action){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){action.run();}public void afterTextChanged(Editable e){}};}
    private void cover(LinearLayout parent,String encoded){if(encoded.isEmpty())return;try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);opt.inSampleSize=Math.max(1,Math.max(opt.outWidth,opt.outHeight)/600);opt.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(bitmap!=null){ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setContentDescription("Capa do livro");image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(210));lp.setMargins(0,0,0,dp(12));parent.addView(image,lp);}}catch(Exception ignored){}}
    private void coverThumb(LinearLayout parent,String encoded){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(72),dp(108));lp.setMargins(0,0,dp(14),0);if(encoded==null||encoded.isEmpty()){TextView p=new TextView(this);p.setText("▤");p.setTextSize(28);p.setTextColor(wine);p.setGravity(Gravity.CENTER);p.setBackground(rounded(blush,10));parent.addView(p,lp);return;}try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap!=null){ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setContentDescription("Capa do livro");image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(rounded(blush,10));parent.addView(image,lp);return;}}catch(Exception ignored){}TextView p=new TextView(this);p.setText("▤");p.setTextSize(28);p.setTextColor(wine);p.setGravity(Gravity.CENTER);p.setBackground(rounded(blush,10));parent.addView(p,lp);}
    private void book(long id){
        List<JSONObject> found=db.rows("SELECT * FROM books WHERE id=?",""+id);if(found.isEmpty()){books();return;}JSONObject b=found.get(0);selected=id;
        layout("Detalhes do livro","book");cover(body,b.optString("cover",""));TextView bookTitle=text(body,b.optString("title"),25);bookTitle.setTypeface(Typeface.SERIF,Typeface.BOLD);text(body,b.optString("author"),19);
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
            o.put("cover",pendingCover==null?"":pendingCover);
        }catch(Exception ignored){}
        return o;
    }
    private void edit(long id,JSONObject provided){
        JSONObject b=provided;if(b==null&&id>0){List<JSONObject> found=db.rows("SELECT * FROM books WHERE id=?",""+id);if(!found.isEmpty())b=found.get(0);}if(b==null)b=new JSONObject();
        editing=id;layout(id==0?"Adicionar livro":"Editar livro","edit");
        pendingCover=b.optString("cover","");
        text(body,"Foto do seu livro",14).setTypeface(null,Typeface.BOLD);
        if(!pendingCover.isEmpty())cover(body,pendingCover);
        button(body,pendingCover.isEmpty()?"📷  Fazer foto do livro":"📷  Refazer foto do livro",this::takeBookPhoto);
        if(!pendingCover.isEmpty())button(body,"Remover foto",()->{pendingCover="";JSONObject current=draft();try{current.put("_initial",initial);}catch(Exception ignored){}edit(editing,current);});
        text(body,"Título e autor são obrigatórios. A avaliação é sua e pode ser alterada depois.",15);
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
        button(body,"⌕  Buscar informações online",this::search);
        primaryButton(body,"Salvar livro",()->{
            if(fields.get("title").getText().toString().trim().isEmpty()){fields.get("title").setError("Informe o título");return;}
            if(fields.get("author").getText().toString().trim().isEmpty()){fields.get("author").setError("Informe o autor");return;}
            safe(()->{long saved=db.saveBook(id,draft());book(saved);toast("Livro salvo.");});
        });
        button(body,"Cancelar",()->leave(()->{if(id>0)book(id);else books();}));
    }
    private void takeBookPhoto(){
        Intent camera=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(camera.resolveActivity(getPackageManager())==null){toast("Nenhum aplicativo de câmera disponível.");return;}
        try{startActivityForResult(camera,43);}catch(Exception e){toast("Não foi possível abrir a câmera.");}
    }
    private String encodePhoto(Bitmap bitmap){
        if(bitmap==null)return "";
        int max=900,w=bitmap.getWidth(),h=bitmap.getHeight();
        if(w>max||h>max){
            float scale=Math.min((float)max/w,(float)max/h);
            bitmap=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),true);
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,88,out);
        return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
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
            confirm("Usar estas informações?","Título, autor e ano encontrados serão preenchidos. Sua avaliação e a foto do livro não serão alteradas.",()->{
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
        ListView bookList=new ListView(this);
        bookList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        bookList.setNestedScrollingEnabled(true);
        bookList.setOnTouchListener((v,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE)v.getParent().requestDisallowInterceptTouchEvent(true);
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
        LinearLayout.LayoutParams bookListParams=new LinearLayout.LayoutParams(-1,dp(270));
        bookListParams.setMargins(0,dp(4),0,dp(10));
        body.addView(bookList,bookListParams);
        Set<Long> selectedBooks=new LinkedHashSet<>();
        if(preselected>0)selectedBooks.add(preselected);
        ArrayList<JSONObject> filteredBooks=new ArrayList<>();
        ArrayList<String> bookLabels=new ArrayList<>();

        Runnable renderBooks=()->{
            String term=bookFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            filteredBooks.clear();bookLabels.clear();
            for(JSONObject b:available){
                String title=b.optString("title").toLowerCase(Locale.ROOT);
                String author=b.optString("author").toLowerCase(Locale.ROOT);
                String id=String.valueOf(b.optLong("id"));
                if(!term.isEmpty()&&!title.contains(term)&&!author.contains(term)&&!id.contains(term))continue;
                filteredBooks.add(b);
                bookLabels.add(b.optString("title")+" · "+b.optString("author")+" (#"+b.optLong("id")+")");
            }
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,bookLabels){
                @Override public View getView(int position,View convertView,ViewGroup parent){
                    View v=super.getView(position,convertView,parent);
                    v.setMinimumHeight(dp(52));
                    return v;
                }
            };
            bookList.setAdapter(adapter);
            for(int i=0;i<filteredBooks.size();i++)bookList.setItemChecked(i,selectedBooks.contains(filteredBooks.get(i).optLong("id")));
            selectedCount.setText(selectedBooks.isEmpty()?"Nenhum livro selecionado":selectedBooks.size()+" livro(s) selecionado(s)");
        };
        bookList.setOnItemClickListener((parent,view,position,id)->{
            if(position<0||position>=filteredBooks.size())return;
            long bookId=filteredBooks.get(position).optLong("id");
            if(bookList.isItemChecked(position))selectedBooks.add(bookId);else selectedBooks.remove(bookId);
            selectedCount.setText(selectedBooks.isEmpty()?"Nenhum livro selecionado":selectedBooks.size()+" livro(s) selecionado(s)");
        });
        bookFilter.addTextChangedListener(watcher(renderBooks));renderBooks.run();

        List<JSONObject> ps=db.rows("SELECT * FROM people ORDER BY name COLLATE NOCASE");
        text(body,"Selecionar pessoa existente",16);
        EditText personFilter=input(body,"Filtrar pessoa por nome ou ID","");
        TextView selectedPersonLabel=text(body,"Nenhuma pessoa selecionada",14);
        ScrollView personScroll=new ScrollView(this);
        personScroll.setNestedScrollingEnabled(true);
        personScroll.setOnTouchListener((v,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE)v.getParent().requestDisallowInterceptTouchEvent(true);
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
        LinearLayout.LayoutParams personScrollParams=new LinearLayout.LayoutParams(-1,dp(270));
        personScrollParams.setMargins(0,dp(4),0,dp(10));
        body.addView(personScroll,personScrollParams);
        RadioGroup personResults=new RadioGroup(this);personResults.setOrientation(RadioGroup.VERTICAL);
        personScroll.addView(personResults,new ScrollView.LayoutParams(-1,-2));
        final long[] selectedPersonId={0};
        final String[] selectedPersonName={""};

        Runnable renderPeople=()->{
            personResults.removeAllViews();
            String term=personFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            int shown=0;
            for(JSONObject p:ps){
                String personName=p.optString("name");
                String id=String.valueOf(p.optLong("id"));
                if(!term.isEmpty()&&!personName.toLowerCase(Locale.ROOT).contains(term)&&!id.contains(term))continue;
                shown++;
                RadioButton option=new RadioButton(this);
                long personId=p.optLong("id");
                option.setText(personName+" (#"+personId+")");
                option.setTextSize(16);option.setMinHeight(dp(52));
                option.setChecked(selectedPersonId[0]==personId);
                option.setOnClickListener(v->{
                    selectedPersonId[0]=personId;
                    selectedPersonName[0]=personName;
                    selectedPersonLabel.setText("Selecionado: "+personName);
                });
                personResults.addView(option,new RadioGroup.LayoutParams(-1,-2));
            }
            if(shown==0)text(personResults,"Nenhuma pessoa encontrada.",14);
        };
        personFilter.addTextChangedListener(watcher(renderPeople));renderPeople.run();

        text(body,"Ou cadastrar nova pessoa",16);
        EditText name=input(body,"Nome da nova pessoa","");
        name.setOnFocusChangeListener((v,hasFocus)->{
            if(hasFocus&&selectedPersonId[0]>0){
                selectedPersonId[0]=0;selectedPersonName[0]="";
                personResults.clearCheck();
                selectedPersonLabel.setText("Nenhuma pessoa selecionada");
            }
        });

        primaryButton(body,"Confirmar empréstimo",()->{
            if(selectedBooks.isEmpty()){toast("Selecione ao menos um livro.");return;}
            String n=name.getText().toString().trim();
            long person=selectedPersonId[0];
            String personName=person>0?selectedPersonName[0]:n;
            if(person==0&&n.isEmpty()){name.setError("Selecione uma pessoa ou informe um novo nome");return;}
            ArrayList<Long> booksToLend=new ArrayList<>(selectedBooks);
            String message=booksToLend.size()+" livro(s) para "+personName;
            Runnable commit=()->safe(()->{db.lendMany(booksToLend,person,n);home();toast(booksToLend.size()+" empréstimo(s) registrado(s).");});
            if(person==0){
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
        super.onActivityResult(request,result,data);
        if(request==43){
            if(result!=RESULT_OK||data==null)return;
            Bundle extras=data.getExtras();Bitmap bitmap=extras==null?null:(Bitmap)extras.get("data");
            if(bitmap==null){toast("Não foi possível obter a foto.");return;}
            pendingCover=encodePhoto(bitmap);
            JSONObject current=draft();try{current.put("_initial",initial);}catch(Exception ignored){}
            edit(editing,current);toast("Foto do livro adicionada.");return;
        }
        if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
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
