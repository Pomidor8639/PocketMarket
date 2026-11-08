package com.pocketmarket.client.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.pocketmarket.client.R;

public class FontUtils {

    private static Typeface minecraftTypeface;

    public static Typeface getMinecraftFont(Context context) {
        if (minecraftTypeface == null) {
            try {
                minecraftTypeface = ResourcesCompat.getFont(context, R.font.minecraft);
            } catch (Exception e) {
                try {
                    minecraftTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/minecraft.otf");
                } catch (Exception ignored) {
                    minecraftTypeface = Typeface.MONOSPACE;
                }
            }
        }
        return minecraftTypeface;
    }

    public static void applyMinecraftFont(View view) {
        if (view == null) return;

        Typeface tf = getMinecraftFont(view.getContext());

        if (view instanceof TextView) {
            ((TextView) view).setTypeface(tf);
        } else if (view instanceof Button) {
            ((Button) view).setTypeface(tf);
        } else if (view instanceof EditText) {
            ((EditText) view).setTypeface(tf);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyMinecraftFont(vg.getChildAt(i));
            }
        }
    }
}
