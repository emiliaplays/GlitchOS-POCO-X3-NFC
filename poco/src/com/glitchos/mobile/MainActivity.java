package com.glitchos.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.Collator;
import java.util.*;

/** Touch adaptation of GlitchOS TV 1.4.0. No Internet or privileged launcher APIs. */
public final class MainActivity extends Activity {
    private static final int WHITE=0xFFF9FAFC, MUTED=0xFFB9CDD6, MINT=0xFF8BFFE0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<App> apps=new ArrayList<>();
    private final List<String> favorites=new ArrayList<>();
    private final Map<String, Drawable.ConstantState> icons=new HashMap<>();
    private SharedPreferences prefs;
    private LinearLayout body, grid;
    private MobileNowPlayingView player;
    private ScrollView scroll;
    private EditText search;
    private TextView clock, date, count;
    private String page="home", query="";
    private NowPlayingController media;
    private boolean resumed, receiverRegistered;
    private int columns=4;
    private final Runnable tick=new Runnable(){ public void run(){updateClock();handler.postDelayed(this,30000);} };
    private final BroadcastReceiver packageChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        icons.clear(); if(resumed){refreshApps();render();}
    }};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("glitchos-mobile",MODE_PRIVATE);
        String saved=prefs.getString("favorites",null);
        if(saved==null) favorites.addAll(Arrays.asList("com.android.vending","com.android.settings","com.termux","com.alphainventor.filemanager","com.android.chrome","com.miui.screenrecorder","com.android.camera","com.google.android.apps.messaging"));
        else if(!saved.isEmpty()) favorites.addAll(Arrays.asList(saved.split(",")));
        if(state!=null){page=state.getString("page","home");query=state.getString("query","");}
        media=new NowPlayingController(this,this::renderMedia);
        refreshApps(); render();
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addAction(Intent.ACTION_PACKAGE_REPLACED);filter.addDataScheme("package");
        if(Build.VERSION.SDK_INT>=33) registerReceiver(packageChanges,filter,Context.RECEIVER_EXPORTED);
        else registerReceiver(packageChanges,filter);
        receiverRegistered=true;
    }
    @Override protected void onResume(){super.onResume();resumed=true;String old=fingerprint();refreshApps();if(!old.equals(fingerprint())){icons.clear();render();}updateClock();handler.removeCallbacks(tick);handler.post(tick);if(page.equals("media")){media.start();if(player!=null)player.start();}}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);if(player!=null)player.stop();media.stop();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);media.stop();if(receiverRegistered)unregisterReceiver(packageChanges);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("page",page);out.putString("query",query);super.onSaveInstanceState(out);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.hasCategory(Intent.CATEGORY_HOME))navigate("home");}
    @Override public void onBackPressed(){if(page.equals("apps")&&!query.isEmpty()){search.setText("");}else navigate("home");}

    private void refreshApps(){
        PackageManager pm=getPackageManager();Map<String,App> found=new LinkedHashMap<>();
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:pm.queryIntentActivities(intent,0)){
            if(r.activityInfo==null||!r.activityInfo.exported||r.activityInfo.packageName.equals(getPackageName()))continue;
            ComponentName component=new ComponentName(r.activityInfo.packageName,r.activityInfo.name);
            // Keep activity aliases: developer builds and multiple entry points must remain launchable.
            String key=component.flattenToString();if(found.containsKey(key))continue;
            CharSequence label;try{label=r.loadLabel(pm);}catch(RuntimeException e){label=r.activityInfo.packageName;}
            found.put(key,new App(component,label==null?r.activityInfo.packageName:label.toString(),r));
        }
        apps.clear();apps.addAll(found.values());Collator col=Collator.getInstance();apps.sort((a,b)->col.compare(a.label,b.label));
    }
    private String fingerprint(){StringBuilder b=new StringBuilder();for(App a:apps)b.append(a.key()).append(a.label);return b.toString();}
    private void navigate(String next){hideKeyboard();query="";page=next;media.stop();render();if(resumed&&page.equals("media"))media.start();}
    private void render(){
        if(player!=null)player.stop();player=null;clock=null;date=null;grid=null;search=null;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GlitchTheme.Background());setContentView(root);
        int width=getResources().getConfiguration().screenWidthDp;columns=Math.max(3,Math.min(7,(width-24)/88));
        LinearLayout brand=row();brand.setPadding(dp(24),dp(16),dp(20),dp(10));
        LinearLayout wordmark=column();TextView name=text("GLITCH UI",17,WHITE);name.setTypeface(Typeface.create("sans-serif-medium",0));name.setLetterSpacing(.18f);wordmark.addView(name);
        TextView sub=text("POCO EDITION",9,MINT);LinearLayout.LayoutParams subtitle=new LinearLayout.LayoutParams(-2,-2);subtitle.topMargin=dp(4);wordmark.addView(sub,subtitle);
        brand.addView(wordmark,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=button("⚙",this::showSettings);settings.setTextSize(25);settings.setContentDescription("GlitchOS-Einstellungen");brand.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(brand);
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        body=column();body.setPadding(dp(20),dp(8),dp(20),dp(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("home"))renderHome();else if(page.equals("apps"))renderApps();else renderMediaPage();
        LinearLayout nav=row();nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(glass(28));
        addNav(nav,"⌂  Start","home");addNav(nav,"▦  Apps","apps");addNav(nav,"♫  Medien","media");
        LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(-1,dp(58));navParams.setMargins(dp(20),dp(6),dp(20),dp(12));root.addView(nav,navParams);updateClock();
    }
    private void addNav(LinearLayout nav,String label,String destination){TextView tab=button(label,()->navigate(destination));tab.setTextSize(13);tab.setTextColor(page.equals(destination)?0xFF102B32:MUTED);if(page.equals(destination))tab.setBackground(pill(MINT,24));nav.addView(tab,new LinearLayout.LayoutParams(0,-1,1));}
    private void renderHome(){
        LinearLayout hero=column();hero.setPadding(dp(12),dp(14),dp(12),dp(26));
        date=text("",14,MUTED);hero.addView(date);clock=text("",76,WHITE);clock.setSingleLine(true);if(Build.VERSION.SDK_INT>=26)clock.setAutoSizeTextTypeUniformWithConfiguration(32,76,1,android.util.TypedValue.COMPLEX_UNIT_SP);clock.setTypeface(Typeface.create("sans-serif-light",0));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(100));cp.topMargin=dp(6);hero.addView(clock,cp);
        TextView intro=text("Dein POCO. Dein Glitch UI.",15,MUTED);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=dp(8);hero.addView(intro,ip);body.addView(hero);
        TextView find=button("⌕   Apps durchsuchen",()->{navigate("apps");search.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT);});find.setGravity(Gravity.CENTER_VERTICAL);find.setPadding(dp(20),0,dp(20),0);find.setTextColor(MUTED);find.setBackground(glass(22));body.addView(find,new LinearLayout.LayoutParams(-1,dp(56)));
        section("Favoriten","Lange drücken zum Anpassen");
        List<App> pinned=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String key:favorites)for(App a:apps)if((a.key().equals(key)||a.component.getPackageName().equals(key))&&seen.add(a.key())){pinned.add(a);break;}
        if(pinned.isEmpty()){TextView empty=text("Noch keine Favoriten. Halte eine App in der App-Liste gedrückt, um sie hier anzuheften.",14,MUTED);body.addView(empty);}
        else {LinearLayout favGrid=column();body.addView(favGrid);fillGrid(favGrid,pinned);}
        LinearLayout info=row();info.setPadding(dp(18),dp(15),dp(18),dp(15));info.setBackground(glass(22));LinearLayout details=column();details.addView(text("Alles an einem Ort",15,WHITE));TextView summary=text(apps.size()+" App-Einstiege · ohne Empfehlungsfeed",11,MUTED);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(6);details.addView(summary,sp);info.addView(details,new LinearLayout.LayoutParams(0,-2,1));TextView open=button("↗",()->navigate("apps"));open.setTextColor(MINT);info.addView(open,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(-1,-2);card.topMargin=dp(20);body.addView(info,card);
    }
    private void renderApps(){
        TextView title=text("Deine Apps",32,WHITE);title.setTypeface(Typeface.create("sans-serif-light",0));body.addView(title);
        search=new EditText(this);search.setTextSize(16);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Name oder Paket suchen");search.setContentDescription("Apps suchen");search.setSingleLine(true);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);search.setPadding(dp(18),0,dp(14),0);search.setBackground(glass(20));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));p.topMargin=dp(20);p.bottomMargin=dp(16);body.addView(search,p);search.setText(query);
        count=text("",12,MUTED);body.addView(count);grid=column();LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=dp(14);body.addView(grid,gp);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();filterApps();}public void afterTextChanged(Editable e){}});
        search.setOnEditorActionListener((v,action,event)->{hideKeyboard();return true;});filterApps();
    }
    private void filterApps(){if(grid==null)return;String q=query.trim().toLowerCase(Locale.ROOT);List<App> filtered=new ArrayList<>();for(App a:apps)if(a.label.toLowerCase(Locale.ROOT).contains(q)||a.component.getPackageName().toLowerCase(Locale.ROOT).contains(q))filtered.add(a);count.setText(filtered.size()+" von "+apps.size()+" App-Einstiegen");fillGrid(grid,filtered);if(filtered.isEmpty()){TextView empty=text("Keine passende App gefunden.",16,MUTED);empty.setPadding(dp(8),dp(32),dp(8),dp(32));grid.addView(empty);}}
    private void fillGrid(LinearLayout parent,List<App> list){
        parent.removeAllViews();for(int start=0;start<list.size();start+=columns){LinearLayout r=row();r.setGravity(Gravity.TOP);for(int c=0;c<columns;c++){int index=start+c;if(index<list.size())r.addView(appTile(list.get(index)),new LinearLayout.LayoutParams(0,dp(112),1));else r.addView(new View(this),new LinearLayout.LayoutParams(0,dp(112),1));}parent.addView(r,new LinearLayout.LayoutParams(-1,dp(112)));}
    }
    private View appTile(App app){
        LinearLayout tile=column();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(2),dp(6),dp(2),dp(4));tile.setFocusable(true);tile.setClickable(true);
        android.util.TypedValue selectable=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,selectable,true);tile.setBackgroundResource(selectable.resourceId);
        FrameLayout iconBox=new FrameLayout(this);iconBox.setBackground(glass(18));iconBox.setPadding(dp(7),dp(7),dp(7),dp(7));ImageView icon=new ImageView(this);icon.setImageDrawable(icon(app));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);iconBox.addView(icon,new FrameLayout.LayoutParams(-1,-1));tile.addView(iconBox,new LinearLayout.LayoutParams(dp(60),dp(60)));
        TextView label=text(app.label,11,WHITE);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setLineSpacing(dp(1),1);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);tile.addView(label,lp);
        tile.setContentDescription(app.label);tile.setTooltipText(app.component.getPackageName());tile.setOnClickListener(v->launch(app));tile.setOnLongClickListener(v->{appActions(app);return true;});return tile;
    }
    private Drawable icon(App app){try{Drawable.ConstantState cached=icons.get(app.key());if(cached!=null)return GlitchTheme.icon(cached.newDrawable(getResources()).mutate());Drawable d=app.resolve.loadIcon(getPackageManager());if(d!=null&&d.getConstantState()!=null)icons.put(app.key(),d.getConstantState());return GlitchTheme.icon(d);}catch(RuntimeException e){return GlitchTheme.icon(getPackageManager().getDefaultActivityIcon());}}
    private void launch(App app){hideKeyboard();Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);safeStart(i);}
    private boolean isFavorite(App app){return favorites.contains(app.key())||favorites.contains(app.component.getPackageName());}
    private void appActions(App app){
        boolean pinned=isFavorite(app);String[] choices={pinned?"Aus Favoriten entfernen":"Zu Favoriten hinzufügen","App-Informationen","Paketnamen kopieren"};
        new AlertDialog.Builder(this).setTitle(app.label).setItems(choices,(d,which)->{if(which==0){if(pinned){favorites.remove(app.key());favorites.remove(app.component.getPackageName());}else favorites.add(app.key());prefs.edit().putString("favorites",TextUtils.join(",",favorites)).apply();render();}else if(which==1){safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.component.getPackageName())));}else{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Paketname",app.component.getPackageName()));Toast.makeText(this,"Paketname kopiert",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Schließen",null).show();
    }
    private void showSettings(){
        String[] choices={"Glitch-Design anwenden","Xiaomi-Designs","Android-Einstellungen","Entwickleroptionen","Standard-Launcher wählen","Medienzugriff verwalten","Über Glitch UI"};
        new AlertDialog.Builder(this).setTitle("Glitch UI · POCO").setItems(choices,(d,which)->{
            if(which==0)GlitchTheme.apply(this);
            else if(which==1){Intent theme=getPackageManager().getLaunchIntentForPackage("com.android.thememanager");if(theme!=null)safeStart(theme);}
            else if(which==2)safeStart(new Intent(Settings.ACTION_SETTINGS));
            else if(which==3)safeStart(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            else if(which==4)chooseHome();
            else if(which==5)safeStart(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            else new AlertDialog.Builder(this).setTitle("GlitchOS Mobile 1.2.0").setMessage("Glitch UI · POCO Edition\n\nEigene Navy-/Mint-Wallpaper, abgestimmte App-Icons, Suche, Favoriten und Mediensteuerung.\n\nGlitch-Design anwenden setzt Start- und Sperrbildschirm und exportiert ein Xiaomi-Theme (.mtz).\n\nDer echte Xiaomi-App-Switcher bleibt durch den POCO-Systemdienst erhalten. Er wird durch Systemdesign und Wallpaper angepasst. Keine Root- oder SystemUI-Ersetzung.").setPositiveButton("OK",null).show();
        }).setNegativeButton("Schließen",null).show();
    }
    private void chooseHome(){if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME)&&!roles.isRoleHeld(RoleManager.ROLE_HOME)){safeStart(roles.createRequestRoleIntent(RoleManager.ROLE_HOME));return;}}safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));}
    private void renderMediaPage(){
        TextView heading=text("Jetzt läuft",32,WHITE);heading.setTypeface(Typeface.create("sans-serif-light",0));body.addView(heading);
        TextView subtitle=text("Deine Wiedergabe. Direkt erreichbar.",14,MUTED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);p.bottomMargin=dp(20);body.addView(subtitle,p);
        player=new MobileNowPlayingView(this,media,()->navigate("apps"));body.addView(player,new LinearLayout.LayoutParams(-1,-2));if(resumed)player.start();
    }
    private void renderMedia(){if(player!=null&&page.equals("media"))player.render();}
    private void addAction(LinearLayout parent,String label,Runnable action){TextView b=button(label,action);b.setTextColor(MINT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(10);parent.addView(b,p);}
    private void section(String label,String caption){LinearLayout line=row();line.setPadding(dp(4),dp(25),dp(4),dp(16));line.addView(text(label,20,WHITE),new LinearLayout.LayoutParams(0,-2,1));body.addView(line);TextView help=text(caption,11,MUTED);help.setPadding(dp(4),0,0,dp(10));body.addView(help);}
    private void updateClock(){if(clock!=null)clock.setText(new java.text.SimpleDateFormat("HH:mm",Locale.GERMAN).format(new Date()));if(date!=null)date.setText(new java.text.SimpleDateFormat("EEEE, d. MMMM",Locale.GERMAN).format(new Date()));}
    private void safeStart(Intent i){try{startActivity(i);}catch(RuntimeException e){Toast.makeText(this,"Diese Funktion ist nicht verfügbar.",Toast.LENGTH_SHORT).show();}}
    private void hideKeyboard(){View focused=getCurrentFocus();if(focused!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String label,float size,int color){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",0));return t;}
    private TextView button(String label,Runnable action){TextView t=text(label,15,WHITE);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));t.setFocusable(true);t.setClickable(true);android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,value,true);t.setBackgroundResource(value.resourceId);t.setOnClickListener(v->action.run());return t;}
    private Drawable glass(float radius){return new MobileVisuals.GlassSurfaceDrawable(dp(radius),false);}
    private Drawable pill(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private static final class App {final ComponentName component;final String label;final ResolveInfo resolve;App(ComponentName c,String l,ResolveInfo r){component=c;label=l;resolve=r;}String key(){return component.flattenToString();}}
}



