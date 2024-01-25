package kocot.klass.auxiliary;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import kocot.klass.R;

public final class Toaster {

    private static final int cooldown = 3500;
    private static int last = 0;
    public static void show(Context context, String text){

        int current = (int) System.currentTimeMillis();
        current = Math.abs(current);
        if(Math.abs(current - last) < cooldown)
            return;

        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_toast,null);
        TextView textView = view.findViewById(R.id.textViewToast);
        textView.setText(text);
        toast.setView(view);
        toast.setGravity(Gravity.TOP,0,0);



        toast.show();

        last = (int) System.currentTimeMillis();
        last = Math.abs(last);

    }

    public static void show(Context context, int resId){

        show(context,context.getString(resId));

    }

}
