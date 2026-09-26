package com.smartcalculator.ai

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MatrixActivity:AppCompatActivity(){
    private val bg=Color.rgb(7,11,20);private val card=Color.rgb(17,26,43);private val blue=Color.rgb(36,107,253)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun p(h:Int=ViewGroup.LayoutParams.WRAP_CONTENT)=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,if(h==ViewGroup.LayoutParams.WRAP_CONTENT)h else dp(h)).apply{topMargin=dp(10)}
    private fun e(h:String)=EditText(this).apply{hint=h;setTextColor(Color.WHITE);setHintTextColor(Color.rgb(145,160,184));textSize=16f;setSingleLine(true);setBackgroundColor(card);setPadding(dp(12),0,dp(12),0)}
    private fun out(s:String)=TextView(this).apply{text=s;setTextColor(Color.WHITE);textSize=16f;setPadding(dp(16),dp(16),dp(16),dp(16));setBackgroundColor(card)}
    override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(20),dp(20),dp(30));setBackgroundColor(bg)};setContentView(ScrollView(this).apply{addView(root)})
        root.addView(Button(this).apply{text="← Назад";setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},p(52))
        root.addView(TextView(this).apply{text="🔢 Матрица 2×2";setTextColor(Color.WHITE);textSize=27f})
        root.addView(TextView(this).apply{text="Определитель и обратная матрица.";setTextColor(Color.rgb(180,195,215));textSize=14f})
        val row1=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val a=e("a");val bb=e("b");row1.addView(a,LinearLayout.LayoutParams(0,dp(58),1f));row1.addView(bb,LinearLayout.LayoutParams(0,dp(58),1f));root.addView(row1,p())
        val row2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val c=e("c");val d=e("d");row2.addView(c,LinearLayout.LayoutParams(0,dp(58),1f));row2.addView(d,LinearLayout.LayoutParams(0,dp(58),1f));root.addView(row2,p())
        val calc=Button(this).apply{text="Рассчитать";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(calc,p(58));val result=out("Введите 4 числа.");root.addView(result,p())
        calc.setOnClickListener{val av=a.v();val bv=bb.v();val cv=c.v();val dv=d.v();if(av==null||bv==null||cv==null||dv==null){result.text="Ошибка: заполните все поля."}else{val det=av*dv-bv*cv;if(kotlin.math.abs(det)<1e-10){result.text="Определитель: 0\nОбратной матрицы нет."}else{result.text="Определитель: ${fmt(det)}\nОбратная матрица:\n[ @@{fmt(dv/det)}   @@{fmt(-bv/det)} ]\n[ @@{fmt(-cv/det)}   @@{fmt(av/det)} ]"}}}
    }
    private fun EditText.v()=text.toString().trim().replace(',','.').toDoubleOrNull()
    private fun fmt(v:Double)=if(v.isFinite())"%.4f".format(java.util.Locale.US,v).trimEnd('0').trimEnd('.') else "—"
}
