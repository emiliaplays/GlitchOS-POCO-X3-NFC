package com.glitchos.mobile;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Original Glitch UI art and a portable MIUI wallpaper/icon theme; no privileged overlays. */
final class GlitchTheme {
    static final int MINT=0xFF8BFFE0;
    static void paint(Canvas c,float w,float h){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF071A22,0xFF102D3B,0xFF0B1428},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
        p.setShader(new RadialGradient(w*.15f,h*.13f,w*.9f,new int[]{0x554CBEA9,0x002C857F},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
        p.setShader(new RadialGradient(w*.97f,h*.62f,w*.75f,new int[]{0x403865AE,0x003865AE},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
        Path ribbon=new Path();ribbon.moveTo(-w*.2f,h*.69f);ribbon.cubicTo(w*.35f,h*.12f,w*.57f,h*.88f,w*1.22f,h*.25f);ribbon.lineTo(w*1.22f,h*.39f);ribbon.cubicTo(w*.61f,h*.99f,w*.29f,h*.34f,-w*.2f,h*.84f);ribbon.close();
        p.setShader(new LinearGradient(0,h*.3f,w,h*.8f,new int[]{0x258BFFE0,0x0A8BFFE0,0x1E729FFF},null,Shader.TileMode.CLAMP));c.drawPath(ribbon,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w/900));p.setColor(0x168BFFE0);
        for(int i=0;i<3;i++){Path line=new Path();line.moveTo(-w*.1f,h*(.72f+i*.025f));line.cubicTo(w*.29f,h*.25f,w*.62f,h*.92f,w*1.1f,h*(.35f+i*.025f));c.drawPath(line,p);}
    }
    static Bitmap wallpaper(){Bitmap b=Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888);paint(new Canvas(b),1080,2400);return b;}
    static final class Background extends Drawable {
        public void draw(Canvas c){paint(c,getBounds().width(),getBounds().height());}
        public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    static Drawable icon(Drawable original){return new Drawable(){
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        public void draw(Canvas c){Rect b=getBounds();float radius=b.width()*.25f;p.setColor(0xFF183943);c.drawRoundRect(new RectF(b),radius,radius,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,b.width()/160f));p.setColor(0x778BFFE0);c.drawRoundRect(new RectF(b.left+1,b.top+1,b.right-1,b.bottom-1),radius,radius,p);p.setStyle(Paint.Style.FILL);int inset=Math.round(b.width()*.16f);original.setBounds(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);original.draw(c);}
        public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
    private static byte[] png(Bitmap b){ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);return out.toByteArray();}
    private static void entry(ZipOutputStream zip,String name,byte[] bytes)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();}
    static void apply(Activity activity){
        Toast.makeText(activity,"Glitch-Design wird erstellt …",Toast.LENGTH_SHORT).show();
        new Thread(()->{try{
            File dir=new File(activity.getExternalFilesDir(null),"theme");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Theme folder unavailable");
            Bitmap wall=wallpaper();byte[] wallpaper=png(wall);
            File image=new File(dir,"GlitchUI-Wallpaper.png");try(FileOutputStream out=new FileOutputStream(image)){out.write(wallpaper);}
            ByteArrayOutputStream iconBytes=new ByteArrayOutputStream();Set<String> found=new HashSet<>();
            try(ZipOutputStream icons=new ZipOutputStream(iconBytes)){
                List<ResolveInfo> list=activity.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
                for(ResolveInfo info:list){String pkg=info.activityInfo.packageName;if(!found.add(pkg))continue;Drawable source;try{source=info.loadIcon(activity.getPackageManager());}catch(RuntimeException e){continue;}Bitmap b=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Drawable themed=icon(source);themed.setBounds(0,0,192,192);themed.draw(new Canvas(b));byte[] bytes=png(b);entry(icons,pkg+".png",bytes);entry(icons,"res/drawable-xxhdpi/"+pkg+".png",bytes);b.recycle();}
            }
            String xml="<?xml version=\"1.0\" encoding=\"UTF-8\"?><MIUI-Theme><title>Glitch UI</title><designer>glitchbyemilia</designer><author>Emilia</author><version>1.2.0</version><uiVersion>14</uiVersion><description>Glitch UI: Navy, Mint, eigene Wallpaper, App-Icons und System-Farbmodule für POCO X3 NFC.</description></MIUI-Theme>";
            try(ZipOutputStream theme=new ZipOutputStream(new FileOutputStream(new File(dir,"GlitchUI-Poco.mtz")))){
                entry(theme,"description.xml",xml.getBytes(StandardCharsets.UTF_8));entry(theme,"icons",iconBytes.toByteArray());entry(theme,"wallpaper/default_wallpaper.jpg",jpeg(wall));entry(theme,"wallpaper/default_lock_wallpaper.jpg",jpeg(wall));
                try(ZipInputStream modules=new ZipInputStream(activity.getAssets().open("theme-modules.zip"))){
                    ZipEntry module;byte[] buffer=new byte[8192];
                    while((module=modules.getNextEntry())!=null){
                        if(module.isDirectory())continue;
                        theme.putNextEntry(new ZipEntry(module.getName()));int count;
                        while((count=modules.read(buffer))!=-1)theme.write(buffer,0,count);
                        theme.closeEntry();modules.closeEntry();
                    }
                }
            }
            WallpaperManager manager=WallpaperManager.getInstance(activity);manager.setBitmap(wall,null,true,WallpaperManager.FLAG_SYSTEM|WallpaperManager.FLAG_LOCK);wall.recycle();
            new Handler(Looper.getMainLooper()).post(()->Toast.makeText(activity,"Glitch UI auf Start- und Sperrbildschirm angewendet. Xiaomi-Theme exportiert.",Toast.LENGTH_LONG).show());
        }catch(Exception e){new Handler(Looper.getMainLooper()).post(()->Toast.makeText(activity,"Design konnte nicht vollständig angewendet werden: "+e.getMessage(),Toast.LENGTH_LONG).show());}},"GlitchTheme").start();
    }
    private static byte[] jpeg(Bitmap b){ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,94,out);return out.toByteArray();}
}
