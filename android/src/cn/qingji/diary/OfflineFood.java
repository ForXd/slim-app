package cn.qingji.diary;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OfflineFood {
    private final Context context;
    private final List<String> labels=new ArrayList<>();
    private Interpreter model;
    OfflineFood(Context context){this.context=context.getApplicationContext();}

    private void load() throws Exception {
        if(model!=null)return;
        AssetManager assets=context.getAssets();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(assets.open("labels.txt"),"UTF-8"))){
            labels.clear();
            String line;
            while((line=reader.readLine())!=null)labels.add(line.trim());
        }
        // The asset length is fixed by the packaged model, not by the input photo.
        try(InputStream stream=assets.open("food.tflite")){
            ByteBuffer buffer=ByteBuffer.allocateDirect(stream.available()).order(ByteOrder.nativeOrder());
            byte[] chunk=new byte[65536];int n;
            while((n=stream.read(chunk))!=-1)buffer.put(chunk,0,n);
            buffer.rewind();
            model=new Interpreter(buffer,new Interpreter.Options().setNumThreads(2));
        }
    }
    synchronized JSONArray predict(Bitmap image) throws Exception {
        load();
        int size=model.getInputTensor(0).shape()[1];
        Bitmap scaled=Bitmap.createScaledBitmap(image,size,size,true);
        int[] pixels=new int[size*size];
        scaled.getPixels(pixels,0,size,0,0,size,size);
        if(scaled!=image)scaled.recycle();

        ByteBuffer input=ByteBuffer.allocateDirect(pixels.length*3);
        for(int p:pixels)input.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p);
        input.rewind();
        byte[][] output=new byte[1][labels.size()];
        model.run(input,output);
        List<Integer> ranks=new ArrayList<>();
        for(int i=1;i<labels.size();i++)ranks.add(i);
        ranks.sort((a,b)->Integer.compare(output[0][b]&255,output[0][a]&255));
        JSONArray results=new JSONArray();
        int background=output[0][0]&255;
        for(int i:ranks){
            int score=output[0][i]&255;
            if(results.length()==3||score==0||score<background)break;
            results.put(new JSONObject().put("name",labels.get(i)).put("score",score/256.0));
        }
        return results;
    }
    synchronized void close(){if(model!=null){model.close();model=null;}}
}
