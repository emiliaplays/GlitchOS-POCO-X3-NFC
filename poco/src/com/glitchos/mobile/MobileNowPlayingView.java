package com.glitchos.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;
import java.util.Locale;

/** Mobile media surface: update progress in place so callbacks never interrupt scrubbing. */
final class MobileNowPlayingView extends LinearLayout {
    private static final int WHITE=0xFFF9FAFC, MUTED=0xFFBDD0DD, MINT=0xFF8BFFE0;
    private final Activity activity;
    private final NowPlayingController media;
    private final Runnable browseApps;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout activeCard, emptyCard;
    private TextView source,title,artist,state,elapsed,duration,play,previous,next,back,forward;
    private ImageView artwork,sourceIcon;
    private SeekBar seek;
    private boolean running,scrubbing;
    private MediaSession.Token displayedToken, scrubbingToken;
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;updateProgress();handler.postDelayed(this,500);}};

    MobileNowPlayingView(Activity activity,NowPlayingController media,Runnable browseApps){
        super(activity);this.activity=activity;this.media=media;this.browseApps=browseApps;
        setOrientation(VERTICAL);setPadding(dp(18),dp(18),dp(18),dp(18));
        setBackground(new MobileVisuals.GlassSurfaceDrawable(dp(26),false));buildActive();render();
    }
    void start(){running=true;handler.removeCallbacks(tick);handler.post(tick);}
    void stop(){running=false;handler.removeCallbacksAndMessages(null);scrubbing=false;}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    private void buildActive(){
        activeCard=column();addView(activeCard,new LayoutParams(-1,-2));
        LinearLayout appRow=row();sourceIcon=new ImageView(activity);sourceIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);appRow.addView(sourceIcon,new LayoutParams(dp(24),dp(24)));
        source=text("",12,MUTED);source.setSingleLine(true);source.setEllipsize(TextUtils.TruncateAt.END);LayoutParams sourceParams=new LayoutParams(0,-2,1);sourceParams.leftMargin=dp(10);appRow.addView(source,sourceParams);
        TextView choose=button("⌄",this::choosePlayer);choose.setTextSize(24);choose.setContentDescription("Player wählen");appRow.addView(choose,new LayoutParams(dp(48),dp(48)));activeCard.addView(appRow);
        FrameLayout artFrame=new FrameLayout(activity);GradientDrawable artBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF285665,0xFF293952});artBg.setCornerRadius(dp(20));artFrame.setBackground(artBg);artFrame.setClipToOutline(true);
        artwork=new ImageView(activity);artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);artwork.setPadding(dp(10),dp(10),dp(10),dp(10));artwork.setContentDescription("Cover");artFrame.addView(artwork,new FrameLayout.LayoutParams(-1,-1));LayoutParams artParams=new LayoutParams(-1,dp(156));artParams.topMargin=dp(8);artParams.bottomMargin=dp(16);activeCard.addView(artFrame,artParams);
        title=text("Wiedergabe",20,WHITE);title.setTypeface(Typeface.create("sans-serif-medium",0));title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);activeCard.addView(title);
        artist=text("",13,MUTED);artist.setMaxLines(2);artist.setPadding(0,dp(6),0,dp(8));activeCard.addView(artist);
        state=text("",10,MINT);state.setLetterSpacing(.10f);activeCard.addView(state);
        seek=new SeekBar(activity);seek.setMax(1000);seek.setContentDescription("Wiedergabeposition");seek.setProgressTintList(ColorStateList.valueOf(MINT));seek.setThumbTintList(ColorStateList.valueOf(MINT));seek.setProgressBackgroundTintList(ColorStateList.valueOf(0x447EA4B4));activeCard.addView(seek,new LayoutParams(-1,dp(48)));
        LinearLayout times=row();elapsed=text("--:--",11,MUTED);duration=text("--:--",11,MUTED);duration.setGravity(Gravity.END);times.addView(elapsed,new LayoutParams(0,-2,1));times.addView(duration,new LayoutParams(0,-2,1));activeCard.addView(times);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){scrubbing=true;scrubbingToken=media.getSelected()==null?null:media.getSelected().getSessionToken();}
            public void onProgressChanged(SeekBar bar,int value,boolean user){if(user)elapsed.setText(format((long)(media.getDurationMs()*(value/1000d))));}
            public void onStopTrackingTouch(SeekBar bar){boolean same=media.getSelected()!=null&&media.getSelected().getSessionToken().equals(scrubbingToken);if(same&&!media.seekTo((long)(media.getDurationMs()*(bar.getProgress()/1000d))))Toast.makeText(activity,"Spulen ist momentan nicht möglich",Toast.LENGTH_SHORT).show();scrubbing=false;scrubbingToken=null;updateProgress();}
        });
        LinearLayout controls=row();previous=button("⏮",()->command(media::previous));previous.setContentDescription("Vorheriger Titel");back=button("−10",()->command(()->media.seekBy(-10000)));back.setContentDescription("10 Sekunden zurück");play=button("▶",()->command(media::togglePlayPause));play.setTextColor(0xFF102B32);play.setBackground(round(MINT,28));play.setTextSize(24);forward=button("+10",()->command(()->media.seekBy(10000)));forward.setContentDescription("10 Sekunden vor");next=button("⏭",()->command(media::next));next.setContentDescription("Nächster Titel");
        for(TextView b:new TextView[]{previous,back,play,forward,next}){LayoutParams p=new LayoutParams(0,dp(56),b==play?1.3f:1);p.setMargins(dp(2),0,dp(2),0);controls.addView(b,p);}
        LayoutParams controlsParams=new LayoutParams(-1,dp(56));controlsParams.topMargin=dp(16);controlsParams.bottomMargin=dp(12);activeCard.addView(controls,controlsParams);
        TextView open=button("Medien-App öffnen  ↗",this::openPlayer);open.setTextColor(MINT);activeCard.addView(open,new LayoutParams(-1,dp(48)));
        emptyCard=column();addView(emptyCard,new LayoutParams(-1,-2));
    }
    void render(){
        if(!media.hasAccess()){showEmpty("Mediensteuerung aktivieren","Optional: Android benötigt Benachrichtigungszugriff, um Medien anderer Apps zu steuern. GlitchOS speichert keine Benachrichtigungen.","Zugriff in Android verwalten",()->launch(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));return;}
        MediaController selected=media.getSelected();if(selected==null){displayedToken=null;artwork.setImageDrawable(null);sourceIcon.setImageDrawable(null);showEmpty("Gerade keine Wiedergabe","Starte Musik oder ein Video in einer App mit Android-Mediensteuerung.","Apps öffnen",browseApps);return;}
        try{
            activeCard.setVisibility(VISIBLE);emptyCard.setVisibility(GONE);
            if(!selected.getSessionToken().equals(displayedToken)){scrubbing=false;displayedToken=selected.getSessionToken();}
            String appName=label(selected.getPackageName());source.setText(appName);
            android.graphics.drawable.Drawable appIcon;try{appIcon=activity.getPackageManager().getApplicationIcon(selected.getPackageName());}catch(PackageManager.NameNotFoundException e){appIcon=activity.getPackageManager().getDefaultActivityIcon();}sourceIcon.setImageDrawable(appIcon);
            MediaMetadata metadata=selected.getMetadata();CharSequence track=null,performer=null;Bitmap cover=null;
            if(metadata!=null){track=metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);if(TextUtils.isEmpty(track))track=metadata.getText(MediaMetadata.METADATA_KEY_TITLE);performer=metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);if(TextUtils.isEmpty(performer))performer=metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);cover=metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);if(cover==null)cover=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(cover==null)cover=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);}
            title.setText(TextUtils.isEmpty(track)?"Wiedergabe":track);artist.setText(performer);artist.setVisibility(TextUtils.isEmpty(performer)?GONE:VISIBLE);
            if(cover!=null){artwork.setImageBitmap(cover);artwork.setPadding(0,0,0,0);artwork.setContentDescription("Cover von "+title.getText());}
            else {artwork.setImageDrawable(appIcon);artwork.setPadding(dp(44),dp(28),dp(44),dp(28));artwork.setContentDescription(appName+" · kein Cover vom Player bereitgestellt");}
            PlaybackState playback=selected.getPlaybackState();int status=playback==null?PlaybackState.STATE_NONE:playback.getState();boolean busy=status==PlaybackState.STATE_BUFFERING||status==PlaybackState.STATE_CONNECTING;
            state.setText(busy?"WIRD GELADEN":media.isPlaying()?"WIEDERGABE":"PAUSIERT");play.setText(media.isPlaying()?"Ⅱ":"▶");play.setContentDescription(media.isPlaying()?"Pausieren":"Wiedergeben");
            enable(play,media.canPlayPause());enable(previous,media.canPrevious());enable(next,media.canNext());enable(back,media.canSeek());enable(forward,media.canSeek());updateProgress();
        }catch(RuntimeException unavailable){showEmpty("Player momentan nicht verfügbar","Die Mediensitzung wurde beendet. Starte die Wiedergabe erneut.","Apps öffnen",browseApps);}
    }
    private void updateProgress(){
        if(activeCard.getVisibility()!=VISIBLE||scrubbing)return;long position=media.getPositionMs(),length=media.getDurationMs();String time=format(position),total=length>0?format(length):"LIVE / --:--";if(!TextUtils.equals(elapsed.getText(),time))elapsed.setText(time);if(!TextUtils.equals(duration.getText(),total))duration.setText(total);seek.setEnabled(media.canSeek()&&length>0);seek.setAlpha(seek.isEnabled()?1:.45f);int progress=position>=0&&length>0?(int)Math.min(1000,1000d*position/length):0;if(seek.getProgress()!=progress)seek.setProgress(progress);
    }
    private void showEmpty(String heading,String detail,String action,Runnable click){activeCard.setVisibility(GONE);emptyCard.setVisibility(VISIBLE);emptyCard.removeAllViews();TextView h=text(heading,20,WHITE);emptyCard.addView(h);TextView d=text(detail,14,MUTED);d.setPadding(0,dp(14),0,dp(20));emptyCard.addView(d);TextView b=button(action,click);b.setTextColor(MINT);emptyCard.addView(b,new LayoutParams(-1,dp(52)));}
    private void choosePlayer(){List<MediaController> sessions=media.getSessions();String[] labels=new String[sessions.size()];int checked=-1;for(int i=0;i<labels.length;i++){labels[i]=label(sessions.get(i).getPackageName());if(sessions.get(i).getSessionToken().equals(displayedToken))checked=i;}new AlertDialog.Builder(activity).setTitle("Player wählen").setSingleChoiceItems(labels,checked,(dialog,i)->{media.select(sessions.get(i).getSessionToken());dialog.dismiss();}).setNegativeButton("Schließen",null).show();}
    private void openPlayer(){MediaController c=media.getSelected();if(c==null)return;try{android.app.PendingIntent sessionIntent=c.getSessionActivity();if(sessionIntent!=null){sessionIntent.send();return;}}catch(android.app.PendingIntent.CanceledException unavailable){}Intent i=activity.getPackageManager().getLaunchIntentForPackage(c.getPackageName());if(i!=null)launch(i);}
    private void launch(Intent intent){try{activity.startActivity(intent);}catch(RuntimeException unavailable){Toast.makeText(activity,"Diese Funktion ist nicht verfügbar",Toast.LENGTH_SHORT).show();}}
    private interface Command{boolean run();}
    private void command(Command action){if(!action.run())Toast.makeText(activity,"Der Player unterstützt diese Aktion momentan nicht",Toast.LENGTH_SHORT).show();handler.postDelayed(this::render,200);}
    private String label(String pkg){try{return activity.getPackageManager().getApplicationLabel(activity.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(PackageManager.NameNotFoundException e){return "Medienplayer";}}
    private String format(long ms){if(ms<0)return "--:--";long s=ms/1000;return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,(s/60)%60,s%60):String.format(Locale.ROOT,"%d:%02d",s/60,s%60);}
    private void enable(View v,boolean enabled){v.setEnabled(enabled);v.setAlpha(enabled?1:.30f);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(activity);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String value,float size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);return t;}
    private TextView button(String value,Runnable click){TextView b=text(value,14,WHITE);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setFocusable(true);b.setClickable(true);android.util.TypedValue v=new android.util.TypedValue();activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,v,true);b.setBackgroundResource(v.resourceId);b.setOnClickListener(w->click.run());return b;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
